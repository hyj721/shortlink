package com.uestc.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestc.shortlink.admin.common.constant.RedisCacheConstant;
import com.uestc.shortlink.admin.common.convention.exception.ClientException;
import com.uestc.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.uestc.shortlink.admin.dao.entity.UserDO;
import com.uestc.shortlink.admin.dao.mapper.UserMapper;
import com.uestc.shortlink.admin.dto.req.UserLoginReqDTO;
import com.uestc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.uestc.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.uestc.shortlink.admin.dto.res.UserLoginRespDTO;
import com.uestc.shortlink.admin.dto.res.UserRespDTO;
import com.uestc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class).eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        return UserRespDTO.builder()
                .id(userDO.getId())
                .username(userDO.getUsername())
                .realName(userDO.getRealName())
                .phone(userDO.getPhone())
                .mail(userDO.getMail())
                .build();
    }

    @Override
    public Boolean hasUsername(String username) {
        return userRegisterBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (hasUsername(requestParam.getUsername())) {
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(RedisCacheConstant.LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        // 如果没有获取到锁，直接返回，不等待
        if (!lock.tryLock()) {
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }

        try {
            int insert = baseMapper.insert(UserDO.builder()
                    .username(requestParam.getUsername())
                    .password(requestParam.getPassword())
                    .realName(requestParam.getRealName())
                    .phone(requestParam.getPhone())
                    .mail(requestParam.getMail())
                    .build()
            );
            if (insert != 1) {
                throw new ClientException(UserErrorCodeEnum.USER_SAVE_ERROR);
            }
            userRegisterBloomFilter.add(requestParam.getUsername());
        } catch (DuplicateKeyException e) {
            throw new ClientException(UserErrorCodeEnum.USER_EXIST);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateUser(UserUpdateReqDTO requestParam) {
        // TODO 验证当前用户是否为登录用户
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        UserDO userDO = baseMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag, 0));
        if (userDO == null) {
            throw new ClientException("用户不存在");
        }
        /**
         * Hash
         * Key：login_用户名
         * Value：
         *  Key：token标识
         *  Val：JSON 字符串（用户信息）
         */
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(RedisCacheConstant.USER_LOGIN_KEY + userDO.getUsername());
        // 如果已经登录，则刷新token剩余时间并返回原token
        if (CollUtil.isNotEmpty(entries)) {
            stringRedisTemplate.expire(RedisCacheConstant.USER_LOGIN_KEY + userDO.getUsername(), 30, TimeUnit.MINUTES);
            String token = (String) entries.keySet().iterator().next();
            return new UserLoginRespDTO(token);
        }
        String uuid = UUID.randomUUID().toString();
        try {
            stringRedisTemplate.opsForHash().put(RedisCacheConstant.USER_LOGIN_KEY + userDO.getUsername(), uuid, objectMapper.writeValueAsString(userDO));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        stringRedisTemplate.expire(RedisCacheConstant.USER_LOGIN_KEY + userDO.getUsername(), 30, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get(RedisCacheConstant.USER_LOGIN_KEY + username, token) != null;
    }
}

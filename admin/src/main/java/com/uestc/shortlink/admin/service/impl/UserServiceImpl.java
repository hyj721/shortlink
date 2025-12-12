package com.uestc.shortlink.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestc.shortlink.admin.common.biz.user.UserContext;
import com.uestc.shortlink.admin.common.biz.user.UserInfoDTO;
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
import com.uestc.shortlink.admin.service.GroupService;
import com.uestc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;
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
            // 注册成功后，创建默认分组
            groupService.saveGroup(requestParam.getUsername(), "默认分组");
        } catch (DuplicateKeyException e) {
            throw new ClientException(UserErrorCodeEnum.USER_EXIST);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateUser(UserUpdateReqDTO requestParam) {
        if (!requestParam.getUsername().equals(UserContext.getUsername())) {
            throw new ClientException("当前登录用户修改请求异常");
        }
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
        StpUtil.login(userDO.getUsername());
        UserInfoDTO userInfo = UserInfoDTO.builder()
                .userId(userDO.getId())
                .username(userDO.getUsername())
                .realName(userDO.getRealName())
                .build();
        StpUtil.getSession().set("userInfo", userInfo);
        return new UserLoginRespDTO(StpUtil.getTokenValue());
    }

    @Override
    public Boolean checkLogin() {
        return StpUtil.isLogin();
    }

    @Override
    public void logout() {
        // 先检查用户是否登录
        if (!checkLogin()) {
            throw new ClientException("用户Token不存在或未登录");
        }
        StpUtil.logout();
    }
}

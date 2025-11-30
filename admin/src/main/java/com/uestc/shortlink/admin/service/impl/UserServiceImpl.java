package com.uestc.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.admin.common.constant.RedisCacheConstant;
import com.uestc.shortlink.admin.common.convention.exception.ClientException;
import com.uestc.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.uestc.shortlink.admin.dao.entity.UserDO;
import com.uestc.shortlink.admin.dao.mapper.UserMapper;
import com.uestc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.uestc.shortlink.admin.dto.res.UserRespDTO;
import com.uestc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterBloomFilter;
    private final RedissonClient redissonClient;

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
}

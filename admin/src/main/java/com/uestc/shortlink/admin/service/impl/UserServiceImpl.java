package com.uestc.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.admin.common.convention.exception.ClientException;
import com.uestc.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.uestc.shortlink.admin.dao.entity.UserDO;
import com.uestc.shortlink.admin.dao.mapper.UserMapper;
import com.uestc.shortlink.admin.dto.res.UserRespDTO;
import com.uestc.shortlink.admin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

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
}

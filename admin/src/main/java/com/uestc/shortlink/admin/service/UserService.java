package com.uestc.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uestc.shortlink.admin.dao.entity.UserDO;
import com.uestc.shortlink.admin.dto.req.UserLoginReqDTO;
import com.uestc.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.uestc.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.uestc.shortlink.admin.dto.res.UserLoginRespDTO;
import com.uestc.shortlink.admin.dto.res.UserRespDTO;

public interface UserService extends IService<UserDO> {
    UserRespDTO getUserByUsername(String username);

    Boolean hasUsername(String username);

    void register(UserRegisterReqDTO requestParam);

    void updateUser(UserUpdateReqDTO requestParam);

    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    Boolean checkLogin();

    void logout();
}

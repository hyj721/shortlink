package com.uestc.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uestc.shortlink.admin.dao.entity.UserDO;
import com.uestc.shortlink.admin.dto.res.UserRespDTO;

public interface UserService extends IService<UserDO> {
    UserRespDTO getUserByUsername(String username);
}

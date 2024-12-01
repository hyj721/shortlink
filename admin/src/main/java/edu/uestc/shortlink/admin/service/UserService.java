package edu.uestc.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.uestc.shortlink.admin.dao.entity.UserDO;
import edu.uestc.shortlink.admin.dto.req.UserRegisterReqDTO;
import edu.uestc.shortlink.admin.dto.resp.UserRespDTO;

public interface UserService extends IService<UserDO> {
    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 用户返回实体
     */
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    Boolean hasUserName(String username);

    /**
     * 注册用户
     * @param requestParam 注册用户请求参数
     */
    void register(UserRegisterReqDTO requestParam);
}

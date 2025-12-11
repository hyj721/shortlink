package com.uestc.shortlink.admin.common.biz.user;

import cn.dev33.satoken.stp.StpUtil;
import com.uestc.shortlink.admin.common.convention.exception.ClientException;

/**
 * 用户上下文
 */
public final class UserContext {

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     * @throws ClientException 未登录或用户信息不存在时抛出
     */
    public static UserInfoDTO getUserInfo() {
        UserInfoDTO userInfo = (UserInfoDTO) StpUtil.getSession().get("userInfo");
        if (userInfo == null) {
            throw new ClientException("用户未登录或登录已过期");
        }
        return userInfo;
    }

    /**
     * 获取上下文中用户 ID
     *
     * @return 用户 ID
     */
    public static Long getUserId() {
        return getUserInfo().getUserId();
    }

    /**
     * 获取上下文中用户名称
     *
     * @return 用户名称
     */
    public static String getUsername() {
        return getUserInfo().getUsername();
    }

    /**
     * 获取上下文中用户真实姓名
     *
     * @return 用户真实姓名
     */
    public static String getRealName() {
        return getUserInfo().getRealName();
    }
}
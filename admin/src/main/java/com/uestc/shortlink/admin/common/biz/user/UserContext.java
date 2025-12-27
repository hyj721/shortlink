package com.uestc.shortlink.admin.common.biz.user;

import com.uestc.shortlink.admin.common.convention.exception.ClientException;

/**
 * 用户上下文
 * 基于 ThreadLocal 存储当前请求的用户信息
 */
public final class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置用户信息到当前线程
     */
    public static void setUser(UserInfoDTO userInfo) {
        USER_THREAD_LOCAL.set(userInfo);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     * @throws ClientException 未登录或用户信息不存在时抛出
     */
    public static UserInfoDTO getUserInfo() {
        UserInfoDTO userInfo = USER_THREAD_LOCAL.get();
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

    /**
     * 清除当前线程的用户信息
     * 必须在请求完成后调用，防止内存泄漏
     */
    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
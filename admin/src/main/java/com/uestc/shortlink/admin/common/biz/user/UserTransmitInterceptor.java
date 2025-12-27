package com.uestc.shortlink.admin.common.biz.user;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户信息传递拦截器
 * 从 Sa-Token Session 中获取用户信息，存入 UserContext (ThreadLocal)
 */
@Slf4j
@Component
public class UserTransmitInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从 Sa-Token Session 获取用户信息
        try {
            if (StpUtil.isLogin()) {
                Object userInfoObj = StpUtil.getSession().get("userInfo");
                if (userInfoObj instanceof UserInfoDTO userInfo) {
                    UserContext.setUser(userInfo);
                    log.debug("用户信息已存入 UserContext: userId={}, username={}", 
                            userInfo.getUserId(), userInfo.getUsername());
                }
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败: {}", e.getMessage());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清理 ThreadLocal，防止内存泄漏
        UserContext.removeUser();
    }
}

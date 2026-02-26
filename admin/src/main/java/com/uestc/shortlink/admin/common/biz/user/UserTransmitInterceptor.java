package com.uestc.shortlink.admin.common.biz.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户信息传递拦截器
 * 从网关透传请求头中获取用户信息，存入 UserContext (ThreadLocal)
 */
@Slf4j
@Component
public class UserTransmitInterceptor implements HandlerInterceptor {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_REAL_NAME = "X-Real-Name";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从 Gateway 注入的请求头获取用户上下文
        try {
            String userIdHeader = request.getHeader(HEADER_USER_ID);
            String username = request.getHeader(HEADER_USERNAME);
            String realName = request.getHeader(HEADER_REAL_NAME);
            if (StringUtils.hasText(username)) {
                Long userId = StringUtils.hasText(userIdHeader) ? Long.parseLong(userIdHeader) : null;
                UserInfoDTO userInfo = UserInfoDTO.builder()
                        .userId(userId)
                        .username(username)
                        .realName(realName)
                        .build();
                UserContext.setUser(userInfo);
                log.debug("User context propagated: userId={}, username={}", userId, username);
            }
        } catch (Exception e) {
            log.debug("Failed to propagate user context: {}", e.getMessage());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清理 ThreadLocal，防止内存泄漏
        UserContext.removeUser();
    }
}

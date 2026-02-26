package com.uestc.shortlink.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.uestc.shortlink.gateway.dto.GatewayErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Sa-Token 配置类
 * 在 SaReactorFilter 中只做 Token 验证，用户信息由下游服务自行获取
 */
@Slf4j
@Configuration
public class SaTokenConfiguration {

    public static final String ATTR_USER_ID = "gateway.auth.userId";
    public static final String ATTR_USERNAME = "gateway.auth.username";
    public static final String ATTR_REAL_NAME = "gateway.auth.realName";

    /**
     * 白名单路径
     */
    private static final List<String> WHITE_PATH_LIST = List.of(
            "/api/short-link/admin/v1/user/login",
            "/api/short-link/admin/v1/user/has-username"
    );

    /**
     * 注册 Sa-Token 全局过滤器
     */
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 拦截所有路由
                .addInclude("/**")
                // 认证函数
                .setAuth(obj -> {
                    String path = SaHolder.getRequest().getRequestPath();
                    String method = SaHolder.getRequest().getMethod();

                    // 1. 白名单路径直接放行
                    if (isWhitelist(path)) {
                        log.info("Whitelist path passed: {}", path);
                        SaRouter.stop();
                        return;
                    }

                    // 2. POST /api/short-link/admin/v1/user 是注册接口，放行
                    if ("POST".equalsIgnoreCase(method) && "/api/short-link/admin/v1/user".equals(path)) {
                        log.info("Register path passed: {}", path);
                        SaRouter.stop();
                        return;
                    }

                    // 3. 验证登录状态
                    String tokenValue = StpUtil.getTokenValue();
                    StpUtil.checkLogin();
                    // 将用户信息存入 exchange 中
                    putTrustedUserAttributes();
                    log.info("Token validation - path={}, token={}",
                            path,
                            tokenValue != null ? tokenValue : "no-token");
                })
                // 异常处理
                .setError(e -> {
                    log.warn("Token 验证失败: {}", e.getMessage());
                    GatewayErrorResult errorResult = GatewayErrorResult.builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message("未登录或登录已过期")
                            .build();
                    return JSON.toJSONString(errorResult);
                });
    }

    /**
     * 判断是否为白名单路径
     */
    private boolean isWhitelist(String path) {
        return WHITE_PATH_LIST.stream().anyMatch(path::startsWith);
    }

    private void putTrustedUserAttributes() {
        Object userInfo = StpUtil.getSession().get("userInfo");
        JSONObject userInfoJson = JSON.parseObject(JSON.toJSONString(userInfo));
        String userId = stringify(userInfoJson.get("userId"));
        String username = stringify(userInfoJson.get("username"));
        String realName = stringify(userInfoJson.get("realName"));
        if (userId == null || username == null || realName == null) {
            return;
        }
        SaReactorSyncHolder.getExchange().getAttributes().put(ATTR_USER_ID, userId);
        SaReactorSyncHolder.getExchange().getAttributes().put(ATTR_USERNAME, username);
        SaReactorSyncHolder.getExchange().getAttributes().put(ATTR_REAL_NAME, realName);
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

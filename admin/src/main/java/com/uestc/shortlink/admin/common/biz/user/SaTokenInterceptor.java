package com.uestc.shortlink.admin.common.biz.user;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * SaToken 认证拦截器
 * 负责用户登录状态校验
 */
@Slf4j
@Component
public class SaTokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        SaRouter.match("/**")
                .notMatch(
                        "/api/short-link/admin/v1/user/login",
                        "/api/short-link/admin/v1/user/has-username"
                )
                .check(r -> {
                    log.info("----- 请求path={} {}",
                            SaHolder.getRequest().getRequestPath(),
                            Optional.ofNullable(StpUtil.getTokenValue()).map(t -> "提交token=" + t).orElse("无token")
                    );
                    // POST /api/short-link/admin/v1/user 是注册接口，放行
                    if (SaRouter.match(SaHttpMethod.POST).match("/api/short-link/admin/v1/user").isHit()) {
                        return;
                    }
                    StpUtil.checkLogin();
                });
        return true;
    }
}

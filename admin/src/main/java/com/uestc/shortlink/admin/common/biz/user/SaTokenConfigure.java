package com.uestc.shortlink.admin.common.biz.user;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 1. 指定拦截所有路径 "/**"
            SaRouter.match("/**")
                    // 2. 排除 API 接口白名单
                    .notMatch(
                            "/api/short-link/admin/v1/user/login",
                            "/api/short-link/admin/v1/user/has-username"
                    )
                    // 3. 执行校验函数
                    .check(r -> {
                        log.info("----- 请求path={} {}",
                                SaHolder.getRequest().getRequestPath(),
                                Optional.ofNullable(StpUtil.getTokenValue()).map(t -> "提交token=" + t).orElse("无token")
                        );
                        // 如果请求是 POST 并且路径是 /api/short-link/admin/v1/user(注册接口)
                        // 利用 SaRouter.match 组合匹配，如果命中则 isHit() 返回 true
                        if (SaRouter.match(SaHttpMethod.POST).match("/api/short-link/admin/v1/user").isHit()) {
                            return; // 直接返回，不执行下面的 checkLogin，相当于放行
                        }
                        // 4. 其他情况，必须登录
                        StpUtil.checkLogin();
                    });

        })).addPathPatterns("/**")
           .excludePathPatterns(
                   "/error",
                   "/doc.html",
                   "/doc.html/**",
                   "/v3/api-docs/**",
                   "/webjars/**",
                   // 排除常见的静态资源后缀，一劳永逸
                   "/favicon.ico",
                   "/*.html",
                   "/*.css",
                   "/*.js",
                   "/*.png",
                   "/*.jpg",
                   "/*.jpeg",
                   "/*.gif",
                   "/*.svg",
                   "/*.ico",
                   "/.well-known/**"
           );
    }
}
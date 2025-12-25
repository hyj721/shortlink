package com.uestc.shortlink.admin.config;

import com.uestc.shortlink.admin.common.biz.user.SaTokenInterceptor;
import com.uestc.shortlink.admin.common.biz.user.UserFlowRiskControlInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Web 拦截器配置
 * 统一管理所有拦截器的注册和顺序
 */
@Configuration
@RequiredArgsConstructor
public class WebInterceptorConfiguration implements WebMvcConfigurer {

    private final SaTokenInterceptor saTokenInterceptor;
    private final UserFlowRiskControlInterceptor userFlowRiskControlInterceptor;

    /**
     * 静态资源白名单
     */
    private static final List<String> STATIC_RESOURCE_PATTERNS = Arrays.asList(
            "/error",
            "/doc.html",
            "/doc.html/**",
            "/v3/api-docs/**",
            "/webjars/**",
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. SaToken 认证拦截器 (order=0，最先执行)
        registry.addInterceptor(saTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(STATIC_RESOURCE_PATTERNS)
                .order(0);

        // 2. 用户流量风控拦截器 (order=10，在认证之后执行)
        registry.addInterceptor(userFlowRiskControlInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(STATIC_RESOURCE_PATTERNS)
                .order(10);
    }
}

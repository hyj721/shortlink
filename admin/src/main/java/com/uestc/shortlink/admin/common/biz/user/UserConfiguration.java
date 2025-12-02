package com.uestc.shortlink.admin.common.biz.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserConfiguration implements WebMvcConfigurer {

    private final UserTransmitInterceptor userTransmitInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userTransmitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/short-link/admin/v1/user/login")
                .excludePathPatterns("/user/has-username")
                .excludePathPatterns("/doc.html/**")
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/webjars/**")
                .excludePathPatterns("/error")
        ;
    }

}

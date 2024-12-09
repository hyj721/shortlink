package edu.uestc.shortlink.admin.config;

import edu.uestc.shortlink.admin.common.biz.user.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class UserConfiguration implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/short-link/v1/user/register")
                .excludePathPatterns("/api/short-link/v1/user/login")
                .excludePathPatterns("/api/short-link/v1/user/check-login");
    }
}
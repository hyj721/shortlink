package com.uestc.shortlink.admin.common.biz.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestc.shortlink.admin.common.constant.RedisCacheConstant;
import com.uestc.shortlink.admin.common.convention.exception.ClientException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserTransmitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
        String token = request.getHeader("token");
        String username = request.getHeader("username");
        Object jsonUserInfo = null;
        try {
            jsonUserInfo = stringRedisTemplate.opsForHash().get(RedisCacheConstant.USER_LOGIN_KEY + username, token);
        } catch (IllegalArgumentException e) {
            throw new ClientException("用户token不存在");
        }
        if (jsonUserInfo != null) {
            UserInfoDTO userInfoDTO = objectMapper.readValue(jsonUserInfo.toString(), UserInfoDTO.class);
            UserContext.setUser(userInfoDTO);
        }
        return true;
    }

    @Override
    public void afterCompletion(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler, Exception exception) throws Exception {
        UserContext.removeUser();
    }
}
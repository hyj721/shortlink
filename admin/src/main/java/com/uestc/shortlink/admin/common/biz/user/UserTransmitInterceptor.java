package com.uestc.shortlink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
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

import static com.uestc.shortlink.admin.common.enums.UserErrorCodeEnum.USER_TOKEN_FAIL;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserTransmitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
        // 用户注册接口（POST 请求）直接放行
        if ("POST".equalsIgnoreCase(request.getMethod()) &&
            "/api/short-link/admin/v1/user".equals(request.getRequestURI())) {
            return true;
        }

        String token = request.getHeader("token");
        String username = request.getHeader("username");
        // 如果二者有一个为空
        if (!StrUtil.isAllNotBlank(token, username)) {
            log.warn("请求路径uri：{}未提供token或username", request.getRequestURI());
            throw new ClientException(USER_TOKEN_FAIL);
        }
        Object jsonUserInfo = null;
        try {
            jsonUserInfo = stringRedisTemplate.opsForHash().get(RedisCacheConstant.USER_LOGIN_KEY + username, token);
            if (jsonUserInfo == null) {
                throw new ClientException(USER_TOKEN_FAIL);
            }
        } catch (IllegalArgumentException e) {
            throw new ClientException(USER_TOKEN_FAIL);
        }
        UserInfoDTO userInfoDTO = objectMapper.readValue(jsonUserInfo.toString(), UserInfoDTO.class);
        UserContext.setUser(userInfoDTO);
        return true;
    }

    @Override
    public void afterCompletion(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler, Exception exception) throws Exception {
        UserContext.removeUser();
    }
}
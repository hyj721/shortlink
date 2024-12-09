package edu.uestc.shortlink.admin.common.biz.user;

import com.alibaba.fastjson2.JSON;
import edu.uestc.shortlink.admin.common.convention.exception.ClientException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String username = request.getHeader("username");
        String token = request.getHeader("token");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(token)) {
            throw new ClientException("用户未登录");
        }


        Object userInfoJsonStr = stringRedisTemplate.opsForHash().get("login_" + username, token);
        if (userInfoJsonStr == null) {
            throw new ClientException("用户未登录");
        }
        UserInfoDTO userInfoDTO = JSON.parseObject(userInfoJsonStr.toString(), UserInfoDTO.class);
        UserContext.setUser(userInfoDTO);
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {
        // 请求完成后清理上下文
        UserContext.removeUser();
    }
}

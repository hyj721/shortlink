package com.uestc.shortlink.admin.common.biz.user;

import com.uestc.shortlink.admin.common.convention.errorcode.BaseErrorCode;
import com.uestc.shortlink.admin.common.convention.exception.ClientException;
import com.uestc.shortlink.admin.config.UserFlowRiskControlConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.Optional;

/**
 * 用户流量风控拦截器
 * 基于用户名进行限流，防止单用户请求过于频繁
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserFlowRiskControlInterceptor implements HandlerInterceptor {

    /**
     * 未登录用户的限流 key 前缀
     * 用于注册、登录、检查用户名等不需要认证的接口
     */
    private static final String ANONYMOUS_USER_FLOW_RISK_CONTROL_KEY = "short-link:flow-risk-control:anonymous-user";

    /**
     * 已登录用户的限流 key 前缀
     */
    private static final String USER_FLOW_RISK_CONTROL_KEY_PREFIX = "short-link:flow-risk-control:user:";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserFlowRiskControlConfiguration flowRiskControlConfig;

    private static final DefaultRedisScript<Long> FLOW_RISK_CONTROL_SCRIPT;

    static {
        FLOW_RISK_CONTROL_SCRIPT = new DefaultRedisScript<>();
        FLOW_RISK_CONTROL_SCRIPT.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/user_flow_risk_control.lua"))
        );
        FLOW_RISK_CONTROL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未开启流量风控，直接放行
        if (!Boolean.TRUE.equals(flowRiskControlConfig.getEnable())) {
            return true;
        }

        // 获取限流 key
        String flowRiskControlKey = getFlowRiskControlKey();

        // 执行 Lua 脚本，返回当前时间窗口内的请求次数
        Long currentCount = stringRedisTemplate.execute(
                FLOW_RISK_CONTROL_SCRIPT,
                Collections.singletonList(flowRiskControlKey),
                flowRiskControlConfig.getTimeWindow()
        );

        // 判断是否超过最大访问次数
        if (currentCount != null && currentCount > flowRiskControlConfig.getMaxAccessCount()) {
            log.warn("用户请求被限流，key: {}, 当前次数: {}, 最大允许: {}",
                    flowRiskControlKey, currentCount, flowRiskControlConfig.getMaxAccessCount());
            throw new ClientException(BaseErrorCode.FLOW_LIMIT_ERROR);
        }

        return true;
    }

    /**
     * 获取限流 key
     * 已登录用户使用 username，未登录用户使用特殊标识
     */
    private String getFlowRiskControlKey() {
        return Optional.ofNullable(getUsernameOrNull())
                .map(username -> USER_FLOW_RISK_CONTROL_KEY_PREFIX + username)
                .orElse(ANONYMOUS_USER_FLOW_RISK_CONTROL_KEY);
    }

    /**
     * 安全获取用户名，未登录返回 null
     */
    private String getUsernameOrNull() {
        try {
            return UserContext.getUsername();
        } catch (Exception e) {
            // 未登录或获取失败，返回 null
            return null;
        }
    }
}

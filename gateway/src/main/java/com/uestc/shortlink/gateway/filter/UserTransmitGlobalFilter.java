package com.uestc.shortlink.gateway.filter;

import com.uestc.shortlink.gateway.config.SaTokenConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Sanitizes client user headers and propagates trusted user headers from gateway.
 */
@Slf4j
@Component
public class UserTransmitGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_REAL_NAME = "X-Real-Name";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
        requestBuilder.headers(headers -> {
            headers.remove(HEADER_USER_ID);
            headers.remove(HEADER_USERNAME);
            headers.remove(HEADER_REAL_NAME);
        });

        UserHeaders trustedUserHeaders = resolveTrustedUserHeaders(exchange);
        if (trustedUserHeaders != null) {
            requestBuilder.header(HEADER_USER_ID, trustedUserHeaders.userId());
            requestBuilder.header(HEADER_USERNAME, trustedUserHeaders.username());
            requestBuilder.header(HEADER_REAL_NAME, trustedUserHeaders.realName());
        }

        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private UserHeaders resolveTrustedUserHeaders(ServerWebExchange exchange) {
        try {
            String userId = stringify(exchange.getAttribute(SaTokenConfiguration.ATTR_USER_ID));
            String username = stringify(exchange.getAttribute(SaTokenConfiguration.ATTR_USERNAME));
            String realName = stringify(exchange.getAttribute(SaTokenConfiguration.ATTR_REAL_NAME));
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(username) || !StringUtils.hasText(realName)) {
                return null;
            }
            return new UserHeaders(userId, username, realName);
        } catch (Exception ex) {
            log.debug("Failed to resolve trusted user headers: {}", ex.getMessage());
            return null;
        }
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record UserHeaders(String userId, String username, String realName) {
    }
}

package com.uestc.shortlink.gateway.filter;

import com.uestc.shortlink.gateway.config.SaTokenConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Records unified access logs for all gateway requests.
 */
@Slf4j
@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTimeMillis = System.currentTimeMillis();
        String traceId = resolveTraceId(exchange);

        ServerHttpRequest requestWithTraceId = exchange.getRequest().mutate()
                .header(HEADER_TRACE_ID, traceId)
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(requestWithTraceId).build();
        mutatedExchange.getResponse().getHeaders().set(HEADER_TRACE_ID, traceId);

        return chain.filter(mutatedExchange)
                .doOnSuccess(unused -> logAccess(mutatedExchange, traceId, startTimeMillis, null))
                .doOnError(error -> logAccess(mutatedExchange, traceId, startTimeMillis, error));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }

    private void logAccess(ServerWebExchange exchange, String traceId, long startTimeMillis, Throwable error) {
        long durationMs = System.currentTimeMillis() - startTimeMillis;
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        int status = statusCode != null ? statusCode.value() : 0;

        String userId = stringify(exchange.getAttribute(SaTokenConfiguration.ATTR_USER_ID));
        String username = stringify(exchange.getAttribute(SaTokenConfiguration.ATTR_USERNAME));
        String who = StringUtils.hasText(username)
                ? String.format("%s(%s)", username, StringUtils.hasText(userId) ? userId : "-")
                : "anonymous";

        String when = OffsetDateTime.now().format(TIME_FORMATTER);
        String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN";
        String path = exchange.getRequest().getURI().getRawPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        String fullPath = StringUtils.hasText(query) ? path + "?" + query : path;

        if (error == null) {
            log.info("Gateway access who={} when={} method={} path={} status={} durationMs={} traceId={}",
                    who, when, method, fullPath, status, durationMs, traceId);
            return;
        }

        log.warn("Gateway access who={} when={} method={} path={} status={} durationMs={} traceId={} error={}",
                who, when, method, fullPath, status, durationMs, traceId, error.getMessage());
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(HEADER_TRACE_ID);
        return StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString();
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

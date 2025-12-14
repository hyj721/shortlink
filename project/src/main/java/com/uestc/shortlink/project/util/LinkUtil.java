package com.uestc.shortlink.project.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Optional;

import static com.uestc.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

/**
 * 短链接工具类
 */
public class LinkUtil {

    /**
     * 获取短链接缓存有效时间（毫秒）
     * <p>
     * 如果短链接设置了有效期，返回从当前时间到有效期的剩余时间；
     * 如果未指定有效期（validDate 为空），返回默认缓存时间。
     *
     * @param validDate 短链接有效期，可为 null（使用默认缓存时间）
     * @return 缓存有效时间（毫秒）
     */
    public static long getLinkCacheValidTime(Date validDate) {
        return Optional.ofNullable(validDate)
                .map(date -> {
                    long remainingTime = date.getTime() - System.currentTimeMillis();
                    return remainingTime > 0 ? remainingTime : 0L;
                })
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 优先从代理头中获取（支持 Nginx、CDN 等），依次检查：
     * X-Forwarded-For、Proxy-Client-IP、WL-Proxy-Client-IP、
     * HTTP_CLIENT_IP、HTTP_X_FORWARDED_FOR、X-Real-IP
     *
     * @param request HttpServletRequest
     * @return 客户端真实 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // X-Forwarded-For 可能包含多个 IP，取第一个非 unknown 的
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        // 都没有则直接获取远程地址
        return request.getRemoteAddr();
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
}


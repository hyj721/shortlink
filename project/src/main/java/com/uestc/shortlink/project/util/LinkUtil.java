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

    /**
     * 从 User-Agent 解析操作系统类型
     *
     * @param request HttpServletRequest
     * @return 操作系统类型（Windows、Mac OS、Linux、Android、iOS、Unknown）
     */

    public static String getOs(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        // 1. 判空
        if (userAgent == null || userAgent.length() == 0) {
            return "Unknown";
        }

        // 2. 转小写，一劳永逸解决大小写问题
        String ua = userAgent.toLowerCase();

        // ---------------------------------------------------
        // 第一梯队：移动端 (必须先判断，防止被桌面端关键字误伤)
        // ---------------------------------------------------

        // 鸿蒙 (放在 Android 前面，因为鸿蒙 UA 通常也包含 Android)
        if (ua.contains("harmonyos")) {
            return "HarmonyOS";
        }

        // Android
        if (ua.contains("android")) {
            return "Android";
        }

        // iOS (包括 iPhone, iPad, iPod)
        // 注意：iPadOS 13+ 默认开启“桌面网站”，UA 会变成 Macintosh。
        // 如果必须区分桌面版 iPad，需要前端配合检测触控点，后端纯 UA 无法 100% 区分。
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod") || ua.contains("ios")) {
            return "iOS";
        }

        // ---------------------------------------------------
        // 第二梯队：桌面端
        // ---------------------------------------------------

        // Windows
        if (ua.contains("windows")) {
            return "Windows";
        }

        // Mac OS
        // 此时已经排除了 iPhone/iPad，这里剩下的包含 mac 的基本上就是电脑了
        if (ua.contains("mac os") || ua.contains("macintosh") || ua.contains("os x")) {
            return "Mac OS";
        }

        // Linux
        // 此时已经排除了 Android，剩下的通常是服务器或 Linux 桌面
        if (ua.contains("linux")) {
            return "Linux";
        }

        return "Unknown";
    }

    /**
     * 从 User-Agent 解析浏览器类型
     *
     * @param request HttpServletRequest
     * @return 浏览器类型（Chrome、Firefox、Safari、Edge、Opera、IE、Unknown）
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        // 1. 判空
        if (userAgent == null || userAgent.length() == 0) {
            return "Unknown";
        }

        // 2. 转小写，一劳永逸解决大小写问题
        String ua = userAgent.toLowerCase();

        // ---------------------------------------------------
        // 浏览器检测顺序很重要：需要先检测更具体的浏览器
        // 因为很多浏览器 UA 中都包含 "chrome" 或 "safari"
        // ---------------------------------------------------

        // Edge (新版基于 Chromium 的 Edge 包含 "edg"，旧版包含 "edge")
        if (ua.contains("edg") || ua.contains("edge")) {
            return "Microsoft Edge";
        }

        // Opera (Opera 15+ 基于 Chromium，UA 包含 "opr" 或 "opera")
        if (ua.contains("opr") || ua.contains("opera")) {
            return "Opera";
        }

        // Firefox
        if (ua.contains("firefox")) {
            return "Mozilla Firefox";
        }

        // Chrome (注意：需要在 Edge/Opera 之后检测，因为它们的 UA 也包含 "chrome")
        if (ua.contains("chrome") || ua.contains("chromium")) {
            return "Google Chrome";
        }

        // Safari (需要在 Chrome 之后检测，因为 Chrome 的 UA 也包含 "safari")
        if (ua.contains("safari")) {
            return "Apple Safari";
        }

        // IE (Internet Explorer)
        if (ua.contains("msie") || ua.contains("trident")) {
            return "Internet Explorer";
        }

        return "Unknown";
    }

    /**
     * 从 User-Agent 解析设备类型
     *
     * @param request HttpServletRequest
     * @return 设备类型（Mobile、Tablet、PC、Unknown）
     */
    public static String getDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        // ---------------------------------------------------
        // 1. Tablet 检测 (需要在 Mobile 之前，因为平板也可能包含 mobile 关键字)
        // ---------------------------------------------------
        // 包含 kindle, silk (Amazon 设备)
        if (ua.contains("ipad")
                || ua.contains("tablet")
                || ua.contains("kindle")
                || ua.contains("silk")
                || (ua.contains("android") && !ua.contains("mobile"))) {
            return "Tablet";
        }

        // ---------------------------------------------------
        // 2. Mobile 检测
        // ---------------------------------------------------
        // 包含 blackberry, windows phone 等
        if (ua.contains("mobile")
                || ua.contains("iphone")
                || ua.contains("ipod")
                || ua.contains("android")
                || ua.contains("phone")
                || ua.contains("wap")
                || ua.contains("blackberry")
                || ua.contains("windows phone")) {
            return "Mobile";
        }

        // ---------------------------------------------------
        // 3. 默认为 PC
        // ---------------------------------------------------
        return "PC";
    }
}

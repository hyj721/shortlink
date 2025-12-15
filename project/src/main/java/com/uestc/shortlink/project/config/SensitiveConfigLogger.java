package com.uestc.shortlink.project.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 敏感配置检查器
 * 在应用启动时打印关键配置的加载状态
 */
@Slf4j
@Component
public class SensitiveConfigLogger {

    @Value("${short-link.stats.locale.amap-key:}")
    private String amapKey;

    @PostConstruct
    public void logConfigStatus() {
        if (StringUtils.hasText(amapKey)) {
            // 只显示前4位，隐藏其余部分，避免泄露完整 key
            String maskedKey = amapKey.substring(0, Math.min(4, amapKey.length())) + "****";
            log.info("[CONFIG] 高德地图 API Key 已配置: {}", maskedKey);
        } else {
            log.warn("[CONFIG] 高德地图 API Key 未配置！地理位置统计功能将不可用。请设置环境变量 AMAP_KEY");
        }
    }
}

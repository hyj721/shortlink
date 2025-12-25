package com.uestc.shortlink.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 跳转域名白名单配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "short-link.goto-domain.white-list")
public class GotoDomainWhiteListConfiguration {

    /**
     * 是否启用白名单功能
     */
    private Boolean enable;

    /**
     * 白名单域名的名字
     */
    private String names;

    /**
     * 白名单域名列表
     */
    private List<String> domains;

}

package com.uestc.shortlink.project.service.impl;

import cn.hutool.core.util.StrUtil;
import com.uestc.shortlink.project.common.convention.exception.ClientException;
import com.uestc.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.uestc.shortlink.project.util.LinkUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Provides whitelist validation for destination URLs.
 */
@Service
@RequiredArgsConstructor
public class ShortLinkWhitelistService {

    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfig;

    /**
     * Verifies whether destination URL is allowed by configured domain whitelist.
     *
     * @param originUrl destination URL
     */
    public void verifyOriginUrl(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfig.getEnable();
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("Invalid URL format.");
        }
        boolean allowed = gotoDomainWhiteListConfig.getDomains().stream()
                .anyMatch(domain::endsWith);
        if (!allowed) {
            throw new ClientException("Domain is not in whitelist: " + gotoDomainWhiteListConfig.getNames());
        }
    }
}

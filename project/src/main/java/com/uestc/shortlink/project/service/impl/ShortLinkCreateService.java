package com.uestc.shortlink.project.service.impl;

import com.uestc.shortlink.project.common.convention.exception.ServiceException;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.uestc.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.uestc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.uestc.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkBaseInfoRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.project.util.HashUtil;
import com.uestc.shortlink.project.util.LinkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;

/**
 * Handles short-link creation workflows.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkCreateService {

    private final RBloomFilter<String> shortUrlCreateBloomFilter;
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ShortLinkWhitelistService shortLinkWhitelistService;

    @Value("${short-link.domain.default}")
    private String defaultDomain;

    /**
     * Creates one short-link and preheats redirect cache.
     *
     * @param requestParam create request
     * @return create response
     */
    @Transactional(rollbackFor = Exception.class)
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        shortLinkWhitelistService.verifyOriginUrl(requestParam.getOriginUrl());

        String suffix = generateSuffix(requestParam);
        String fullShortUrl = defaultDomain + "/" + suffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(defaultDomain)
                .shortUri(suffix)
                .fullShortUrl(fullShortUrl)
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .enableStatus(1)
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .delTime(0L)
                .build();
        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            shortLinkMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        } catch (DuplicateKeyException e) {
            log.warn("Failed to create duplicated short-link: {}", fullShortUrl);
            throw new ServiceException(String.format("Duplicated short-link generated: %s", fullShortUrl));
        }
        shortUrlCreateBloomFilter.add(fullShortUrl);
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                requestParam.getOriginUrl(),
                LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()),
                TimeUnit.MILLISECONDS
        );
        return ShortLinkCreateRespDTO.builder()
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .fullShortUrl(fullShortUrl)
                .build();
    }

    /**
     * Creates short-links in batch and returns successful records only.
     *
     * @param requestParam batch create request
     * @return batch create response
     */
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = ShortLinkCreateReqDTO.builder()
                    .originUrl(originUrls.get(i))
                    .gid(requestParam.getGid())
                    .createdType(requestParam.getCreatedType())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .describe(describes.get(i))
                    .build();
            try {
                ShortLinkCreateRespDTO shortLink = this.createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Exception e) {
                log.error("Failed to batch create short-link, originUrl={}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        int retryCount = 0;
        String shortUri;
        while (true) {
            if (retryCount > 10) {
                throw new ServiceException("Short-link generation is too frequent, please retry later.");
            }
            String originUrl = requestParam.getOriginUrl() + UUID.randomUUID();
            shortUri = HashUtil.hashToBase62(originUrl);
            String fullShortUrl = defaultDomain + "/" + shortUri;
            if (!shortUrlCreateBloomFilter.contains(fullShortUrl)) {
                break;
            }
            retryCount++;
        }
        return shortUri;
    }

    private String getFavicon(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .timeout(5000)
                    .userAgent("Mozilla/5.0")
                    .get();

            String[] selectors = {
                    "link[rel='icon']",
                    "link[rel='shortcut icon']",
                    "link[rel~=(?i)^(shortcut )?icon]",
                    "link[rel='apple-touch-icon']"
            };

            for (String selector : selectors) {
                Element link = document.select(selector).first();
                if (link != null && link.hasAttr("href")) {
                    return link.attr("abs:href");
                }
            }

            URL targetUrl = new URL(url);
            return targetUrl.getProtocol() + "://" + targetUrl.getHost() + "/favicon.ico";
        } catch (Exception e) {
            log.warn("Failed to load favicon: url={}, error={}", url, e.getMessage());
            return null;
        }
    }
}

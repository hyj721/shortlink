package com.uestc.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.uestc.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.uestc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.uestc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.uestc.shortlink.project.util.LinkUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_UIP_DAY_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_UIP_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_UV_DAY_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_UV_KEY;
import static com.uestc.shortlink.project.common.constant.ShortLinkConstant.NOT_FOUND_PAGE_PATH;

/**
 * Handles short-link redirect flow, including cache lookup, DB fallback and stats publishing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkRedirectService {

    private final RBloomFilter<String> shortUrlCreateBloomFilter;
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ShortLinkStatsDispatchService shortLinkStatsDispatchService;

    /**
     * Restores original URL by short URI and redirects client.
     *
     * @param shortUri short-link suffix
     * @param request HTTP request
     * @param response HTTP response
     */
    @SneakyThrows
    public void restoreLongLink(String shortUri, HttpServletRequest request, HttpServletResponse response) {
        String fullShortUrl = request.getServerName() + "/" + shortUri;
        String cachedOriginUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StringUtils.hasText(cachedOriginUrl)) {
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            publishStats(null, statsRecord);
            response.sendRedirect(cachedOriginUrl);
            return;
        }
        if (!shortUrlCreateBloomFilter.contains(fullShortUrl)) {
            response.sendRedirect(NOT_FOUND_PAGE_PATH);
            return;
        }
        if (hasNullCache(fullShortUrl)) {
            response.sendRedirect(NOT_FOUND_PAGE_PATH);
            return;
        }

        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {
            // 二次检查，避免并发穿透导致重复查询数据库
            cachedOriginUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StringUtils.hasText(cachedOriginUrl)) {
                ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
                publishStats(null, statsRecord);
                response.sendRedirect(cachedOriginUrl);
                return;
            }
            if (hasNullCache(fullShortUrl)) {
                response.sendRedirect(NOT_FOUND_PAGE_PATH);
                return;
            }

            LambdaQueryWrapper<ShortLinkGotoDO> gotoQuery = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(gotoQuery);
            if (shortLinkGotoDO == null) {
                setNullCache(fullShortUrl);
                response.sendRedirect(NOT_FOUND_PAGE_PATH);
                return;
            }
            String gid = shortLinkGotoDO.getGid();

            LambdaQueryWrapper<ShortLinkDO> shortLinkQuery = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, gid)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 1);
            ShortLinkDO shortLinkDO = shortLinkMapper.selectOne(shortLinkQuery);
            if (shortLinkDO == null) {
                setNullCache(fullShortUrl);
                response.sendRedirect(NOT_FOUND_PAGE_PATH);
                return;
            }
            if (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())) {
                setNullCache(fullShortUrl);
                response.sendRedirect(NOT_FOUND_PAGE_PATH);
                return;
            }

            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                    TimeUnit.MILLISECONDS
            );
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            publishStats(gid, statsRecord);
            response.sendRedirect(shortLinkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }
    }

    private boolean hasNullCache(String fullShortUrl) {
        return StringUtils.hasText(stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl)));
    }

    private void setNullCache(String fullShortUrl) {
        stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "!!!", 30, TimeUnit.MINUTES);
    }

    private void publishStats(String gid, ShortLinkStatsRecordDTO statsRecord) {
        shortLinkStatsDispatchService.dispatch(gid, statsRecord);
    }

    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl,
                                                                   HttpServletRequest request,
                                                                   HttpServletResponse response) {
        String clientIp = LinkUtil.getClientIp(request);
        String browser = LinkUtil.getBrowser(request);
        String os = LinkUtil.getOs(request);
        String device = LinkUtil.getDevice(request);
        String network = LinkUtil.getNetwork(request);
        String uvValue = getOrCreateUvValue(request, response);

        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .clientIp(clientIp)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .uv(uvValue)
                .uvFirstFlag(verificationUv(fullShortUrl, uvValue))
                .uipFirstFlag(verificationUip(fullShortUrl, clientIp))
                .dailyUvFirstFlag(verificationDailyUv(fullShortUrl, uvValue))
                .dailyUipFirstFlag(verificationDailyUip(fullShortUrl, clientIp))
                .build();
    }

    private int verificationUv(String fullShortUrl, String uvValue) {
        String uvKey = String.format(SHORT_LINK_STATS_UV_KEY, fullShortUrl);
        Long added = stringRedisTemplate.opsForSet().add(uvKey, uvValue);
        return (added != null && added > 0) ? 1 : 0;
    }

    private int verificationUip(String fullShortUrl, String clientIp) {
        String uipKey = String.format(SHORT_LINK_STATS_UIP_KEY, fullShortUrl);
        Long added = stringRedisTemplate.opsForSet().add(uipKey, clientIp);
        return (added != null && added > 0) ? 1 : 0;
    }

    private int verificationDailyUv(String fullShortUrl, String uvValue) {
        String uvDailyKey = String.format(SHORT_LINK_STATS_UV_DAY_KEY, fullShortUrl, currentStatsDate());
        Long added = stringRedisTemplate.opsForSet().add(uvDailyKey, uvValue);
        if (added != null && added > 0) {
            stringRedisTemplate.expire(uvDailyKey, 2, TimeUnit.DAYS);
        }
        return (added != null && added > 0) ? 1 : 0;
    }

    private int verificationDailyUip(String fullShortUrl, String clientIp) {
        String uipDailyKey = String.format(SHORT_LINK_STATS_UIP_DAY_KEY, fullShortUrl, currentStatsDate());
        Long added = stringRedisTemplate.opsForSet().add(uipDailyKey, clientIp);
        if (added != null && added > 0) {
            stringRedisTemplate.expire(uipDailyKey, 2, TimeUnit.DAYS);
        }
        return (added != null && added > 0) ? 1 : 0;
    }

    private String currentStatsDate() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String getOrCreateUvValue(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("uv".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String uvValue = UUID.randomUUID().toString();
        Cookie uvCookie = new Cookie("uv", uvValue);
        uvCookie.setMaxAge(60 * 60 * 24 * 30);
        uvCookie.setPath(request.getRequestURI());
        response.addCookie(uvCookie);
        return uvValue;
    }
}

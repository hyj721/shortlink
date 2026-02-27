package com.uestc.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.uestc.shortlink.project.common.convention.exception.ClientException;
import com.uestc.shortlink.project.common.convention.exception.ServiceException;
import com.uestc.shortlink.project.dao.entity.LinkAccessLogsDO;
import com.uestc.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.uestc.shortlink.project.dao.entity.LinkBrowserStatsDO;
import com.uestc.shortlink.project.dao.entity.LinkDeviceStatsDO;
import com.uestc.shortlink.project.dao.entity.LinkLocaleStatsDO;
import com.uestc.shortlink.project.dao.entity.LinkNetworkStatsDO;
import com.uestc.shortlink.project.dao.entity.LinkOsStatsDO;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.uestc.shortlink.project.dao.mapper.LinkAccessLogsMapper;
import com.uestc.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.uestc.shortlink.project.dao.mapper.LinkBrowserStatsMapper;
import com.uestc.shortlink.project.dao.mapper.LinkDeviceStatsMapper;
import com.uestc.shortlink.project.dao.mapper.LinkLocaleStatsMapper;
import com.uestc.shortlink.project.dao.mapper.LinkNetworkStatsMapper;
import com.uestc.shortlink.project.dao.mapper.LinkOsStatsMapper;
import com.uestc.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.uestc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.uestc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;

/**
 * Orchestrates short-link update workflow across sharded and non-sharded tables.
 */
@Service
@RequiredArgsConstructor
public class ShortLinkUpdateService {

    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Updates short-link metadata and handles cross-group migration when shard key changes.
     *
     * @param requestParam update request
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 先确认原记录存在，避免在后续迁移流程中出现脏写
        ShortLinkDO existingShortLink = shortLinkMapper.selectOne(
                Wrappers.lambdaQuery(ShortLinkDO.class)
                        .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 1)
        );
        if (existingShortLink == null) {
            throw new ClientException("Short link record does not exist.");
        }

        boolean sameGroup = !StringUtils.hasText(requestParam.getGid())
                || Objects.equals(existingShortLink.getGid(), requestParam.getGid());
        boolean shouldClearValidDate = Objects.nonNull(requestParam.getValidDateType())
                && requestParam.getValidDateType() == 0;

        if (sameGroup) {
            updateInSameGroup(requestParam, shouldClearValidDate);
        } else {
            migrateAcrossGroup(requestParam, existingShortLink, shouldClearValidDate);
        }

        invalidateRedirectCacheIfNeeded(requestParam, existingShortLink);
    }

    private void updateInSameGroup(ShortLinkUpdateReqDTO requestParam, boolean shouldClearValidDate) {
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getDelTime, 0L)
                .eq(ShortLinkDO::getEnableStatus, 1)
                .set(Objects.nonNull(requestParam.getOriginUrl()), ShortLinkDO::getOriginUrl, requestParam.getOriginUrl())
                .set(Objects.nonNull(requestParam.getValidDateType()), ShortLinkDO::getValidDateType, requestParam.getValidDateType())
                .set(shouldClearValidDate, ShortLinkDO::getValidDate, null)
                .set(!shouldClearValidDate && Objects.nonNull(requestParam.getValidDate()), ShortLinkDO::getValidDate, requestParam.getValidDate())
                .set(Objects.nonNull(requestParam.getDescribe()), ShortLinkDO::getDescribe, requestParam.getDescribe())
                .set(Objects.nonNull(requestParam.getFavicon()), ShortLinkDO::getFavicon, requestParam.getFavicon());
        shortLinkMapper.update(null, updateWrapper);
    }

    private void migrateAcrossGroup(ShortLinkUpdateReqDTO requestParam,
                                    ShortLinkDO existingShortLink,
                                    boolean shouldClearValidDate) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
        RLock writeLock = readWriteLock.writeLock();
        boolean locked = writeLock.tryLock();
        if (!locked) {
            throw new ServiceException("Short link is being visited, please retry later.");
        }
        try {
            softDeleteOriginalRecord(requestParam);
            insertMigratedRecord(requestParam, existingShortLink, shouldClearValidDate);
            migrateGotoRecord(requestParam, existingShortLink);
            migrateNonShardingTables(requestParam.getFullShortUrl(), existingShortLink.getGid(), requestParam.getGid());
        } finally {
            writeLock.unlock();
        }
    }

    private void softDeleteOriginalRecord(ShortLinkUpdateReqDTO requestParam) {
        shortLinkMapper.update(
                null,
                Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 1)
                        .set(ShortLinkDO::getDelFlag, 1)
                        .set(ShortLinkDO::getDelTime, System.currentTimeMillis())
        );
    }

    private void insertMigratedRecord(ShortLinkUpdateReqDTO requestParam,
                                      ShortLinkDO existingShortLink,
                                      boolean shouldClearValidDate) {
        ShortLinkDO migratedShortLink = ShortLinkDO.builder()
                .domain(existingShortLink.getDomain())
                .shortUri(existingShortLink.getShortUri())
                .fullShortUrl(existingShortLink.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl() != null ? requestParam.getOriginUrl() : existingShortLink.getOriginUrl())
                .gid(requestParam.getGid())
                .enableStatus(existingShortLink.getEnableStatus())
                .createdType(existingShortLink.getCreatedType())
                .validDateType(requestParam.getValidDateType() != null ? requestParam.getValidDateType() : existingShortLink.getValidDateType())
                .validDate(shouldClearValidDate ? null : (requestParam.getValidDate() != null ? requestParam.getValidDate() : existingShortLink.getValidDate()))
                .describe(requestParam.getDescribe() != null ? requestParam.getDescribe() : existingShortLink.getDescribe())
                .favicon(requestParam.getFavicon() != null ? requestParam.getFavicon() : existingShortLink.getFavicon())
                .delTime(0L)
                .build();
        shortLinkMapper.insert(migratedShortLink);
    }

    private void migrateGotoRecord(ShortLinkUpdateReqDTO requestParam, ShortLinkDO existingShortLink) {
        LambdaQueryWrapper<ShortLinkGotoDO> gotoQuery = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkGotoDO::getGid, existingShortLink.getGid());
        ShortLinkGotoDO existingGotoRecord = shortLinkGotoMapper.selectOne(gotoQuery);
        if (existingGotoRecord == null) {
            throw new ServiceException("Short link route record does not exist.");
        }

        shortLinkGotoMapper.deleteById(existingGotoRecord.getId());
        shortLinkGotoMapper.insert(ShortLinkGotoDO.builder()
                .fullShortUrl(requestParam.getFullShortUrl())
                .gid(requestParam.getGid())
                .build());
    }

    private void migrateNonShardingTables(String fullShortUrl, String originGid, String targetGid) {
        updateGidInAccessStats(fullShortUrl, originGid, targetGid);
        updateGidInLocaleStats(fullShortUrl, originGid, targetGid);
        updateGidInOsStats(fullShortUrl, originGid, targetGid);
        updateGidInBrowserStats(fullShortUrl, originGid, targetGid);
        updateGidInDeviceStats(fullShortUrl, originGid, targetGid);
        updateGidInNetworkStats(fullShortUrl, originGid, targetGid);
        updateGidInAccessLogs(fullShortUrl, originGid, targetGid);
    }

    private void updateGidInAccessStats(String fullShortUrl, String originGid, String targetGid) {
        linkAccessStatsMapper.update(
                LinkAccessStatsDO.builder().gid(targetGid).build(),
                Wrappers.lambdaUpdate(LinkAccessStatsDO.class)
                        .eq(LinkAccessStatsDO::getFullShortUrl, fullShortUrl)
                        .eq(LinkAccessStatsDO::getGid, originGid)
                        .eq(LinkAccessStatsDO::getDelFlag, 0)
        );
    }

    private void updateGidInLocaleStats(String fullShortUrl, String originGid, String targetGid) {
        linkLocaleStatsMapper.update(
                LinkLocaleStatsDO.builder().gid(targetGid).build(),
                Wrappers.lambdaUpdate(LinkLocaleStatsDO.class)
                        .eq(LinkLocaleStatsDO::getFullShortUrl, fullShortUrl)
                        .eq(LinkLocaleStatsDO::getGid, originGid)
                        .eq(LinkLocaleStatsDO::getDelFlag, 0)
        );
    }

    private void updateGidInOsStats(String fullShortUrl, String originGid, String targetGid) {
        linkOsStatsMapper.update(
                LinkOsStatsDO.builder().gid(targetGid).build(),
                Wrappers.lambdaUpdate(LinkOsStatsDO.class)
                        .eq(LinkOsStatsDO::getFullShortUrl, fullShortUrl)
                        .eq(LinkOsStatsDO::getGid, originGid)
                        .eq(LinkOsStatsDO::getDelFlag, 0)
        );
    }

    private void updateGidInBrowserStats(String fullShortUrl, String originGid, String targetGid) {
        linkBrowserStatsMapper.update(
                LinkBrowserStatsDO.builder().gid(targetGid).build(),
                Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
                        .eq(LinkBrowserStatsDO::getFullShortUrl, fullShortUrl)
                        .eq(LinkBrowserStatsDO::getGid, originGid)
                        .eq(LinkBrowserStatsDO::getDelFlag, 0)
        );
    }

    private void updateGidInDeviceStats(String fullShortUrl, String originGid, String targetGid) {
        linkDeviceStatsMapper.update(
                LinkDeviceStatsDO.builder().gid(targetGid).build(),
                Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
                        .eq(LinkDeviceStatsDO::getFullShortUrl, fullShortUrl)
                        .eq(LinkDeviceStatsDO::getGid, originGid)
                        .eq(LinkDeviceStatsDO::getDelFlag, 0)
        );
    }

    private void updateGidInNetworkStats(String fullShortUrl, String originGid, String targetGid) {
        linkNetworkStatsMapper.update(
                LinkNetworkStatsDO.builder().gid(targetGid).build(),
                Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
                        .eq(LinkNetworkStatsDO::getFullShortUrl, fullShortUrl)
                        .eq(LinkNetworkStatsDO::getGid, originGid)
                        .eq(LinkNetworkStatsDO::getDelFlag, 0)
        );
    }

    private void updateGidInAccessLogs(String fullShortUrl, String originGid, String targetGid) {
        linkAccessLogsMapper.update(
                LinkAccessLogsDO.builder().gid(targetGid).build(),
                Wrappers.lambdaUpdate(LinkAccessLogsDO.class)
                        .eq(LinkAccessLogsDO::getFullShortUrl, fullShortUrl)
                        .eq(LinkAccessLogsDO::getGid, originGid)
                        .eq(LinkAccessLogsDO::getDelFlag, 0)
        );
    }

    private void invalidateRedirectCacheIfNeeded(ShortLinkUpdateReqDTO requestParam, ShortLinkDO existingShortLink) {
        boolean needInvalidate = !Objects.equals(existingShortLink.getValidDateType(), requestParam.getValidDateType())
                || !Objects.equals(existingShortLink.getValidDate(), requestParam.getValidDate())
                || (requestParam.getOriginUrl() != null && !Objects.equals(existingShortLink.getOriginUrl(), requestParam.getOriginUrl()));
        if (!needInvalidate) {
            return;
        }
        stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
    }
}

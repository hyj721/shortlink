package com.uestc.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.project.common.convention.exception.ClientException;
import com.uestc.shortlink.project.common.convention.exception.ServiceException;
import com.uestc.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.uestc.shortlink.project.dao.entity.*;
import com.uestc.shortlink.project.dao.mapper.*;
import com.uestc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.uestc.shortlink.project.dto.resp.*;
import com.uestc.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import com.uestc.shortlink.project.service.ShortLinkService;
import com.uestc.shortlink.project.util.HashUtil;
import com.uestc.shortlink.project.util.LinkUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUrlCreateBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfig;
    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;

    @Value("${short-link.domain.default}")
    private String defaultDomain;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        // 0. 白名单验证
        verifyWhitelist(requestParam.getOriginUrl());
        // 1.加盐哈希算法生成短链接
        String suffix = generateSuffix(requestParam);
        String fullShortUrl = defaultDomain + "/" + suffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(defaultDomain)
                .shortUri(suffix)
                .fullShortUrl(fullShortUrl)
                .originUrl(requestParam.getOriginUrl())
                .clickNum(0)
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
        // 2. 插入 t_link, t_link_goto
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        } catch (DuplicateKeyException e) {
            log.warn("短链接{}重复入库", fullShortUrl);
            throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
        }
        // 3. 加入布隆过滤器，并根据过期时间预热缓存
        shortUrlCreateBloomFilter.add(fullShortUrl);
        // 缓存预热：创建短链接时直接写入 Redis
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

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<ShortLinkGroupCountResp> listGroupShortLinkCount(List<String> requestParam) {
        return baseMapper.listGroupShortLinkCount(requestParam);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 0. 白名单验证（仅在 originUrl 变更时验证）
        if (StrUtil.isNotBlank(requestParam.getOriginUrl())) {
            verifyWhitelist(requestParam.getOriginUrl());
        }
        // 1. 查询数据库中是否存在要修改的短链接
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getDelTime, 0L)
                .eq(ShortLinkDO::getEnableStatus, 1);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }

        // 2. 判断是否同组：新gid为空或与原gid相同时视为同组
        boolean sameGroup = !StringUtils.hasText(requestParam.getGid()) || Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid());
        if (sameGroup) {
            // gid相同，直接更新
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getDelTime, 0L)
                    .eq(ShortLinkDO::getEnableStatus, 1)
                    .set(Objects.nonNull(requestParam.getOriginUrl()), ShortLinkDO::getOriginUrl, requestParam.getOriginUrl())
                    .set(Objects.nonNull(requestParam.getValidDateType()), ShortLinkDO::getValidDateType, requestParam.getValidDateType())
                    .set(Objects.nonNull(requestParam.getValidDate()), ShortLinkDO::getValidDate, requestParam.getValidDate())
                    .set(Objects.nonNull(requestParam.getDescribe()), ShortLinkDO::getDescribe, requestParam.getDescribe())
                    .set(Objects.nonNull(requestParam.getFavicon()), ShortLinkDO::getFavicon, requestParam.getFavicon());
            baseMapper.update(null, updateWrapper);
        } else {
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            boolean writeLocked = rLock.tryLock();
            if (!writeLocked) {
                throw new ServiceException("短链接正在被访问，请稍后再试...");
            }
            try {
                // gid不同，需要删除原记录再新增（因为gid是分片键）
                // 3.1 软删除原记录
                LambdaUpdateWrapper<ShortLinkDO> deleteWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 1)
                        .set(ShortLinkDO::getDelFlag, 1)
                        .set(ShortLinkDO::getDelTime, System.currentTimeMillis());
                baseMapper.update(null, deleteWrapper);

                // 3.2 创建新记录
                ShortLinkDO newShortLinkDO = ShortLinkDO.builder()
                        .domain(hasShortLinkDO.getDomain())
                        .shortUri(hasShortLinkDO.getShortUri())
                        .fullShortUrl(hasShortLinkDO.getFullShortUrl())
                        .originUrl(requestParam.getOriginUrl() != null ? requestParam.getOriginUrl() : hasShortLinkDO.getOriginUrl())
                        .clickNum(hasShortLinkDO.getClickNum())
                        .gid(requestParam.getGid())  // 使用新的gid
                        .enableStatus(hasShortLinkDO.getEnableStatus())
                        .createdType(hasShortLinkDO.getCreatedType())
                        .validDateType(requestParam.getValidDateType() != null ? requestParam.getValidDateType() : hasShortLinkDO.getValidDateType())
                        .validDate(requestParam.getValidDate() != null ? requestParam.getValidDate() : hasShortLinkDO.getValidDate())
                        .describe(requestParam.getDescribe() != null ? requestParam.getDescribe() : hasShortLinkDO.getDescribe())
                        .favicon(requestParam.getFavicon() != null ? requestParam.getFavicon() : hasShortLinkDO.getFavicon())
                        .delTime(0L)
                        .build();
                baseMapper.insert(newShortLinkDO);
                // goto表进行了分表，因此需要删除原记录再新增
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, hasShortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());
                ShortLinkGotoDO newGotoDO = ShortLinkGotoDO.builder()
                        .fullShortUrl(requestParam.getFullShortUrl())
                        .gid(requestParam.getGid())
                        .build();
                shortLinkGotoMapper.insert(newGotoDO);
                // 下面的表没有进行分表，因此无需做删除后再插入的操作
                LambdaUpdateWrapper<LinkStatsTodayDO> linkStatsTodayUpdateWrapper = Wrappers.lambdaUpdate(LinkStatsTodayDO.class)
                        .eq(LinkStatsTodayDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkStatsTodayDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkStatsTodayDO::getDelFlag, 0);
                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder().gid(requestParam.getGid()).build();
                linkStatsTodayMapper.update(linkStatsTodayDO, linkStatsTodayUpdateWrapper);
                LambdaUpdateWrapper<LinkAccessStatsDO> linkAccessStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessStatsDO.class)
                        .eq(LinkAccessStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkAccessStatsDO::getDelFlag, 0);
                LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkLocaleStatsDO> linkLocaleStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkLocaleStatsDO.class)
                        .eq(LinkLocaleStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkLocaleStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkLocaleStatsDO::getDelFlag, 0);
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkLocaleStatsMapper.update(linkLocaleStatsDO, linkLocaleStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkOsStatsDO> linkOsStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkOsStatsDO.class)
                        .eq(LinkOsStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkOsStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkOsStatsDO::getDelFlag, 0);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkOsStatsMapper.update(linkOsStatsDO, linkOsStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkBrowserStatsDO> linkBrowserStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
                        .eq(LinkBrowserStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkBrowserStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkBrowserStatsDO::getDelFlag, 0);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkBrowserStatsMapper.update(linkBrowserStatsDO, linkBrowserStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkDeviceStatsDO> linkDeviceStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
                        .eq(LinkDeviceStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkDeviceStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkDeviceStatsDO::getDelFlag, 0);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkDeviceStatsMapper.update(linkDeviceStatsDO, linkDeviceStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkNetworkStatsDO> linkNetworkStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
                        .eq(LinkNetworkStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkNetworkStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkNetworkStatsDO::getDelFlag, 0);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkNetworkStatsMapper.update(linkNetworkStatsDO, linkNetworkStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkAccessLogsDO> linkAccessLogsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessLogsDO.class)
                        .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessLogsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkAccessLogsDO::getDelFlag, 0);
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessLogsMapper.update(linkAccessLogsDO, linkAccessLogsUpdateWrapper);
            } finally {
                rLock.unlock();
            }
        }

        // 当有效期类型、有效期时间或原始链接发生变化时，删除缓存以保证一致性
        if (!Objects.equals(hasShortLinkDO.getValidDateType(), requestParam.getValidDateType())
                || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())
                || (requestParam.getOriginUrl() != null && !Objects.equals(hasShortLinkDO.getOriginUrl(), requestParam.getOriginUrl()))) {
            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
            stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
        }
    }

    /**
     * 短链接跳转：用户只传短链接，不知道 gid。而 t_link 按 gid 分表，需先查路由表 t_link_goto 获取 gid，再查 t_link 分表。
     */
    @Override
    @SneakyThrows
    public void restoreLongLink(String shortUri, HttpServletRequest request, HttpServletResponse response) {
        String serverName = request.getServerName();
        String fullShortUrl = serverName + "/" + shortUri;
        String originalUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        // 根据短链接获取原始链接，若原始链接不为空，则直接返回；否则查询数据库
        if (StringUtils.hasText(originalUrl)) {
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(null, statsRecord);
            response.sendRedirect(originalUrl);
            return;
        }
        if (!shortUrlCreateBloomFilter.contains(fullShortUrl)) {
            response.sendRedirect("/page/notfound");
            return;
        }
        // 有空缓存，则直接返回
        if (StringUtils.hasText(stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl)))) {
            response.sendRedirect("/page/notfound");
            return;
        }

        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {
            // 二次检查是否有其他线程提前抢到redisson锁并建立好了缓存
            originalUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StringUtils.hasText(originalUrl)) {
                ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
                shortLinkStats(null, statsRecord);
                response.sendRedirect(originalUrl);
                return;
            }
            // 二次检查空值缓存，避免并发恶意请求重复查库
            if (StringUtils.hasText(stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl)))) {
                response.sendRedirect("/page/notfound");
                return;
            }

            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
            // 缓存空，解决缓存穿透
            if (shortLinkGotoDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "!!!", 30, TimeUnit.MINUTES);
                response.sendRedirect("/page/notfound");
                return;
            }
            String gid = shortLinkGotoDO.getGid();
            // 用 gid 定位分表，查 t_link 获取 origin_url
            LambdaQueryWrapper<ShortLinkDO> shortLinkDOLambdaQueryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, gid)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 1);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(shortLinkDOLambdaQueryWrapper);
            if (shortLinkDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "!!!", 30, TimeUnit.MINUTES);
                response.sendRedirect("/page/notfound");
                return;
            }
            // 处理已经过期的短链接
            if (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "!!!", 30, TimeUnit.MINUTES);
                response.sendRedirect("/page/notfound");
                return;
            }
            // 计算剩余有效期，回写缓存，跳转
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                    TimeUnit.MILLISECONDS
            );
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(gid, statsRecord);
            response.sendRedirect(shortLinkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }

    }

    @Override
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
                log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }

    @Override
    public void shortLinkStats(String gid, ShortLinkStatsRecordDTO statsRecord) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("gid", gid);
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);
    }

    /**
     * 构建统计记录，并设置用户cookie到response中
     *
     * @param fullShortUrl
     * @param request
     * @param response
     * @return
     */
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
                .build();
    }

    /**
     * 统计 UV（独立访客）
     * <pre>
     * 流程图：
     * ┌─────────────────────────────────────────┐
     * │           用户访问短链接                  │
     * └─────────────────────────────────────────┘
     *                     │
     *                     ▼
     *          ┌─────────────────────┐
     *          │  获取 Cookie 列表    │
     *          │  查找 "uv" Cookie    │
     *          └─────────────────────┘
     *                     │
     *        ┌────────────┴────────────┐
     *        │ 有                      │ 无
     *        ▼                         ▼
     * ┌─────────────┐         ┌─────────────────────┐
     * │ uvValue =   │         │ 生成新 UUID          │
     * │ cookie值    │         │ 创建 Cookie 并设置    │
     * └─────────────┘         │ path & maxAge       │
     *        │                │ 添加到 Response      │
     *        │                └─────────────────────┘
     *        │                         │
     *        └────────────┬────────────┘
     *                     ▼
     *          ┌─────────────────────┐
     *          │ Redis SADD(key, uv) │
     *          └─────────────────────┘
     *                     │
     *        ┌────────────┴────────────┐
     *        │ 返回 > 0               │ 返回 = 0
     *        ▼                         ▼
     *   ┌─────────┐              ┌─────────┐
     *   │ 新访客   │              │ 老访客   │
     *   │ return 1│              │ return 0│
     *   └─────────┘              └─────────┘
     * </pre>
     *
     * @return 1 表示新访客，0 表示老访客
     */
    private int verificationUv(String fullShortUrl, String uvValue) {
        String uvKey = String.format(SHORT_LINK_STATS_UV_KEY, fullShortUrl);
        Long added = stringRedisTemplate.opsForSet().add(uvKey, uvValue);
        return (added != null && added > 0) ? 1 : 0;
    }

    /**
     * 统计 UIP（独立 IP）
     * <p>
     * 使用 Redis Set 记录每个短链接的访问 IP，SADD 返回值判断是否为新 IP。
     *
     * @return 1 表示新 IP，0 表示已访问过的 IP
     */
    private int verificationUip(String fullShortUrl, String clientIp) {
        String uipKey = String.format(SHORT_LINK_STATS_UIP_KEY, fullShortUrl);
        Long added = stringRedisTemplate.opsForSet().add(uipKey, clientIp);
        return (added != null && added > 0) ? 1 : 0;
    }


    /**
     * 获取或创建 UV Cookie 值
     * <p>
     * 如果 Cookie 中已有 uv 值则直接返回，否则生成新的 UUID 并设置 Cookie
     *
     * @return uvValue
     */
    private String getOrCreateUvValue(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("uv".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        // 没有 uv Cookie 则创建新的
        String uvValue = UUID.randomUUID().toString();
        Cookie uvCookie = new Cookie("uv", uvValue);
        uvCookie.setMaxAge(60 * 60 * 24 * 30);  // 30 天有效期
        uvCookie.setPath(request.getRequestURI());
        response.addCookie(uvCookie);
        return uvValue;
    }


    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        int retryCount = 0;
        String shortUri;
        while (true) {
            if (retryCount > 10) {
                throw new RuntimeException("短链接频繁生成，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            // 加盐，让每次重试时的结果不同，否则是无意义重试
            originUrl += UUID.randomUUID().toString();
            shortUri = HashUtil.hashToBase62(originUrl);
            String fullShortUrl = defaultDomain + "/" + shortUri;
            if (!shortUrlCreateBloomFilter.contains(fullShortUrl)) {
                break;
            }
            retryCount++;
        }
        return shortUri;
    }

    /**
     * 获取网站 Favicon 图标地址
     * <p>
     * 发生网络异常（如 403、超时等）时返回 null
     *
     * @param url 目标网站 URL
     * @return Favicon URL，获取失败返回 null
     */
    private String getFavicon(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .timeout(5000)
                    .userAgent("Mozilla/5.0")  // 有些网站需要 UA
                    .get();

            // 按优先级查找
            String[] selectors = {
                    "link[rel='icon']",
                    "link[rel='shortcut icon']",
                    "link[rel~=(?i)^(shortcut )?icon]",
                    "link[rel='apple-touch-icon']"  // iOS 图标作为备选
            };

            for (String selector : selectors) {
                Element link = document.select(selector).first();
                if (link != null && link.hasAttr("href")) {
                    return link.attr("abs:href");
                }
            }

            // 最后尝试默认路径
            URL targetUrl = new URL(url);
            return targetUrl.getProtocol() + "://" + targetUrl.getHost() + "/favicon.ico";
        } catch (Exception e) {
            log.warn("获取 Favicon 失败: url={}, error={}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 验证目标 URL 域名是否在白名单中
     *
     * @param originUrl 目标 URL
     * @throws ClientException 域名不在白名单中时抛出异常
     */
    private void verifyWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfig.getEnable();
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("URL 格式不正确");
        }
        boolean allowed = gotoDomainWhiteListConfig.getDomains().stream()
                .anyMatch(domain::endsWith);
        if (!allowed) {
            throw new ClientException("该域名不在白名单中，支持的域名：" + gotoDomainWhiteListConfig.getNames());
        }
    }
}

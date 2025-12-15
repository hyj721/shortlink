package com.uestc.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.project.common.convention.exception.ClientException;
import com.uestc.shortlink.project.common.convention.exception.ServiceException;
import com.uestc.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.uestc.shortlink.project.dao.entity.LinkLocaleStatsDO;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.uestc.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.uestc.shortlink.project.dao.mapper.LinkLocaleStatsMapper;
import com.uestc.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.uestc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.uestc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkGroupCountResp;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
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
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.*;
import static com.uestc.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;


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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${short-link.stats.locale.amap-key}")
    private String amapKey;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        // 1.加盐哈希算法生成短链接
        String suffix = generateSuffix(requestParam);
        String fullShortUrl = requestParam.getDomain() + "/" + suffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
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
                String.format(GOTO_SHORT_SHORT_LINK_KEY, fullShortUrl),
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
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<ShortLinkGroupCountResp> listGroupShortLinkCount(List<String> requestParam) {
        return baseMapper.listGroupShortLinkCount(requestParam);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 1. 查询数据库中是否存在要修改的短链接
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
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
                    .eq(ShortLinkDO::getEnableStatus, 1)
                    .set(Objects.nonNull(requestParam.getOriginUrl()), ShortLinkDO::getOriginUrl, requestParam.getOriginUrl())
                    .set(Objects.nonNull(requestParam.getValidDateType()), ShortLinkDO::getValidDateType, requestParam.getValidDateType())
                    .set(Objects.nonNull(requestParam.getValidDate()), ShortLinkDO::getValidDate, requestParam.getValidDate())
                    .set(Objects.nonNull(requestParam.getDescribe()), ShortLinkDO::getDescribe, requestParam.getDescribe())
                    .set(Objects.nonNull(requestParam.getFavicon()), ShortLinkDO::getFavicon, requestParam.getFavicon());
            baseMapper.update(null, updateWrapper);
        } else {
            // gid不同，需要删除原记录再新增（因为gid是分片键）
            // 3.1 软删除原记录
            LambdaUpdateWrapper<ShortLinkDO> deleteWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 1)
                    .set(ShortLinkDO::getDelFlag, 1);
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
                    .build();
            baseMapper.insert(newShortLinkDO);
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
        String originalUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_SHORT_LINK_KEY, fullShortUrl));
        // 根据短链接获取原始链接，若原始链接不为空，则直接返回；否则查询数据库
        if (StringUtils.hasText(originalUrl)) {
            shortLinkStats(fullShortUrl, null, request, response);
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

        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {
            // 二次检查是否有其他线程提前抢到redisson锁并建立好了缓存
            originalUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_SHORT_LINK_KEY, fullShortUrl));
            if (StringUtils.hasText(originalUrl)) {
                shortLinkStats(fullShortUrl, null, request, response);
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
                    String.format(GOTO_SHORT_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                    TimeUnit.MILLISECONDS
            );
            shortLinkStats(fullShortUrl, gid, request, response);
            response.sendRedirect(shortLinkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }

    }

    private void shortLinkStats(String fullShortUrl, String gid,
                                HttpServletRequest request, HttpServletResponse response) {
        try {
            if (!StringUtils.hasText(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            // 统计 UV
            int uv = statsUv(fullShortUrl, request, response);
            // 统计 UIP
            String clientIp = LinkUtil.getClientIp(request);
            int uip = statsUip(fullShortUrl, clientIp);
            // 统计Locale
            statsLocale(fullShortUrl, gid, clientIp);
            // 获取当前时间信息
            LocalDateTime now = LocalDateTime.now();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(Date.from(now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .pv(1)
                    .uv(uv)
                    .uip(uip)
                    .hour(now.getHour())
                    .weekday(now.getDayOfWeek().getValue())
                    .build();
            linkAccessStatsMapper.shortLinkAccessStats(linkAccessStatsDO);
        } catch (Exception e) {
            log.error("短链接访问量统计异常", e);
        }
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
     * @return 1 表示新访客，0 表示老访客
     */
    private int statsUv(String fullShortUrl, HttpServletRequest request, HttpServletResponse response) {
        String uvKey = String.format(SHORT_LINK_STATS_UV_KEY, fullShortUrl);
        String uvValue = null;

        // 1. 尝试从 Cookie 中获取 uv 值
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("uv".equals(cookie.getName())) {
                    uvValue = cookie.getValue();
                    break;
                }
            }
        }

        // 2. 没有 uv Cookie 则创建新的
        if (uvValue == null) {
            uvValue = UUID.randomUUID().toString();
            Cookie uvCookie = new Cookie("uv", uvValue);
            uvCookie.setMaxAge(60 * 60 * 24 * 30);  // 30 天有效期
            uvCookie.setPath(request.getRequestURI());
            response.addCookie(uvCookie);
        }

        // 3. SADD 返回 > 0 表示新访客
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
    private int statsUip(String fullShortUrl, String clientIp) {
        String uipKey = String.format(SHORT_LINK_STATS_UIP_KEY, fullShortUrl);
        Long added = stringRedisTemplate.opsForSet().add(uipKey, clientIp);
        return (added != null && added > 0) ? 1 : 0;
    }

    private void statsLocale(String fullShortUrl, String gid, String clientIp) {
        Map<String, Object> requestParam = new HashMap<>();
        requestParam.put("key", amapKey);
        requestParam.put("ip", clientIp);
        String jsonLocaleResult = HttpUtil.get(AMAP_REMOTE_URL, requestParam);
        try {
            JsonNode rootNode = objectMapper.readTree(jsonLocaleResult);
            String infoCode = rootNode.path("infocode").asText();
            if (!"10000".equals(infoCode)) {
                log.warn("高德地图API调用失败: {}", jsonLocaleResult);
                return;
            }
            String province = rootNode.path("province").asText();
            // 判断省份是否为空，空值统一设置为 "unknown"
            boolean isLocaleInfoEmpty = !StringUtils.hasText(province);
            String actualProvince = isLocaleInfoEmpty ? "unknown" : province;
            String actualCity = isLocaleInfoEmpty ? "unknown" : rootNode.path("city").asText();
            String actualAdcode = isLocaleInfoEmpty ? "unknown" : rootNode.path("adcode").asText();
            // 国内地址，country 默认为 "中国"
            LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .cnt(1)
                    .province(actualProvince)
                    .city(actualCity)
                    .adcode(actualAdcode)
                    .country("中国")
                    .build();
            linkLocaleStatsMapper.shortLinkLocaleStats(linkLocaleStatsDO);
        } catch (Exception e) {
            log.error("解析高德地图API响应异常", e);
        }
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
            originUrl += System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            String fullShortUrl = requestParam.getDomain() + "/" + shortUri;
            if (!shortUrlCreateBloomFilter.contains(fullShortUrl)) {
                break;
            }
            retryCount++;
        }
        return shortUri;
    }

    @SneakyThrows
    private String getFavicon(String url) {
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
    }
}

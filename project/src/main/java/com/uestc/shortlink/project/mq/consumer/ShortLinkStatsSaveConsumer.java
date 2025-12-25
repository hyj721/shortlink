package com.uestc.shortlink.project.mq.consumer;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uestc.shortlink.project.dao.entity.*;
import com.uestc.shortlink.project.dao.mapper.*;
import com.uestc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.uestc.shortlink.project.mq.producer.DelayShortLinkStatsProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static com.uestc.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

@Component
@Slf4j
@RequiredArgsConstructor
public class ShortLinkStatsSaveConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final RedissonClient redissonClient;
    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final DelayShortLinkStatsProducer delayShortLinkStatsProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${short-link.stats.locale.amap-key}")
    private String amapKey;


    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String stream = message.getStream();
        RecordId id = message.getId();
        Map<String, String> producerMap = message.getValue();
        ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), ShortLinkStatsRecordDTO.class);
        String gid = producerMap.get("gid");

        actualSaveShortLinkStats(gid, statsRecord);
        stringRedisTemplate.opsForStream().delete(Objects.requireNonNull(stream), id.getValue());

    }

    public void actualSaveShortLinkStats(String gid, ShortLinkStatsRecordDTO statsRecord) {
        String fullShortUrl = statsRecord.getFullShortUrl();
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        boolean readLocked = rLock.tryLock();
        if (!readLocked) {
            // 未获取到锁，说明正在更新分组
            log.warn("短链接正在更新,统计任务稍后执行: {}", fullShortUrl);
            delayShortLinkStatsProducer.send(statsRecord);
            return;  // 不阻塞，直接返回让用户跳转
        }

        try {
            if (!StringUtils.hasText(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }

            String clientIp = statsRecord.getClientIp();
            String browser = statsRecord.getBrowser();
            String os = statsRecord.getOs();
            String device = statsRecord.getDevice();
            String network = statsRecord.getNetwork();
            String uvValue = statsRecord.getUv();
            Integer uvFirstFlag = statsRecord.getUvFirstFlag();
            Integer uipFirstFlag = statsRecord.getUipFirstFlag();

            Map<String, String> locale = getLocaleByIp(clientIp);
            LocalDateTime now = LocalDateTime.now();
            String localeInLog = locale != null && !"unknown".equals(locale.get("province"))
                    ? String.join("-", locale.get("country"), locale.get("province"), locale.get("city"))
                    : "unknown";
            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .user(uvValue)
                    .browser(browser)
                    .os(os)
                    .ip(clientIp)
                    .device(device)
                    .network(network)
                    .locale(localeInLog)
                    .build();

            // ==================== 统计入库 ====================
            // TODO BUG: uv/uip 是全量 Redis Key 的返回值（历史首次访问标志），
            //  但 LinkStatsTodayDO 需要的是"当天首次访问"标志。
            statsLocale(fullShortUrl, gid, locale);
            statsBrowser(fullShortUrl, gid, browser);
            statsOs(fullShortUrl, gid, os);
            statsDevice(fullShortUrl, gid, device);
            statsNetwork(fullShortUrl, gid, network);
            statsAccessLogs(linkAccessLogsDO);

            shortLinkMapper.incrementStats(gid, fullShortUrl, 1, uvFirstFlag, uipFirstFlag);
            LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                    .todayPv(1)
                    .todayUv(uvFirstFlag)   // TODO BUG: 应使用每日 UV 标志
                    .todayUip(uipFirstFlag) // TODO BUG: 应使用每日 UIP 标志
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);

            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(Date.from(now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .pv(1)
                    .uv(uvFirstFlag)
                    .uip(uipFirstFlag)
                    .hour(now.getHour())
                    .weekday(now.getDayOfWeek().getValue())
                    .build();
            linkAccessStatsMapper.shortLinkAccessStats(linkAccessStatsDO);
        } catch (Exception e) {
            log.error("短链接访问量统计异常", e);
        } finally {
            rLock.unlock();
        }
    }

    private void statsLocale(String fullShortUrl, String gid, Map<String, String> locale) {
        if (locale == null) {
            return;
        }
        LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(gid)
                .date(new Date())
                .cnt(1)
                .province(locale.get("province"))
                .city(locale.get("city"))
                .adcode(locale.get("adcode"))
                .country("中国")
                .build();
        linkLocaleStatsMapper.shortLinkLocaleStats(linkLocaleStatsDO);
    }

    /**
     * 根据 IP 查询地理位置信息
     * <p>
     * 调用高德地图 IP 定位 API，解析返回结果获取省份、城市、行政区划代码。
     *
     * @param clientIp 客户端 IP 地址
     * @return 包含 province/city/adcode 的 Map，失败时返回 null
     */
    private Map<String, String> getLocaleByIp(String clientIp) {
        Map<String, Object> requestParam = new HashMap<>();
        requestParam.put("key", amapKey);
        requestParam.put("ip", clientIp);
        String jsonLocaleResult = HttpUtil.get(AMAP_REMOTE_URL, requestParam);
        try {
            JsonNode rootNode = objectMapper.readTree(jsonLocaleResult);
            String infoCode = rootNode.path("infocode").asText();
            if (!"10000".equals(infoCode)) {
                log.warn("高德地图API调用失败: {}", jsonLocaleResult);
                return null;
            }
            String province = rootNode.path("province").asText();
            boolean isLocaleInfoEmpty = !StringUtils.hasText(province);
            Map<String, String> result = new HashMap<>();
            result.put("province", isLocaleInfoEmpty ? "unknown" : province);
            result.put("city", isLocaleInfoEmpty ? "unknown" : rootNode.path("city").asText());
            result.put("adcode", isLocaleInfoEmpty ? "unknown" : rootNode.path("adcode").asText());
            result.put("country", "中国");
            return result;
        } catch (Exception e) {
            log.error("解析高德地图API响应异常", e);
            return null;
        }
    }

    /**
     * 统计操作系统访问量
     */
    private void statsOs(String fullShortUrl, String gid, String os) {
        LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(gid)
                .date(new Date())
                .cnt(1)
                .os(os)
                .build();
        linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);
    }

    /**
     * 统计访问设备
     */
    private void statsDevice(String fullShortUrl, String gid, String device) {
        LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(gid)
                .date(new Date())
                .cnt(1)
                .device(device)
                .build();
        linkDeviceStatsMapper.shortLinkDeviceStats(linkDeviceStatsDO);
    }

    /**
     * 统计访问网络
     */
    private void statsNetwork(String fullShortUrl, String gid, String network) {
        LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(gid)
                .date(new Date())
                .cnt(1)
                .network(network)
                .build();
        linkNetworkStatsMapper.shortLinkNetworkStats(linkNetworkStatsDO);
    }

    /**
     * 统计浏览器访问量
     */
    private void statsBrowser(String fullShortUrl, String gid, String browser) {
        LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(gid)
                .date(new Date())
                .cnt(1)
                .browser(browser)
                .build();
        linkBrowserStatsMapper.shortLinkBrowserStats(linkBrowserStatsDO);
    }

    /**
     * 记录访问日志
     */
    private void statsAccessLogs(LinkAccessLogsDO linkAccessLogsDO) {
        linkAccessLogsMapper.insert(linkAccessLogsDO);
    }
}

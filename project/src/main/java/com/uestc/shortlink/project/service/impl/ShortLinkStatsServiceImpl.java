package com.uestc.shortlink.project.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.uestc.shortlink.project.dao.entity.LinkAccessDailyStatsDO;
import com.uestc.shortlink.project.dao.entity.LinkAccessLogsDO;
import com.uestc.shortlink.project.dao.entity.LinkAccessHourlyStatsDO;
import com.uestc.shortlink.project.dao.mapper.*;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.uestc.shortlink.project.dto.resp.*;
import com.uestc.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {

    private final LinkAccessDailyStatsMapper linkAccessDailyStatsMapper;
    private final LinkAccessHourlyStatsMapper linkAccessHourlyStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;

    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        // 1. 查询按天聚合的统计列表
        List<LinkAccessDailyStatsDO> dailyStatsByShortLink = linkAccessDailyStatsMapper.listDailyStatsByShortLink(requestParam);
        // 快速失败：如果没有数据，直接返回 null
        if (CollUtil.isEmpty(dailyStatsByShortLink)) {
            return null;
        }

        // 2. Java 层汇总 PV/UV/UIP
        int pv = 0;
        int uv = 0;
        int uip = 0;
        for (LinkAccessDailyStatsDO stat : dailyStatsByShortLink) {
            pv += Objects.requireNonNullElse(stat.getPvCnt(), 0);
            uv += Objects.requireNonNullElse(stat.getUvCnt(), 0);
            uip += Objects.requireNonNullElse(stat.getUipCnt(), 0);
        }

        // 3. 每日基础访问统计
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
        for (LinkAccessDailyStatsDO stat : dailyStatsByShortLink) {
            if (stat.getStatDate() == null) {
                continue;
            }
            daily.add(ShortLinkStatsAccessDailyRespDTO.builder()
                    .date(DateUtil.format(stat.getStatDate(), "yyyy-MM-dd"))
                    .pv(Objects.requireNonNullElse(stat.getPvCnt(), 0))
                    .uv(Objects.requireNonNullElse(stat.getUvCnt(), 0))
                    .uip(Objects.requireNonNullElse(stat.getUipCnt(), 0))
                    .build());
        }

        // 4. 24 小时访问分布 (下标 0-23)
        List<Integer> hourStats = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hourStats.add(0);
        }
        List<LinkAccessHourlyStatsDO> hourlyStatsByShortLink = linkAccessHourlyStatsMapper.listHourlyStatsByShortLink(requestParam);
        if (CollUtil.isNotEmpty(hourlyStatsByShortLink)) {
            for (LinkAccessHourlyStatsDO stat : hourlyStatsByShortLink) {
                int hour = Objects.requireNonNullElse(stat.getStatHour(), -1);
                if (hour < 0 || hour > 23) {
                    continue;
                }
                int hourPvCnt = Objects.requireNonNullElse(stat.getPvCnt(), 0);
                hourStats.set(hour, hourStats.get(hour) + hourPvCnt);
            }
        }

        // 5. 一周访问分布 (下标 0-6，对应周一到周日)，按日期动态推导，不在表中冗余存储 weekday
        List<Integer> weekdayStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekdayStats.add(0);
        }
        for (LinkAccessDailyStatsDO stat : dailyStatsByShortLink) {
            if (stat.getStatDate() == null) {
                continue;
            }
            int weekday = stat.getStatDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .getDayOfWeek()
                    .getValue() - 1;
            int dailyPvCnt = Objects.requireNonNullElse(stat.getPvCnt(), 0);
            weekdayStats.set(weekday, weekdayStats.get(weekday) + dailyPvCnt);
        }

        // 6. 地区统计 + 计算占比
        List<HashMap<String, Object>> localeStats = linkLocaleStatsMapper.listLocaleByShortLink(requestParam);
        List<ShortLinkStatsLocaleCNRespDTO> localeCnStats = new ArrayList<>();
        int localeTotalCnt = 0;
        for (HashMap<String, Object> stat : localeStats) {
            localeTotalCnt += ((Number) stat.get("cnt")).intValue();
        }
        for (HashMap<String, Object> stat : localeStats) {
            int cnt = ((Number) stat.get("cnt")).intValue();
            double ratio = localeTotalCnt == 0 ? 0 : (double) cnt / localeTotalCnt;
            localeCnStats.add(ShortLinkStatsLocaleCNRespDTO.builder()
                    .locale((String) stat.get("province"))
                    .cnt(cnt)
                    .ratio(ratio)
                    .build());
        }

        // 7. 浏览器统计 + 计算占比
        List<HashMap<String, Object>> browserStatsList = linkBrowserStatsMapper.listBrowserStatsByShortLink(requestParam);
        List<ShortLinkStatsBrowserRespDTO> browserStats = new ArrayList<>();
        int browserTotalCnt = 0;
        for (HashMap<String, Object> stat : browserStatsList) {
            browserTotalCnt += ((Number) stat.get("cnt")).intValue();
        }
        for (HashMap<String, Object> stat : browserStatsList) {
            int cnt = ((Number) stat.get("cnt")).intValue();
            double ratio = browserTotalCnt == 0 ? 0 : (double) cnt / browserTotalCnt;
            browserStats.add(ShortLinkStatsBrowserRespDTO.builder()
                    .browser((String) stat.get("browser"))
                    .cnt(cnt)
                    .ratio(ratio)
                    .build());
        }

        // 8. 操作系统统计 + 计算占比
        List<HashMap<String, Object>> osStatsList = linkOsStatsMapper.listOsStatsByShortLink(requestParam);
        List<ShortLinkStatsOsRespDTO> osStats = new ArrayList<>();
        int osTotalCnt = 0;
        for (HashMap<String, Object> stat : osStatsList) {
            osTotalCnt += ((Number) stat.get("cnt")).intValue();
        }
        for (HashMap<String, Object> stat : osStatsList) {
            int cnt = ((Number) stat.get("cnt")).intValue();
            double ratio = osTotalCnt == 0 ? 0 : (double) cnt / osTotalCnt;
            osStats.add(ShortLinkStatsOsRespDTO.builder()
                    .os((String) stat.get("os"))
                    .cnt(cnt)
                    .ratio(ratio)
                    .build());
        }

        // 9. 设备类型统计 + 计算占比
        List<HashMap<String, Object>> deviceStatsList = linkDeviceStatsMapper.listDeviceStatsByShortLink(requestParam);
        List<ShortLinkStatsDeviceRespDTO> deviceStats = new ArrayList<>();
        int deviceTotalCnt = 0;
        for (HashMap<String, Object> stat : deviceStatsList) {
            deviceTotalCnt += ((Number) stat.get("cnt")).intValue();
        }
        for (HashMap<String, Object> stat : deviceStatsList) {
            int cnt = ((Number) stat.get("cnt")).intValue();
            double ratio = deviceTotalCnt == 0 ? 0 : (double) cnt / deviceTotalCnt;
            deviceStats.add(ShortLinkStatsDeviceRespDTO.builder()
                    .device((String) stat.get("device"))
                    .cnt(cnt)
                    .ratio(ratio)
                    .build());
        }

        // 10. 网络类型统计 + 计算占比
        List<HashMap<String, Object>> networkStatsList = linkNetworkStatsMapper.listNetworkStatsByShortLink(requestParam);
        List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();
        int networkTotalCnt = 0;
        for (HashMap<String, Object> stat : networkStatsList) {
            networkTotalCnt += ((Number) stat.get("cnt")).intValue();
        }
        for (HashMap<String, Object> stat : networkStatsList) {
            int cnt = ((Number) stat.get("cnt")).intValue();
            double ratio = networkTotalCnt == 0 ? 0 : (double) cnt / networkTotalCnt;
            networkStats.add(ShortLinkStatsNetworkRespDTO.builder()
                    .network((String) stat.get("network"))
                    .cnt(cnt)
                    .ratio(ratio)
                    .build());
        }

        // 11. 高频 IP 统计 (Top 5)
        List<HashMap<String, Object>> topIpStatsList = linkAccessLogsMapper.listTopIpByShortLink(requestParam);
        List<ShortLinkStatsTopIpRespDTO> topIpStats = new ArrayList<>();
        for (HashMap<String, Object> stat : topIpStatsList) {
            topIpStats.add(ShortLinkStatsTopIpRespDTO.builder()
                    .ip((String) stat.get("ip"))
                    .cnt(((Number) stat.get("cnt")).intValue())
                    .build());
        }

        // 12. 新老访客统计 + 计算占比
        HashMap<String, Object> uvTypeCnt = linkAccessLogsMapper.findUvTypeCntByShortLink(requestParam);
        List<ShortLinkStatsUvRespDTO> uvTypeStats = new ArrayList<>();
        int oldUserCnt = uvTypeCnt == null ? 0 : ((Number) uvTypeCnt.getOrDefault("oldUserCnt", 0)).intValue();
        int newUserCnt = uvTypeCnt == null ? 0 : ((Number) uvTypeCnt.getOrDefault("newUserCnt", 0)).intValue();
        int uvTypeTotalCnt = oldUserCnt + newUserCnt;
        double oldUserRatio = uvTypeTotalCnt == 0 ? 0 : (double) oldUserCnt / uvTypeTotalCnt;
        double newUserRatio = uvTypeTotalCnt == 0 ? 0 : (double) newUserCnt / uvTypeTotalCnt;
        uvTypeStats.add(ShortLinkStatsUvRespDTO.builder()
                .uvType("oldUser")
                .cnt(oldUserCnt)
                .ratio(oldUserRatio)
                .build());
        uvTypeStats.add(ShortLinkStatsUvRespDTO.builder()
                .uvType("newUser")
                .cnt(newUserCnt)
                .ratio(newUserRatio)
                .build());

        // 13. 构建响应对象
        return ShortLinkStatsRespDTO.builder()
                .pv(pv)
                .uv(uv)
                .uip(uip)
                .daily(daily)
                .hourStats(hourStats)
                .weekdayStats(weekdayStats)
                .localeCnStats(localeCnStats)
                .browserStats(browserStats)
                .osStats(osStats)
                .deviceStats(deviceStats)
                .networkStats(networkStats)
                .topIpStats(topIpStats)
                .uvTypeStats(uvTypeStats)
                .build();
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        // 1. 分页查询访问日志基本数据
        LambdaQueryWrapper<LinkAccessLogsDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessLogsDO.class)
                .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(LinkAccessLogsDO::getGid, requestParam.getGid())
                .apply("DATE(create_time) BETWEEN {0} AND {1}", requestParam.getStartDate(), requestParam.getEndDate())
                .eq(LinkAccessLogsDO::getDelFlag, 0)
                .orderByDesc(LinkAccessLogsDO::getCreateTime);
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectPage(requestParam, queryWrapper);
        
        // 2. 转换为响应 DTO
        IPage<ShortLinkStatsAccessRecordRespDTO> result = linkAccessLogsDOIPage.convert(each -> ShortLinkStatsAccessRecordRespDTO.builder()
                .user(each.getUser())
                .ip(each.getIp())
                .browser(each.getBrowser())
                .os(each.getOs())
                .network(each.getNetwork())
                .device(each.getDevice())
                .locale(each.getLocale())
                .createTime(each.getCreateTime())
                .build()
        );
        
        // 3. 快速失败：如果没有数据，直接返回
        if (CollUtil.isEmpty(result.getRecords())) {
            return result;
        }
        
        // 4. 提取去重后的用户列表
        List<String> userList = result.getRecords().stream()
                .map(ShortLinkStatsAccessRecordRespDTO::getUser)
                .distinct()
                .toList();
        
        // 5. 批量查询用户访客类型
        List<HashMap<String, Object>> uvTypeList = linkAccessLogsMapper.selectUvTypeByUsers(
                requestParam.getGid(),
                requestParam.getFullShortUrl(),
                requestParam.getStartDate(),
                requestParam.getEndDate(),
                userList
        );
        
        // 6. 构建 user -> uvType 映射
        Map<String, String> userUvTypeMap = new HashMap<>();
        for (HashMap<String, Object> item : uvTypeList) {
            String user = (String) item.get("user");
            String uvType = (String) item.get("uvType");
            userUvTypeMap.put(user, uvType);
        }
        
        // 7. 填充 uvType 到结果集
        result.getRecords().forEach(record -> 
                record.setUvType(userUvTypeMap.getOrDefault(record.getUser(), "old"))
        );
        
        return result;
    }
}

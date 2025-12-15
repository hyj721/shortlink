package com.uestc.shortlink.project.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.uestc.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.uestc.shortlink.project.dao.mapper.*;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.uestc.shortlink.project.dto.resp.*;
import com.uestc.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {

    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;

    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        // 1. 查询基础访问统计列表
        List<LinkAccessStatsDO> listStatsByShortLink = linkAccessStatsMapper.listStatsByShortLink(requestParam);
        // 快速失败：如果没有数据，直接返回 null
        if (CollUtil.isEmpty(listStatsByShortLink)) {
            return null;
        }

        // 2. Java 层汇总 PV/UV/UIP
        int pv = 0;
        int uv = 0;
        int uip = 0;
        for (LinkAccessStatsDO stat : listStatsByShortLink) {
            pv += stat.getPv();
            uv += stat.getUv();
            uip += stat.getUip();
        }

        // 3. 每日基础访问统计 (按日期分组汇总)
        Map<String, int[]> dailyMap = new HashMap<>();
        for (LinkAccessStatsDO stat : listStatsByShortLink) {
            String dateKey = DateUtil.format(stat.getDate(), "yyyy-MM-dd");
            int[] values = dailyMap.getOrDefault(dateKey, new int[]{0, 0, 0});
            values[0] += stat.getPv();
            values[1] += stat.getUv();
            values[2] += stat.getUip();
            dailyMap.put(dateKey, values);
        }
        List<String> sortedDates = new ArrayList<>(dailyMap.keySet());
        sortedDates.sort(String::compareTo);
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
        for (String dateKey : sortedDates) {
            int[] values = dailyMap.get(dateKey);
            daily.add(ShortLinkStatsAccessDailyRespDTO.builder()
                    .date(dateKey)
                    .pv(values[0])
                    .uv(values[1])
                    .uip(values[2])
                    .build());
        }

        // 4. 24 小时访问分布 (下标 0-23)
        List<Integer> hourStats = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hourStats.add(0);
        }
        for (LinkAccessStatsDO stat : listStatsByShortLink) {
            int hour = stat.getHour();
            hourStats.set(hour, hourStats.get(hour) + stat.getPv());
        }

        // 5. 一周访问分布 (下标 0-6，对应周一到周日)
        List<Integer> weekdayStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekdayStats.add(0);
        }
        for (LinkAccessStatsDO stat : listStatsByShortLink) {
            int weekday = stat.getWeekday(); // weekday 字段存的是 1-7
            if (weekday >= 1 && weekday <= 7) {
                weekdayStats.set(weekday - 1, weekdayStats.get(weekday - 1) + stat.getPv());
            }
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
}

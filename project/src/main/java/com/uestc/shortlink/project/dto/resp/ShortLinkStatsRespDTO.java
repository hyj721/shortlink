package com.uestc.shortlink.project.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短链接监控统计响应参数")
public class ShortLinkStatsRespDTO {

    @Schema(description = "访问量(PV)")
    private Integer pv;

    @Schema(description = "独立访客数(UV)")
    private Integer uv;

    @Schema(description = "独立IP数")
    private Integer uip;

    @Schema(description = "每日基础访问统计列表")
    private List<ShortLinkStatsAccessDailyRespDTO> daily;

    @Schema(description = "地区访问统计列表（仅国内）")
    private List<ShortLinkStatsLocaleCNRespDTO> localeCnStats;

    @Schema(description = "24小时访问分布，数组下标0-23对应0点-23点的访问量")
    private List<Integer> hourStats;

    @Schema(description = "高频访问IP统计列表")
    private List<ShortLinkStatsTopIpRespDTO> topIpStats;

    @Schema(description = "一周访问分布，数组下标0-6对应周一到周日的访问量")
    private List<Integer> weekdayStats;

    @Schema(description = "浏览器访问统计列表")
    private List<ShortLinkStatsBrowserRespDTO> browserStats;

    @Schema(description = "操作系统访问统计列表")
    private List<ShortLinkStatsOsRespDTO> osStats;

    @Schema(description = "访客类型统计列表（新访客/老访客）")
    private List<ShortLinkStatsUvRespDTO> uvTypeStats;

    @Schema(description = "设备类型访问统计列表")
    private List<ShortLinkStatsDeviceRespDTO> deviceStats;

    @Schema(description = "网络类型访问统计列表")
    private List<ShortLinkStatsNetworkRespDTO> networkStats;
}

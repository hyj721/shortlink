package com.uestc.shortlink.project.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短链接基础访问监控响应参数")
public class ShortLinkStatsAccessDailyRespDTO {

    @Schema(description = "日期", example = "2025-12-15")
    private String date;

    @Schema(description = "访问量(PV)")
    private Integer pv;

    @Schema(description = "独立访客数(UV)")
    private Integer uv;

    @Schema(description = "独立IP数")
    private Integer uip;
}

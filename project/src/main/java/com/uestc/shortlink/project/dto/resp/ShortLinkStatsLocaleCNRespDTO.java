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
@Schema(description = "短链接地区监控响应参数（仅国内）")
public class ShortLinkStatsLocaleCNRespDTO {

    @Schema(description = "该地区的访问次数")
    private Integer cnt;

    @Schema(description = "地区名称", example = "北京")
    private String locale;

    @Schema(description = "占比", example = "0.25")
    private Double ratio;
}

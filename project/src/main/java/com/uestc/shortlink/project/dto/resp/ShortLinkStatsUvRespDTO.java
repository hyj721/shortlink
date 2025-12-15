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
@Schema(description = "短链接访客类型监控响应参数")
public class ShortLinkStatsUvRespDTO {

    @Schema(description = "该访客类型的访问次数")
    private Integer cnt;

    @Schema(description = "访客类型：0-老访客，1-新访客")
    private String uvType;

    @Schema(description = "占比", example = "0.55")
    private Double ratio;
}

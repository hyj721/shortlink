package com.uestc.shortlink.admin.remote.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短链接操作系统监控响应参数")
public class ShortLinkStatsOsRespDTO {

    @Schema(description = "该操作系统的访问次数")
    private Integer cnt;

    @Schema(description = "操作系统类型", example = "Windows")
    private String os;

    @Schema(description = "占比", example = "0.40")
    private Double ratio;
}

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
@Schema(description = "短链接访问设备监控响应参数")
public class ShortLinkStatsDeviceRespDTO {

    @Schema(description = "该设备类型的访问次数")
    private Integer cnt;

    @Schema(description = "设备类型", example = "Mobile")
    private String device;

    @Schema(description = "占比", example = "0.45")
    private Double ratio;
}

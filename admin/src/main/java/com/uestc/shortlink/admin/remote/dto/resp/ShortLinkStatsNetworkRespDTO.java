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
@Schema(description = "短链接访问网络监控响应参数")
public class ShortLinkStatsNetworkRespDTO {

    @Schema(description = "该网络类型的访问次数")
    private Integer cnt;

    @Schema(description = "网络类型", example = "WIFI")
    private String network;

    @Schema(description = "占比", example = "0.60")
    private Double ratio;
}

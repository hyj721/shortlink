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
@Schema(description = "短链接高频访问IP监控响应参数")
public class ShortLinkStatsTopIpRespDTO {

    @Schema(description = "该IP的访问次数")
    private Integer cnt;

    @Schema(description = "IP地址", example = "192.168.1.1")
    private String ip;
}

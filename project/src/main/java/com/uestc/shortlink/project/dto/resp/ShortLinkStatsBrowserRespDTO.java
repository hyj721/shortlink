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
@Schema(description = "短链接浏览器监控响应参数")
public class ShortLinkStatsBrowserRespDTO {

    @Schema(description = "该浏览器的访问次数")
    private Integer cnt;

    @Schema(description = "浏览器类型", example = "Chrome")
    private String browser;

    @Schema(description = "占比", example = "0.35")
    private Double ratio;
}

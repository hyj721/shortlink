package com.uestc.shortlink.admin.remote.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "短链接基础信息响应参数")
public class ShortLinkBaseInfoRespDTO {

    @Schema(description = "描述信息")
    private String describe;

    @Schema(description = "原始链接")
    private String originUrl;

    @Schema(description = "短链接")
    private String fullShortUrl;
}

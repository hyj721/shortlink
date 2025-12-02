package com.uestc.shortlink.admin.remote.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Schema(description = "短链创建响应参数")
@Builder
public class ShortLinkCreateRespDTO {

    @Schema(description = "短链接分组id")
    private String gid;

    @Schema(description = "原始链接")
    private String originUrl;

    @Schema(description = "短链接")
    private String fullShortUrl;

}

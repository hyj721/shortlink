package com.uestc.shortlink.project.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "回收站恢复请求参数")
public class RecycleBinRecoverReqDTO {

    @Schema(description = "分组id")
    private String gid;

    @Schema(description = "完整短链接")
    private String fullShortUrl;
}

package com.uestc.shortlink.admin.remote.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "回收站删除请求参数")
public class RecycleBinDeleteReqDTO {

    @Schema(description = "分组id")
    private String gid;

    @Schema(description = "完整短链接")
    private String fullShortUrl;
}

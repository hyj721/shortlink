package com.uestc.shortlink.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ShortLinkGroupUpdateReqDTO {

    @Schema(description = "分组标识")
    private String gid;

    @Schema(description = "分组名称", example = "修改名字后的分组名称")
    private String name;
}

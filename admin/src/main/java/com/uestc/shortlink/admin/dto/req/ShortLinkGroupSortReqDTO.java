package com.uestc.shortlink.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ShortLinkGroupSortReqDTO {

    @Schema(description = "分组标识")
    private String gid;

    @Schema(description = "排序")
    private Integer sortOrder;
}

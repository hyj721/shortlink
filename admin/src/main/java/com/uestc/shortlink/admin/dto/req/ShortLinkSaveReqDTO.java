package com.uestc.shortlink.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "新增短链分组请求参数")
@Data
public class ShortLinkSaveReqDTO {
    @Schema(description = "短链分组名称", example = "默认分组")
    private String name;
}

package com.uestc.shortlink.admin.remote.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Schema(description = "短链分页请求参数")
@Data
public class ShortLinkPageReqDTO extends Page {
    @Schema(description = "分组ID")
    private String gid;

    @Schema(description = "排序标识")
    private String orderTag;
}

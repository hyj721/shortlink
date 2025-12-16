package com.uestc.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Schema(description = "短链分页请求参数")
@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {
    @Schema(description = "分组ID")
    private String gid;

    @Schema(description = "排序字段")
    private String orderTag;
}

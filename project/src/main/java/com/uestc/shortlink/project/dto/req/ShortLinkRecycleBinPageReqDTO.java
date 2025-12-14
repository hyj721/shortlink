package com.uestc.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "分页查询回收站中的短链接请求参数")
public class ShortLinkRecycleBinPageReqDTO extends Page<ShortLinkDO> {

    @Schema(description = "用户的短链分组id列表")
    private List<String> gidList;
}

package com.uestc.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uestc.shortlink.project.dao.entity.LinkAccessLogsDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "短链接访问历史记录请求参数")
public class ShortLinkStatsAccessRecordReqDTO extends Page<LinkAccessLogsDO> {

    @Schema(description = "分组标识")
    private String gid;

    @Schema(description = "完整短链接")
    private String fullShortUrl;

    @Schema(description = "开始日期", example = "2025-12-01")
    private String startDate;

    @Schema(description = "结束日期", example = "2025-12-15")
    private String endDate;

}

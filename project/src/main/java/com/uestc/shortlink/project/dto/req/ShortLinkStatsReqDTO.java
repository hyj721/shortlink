package com.uestc.shortlink.project.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "短链接监控请求参数")
public class ShortLinkStatsReqDTO {

    @Schema(description = "分组id")
    private String gid;

    @Schema(description = "完整短链接")
    private String fullShortUrl;

    @Schema(description = "开始时间")
    private String startDate;

    @Schema(description = "结束时间")
    private String endDate;

}

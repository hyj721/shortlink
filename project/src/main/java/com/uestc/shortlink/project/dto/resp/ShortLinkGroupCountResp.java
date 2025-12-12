package com.uestc.shortlink.project.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Schema(description = "短链接分组内的短链接数量")
@Builder
public class ShortLinkGroupCountResp {

    @Schema(description = "短链接分组id")
    private String gid;

    @Schema(description = "短链接分组内的短链接数量")
    private Integer shortLinkCount;
}

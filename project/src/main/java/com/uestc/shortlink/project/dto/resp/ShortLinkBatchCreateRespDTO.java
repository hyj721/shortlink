package com.uestc.shortlink.project.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短链接批量创建响应对象")
public class ShortLinkBatchCreateRespDTO {

    @Schema(description = "成功数量")
    private Integer total;

    @Schema(description = "批量创建返回参数")
    private List<ShortLinkBaseInfoRespDTO> baseLinkInfos;
}

package com.uestc.shortlink.admin.remote.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Schema(description = "短链接批量创建请求对象")
@Data
public class ShortLinkBatchCreateReqDTO {

    @Schema(description = "原始链接列表")
    private List<String> originUrls;

    @Schema(description = "描述列表")
    private List<String> describes;

    @Schema(description = "分组ID")
    private String gid;

    @Schema(description = "创建类型 0：接口创建 1：控制台创建")
    private Integer createdType;

    @Schema(description = "有效期类型 0：永久 1：指定日期")
    private Integer validDateType;

    @Schema(description = "有效期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;
}
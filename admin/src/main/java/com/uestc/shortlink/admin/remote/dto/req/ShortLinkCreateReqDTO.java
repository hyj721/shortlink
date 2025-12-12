package com.uestc.shortlink.admin.remote.dto.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "短链创建请求参数")
public class ShortLinkCreateReqDTO {

    @Schema(description = "域名")
    private String domain;

    @Schema(description = "原始链接")
    private String originUrl;

    @Schema(description = "分组标识")
    private String gid;

    @Schema(description = "创建方式 0：用户创建 1：系统创建")
    private Integer createdType;

    @Schema(description = "有效期类型 0：永久 1：指定时间")
    private Integer validDateType;

    @Schema(description = "有效期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    @Schema(description = "描述")
    @TableField("`describe`")
    private String describe;

}

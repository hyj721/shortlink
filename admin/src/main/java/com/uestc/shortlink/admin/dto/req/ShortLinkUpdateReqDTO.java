package com.uestc.shortlink.admin.dto.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 短链接更新请求参数
 */
@Data
public class ShortLinkUpdateReqDTO {

    @Schema(description = "原始链接")
    private String originUrl;

    @Schema(description = "完整短链接")
    private String fullShortUrl;

    @Schema(description = "原始分组标识（用于查询）")
    private String originGid;

    @Schema(description = "新分组标识")
    private String gid;

    @Schema(description = "有效期类型 0：永久 1：指定时间")
    private Integer validDateType;

    @Schema(description = "有效期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    @Schema(description = "描述")
    @TableField("`describe`")
    private String describe;

    @Schema(description = "图标")
    private String favicon;
}

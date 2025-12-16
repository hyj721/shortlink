package com.uestc.shortlink.admin.remote.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "短链分页返回参数")
public class ShortLinkPageRespDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "域名")
    private String domain;

    @Schema(description = "短链接")
    private String shortUri;

    @Schema(description = "完整短链接")
    private String fullShortUrl;

    @Schema(description = "原始链接")
    private String originUrl;

    @Schema(description = "点击量")
    private Integer clickNum;

    @Schema(description = "分组标识")
    private String gid;

    @Schema(description = "有效期类型", example = "0", allowableValues = {"0", "1"})
    private Integer validDateType;

    @Schema(description = "有效期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "描述")
    private String describe;

    @Schema(description = "网站图标")
    private String favicon;

    @Schema(description = "历史PV")
    private Integer totalPv;

    @Schema(description = "今日PV")
    private Integer toDayPv;

    @Schema(description = "历史UV")
    private Integer totalUv;

    @Schema(description = "今日UV")
    private Integer toDayUv;

    @Schema(description = "历史UIP")
    private Integer totalUIp;

    @Schema(description = "今日UIP")
    private Integer toDayUIp;
}

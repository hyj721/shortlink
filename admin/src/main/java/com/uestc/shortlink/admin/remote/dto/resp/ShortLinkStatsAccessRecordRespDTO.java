package com.uestc.shortlink.admin.remote.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短链接历史访问记录响应参数")
public class ShortLinkStatsAccessRecordRespDTO {

    @Schema(description = "用户信息")
    private String user;

    @Schema(description = "ip")
    private String ip;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "访问网络")
    private String network;

    @Schema(description = "访问设备")
    private String device;

    @Schema(description = "地区")
    private String locale;

    @Schema(description = "访客类型")
    private String uvType;

    @Schema(description = "访问时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}

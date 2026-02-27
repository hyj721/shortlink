package com.uestc.shortlink.project.dto.biz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接统计实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkStatsRecordDTO {

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 访问用户IP
     */
    private String clientIp;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作设备
     */
    private String device;

    /**
     * 网络
     */
    private String network;

    /**
     * UV
     */
    private String uv;

    /**
     * UV访问标识
     */
    private Integer uvFirstFlag;

    /**
     * UIP访问标识
     */
    private Integer uipFirstFlag;

    /**
     * 当日 UV 访问标识
     */
    private Integer dailyUvFirstFlag;

    /**
     * 当日 UIP 访问标识
     */
    private Integer dailyUipFirstFlag;
}

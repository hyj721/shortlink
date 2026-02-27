package com.uestc.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.uestc.shortlink.project.common.database.BaseDO;
import lombok.*;

import java.util.Date;

/**
 * Hourly access statistics entity for short links.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_link_access_hourly_stats")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkAccessHourlyStatsDO extends BaseDO {

    /**
     * Primary key.
     */
    private Long id;

    /**
     * Group identifier.
     */
    private String gid;

    /**
     * Full short URL.
     */
    private String fullShortUrl;

    /**
     * Statistic date at 00:00:00.
     */
    private Date statDate;

    /**
     * Hour bucket, range is 0-23.
     */
    private Integer statHour;

    /**
     * Page view count.
     */
    private Integer pvCnt;
}

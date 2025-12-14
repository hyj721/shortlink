package com.uestc.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.uestc.shortlink.project.common.database.BaseDO;
import lombok.*;

import java.util.Date;

/**
 * 短链接访问统计实体
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_link_access_stats")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkAccessStatsDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 日期
     */
    private Date date;

    /**
     * 访问量
     */
    private Integer pv;

    /**
     * 独立访问数
     */
    private Integer uv;

    /**
     * 独立IP数
     */
    private Integer uip;

    /**
     * 小时
     */
    private Integer hour;

    /**
     * 星期
     */
    private Integer weekday;
}

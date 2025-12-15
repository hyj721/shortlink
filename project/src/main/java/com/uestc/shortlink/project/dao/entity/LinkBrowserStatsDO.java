package com.uestc.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.uestc.shortlink.project.common.database.BaseDO;
import lombok.*;

import java.util.Date;

/**
 * 短链接浏览器访问统计实体
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_link_browser_stats")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkBrowserStatsDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 日期
     */
    private Date date;

    /**
     * 访问量
     */
    private Integer cnt;

    /**
     * 浏览器
     */
    private String browser;
}

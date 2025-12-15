package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkBrowserStatsDO;
import org.apache.ibatis.annotations.Param;

/**
 * 短链接浏览器访问统计持久层
 */
public interface LinkBrowserStatsMapper extends BaseMapper<LinkBrowserStatsDO> {

    void shortLinkBrowserStats(@Param("linkBrowserStats") LinkBrowserStatsDO linkBrowserStatsDO);

}

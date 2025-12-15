package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkOsStatsDO;
import org.apache.ibatis.annotations.Param;

/**
 * 短链接操作系统访问统计持久层
 */
public interface LinkOsStatsMapper extends BaseMapper<LinkOsStatsDO> {

    void shortLinkOsStats(@Param("linkOsStats") LinkOsStatsDO linkOsStatsDO);

}

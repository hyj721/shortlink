package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkLocaleStatsDO;
import org.apache.ibatis.annotations.Param;

public interface LinkLocaleStatsMapper extends BaseMapper<LinkLocaleStatsDO> {
    void shortLinkLocaleStats(@Param("linkLocaleStats") LinkLocaleStatsDO linkLocaleStatsDO);
}

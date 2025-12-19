package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkStatsTodayDO;
import org.apache.ibatis.annotations.Param;

public interface LinkStatsTodayMapper extends BaseMapper<LinkStatsTodayDO> {

    /**
     * 记录今日统计数据
     */
    void shortLinkTodayState(@Param("linkTodayStats") LinkStatsTodayDO linkStatsTodayDO);
}

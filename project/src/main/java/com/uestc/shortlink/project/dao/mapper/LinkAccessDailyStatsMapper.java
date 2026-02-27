package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkAccessDailyStatsDO;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Persistence mapper for daily short-link access statistics.
 */
public interface LinkAccessDailyStatsMapper extends BaseMapper<LinkAccessDailyStatsDO> {

    /**
     * Upserts one daily statistics row.
     */
    void shortLinkAccessDailyStats(@Param("linkAccessDailyStats") LinkAccessDailyStatsDO linkAccessDailyStatsDO);

    /**
     * Queries daily statistics in the given date range.
     */
    List<LinkAccessDailyStatsDO> listDailyStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);
}

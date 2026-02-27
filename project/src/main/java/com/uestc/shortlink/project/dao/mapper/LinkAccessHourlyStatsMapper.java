package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkAccessHourlyStatsDO;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Persistence mapper for hourly short-link access statistics.
 */
public interface LinkAccessHourlyStatsMapper extends BaseMapper<LinkAccessHourlyStatsDO> {

    /**
     * Upserts one hourly statistics row.
     */
    void shortLinkAccessHourlyStats(@Param("linkAccessHourlyStats") LinkAccessHourlyStatsDO linkAccessHourlyStatsDO);

    /**
     * Queries hourly statistics in the given date range.
     */
    List<LinkAccessHourlyStatsDO> listHourlyStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);
}

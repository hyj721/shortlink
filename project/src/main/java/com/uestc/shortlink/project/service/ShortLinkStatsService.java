package com.uestc.shortlink.project.service;

import com.uestc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkStatsRespDTO;

public interface ShortLinkStatsService {
    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);
}

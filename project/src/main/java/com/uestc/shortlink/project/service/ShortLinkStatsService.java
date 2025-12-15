package com.uestc.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkStatsRespDTO;

public interface ShortLinkStatsService {
    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);

    IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam);
}

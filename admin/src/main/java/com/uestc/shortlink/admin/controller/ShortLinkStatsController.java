package com.uestc.shortlink.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.remote.ShortLinkActualRemoteService;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkStatsReqDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkStatsRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "短链接统计接口")
@RequestMapping("/api/short-link/admin/v1")
public class ShortLinkStatsController {

    private final ShortLinkActualRemoteService shortLinkActualRemoteService;

    @Operation(summary = "访问单个短链接指定时间内监控数据")
    @GetMapping("/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        return shortLinkActualRemoteService.oneShortLinkStats(requestParam);
    }

    @Operation(summary = "访问单个短链接指定时间段内访问记录")
    @GetMapping("/stats/access-record")
    public Result<Page<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return shortLinkActualRemoteService.shortLinkStatsAccessRecord(requestParam);
    }
}


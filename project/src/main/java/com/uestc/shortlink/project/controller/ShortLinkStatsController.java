package com.uestc.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.project.common.convention.result.Result;
import com.uestc.shortlink.project.common.convention.result.Results;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkStatsRespDTO;
import com.uestc.shortlink.project.service.ShortLinkStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/short-link/v1")
@RequiredArgsConstructor
@Tag(name = "短链接统计接口")
public class ShortLinkStatsController {

    private final ShortLinkStatsService shortLinkStatsService;

    @GetMapping("/stats")
    @Operation(summary = "访问单个短链接指定时间内监控数据")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        return Results.success(shortLinkStatsService.oneShortLinkStats(requestParam));
    }

    @Operation(summary = "访问单个短链接指定时间段内访问记录")
    @GetMapping("/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return Results.success(shortLinkStatsService.shortLinkStatsAccessRecord(requestParam));
    }
}

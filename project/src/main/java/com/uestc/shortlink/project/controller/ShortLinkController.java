package com.uestc.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.project.common.convention.result.Result;
import com.uestc.shortlink.project.common.convention.result.Results;
import com.uestc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkGroupCountResp;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.uestc.shortlink.project.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "短链接中台管理接口")
@RequestMapping("/api/short-link/v1")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @Operation(summary = "创建短链接")
    @PostMapping("/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return Results.success(shortLinkService.createShortLink(requestParam));
    }

    @Operation(summary = "分页查询短链接")
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return Results.success(shortLinkService.pageShortLink(requestParam));
    }

    @Operation(summary = "查询短链接分组内的短链接数量")
    @GetMapping("/count")
    public Result<List<ShortLinkGroupCountResp>> listGroupShortLinkCount(@RequestParam("requestParam") List<String> requestParam) {
        return Results.success(shortLinkService.listGroupShortLinkCount(requestParam));
    }
}

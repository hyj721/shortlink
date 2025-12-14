package com.uestc.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.project.common.convention.result.Result;
import com.uestc.shortlink.project.common.convention.result.Results;
import com.uestc.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.uestc.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.uestc.shortlink.project.service.RecycleBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/short-link/v1/recycle-bin")
@RequiredArgsConstructor
@Tag(name = "回收站管理接口")
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    @PostMapping("/save")
    @Operation(summary = "短链接移动至回收站")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
        recycleBinService.saveRecycleBin(requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询回收站短链接")
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        return Results.success(recycleBinService.pageShortLink(requestParam));
    }

    @Operation(summary = "恢复回收站短链接")
    @PostMapping("/recover")
    public Result<Void> recoverShortLink(@RequestBody RecycleBinRecoverReqDTO requestParam) {
        recycleBinService.recoverShortLink(requestParam);
        return Results.success();
    }

}

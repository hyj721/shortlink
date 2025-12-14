package com.uestc.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.common.convention.result.Results;
import com.uestc.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.uestc.shortlink.admin.remote.dto.req.RecycleBinRecoverReqDTO;
import com.uestc.shortlink.admin.remote.dto.req.RecycleBinSaveReqDTO;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.uestc.shortlink.admin.service.RecycleBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/short-link/admin/v1/recycle-bin")
@RequiredArgsConstructor
@Tag(name = "回收站管理接口")
public class RecycleBinController {

    /**
     * 后续重构为 SpringCloud Feign 调用
     */
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    private final RecycleBinService recycleBinService;

    /**
     * 保存回收站
     */
    @PostMapping("/save")
    @Operation(summary = "短链接移动至回收站")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
        shortLinkRemoteService.saveRecycleBin(requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询回收站短链接")
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        return recycleBinService.pageRecycleBinShortLink(requestParam);
    }

    @Operation(summary = "恢复回收站短链接")
    @PostMapping("/recover")
    public Result<Void> recoverShortLink(@RequestBody RecycleBinRecoverReqDTO requestParam) {
        shortLinkRemoteService.recoverRecycleBin(requestParam);
        return Results.success();
    }
}
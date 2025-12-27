package com.uestc.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.common.convention.result.Results;
import com.uestc.shortlink.admin.dto.req.ShortLinkUpdateReqDTO;
import com.uestc.shortlink.admin.remote.ShortLinkRemoteService;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkBatchCreateReqDTO;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkBaseInfoRespDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkBatchCreateRespDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.uestc.shortlink.admin.util.EasyExcelWebUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/short-link/admin/v1")
@Tag(name = "短链接后台管理接口")
public class ShortLinkController {
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    @Operation(summary = "创建短链接")
    @PostMapping("/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return shortLinkRemoteService.createShortLink(requestParam);
    }

    @SneakyThrows
    @PostMapping("/batch-create")
    public void batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam, HttpServletResponse response) {
        Result<ShortLinkBatchCreateRespDTO> shortLinkBatchCreateRespDTOResult = shortLinkRemoteService.batchCreateShortLink(requestParam);
        if (shortLinkBatchCreateRespDTOResult.isSuccess()) {
            List<ShortLinkBaseInfoRespDTO> baseLinkInfos = shortLinkBatchCreateRespDTOResult.getData().getBaseLinkInfos();
            EasyExcelWebUtil.write(response, "批量创建短链接-SaaS短链接系统", ShortLinkBaseInfoRespDTO.class, baseLinkInfos);
        }
    }

    @Operation(summary = "修改短链接")
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        shortLinkRemoteService.updateShortLink(requestParam);
        return Results.success();
    }


    @Operation(summary = "分页查询短链接")
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return shortLinkRemoteService.pageShortLink(requestParam);
    }
}

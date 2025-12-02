package com.uestc.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/short-link/admin/v1")
@Tag(name = "短链接管理")
public class ShortLinkController {
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    @Operation(summary = "创建短链接")
    @PostMapping("/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return shortLinkRemoteService.createShortLink(requestParam);
    }

    @Operation(summary = "分页查询短链接")
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return shortLinkRemoteService.pageShortLink(requestParam);
    }
}

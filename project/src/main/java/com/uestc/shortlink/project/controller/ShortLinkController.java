package com.uestc.shortlink.project.controller;

import com.uestc.shortlink.project.common.convention.result.Result;
import com.uestc.shortlink.project.common.convention.result.Results;
import com.uestc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.project.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "短链接管理接口")
@RequestMapping("/api/short-link/v1")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @Operation(summary = "创建短链接")
    @PostMapping("/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam) {
        return Results.success(shortLinkService.createShortLink(requestParam));
    }
}

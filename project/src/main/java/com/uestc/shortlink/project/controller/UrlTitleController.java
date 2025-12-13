package com.uestc.shortlink.project.controller;

import com.uestc.shortlink.project.common.convention.result.Result;
import com.uestc.shortlink.project.common.convention.result.Results;
import com.uestc.shortlink.project.service.UrlTitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * URL 标题获取控制器
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "URL 标题管理接口")
@RequestMapping("/api/short-link/v1")
public class UrlTitleController {

    private final UrlTitleService urlTitleService;

    @Operation(summary = "根据 URL 获取网站标题")
    @GetMapping("/title")
    public Result<String> getTitleByUrl(@RequestParam("url") String url) {
        return Results.success(urlTitleService.getTitleByUrl(url));
    }
}

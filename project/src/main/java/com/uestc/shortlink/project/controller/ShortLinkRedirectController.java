package com.uestc.shortlink.project.controller;

import com.uestc.shortlink.project.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链接跳转接口
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "短链接跳转接口")
public class ShortLinkRedirectController {

    private final ShortLinkService shortLinkService;

    /**
     * 短链接重定向
     *
     * 访问示例：
     *   https://surl.cn/3z7aHp
     */
    @Operation(
            summary = "短链接跳转",
            description = "根据短链接标识恢复并重定向到原始长链接"
    )
    @GetMapping("/{short-uri}")
    public void restoreLongLink(
            @PathVariable("short-uri") String shortUri,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        shortLinkService.restoreLongLink(shortUri, request, response);
    }
}
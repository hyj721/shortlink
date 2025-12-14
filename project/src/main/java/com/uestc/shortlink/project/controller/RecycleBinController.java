package com.uestc.shortlink.project.controller;

import com.uestc.shortlink.project.common.convention.result.Result;
import com.uestc.shortlink.project.common.convention.result.Results;
import com.uestc.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.uestc.shortlink.project.service.RecycleBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}

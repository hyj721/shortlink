package com.uestc.shortlink.admin.controller;

import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.common.convention.result.Results;
import com.uestc.shortlink.admin.dto.req.ShortLinkSaveReqDTO;
import com.uestc.shortlink.admin.service.GroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/short-link/v1")
@Tag(name = "短链接分组管理")
public class GroupController {
    private final GroupService groupService;

    @PostMapping("/group")
    public Result<Void> saveGroup(@RequestBody ShortLinkSaveReqDTO requestParam) {
        groupService.saveGroup(requestParam);
        return Results.success();
    }
}

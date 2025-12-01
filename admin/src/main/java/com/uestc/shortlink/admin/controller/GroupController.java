package com.uestc.shortlink.admin.controller;

import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.common.convention.result.Results;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.uestc.shortlink.admin.dto.res.ShortLinkGroupRespDTO;
import com.uestc.shortlink.admin.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/short-link/v1")
@Tag(name = "短链接分组管理")
public class GroupController {
    private final GroupService groupService;

    @PostMapping("/group")
    @Operation(summary = "新增短链接分组")
    public Result<Void> saveGroup(@RequestBody ShortLinkGroupSaveReqDTO requestParam) {
        groupService.saveGroup(requestParam);
        return Results.success();
    }

    @GetMapping("/group")
    @Operation(summary = "查询短链接分组集合")
    public Result<List<ShortLinkGroupRespDTO>> listGroup() {
        return Results.success(groupService.listGroup());
    }

    @PutMapping("/group")
    @Operation(summary = "修改短链接分组名")
    public Result<Void> updateGroup(@RequestBody ShortLinkGroupUpdateReqDTO requestParam) {
        groupService.updateGroup(requestParam);
        return Results.success();
    }

    @DeleteMapping("/group/{gid}")
    @Operation(summary = "删除短链接分组")
    public Result<Void> deleteGroup(@PathVariable String gid) {
        groupService.deleteGroup(gid);
        return Results.success();
    }

    @PostMapping("/group/sort")
    @Operation(summary = "排序短链接分组")
    public Result<Void> sortGroup(@RequestBody List<ShortLinkGroupSortReqDTO> requestParam) {
        groupService.sortGroup(requestParam);
        return Results.success();
    }
}

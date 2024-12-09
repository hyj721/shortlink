package edu.uestc.shortlink.admin.controller;

import edu.uestc.shortlink.admin.common.convention.result.Result;
import edu.uestc.shortlink.admin.common.convention.result.Results;
import edu.uestc.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import edu.uestc.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import edu.uestc.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import edu.uestc.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/api/short-link/v1/group")
    public Result<Void> save(@RequestBody ShortLinkGroupSaveReqDTO requestParam) {
        groupService.saveGroup(requestParam.getName());
        return Results.success();
    }

    @GetMapping("/api/short-link/v1/group")
    public Result<List<ShortLinkGroupRespDTO>> listGroup() {
        return Results.success(groupService.listGroup());
    }

    /**
     * 修改短链接分组名称
     */
    @PutMapping("/api/short-link/admin/v1/group")
    public Result<Void> updateGroup(@RequestBody ShortLinkGroupUpdateReqDTO requestParam) {
        groupService.updateGroup(requestParam);
        return Results.success();
    }
}
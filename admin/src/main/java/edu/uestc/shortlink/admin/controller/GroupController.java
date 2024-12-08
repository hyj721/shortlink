package edu.uestc.shortlink.admin.controller;

import edu.uestc.shortlink.admin.common.convention.result.Result;
import edu.uestc.shortlink.admin.common.convention.result.Results;
import edu.uestc.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import edu.uestc.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import edu.uestc.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
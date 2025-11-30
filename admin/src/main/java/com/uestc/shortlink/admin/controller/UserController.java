package com.uestc.shortlink.admin.controller;

import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.common.convention.result.Results;
import com.uestc.shortlink.admin.dto.res.UserRespDTO;
import com.uestc.shortlink.admin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shortlink/v1")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    @Operation(summary = "根据用户名查询用户信息")
    @GetMapping("/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable String username) {
        return Results.success(userService.getUserByUsername(username));
    }
}

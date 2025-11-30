package com.uestc.shortlink.admin.controller;

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

    @Operation(summary = "根据用户名查询用户信息")
    @GetMapping("/user/{username}")
    public String getUserByUsername(@PathVariable String username) {
        return "hello " + username;
    }
}

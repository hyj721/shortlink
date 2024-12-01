package edu.uestc.shortlink.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import edu.uestc.shortlink.admin.common.convention.result.Result;
import edu.uestc.shortlink.admin.common.convention.result.Results;
import edu.uestc.shortlink.admin.dto.resp.UserActualRespDTO;
import edu.uestc.shortlink.admin.dto.resp.UserRespDTO;
import edu.uestc.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/shortlink/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable String username) {
        return Results.success(userService.getUserByUsername(username));
    }

    /**
     * 根据用户名查询未脱敏用户信息
     */
    @GetMapping("/api/shortlink/v1/actual/user/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable String username) {
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
    }

    @GetMapping("/api/shortlink/v1/user/has-username")
    public Result<Boolean> hasUserName(@RequestParam("username") String username) {
        return Results.success(userService.hasUserName(username));
    }
}

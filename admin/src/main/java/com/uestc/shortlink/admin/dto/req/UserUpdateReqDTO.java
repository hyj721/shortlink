package com.uestc.shortlink.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户更新请求参数
 */
@Data
@Schema(description = "用户修改个人信息请求参数")
public class UserUpdateReqDTO {

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "lisi")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码", example = "123456")
    private String password;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名", example = "李思")
    private String realName;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "lisi@example.com")
    private String mail;
}

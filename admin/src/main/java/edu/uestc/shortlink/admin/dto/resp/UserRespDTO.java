package edu.uestc.shortlink.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.uestc.shortlink.admin.common.serialize.PhoneDesensitizationSerializer;
import lombok.Data;

/**
 * 用户响应
 */
@Data
public class UserRespDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;

    /**
     * 邮箱
     */
    private String mail;
}

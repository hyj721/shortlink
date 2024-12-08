package edu.uestc.shortlink.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import edu.uestc.shortlink.admin.common.database.BaseDO;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_user")
public class UserDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 注销时间戳
     */
    private Long deletionTime;

}
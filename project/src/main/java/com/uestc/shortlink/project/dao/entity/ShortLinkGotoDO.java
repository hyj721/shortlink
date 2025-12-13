package com.uestc.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("t_link_goto")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortLinkGotoDO {

    private String id;

    private String fullShortUrl;

    private String gid;

}

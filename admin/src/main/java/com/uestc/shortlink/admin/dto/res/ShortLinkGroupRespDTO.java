package com.uestc.shortlink.admin.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortLinkGroupRespDTO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组排序
     */
    private Integer sortOrder;

    /**
     * 分组内短链接的数量
     */
    private Integer shortLinkCount;
}

package com.uestc.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uestc.shortlink.admin.dao.entity.GroupDO;
import com.uestc.shortlink.admin.dto.req.ShortLinkSaveReqDTO;

public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     *
     */
    void saveGroup(ShortLinkSaveReqDTO requestParam);
}

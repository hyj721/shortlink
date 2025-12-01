package com.uestc.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uestc.shortlink.admin.dao.entity.GroupDO;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.uestc.shortlink.admin.dto.req.ShortLinkSaveReqDTO;
import com.uestc.shortlink.admin.dto.res.ShortLinkGroupRespDTO;

import java.util.List;

public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     *
     */
    void saveGroup(ShortLinkSaveReqDTO requestParam);

    /**
     * 查询用户短链接分组集合
     *
     */
    List<ShortLinkGroupRespDTO> listGroup();

    /**
     * 修改短链接分组名
     *
     */
    void updateGroup(ShortLinkGroupUpdateReqDTO requestParam);
}

package com.uestc.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uestc.shortlink.admin.dao.entity.GroupDO;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.uestc.shortlink.admin.dto.res.ShortLinkGroupRespDTO;

import java.util.List;

public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     */
    void saveGroup(ShortLinkGroupSaveReqDTO requestParam);

    /**
     * 新增短链接分组（指定用户名）
     *
     * @param username  用户名
     * @param groupName 分组名称
     */
    void saveGroup(String username, String groupName);

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

    /**
     * 删除短链接分组
     *
     */
    void deleteGroup(String gid);

    /**
     * 排序短链接分组
     *
     */
    void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam);
}

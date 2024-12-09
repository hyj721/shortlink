package edu.uestc.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.uestc.shortlink.admin.dao.entity.GroupDO;
import edu.uestc.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import edu.uestc.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;

import java.util.List;

/**
 * 短链接分组接口层
 */
public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     *
     * @param groupName 短链接分组名
     */
    void saveGroup(String groupName);

    /**
     * 查询用户短链接分组集合
     *
     * @return 用户短链接分组集合
     */
    List<ShortLinkGroupRespDTO> listGroup();

    /**
     * 修改短链接分组
     */
    void updateGroup(ShortLinkGroupUpdateReqDTO requestParam);
}

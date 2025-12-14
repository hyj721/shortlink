package com.uestc.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dto.req.RecycleBinSaveReqDTO;

public interface RecycleBinService extends IService<ShortLinkDO> {
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);
}

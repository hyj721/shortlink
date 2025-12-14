package com.uestc.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;

public interface RecycleBinService extends IService<ShortLinkDO> {
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);

    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);
}

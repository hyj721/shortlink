package com.uestc.shortlink.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

public interface RecycleBinService {
    Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam);
}

package com.uestc.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.uestc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkGroupCountResp;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides read-side queries for short-link resources.
 */
@Service
@RequiredArgsConstructor
public class ShortLinkQueryService {

    private final ShortLinkMapper shortLinkMapper;

    /**
     * Queries short-links with pagination.
     *
     * @param requestParam page query request
     * @return paged short-link view data
     */
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> resultPage = shortLinkMapper.pageLink(requestParam);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    /**
     * Counts active short-links grouped by gid.
     *
     * @param gidList group id list
     * @return count result by group
     */
    public List<ShortLinkGroupCountResp> listGroupShortLinkCount(List<String> gidList) {
        return shortLinkMapper.listGroupShortLinkCount(gidList);
    }
}

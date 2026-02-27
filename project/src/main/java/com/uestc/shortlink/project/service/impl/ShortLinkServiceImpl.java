package com.uestc.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.uestc.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkGroupCountResp;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.uestc.shortlink.project.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final ShortLinkCreateService shortLinkCreateService;
    private final ShortLinkQueryService shortLinkQueryService;
    private final ShortLinkRedirectService shortLinkRedirectService;
    private final ShortLinkUpdateService shortLinkUpdateService;
    private final ShortLinkWhitelistService shortLinkWhitelistService;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        return shortLinkCreateService.createShortLink(requestParam);
    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        return shortLinkCreateService.batchCreateShortLink(requestParam);
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        return shortLinkQueryService.pageShortLink(requestParam);
    }

    @Override
    public List<ShortLinkGroupCountResp> listGroupShortLinkCount(List<String> requestParam) {
        return shortLinkQueryService.listGroupShortLinkCount(requestParam);
    }

    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 0. 白名单验证（仅在 originUrl 变更时验证）
        if (StringUtils.hasText(requestParam.getOriginUrl())) {
            shortLinkWhitelistService.verifyOriginUrl(requestParam.getOriginUrl());
        }
        shortLinkUpdateService.updateShortLink(requestParam);
    }

    /**
     * Restores original URL and redirects by short URI.
     */
    @Override
    public void restoreLongLink(String shortUri, HttpServletRequest request, HttpServletResponse response) {
        shortLinkRedirectService.restoreLongLink(shortUri, request, response);
    }
}

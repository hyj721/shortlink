package com.uestc.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkGroupCountResp;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ShortLinkService extends IService<ShortLinkDO> {

    /**
     * 创建短链
     *
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     * 分页查询短链
     *
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * 获取短链分组内的短链接数量
     *
     */
    List<ShortLinkGroupCountResp> listGroupShortLinkCount(List<String> requestParam);

    /**
     * 修改短链接
     *
     */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    /**
     * 根据短链接恢复长链接
     *
     */
    void restoreLongLink(String shortUri, HttpServletRequest request, HttpServletResponse response);

    /**
     * 批量创建短链接
     *
     */
    ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam);

    /**
     * 短链接统计
     *
     */
    void shortLinkStats(String gid, ShortLinkStatsRecordDTO statsRecord);
}

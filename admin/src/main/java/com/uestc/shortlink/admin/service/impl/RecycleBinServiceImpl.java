package com.uestc.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.uestc.shortlink.admin.common.biz.user.UserContext;
import com.uestc.shortlink.admin.common.convention.exception.ServiceException;
import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.dao.entity.GroupDO;
import com.uestc.shortlink.admin.dao.mapper.GroupMapper;
import com.uestc.shortlink.admin.remote.ShortLinkRemoteService;
import com.uestc.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.uestc.shortlink.admin.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {

    /**
     * 后续重构为 SpringCloud Feign 调用
     */
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    private final GroupMapper groupMapper;

    @Override
    public Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0);
        List<GroupDO> groupDOS = groupMapper.selectList(queryWrapper);
        if (null == groupDOS || groupDOS.isEmpty()) {
            throw new ServiceException("用户无分组信息");
        }
        requestParam.setGidList(groupDOS.stream().map(GroupDO::getGid).toList());
        return shortLinkRemoteService.pageRecycleBinShortLink(requestParam);
    }
}

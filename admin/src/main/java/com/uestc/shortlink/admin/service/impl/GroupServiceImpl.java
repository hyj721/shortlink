package com.uestc.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.admin.common.biz.user.UserContext;
import com.uestc.shortlink.admin.common.convention.result.Result;
import com.uestc.shortlink.admin.dao.entity.GroupDO;
import com.uestc.shortlink.admin.dao.mapper.GroupMapper;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.uestc.shortlink.admin.dto.res.ShortLinkGroupRespDTO;
import com.uestc.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.uestc.shortlink.admin.remote.dto.resp.ShortLinkGroupCountResp;
import com.uestc.shortlink.admin.service.GroupService;
import com.uestc.shortlink.admin.util.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    @Override
    public void saveGroup(ShortLinkGroupSaveReqDTO requestParam) {
        saveGroup(UserContext.getUsername(), requestParam.getName());
    }

    @Override
    public void saveGroup(String username, String groupName) {
        String gid;
        do {
            gid = RandomGenerator.generateGid();
        } while (hasGid(username, gid));

        baseMapper.insert(GroupDO.builder()
                .gid(gid)
                .name(groupName)
                .username(username)
                .sortOrder(0)
                .build());
    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        // 1. 构建查询条件：当前用户未删除的分组，按排序和更新时间降序
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOS = baseMapper.selectList(queryWrapper);

        // 2. 调用远程服务获取每个分组的短链接数量
        Result<List<ShortLinkGroupCountResp>> listResult = shortLinkRemoteService
                .listGroupShortLinkCount(groupDOS.stream().map(GroupDO::getGid).toList());

        // 3. 组装返回结果，匹配每个分组对应的短链接数量
        List<ShortLinkGroupRespDTO> groupRespDTOS = groupDOS.stream()
                .map(groupDO -> {
                    // 根据 gid 匹配短链接数量，默认为 0
                    Integer shortLinkCount = 0;
                    if (listResult != null && listResult.getData() != null) {
                        shortLinkCount = listResult.getData().stream()
                                .filter(item -> item.getGid().equals(groupDO.getGid()))
                                .findFirst()
                                .map(ShortLinkGroupCountResp::getShortLinkCount)
                                .orElse(0);
                    }
                    return ShortLinkGroupRespDTO.builder()
                            .gid(groupDO.getGid())
                            .name(groupDO.getName())
                            .sortOrder(groupDO.getSortOrder())
                            .shortLinkCount(shortLinkCount)
                            .build();
                })
                .toList();
        return groupRespDTOS;
    }


    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0);
        GroupDO newGroup = GroupDO.builder()
                .name(requestParam.getName())
                .build();
        baseMapper.update(newGroup, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO newGroup = new GroupDO();
        newGroup.setDelFlag(1);
        baseMapper.update(newGroup, updateWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        requestParam.forEach(item -> {
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(item.getSortOrder())
                    .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, item.getGid())
                    .eq(GroupDO::getDelFlag, 0);
            baseMapper.update(groupDO, updateWrapper);
        });
    }

    private boolean hasGid(String username, String gid) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, username);
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag != null;
    }
}

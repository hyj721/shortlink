package com.uestc.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.admin.common.biz.user.UserContext;
import com.uestc.shortlink.admin.dao.entity.GroupDO;
import com.uestc.shortlink.admin.dao.mapper.GroupMapper;
import com.uestc.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.uestc.shortlink.admin.dto.req.ShortLinkSaveReqDTO;
import com.uestc.shortlink.admin.dto.res.ShortLinkGroupRespDTO;
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

    @Override
    public void saveGroup(ShortLinkSaveReqDTO requestParam) {
        String gid;
        do {
            gid = RandomGenerator.generateGid();
        } while (hasGid(gid));

        baseMapper.insert(GroupDO.builder()
                .gid(gid)
                .name(requestParam.getName())
                .username(UserContext.getUsername())
                .sortOrder(0)
                .build());
    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOS = baseMapper.selectList(queryWrapper);
        List<ShortLinkGroupRespDTO> groupRespDTOS = groupDOS.stream()
                .map(groupDO -> ShortLinkGroupRespDTO.builder()
                        .gid(groupDO.getGid())
                        .name(groupDO.getName())
                        .username(groupDO.getUsername())
                        .sortOrder(groupDO.getSortOrder())
                        .build())
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

    private boolean hasGid(String gid) {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, UserContext.getUsername());
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag != null;
    }
}

package com.uestc.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.admin.dao.entity.GroupDO;
import com.uestc.shortlink.admin.dao.mapper.GroupMapper;
import com.uestc.shortlink.admin.dto.req.ShortLinkSaveReqDTO;
import com.uestc.shortlink.admin.service.GroupService;
import com.uestc.shortlink.admin.util.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
                .sortOrder(0)
                .build());
    }

    private boolean hasGid(String gid) {
        // TODO queryWrapper设置用户名的查询条件
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, null);
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag != null;
    }
}

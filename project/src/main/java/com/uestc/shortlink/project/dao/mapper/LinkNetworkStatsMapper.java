package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkNetworkStatsDO;
import org.apache.ibatis.annotations.Param;

/**
 * 短链接访问网络统计持久层
 */
public interface LinkNetworkStatsMapper extends BaseMapper<LinkNetworkStatsDO> {

    void shortLinkNetworkStats(@Param("linkNetworkStats") LinkNetworkStatsDO linkNetworkStatsDO);

}

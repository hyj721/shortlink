package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkDeviceStatsDO;
import org.apache.ibatis.annotations.Param;

/**
 * 短链接访问设备统计持久层
 */
public interface LinkDeviceStatsMapper extends BaseMapper<LinkDeviceStatsDO> {

    void shortLinkDeviceStats(@Param("linkDeviceStats") LinkDeviceStatsDO linkDeviceStatsDO);

}

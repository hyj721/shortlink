package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.LinkAccessLogsDO;
import com.uestc.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;

/**
 * 短链接访问日志持久层
 */
public interface LinkAccessLogsMapper extends BaseMapper<LinkAccessLogsDO> {

    /**
     * 查询高频访问 IP Top N
     */
    List<HashMap<String, Object>> listTopIpByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 查询新老访客人数
     */
    HashMap<String, Object> findUvTypeCntByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);

    /**
     * 批量查询用户的访客类型（新访客/老访客）
     * 新访客：首次访问在查询范围内；老访客：首次访问在查询范围之前
     *
     * @param gid 分组标识
     * @param fullShortUrl 完整短链接
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param userList 用户列表
     * @return 用户-访客类型映射列表
     */
    List<HashMap<String, Object>> selectUvTypeByUsers(
            @Param("gid") String gid,
            @Param("fullShortUrl") String fullShortUrl,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("userList") List<String> userList
    );
}


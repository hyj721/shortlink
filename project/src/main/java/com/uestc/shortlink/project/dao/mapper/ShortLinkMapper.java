package com.uestc.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dto.resp.ShortLinkGroupCountResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {

    /**
     * 统计分组内的短链接数量
     */
    List<ShortLinkGroupCountResp> listGroupShortLinkCount(@Param("gidList") List<String> gidList);
}

package com.uestc.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dao.mapper.RecycleBinMapper;
import com.uestc.shortlink.project.dto.req.RecycleBinDeleteReqDTO;
import com.uestc.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.uestc.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.uestc.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY;
import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;

/**
 * 回收站管理接口实现层
 */
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<RecycleBinMapper, ShortLinkDO> implements RecycleBinService {


    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 保存短链接至回收站
     */
    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .set(ShortLinkDO::getEnableStatus, 0);  // 将启用状态设为禁用
        baseMapper.update(null, updateWrapper);
        stringRedisTemplate.delete(
                String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl())
        );
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .in(ShortLinkDO::getGid, requestParam.getGidList())
                .eq(ShortLinkDO::getEnableStatus, 0) // 禁用状态
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getUpdateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> {
            return BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
        });
    }

    @Override
    public void recoverShortLink(RecycleBinRecoverReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .set(ShortLinkDO::getEnableStatus, 1);  // 将启用状态设为启用
        baseMapper.update(null, updateWrapper);
        // 为了防止在短链接被删除至回收站的时候，有人访问该短链接，导致 Redis 缓存了空值以避免缓存穿透，这里需要把空值的缓存删除
        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
        // 从理论上说，从回收站恢复的短链接不会有很多人访问，因此可以考虑不用预热。如果有人访问了，有ShortLinkServiceImpl#restoreLongLink重建缓存解决。
    }

    @Override
    public void removeShortLink(RecycleBinDeleteReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)  // 只能删除回收站中的（禁用状态）
                .eq(ShortLinkDO::getDelFlag, 0)
                .set(ShortLinkDO::getDelFlag, 1);  // 软删除
        baseMapper.update(null, updateWrapper);
        // 清理缓存（虽然回收站中的链接可能没有缓存，但为保险起见还是清理）
        stringRedisTemplate.delete(
                String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl())
        );
        stringRedisTemplate.delete(
                String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl())
        );
    }
}

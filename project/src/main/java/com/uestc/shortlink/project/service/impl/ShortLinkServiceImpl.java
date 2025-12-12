package com.uestc.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uestc.shortlink.project.common.convention.exception.ClientException;
import com.uestc.shortlink.project.common.convention.exception.ServiceException;
import com.uestc.shortlink.project.dao.entity.ShortLinkDO;
import com.uestc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.uestc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.uestc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.uestc.shortlink.project.dto.resp.ShortLinkGroupCountResp;
import com.uestc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.uestc.shortlink.project.service.ShortLinkService;
import com.uestc.shortlink.project.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;


@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUrlCreateBloomFilter;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String suffix = generateSuffix(requestParam);
        String fullShortUrl = requestParam.getDomain() + "/" + suffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .shortUri(suffix)
                .fullShortUrl(fullShortUrl)
                .originUrl(requestParam.getOriginUrl())
                .clickNum(0)
                .gid(requestParam.getGid())
                .enableStatus(1)
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .describe(requestParam.getDescribe())
                .favicon(requestParam.getFavicon())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
        } catch (DuplicateKeyException e) {
            log.warn("短链接{}重复入库", fullShortUrl);
            throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
        }
        shortUrlCreateBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .fullShortUrl(fullShortUrl)
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<ShortLinkGroupCountResp> listGroupShortLinkCount(List<String> requestParam) {
        return baseMapper.listGroupShortLinkCount(requestParam);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 1. 查询数据库中是否存在要修改的短链接
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 1);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }

        // 2. 判断是否同组：新gid为空或与原gid相同时视为同组
        boolean sameGroup = !StringUtils.hasText(requestParam.getGid()) || Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid());
        if (sameGroup) {
            // gid相同，直接更新
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 1)
                    .set(Objects.nonNull(requestParam.getOriginUrl()), ShortLinkDO::getOriginUrl, requestParam.getOriginUrl())
                    .set(Objects.nonNull(requestParam.getValidDateType()), ShortLinkDO::getValidDateType, requestParam.getValidDateType())
                    .set(Objects.nonNull(requestParam.getValidDate()), ShortLinkDO::getValidDate, requestParam.getValidDate())
                    .set(Objects.nonNull(requestParam.getDescribe()), ShortLinkDO::getDescribe, requestParam.getDescribe())
                    .set(Objects.nonNull(requestParam.getFavicon()), ShortLinkDO::getFavicon, requestParam.getFavicon());
            baseMapper.update(null, updateWrapper);
        } else {
            // gid不同，需要删除原记录再新增（因为gid是分片键）
            // 3.1 软删除原记录
            LambdaUpdateWrapper<ShortLinkDO> deleteWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 1)
                    .set(ShortLinkDO::getDelFlag, 1);
            baseMapper.update(null, deleteWrapper);

            // 3.2 创建新记录
            ShortLinkDO newShortLinkDO = ShortLinkDO.builder()
                    .domain(hasShortLinkDO.getDomain())
                    .shortUri(hasShortLinkDO.getShortUri())
                    .fullShortUrl(hasShortLinkDO.getFullShortUrl())
                    .originUrl(requestParam.getOriginUrl() != null ? requestParam.getOriginUrl() : hasShortLinkDO.getOriginUrl())
                    .clickNum(hasShortLinkDO.getClickNum())
                    .gid(requestParam.getGid())  // 使用新的gid
                    .enableStatus(hasShortLinkDO.getEnableStatus())
                    .createdType(hasShortLinkDO.getCreatedType())
                    .validDateType(requestParam.getValidDateType() != null ? requestParam.getValidDateType() : hasShortLinkDO.getValidDateType())
                    .validDate(requestParam.getValidDate() != null ? requestParam.getValidDate() : hasShortLinkDO.getValidDate())
                    .describe(requestParam.getDescribe() != null ? requestParam.getDescribe() : hasShortLinkDO.getDescribe())
                    .favicon(requestParam.getFavicon() != null ? requestParam.getFavicon() : hasShortLinkDO.getFavicon())
                    .build();
            baseMapper.insert(newShortLinkDO);
        }
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        int retryCount = 0;
        String shortUri;
        while (true) {
            if (retryCount > 10) {
                throw new RuntimeException("短链接频繁生成，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            // 加盐，让每次重试时的结果不同，否则是无意义重试
            originUrl += System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            String fullShortUrl = requestParam.getDomain() + "/" + shortUri;
            if (!shortUrlCreateBloomFilter.contains(fullShortUrl)) {
                break;
            }
            retryCount++;
        }
        return shortUri;
    }
}

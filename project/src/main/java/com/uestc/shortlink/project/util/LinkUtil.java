package com.uestc.shortlink.project.util;

import java.util.Date;
import java.util.Optional;

import static com.uestc.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

/**
 * 短链接工具类
 */
public class LinkUtil {

    /**
     * 获取短链接缓存有效时间（毫秒）
     * <p>
     * 如果短链接设置了有效期，返回从当前时间到有效期的剩余时间；
     * 如果未指定有效期（validDate 为空），返回默认缓存时间。
     *
     * @param validDate 短链接有效期，可为 null（使用默认缓存时间）
     * @return 缓存有效时间（毫秒）
     */
    public static long getLinkCacheValidTime(Date validDate) {
        return Optional.ofNullable(validDate)
                .map(date -> {
                    long remainingTime = date.getTime() - System.currentTimeMillis();
                    return remainingTime > 0 ? remainingTime : 0L;
                })
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }
}

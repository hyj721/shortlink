package com.uestc.shortlink.project.common.constant;

import java.util.concurrent.TimeUnit;

/**
 * 短链接常量类
 */
public class ShortLinkConstant {

    /**
     * 永久短链接默认缓存有效时间
     */

    public static final long DEFAULT_CACHE_VALID_TIME = TimeUnit.DAYS.toMillis(30);
}

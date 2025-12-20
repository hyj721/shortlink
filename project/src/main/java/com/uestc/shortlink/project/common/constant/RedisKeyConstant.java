package com.uestc.shortlink.project.common.constant;

/**
 * Redis Key常量
 */
public class RedisKeyConstant {

    /**
     * 短链接跳转前缀key
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link_goto_%s";

    /**
     * 短链接跳转锁前缀key
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link_lock_goto_%s";

    /**
     * 短链接空值跳转前缀 Key
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link:is-null:goto_%s";

    /**
     * 短链接 UV 统计前缀 Key
     */
    public static final String SHORT_LINK_STATS_UV_KEY = "short-link:stats:uv:%s";

    /**
     * 短链接 UIP 统计前缀 Key
     */
    public static final String SHORT_LINK_STATS_UIP_KEY = "short-link:stats:uip:%s";

}

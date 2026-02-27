package com.uestc.shortlink.project.common.constant;

import java.util.concurrent.TimeUnit;

/**
 * Constants for short-link module.
 */
public class ShortLinkConstant {

    /**
     * Default cache TTL for permanent short-links.
     */

    public static final long DEFAULT_CACHE_VALID_TIME = TimeUnit.DAYS.toMillis(30);

    /**
     * AMap geolocation endpoint.
     */
    public static final String AMAP_REMOTE_URL = "https://restapi.amap.com/v3/ip";

    /**
     * Short-link not found page path.
     */
    public static final String NOT_FOUND_PAGE_PATH = "/page/notfound";

}

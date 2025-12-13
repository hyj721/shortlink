package com.uestc.shortlink.project.service.impl;

import com.uestc.shortlink.project.service.UrlTitleService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

/**
 * URL 标题服务实现
 */
@Slf4j
@Service
public class UrlTitleServiceImpl implements UrlTitleService {

    /**
     * 连接超时时间（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private static final int READ_TIMEOUT = 5000;

    @Override
    public String getTitleByUrl(String url) {
        if (url == null || url.isBlank()) {
            return "无效的 URL";
        }
        try {
            Document document = Jsoup.connect(url.trim())
                    .timeout(CONNECT_TIMEOUT)
                    .followRedirects(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get();
            return document.title();
        } catch (Exception e) {
            log.warn("获取网页标题失败, url: {}, error: {}", url, e.getMessage());
            return "Error while fetching title";
        }
    }
}

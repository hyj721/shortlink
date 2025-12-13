package com.uestc.shortlink.project.service;

/**
 * URL 标题服务接口
 */
public interface UrlTitleService {

    /**
     * 根据 URL 获取网站标题
     *
     * @param url 目标网站 URL
     * @return 网站标题
     */
    String getTitleByUrl(String url);
}

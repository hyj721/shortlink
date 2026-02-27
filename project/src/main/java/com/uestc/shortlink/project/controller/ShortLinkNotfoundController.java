
package com.uestc.shortlink.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.uestc.shortlink.project.common.constant.ShortLinkConstant.NOT_FOUND_PAGE_PATH;

/**
 * Provides the fallback page for missing short-links.
 */
@Controller
public class ShortLinkNotfoundController {

    /**
     * Renders not-found page.
     */
    @RequestMapping(NOT_FOUND_PAGE_PATH)
    public String notfound() {
        return "notfound";
    }
}

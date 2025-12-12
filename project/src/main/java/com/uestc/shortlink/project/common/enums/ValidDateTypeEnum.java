package com.uestc.shortlink.project.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 短链接有效期类型枚举
 */
@RequiredArgsConstructor
public enum ValidDateTypeEnum {

    PERMANENT(0),

    CUSTOM(1);

    @Getter
    private final Integer code;
}

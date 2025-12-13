package com.uestc.shortlink.project.common.annotation;

import java.lang.annotation.*;

/**
 * 有效日期一致性校验注解
 * <p>
 * 用于校验 validDateType 和 validDate 参数的一致性：
 * <ul>
 *     <li>validDateType = 0（永久有效）：自动将 validDate 清空为 null</li>
 *     <li>validDateType = 1（限时有效）：validDate 必须存在且晚于当前时间</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidDateConsistency {
}

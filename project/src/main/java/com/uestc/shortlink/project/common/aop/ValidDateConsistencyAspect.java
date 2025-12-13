package com.uestc.shortlink.project.common.aop;

import com.uestc.shortlink.project.common.annotation.ValidDateConsistency;
import com.uestc.shortlink.project.common.convention.exception.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * 有效日期一致性校验切面
 * <p>
 * 拦截带有 {@link ValidDateConsistency} 注解的方法，校验参数中的 validDateType 和 validDate 字段：
 * <ul>
 *     <li>validDateType = 0：自动将 validDate 清空为 null</li>
 *     <li>validDateType = 1：validDate 必须存在且晚于当前时间</li>
 * </ul>
 */
@Aspect
@Component
@Slf4j
public class ValidDateConsistencyAspect {

    private static final int VALID_DATE_TYPE_PERMANENT = 0;
    private static final int VALID_DATE_TYPE_CUSTOM = 1;

    @Around("@annotation(validDateConsistency)")
    public Object validateDateConsistency(ProceedingJoinPoint joinPoint,
                                          ValidDateConsistency validDateConsistency) throws Throwable {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            processValidDate(arg);
        }
        return joinPoint.proceed();
    }

    /**
     * 处理对象中的 validDateType 和 validDate 字段
     */
    private void processValidDate(Object obj) {
        Class<?> clazz = obj.getClass();
        Field validDateTypeField = getField(clazz, "validDateType");
        Field validDateField = getField(clazz, "validDate");

        // 如果对象不包含这两个字段，跳过处理
        if (validDateTypeField == null || validDateField == null) {
            return;
        }

        try {
            validDateTypeField.setAccessible(true);
            validDateField.setAccessible(true);

            Integer validDateType = (Integer) validDateTypeField.get(obj);
            Date validDate = (Date) validDateField.get(obj);

            if (validDateType == null) {
                return;
            }

            if (validDateType == VALID_DATE_TYPE_PERMANENT) {
                // 永久有效：清空 validDate
                validDateField.set(obj, null);
                log.debug("validDateType 为永久有效，已自动清空 validDate");
            } else if (validDateType == VALID_DATE_TYPE_CUSTOM) {
                // 限时有效：校验 validDate
                if (validDate == null) {
                    throw new ClientException("有效期类型为限时时，有效期不能为空");
                }
                if (validDate.before(new Date())) {
                    throw new ClientException("有效期必须晚于当前时间");
                }
            }
        } catch (IllegalAccessException e) {
            log.error("反射访问 validDateType/validDate 字段失败", e);
            throw new RuntimeException("参数校验失败", e);
        }
    }

    /**
     * 获取类中的字段（包括父类）
     */
    private Field getField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
}

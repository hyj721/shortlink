package com.uestc.shortlink.admin.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 随机字符串生成工具类
 */
public class RandomGenerator {

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random RANDOM = new SecureRandom();

    /**
     * 生成指定长度的随机字符串
     * 包含数字和英文字母
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return result.toString();
    }

    /**
     * 生成6位随机分组ID
     * 包含数字和英文字母
     *
     * @return 6位随机字符串
     */
    public static String generateGid() {
        return generateRandomString(6);
    }
}

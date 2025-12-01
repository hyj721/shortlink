package com.uestc.shortlink.project.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 */
@Configuration
public class RBloomFilterConfiguration {
    /**
     * 防止用户查询数据库的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> shortUrlCreateBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> shortUrlCreateBloomFilter = redissonClient.getBloomFilter("shortUrlCreateBloomFilter");
        // 预估插入数量，假阳性概率0.01
        shortUrlCreateBloomFilter.tryInit(100000000L, 0.001);
        return shortUrlCreateBloomFilter;
    }
}

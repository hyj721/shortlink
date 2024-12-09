package edu.uestc.shortlink.admin.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {

    @Value("${snowflake.workerId:1}") // 从配置文件中读取 workerId，默认值为 1
    private long workerId;

    @Value("${snowflake.dataCenterId:1}") // 从配置文件中读取 dataCenterId，默认值为 1
    private long dataCenterId;

    @Bean
    public Snowflake snowflake() {
        return IdUtil.getSnowflake(workerId, dataCenterId);
    }
}
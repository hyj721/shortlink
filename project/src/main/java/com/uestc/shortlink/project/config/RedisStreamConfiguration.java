package com.uestc.shortlink.project.config;


import com.uestc.shortlink.project.mq.consumer.ShortLinkStatsSaveConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis Stream 消息队列配置
 * <p>
 * 用于异步处理短链接统计数据，解耦统计写入与主业务流程
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfiguration {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ShortLinkStatsSaveConsumer shortLinkStatsSaveConsumer;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${spring.data.redis.stream.short-link-stats.topic}")
    private String topic;
    @Value("${spring.data.redis.stream.short-link-stats.group}")
    private String group;

    /**
     * Stream 消费者线程池
     * <p>
     * 核心线程数 = CPU 核心数，最大线程数 = 核心数 * 1.5
     */
    @Bean
    public ExecutorService asyncStreamConsumer() {
        AtomicInteger index = new AtomicInteger();
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                processors,
                processors + processors >> 1,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("stream_consumer_short-link_stats_" + index.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    /**
     * Redis Stream 消息监听容器
     * <p>
     * 负责从 Stream 中拉取消息并分发给消费者处理
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(ExecutorService asyncStreamConsumer) {
        // 初始化 Stream 和 Consumer Group
        initStreamGroup();

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .batchSize(10)                        // 每次拉取最多 10 条消息
                        .executor(asyncStreamConsumer)        // 使用自定义线程池处理消息
                        .pollTimeout(Duration.ofSeconds(3))   // 无消息时阻塞等待 3 秒（长轮询）
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        // 注册消费者：自动 ACK 模式，从上次消费位置继续读取
        container.receiveAutoAck(
                Consumer.from(group, "stats-consumer"),
                StreamOffset.create(topic, ReadOffset.lastConsumed()),
                shortLinkStatsSaveConsumer
        );

        return container;
    }

    private void initStreamGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(topic, group);
        } catch (Exception e) {
            // 如果 group 已存在会抛异常，忽略即可
            log.info("Consumer group '{}' already exists or stream not ready", group);
        }
    }
}

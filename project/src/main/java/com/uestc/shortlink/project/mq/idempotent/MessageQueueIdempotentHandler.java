package com.uestc.shortlink.project.mq.idempotent;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 消息队列幂等性处理器（两阶段状态机）
 * <p>
 * 状态流转: NULL → PROCESSING → COMPLETED
 *              ↓ (超时自动过期)
 *             NULL (允许重新消费)
 * <p>
 * 解决的问题：
 * 1. 业务执行失败 → 不标记 COMPLETED，允许 MQ 重试
 * 2. 进程意外崩溃（kill -9/断电）→ PROCESSING 状态超时过期，自动恢复消费能力
 */
@Component
@RequiredArgsConstructor
public class MessageQueueIdempotentHandler {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String IDEMPOTENT_KEY_PREFIX = "short-link:idempotent:";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    /**
     * 处理中状态的过期时间（分钟）
     * 如果进程在此时间内未完成处理（含崩溃场景），标记将自动过期，允许 MQ 重试
     */
    private static final long PROCESSING_TIMEOUT_MINUTES = 2;

    /**
     * 已完成状态的过期时间（分钟）
     * 用于防止短时间内的重复消费，过期后 key 自动清理
     */
    private static final long COMPLETED_EXPIRE_MINUTES = 60 * 24; // 24小时

    /**
     * 判断消息是否已经处理完成
     *
     * @param messageId 消息id
     * @return true：已完成，应跳过；false：未完成
     */
    public boolean isMessageCompleted(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        return STATUS_COMPLETED.equals(stringRedisTemplate.opsForValue().get(key));
    }

    /**
     * 尝试获取消息处理锁（标记为 PROCESSING）
     * <p>
     * 使用 setIfAbsent 保证并发安全，只有一个消费者能成功占坑
     *
     * @param messageId 消息id
     * @return true：获取成功，可以处理；false：获取失败（其他实例正在处理）
     */
    public boolean tryAcquireProcessingLock(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(
                        key,
                        STATUS_PROCESSING,
                        PROCESSING_TIMEOUT_MINUTES,
                        TimeUnit.MINUTES
                )
        );
    }

    /**
     * 标记消息处理完成
     * <p>
     * 将状态从 PROCESSING 更新为 COMPLETED，并设置较长的过期时间
     *
     * @param messageId 消息id
     */
    public void markAsCompleted(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        stringRedisTemplate.opsForValue().set(key, STATUS_COMPLETED, COMPLETED_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 释放处理锁（删除 PROCESSING 标记）
     * <p>
     * 当业务处理失败时调用，允许 MQ 重试消费该消息
     *
     * @param messageId 消息id
     */
    public void releaseProcessingLock(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        // 只删除 PROCESSING 状态，避免误删 COMPLETED
        String currentStatus = stringRedisTemplate.opsForValue().get(key);
        if (Objects.equals(STATUS_PROCESSING, currentStatus)) {
            stringRedisTemplate.delete(key);
        }
    }

}

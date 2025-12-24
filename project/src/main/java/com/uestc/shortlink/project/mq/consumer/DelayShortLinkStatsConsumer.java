package com.uestc.shortlink.project.mq.consumer;

import com.uestc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.uestc.shortlink.project.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

import static com.uestc.shortlink.project.common.constant.RedisKeyConstant.DELAY_QUEUE_STATS_KEY;

/**
 * 延迟记录短链接统计组件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DelayShortLinkStatsConsumer implements InitializingBean {

    private final RedissonClient redissonClient;
    private final ShortLinkService shortLinkService;

    public void onMessage() {
        Executors.newSingleThreadExecutor(
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("delay_short-link_stats_consumer");
                    thread.setDaemon(Boolean.TRUE);
                    return thread;
                })
                .execute(() -> {
                    RBlockingDeque<ShortLinkStatsRecordDTO> blockingDeque = redissonClient.getBlockingDeque(DELAY_QUEUE_STATS_KEY);
                    RDelayedQueue<ShortLinkStatsRecordDTO> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
                    for (; ; ) {
                        try {
                            ShortLinkStatsRecordDTO statsRecord = blockingDeque.take();
                            shortLinkService.shortLinkStats(null, statsRecord);
                        } catch (InterruptedException e) {
                            // 恢复中断状态或处理停机逻辑
                            Thread.currentThread().interrupt();
                        } catch (Exception e) {
                            log.error("处理统计数据失败", e);
                        }
                    }
                });
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        onMessage();
    }
}

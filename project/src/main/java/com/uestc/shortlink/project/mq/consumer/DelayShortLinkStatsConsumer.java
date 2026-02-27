package com.uestc.shortlink.project.mq.consumer;

import com.uestc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.uestc.shortlink.project.service.impl.ShortLinkStatsDispatchService;
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
 * Consumes delayed short-link statistics and dispatches them asynchronously.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DelayShortLinkStatsConsumer implements InitializingBean {

    private final RedissonClient redissonClient;
    private final ShortLinkStatsDispatchService shortLinkStatsDispatchService;

    /**
     * Starts background consumer for delayed stats queue.
     */
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
                            shortLinkStatsDispatchService.dispatch(null, statsRecord);
                        } catch (InterruptedException e) {
                            // 恢复中断状态，避免线程无界重试
                            Thread.currentThread().interrupt();
                        } catch (Exception e) {
                            log.error("Failed to process short-link stats record.", e);
                        }
                    }
                });
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        onMessage();
    }
}

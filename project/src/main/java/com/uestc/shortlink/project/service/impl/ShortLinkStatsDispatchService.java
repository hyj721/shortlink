package com.uestc.shortlink.project.service.impl;

import com.alibaba.fastjson.JSON;
import com.uestc.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.uestc.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatches short-link statistics records to asynchronous processing pipeline.
 */
@Service
@RequiredArgsConstructor
public class ShortLinkStatsDispatchService {

    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;

    /**
     * Publishes one short-link statistics event.
     *
     * @param gid group identifier, nullable when unknown
     * @param statsRecord statistics payload
     */
    public void dispatch(String gid, ShortLinkStatsRecordDTO statsRecord) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("gid", gid);
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);
    }
}

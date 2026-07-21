package com.demo.upimesh.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IngestionAlertWatcher {

    private static final Logger log = LoggerFactory.getLogger(IngestionAlertWatcher.class);
    private static final int INVALID_SPIKE_THRESHOLD = 5;

    @Autowired private IngestionMetrics metrics;

    @Scheduled(fixedDelay = 60_000)
    public void checkForInvalidSpike() {
        int invalid = metrics.getInvalidCount();
        if (invalid >= INVALID_SPIKE_THRESHOLD) {
            log.warn("ALERT: {} INVALID packets in the last minute — possible tampering, replay, or a misbehaving bridge node",
                    invalid);
        }
        metrics.reset();
    }
}
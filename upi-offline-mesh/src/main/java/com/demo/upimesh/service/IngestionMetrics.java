package com.demo.upimesh.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IngestionMetrics {
    private final AtomicInteger invalidCount = new AtomicInteger();
    private final AtomicInteger settledCount = new AtomicInteger();
    private final AtomicInteger rejectedCount = new AtomicInteger();
    private final AtomicInteger duplicateCount = new AtomicInteger();

    public void recordInvalid() { invalidCount.incrementAndGet(); }
    public void recordSettled() { settledCount.incrementAndGet(); }
    public void recordRejected() { rejectedCount.incrementAndGet(); }
    public void recordDuplicate() { duplicateCount.incrementAndGet(); }

    public int getInvalidCount() { return invalidCount.get(); }
    public int getSettledCount() { return settledCount.get(); }
    public int getRejectedCount() { return rejectedCount.get(); }
    public int getDuplicateCount() { return duplicateCount.get(); }

    public void reset() {
        invalidCount.set(0); settledCount.set(0); rejectedCount.set(0); duplicateCount.set(0);
    }
}
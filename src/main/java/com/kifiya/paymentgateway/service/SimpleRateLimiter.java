package com.kifiya.paymentgateway.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimpleRateLimiter implements RateLimiter {
    
    private static final int MAX_TPS = 2;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> windowStart = new AtomicReference<>(LocalDateTime.now());
    
    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }
    
    @Override
    public synchronized boolean tryAcquire(int permits) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentWindowStart = windowStart.get();
        
        // Reset counter if window has passed
        if (Duration.between(currentWindowStart, now).toSeconds() >= 1) {
            requestCount.set(0);
            windowStart.set(now);
        }
        
        // Check if we can allow the request
        if (requestCount.get() + permits <= MAX_TPS) {
            requestCount.addAndGet(permits);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void waitForPermit() throws InterruptedException {
        while (!tryAcquire()) {
            Thread.sleep(100);
        }
    }
}
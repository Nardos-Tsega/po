package com.kifiya.paymentgateway.service;

public interface RateLimiter {
    
    boolean tryAcquire();
    
    boolean tryAcquire(int permits);
    
    void waitForPermit() throws InterruptedException;
}
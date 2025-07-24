package com.kifiya.paymentgateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleRateLimiterTest {

    private SimpleRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new SimpleRateLimiter();
    }

    @Test
    void tryAcquire_WithinLimit_ReturnsTrue() {
        // When & Then - Should allow first 2 requests (2 TPS limit)
        assertTrue(rateLimiter.tryAcquire(), "First request should be allowed");
        assertTrue(rateLimiter.tryAcquire(), "Second request should be allowed");
    }

    @Test
    void tryAcquire_ExceedsLimit_ReturnsFalse() {
        // Given - Use up the 2 TPS limit
        assertTrue(rateLimiter.tryAcquire(), "First request should be allowed");
        assertTrue(rateLimiter.tryAcquire(), "Second request should be allowed");

        // When & Then - Third request should be denied
        assertFalse(rateLimiter.tryAcquire(), "Third request should be denied (exceeds 2 TPS limit)");
    }

    @Test
    void tryAcquire_SinglePermit_DefaultBehavior() {
        // When & Then - tryAcquire() should be equivalent to tryAcquire(1)
        assertTrue(rateLimiter.tryAcquire(), "Single permit should be allowed");
        assertTrue(rateLimiter.tryAcquire(1), "Single permit with parameter should be allowed");
        assertFalse(rateLimiter.tryAcquire(), "Third single permit should be denied");
    }

    @Test
    void tryAcquire_MultiplePermits_WorksCorrectly() {
        // When & Then - Should be able to acquire 2 permits at once
        assertTrue(rateLimiter.tryAcquire(2), "Should be able to acquire 2 permits (full limit)");
        assertFalse(rateLimiter.tryAcquire(1), "Should not be able to acquire any more permits");
    }

    @Test
    void tryAcquire_ExcessivePermits_ReturnsFalse() {
        // When & Then - Requesting more than the limit should fail immediately
        assertFalse(rateLimiter.tryAcquire(3), "Should not be able to acquire 3 permits (exceeds 2 TPS limit)");
        
        // Should still be able to use the full limit after the failed request
        assertTrue(rateLimiter.tryAcquire(2), "Should still be able to acquire 2 permits after failed excessive request");
    }

    @Test
    void tryAcquire_AfterTimeReset_AllowsNewRequests() throws InterruptedException {
        // Given - Use up the current window
        assertTrue(rateLimiter.tryAcquire(), "First request should be allowed");
        assertTrue(rateLimiter.tryAcquire(), "Second request should be allowed");
        assertFalse(rateLimiter.tryAcquire(), "Third request should be denied");

        // When - Wait for window to reset (1+ seconds)
        Thread.sleep(1100);

        // Then - Should allow new requests in the new window
        assertTrue(rateLimiter.tryAcquire(), "Should allow new request after window reset");
        assertTrue(rateLimiter.tryAcquire(), "Should allow second request in new window");
    }

    @Test
    void tryAcquire_PartialWindowReset_StillDenied() throws InterruptedException {
        // Given - Use up the limit
        assertTrue(rateLimiter.tryAcquire(), "First request should be allowed");
        assertTrue(rateLimiter.tryAcquire(), "Second request should be allowed");
        assertFalse(rateLimiter.tryAcquire(), "Third request should be denied");

        // When - Wait less than the reset time (under 1 second)
        Thread.sleep(500);

        // Then - Should still be denied
        assertFalse(rateLimiter.tryAcquire(), "Should still be denied before window reset");
    }

    @Test
    void waitForPermit_EventuallySucceeds() throws InterruptedException {
        // Given - Use up the current limit
        assertTrue(rateLimiter.tryAcquire(), "First request should be allowed");
        assertTrue(rateLimiter.tryAcquire(), "Second request should be allowed");

        // When - Wait for permit (should succeed after window reset)
        long startTime = System.currentTimeMillis();
        rateLimiter.waitForPermit();
        long endTime = System.currentTimeMillis();
        long waitTime = endTime - startTime;

        // Then - Should have waited approximately 1 second and then succeeded
        assertTrue(waitTime >= 900, "Should have waited at least 900ms for window reset");
        assertTrue(waitTime <= 2000, "Should not have waited more than 2 seconds");
        
        // After waitForPermit() succeeds, should be able to acquire again immediately
        assertTrue(rateLimiter.tryAcquire(), "Should be able to acquire immediately after waitForPermit()");
    }

    @Test
    void waitForPermit_WithAvailablePermit_ReturnsImmediately() throws InterruptedException {
        // Given - Don't use up the limit
        assertTrue(rateLimiter.tryAcquire(), "First request should be allowed");
        // One permit still available

        // When - Wait for permit when one is available
        long startTime = System.currentTimeMillis();
        rateLimiter.waitForPermit();
        long endTime = System.currentTimeMillis();
        long waitTime = endTime - startTime;

        // Then - Should return immediately (within a reasonable time)
        assertTrue(waitTime <= 200, "Should return immediately when permit is available, took: " + waitTime + "ms");
    }

    @Test
    void rateLimiter_ConsecutiveWindows_IndependentLimits() throws InterruptedException {
        // First window
        assertTrue(rateLimiter.tryAcquire(2), "Should acquire 2 permits in first window");
        assertFalse(rateLimiter.tryAcquire(), "Should be at limit in first window");

        // Wait for next window
        Thread.sleep(1100);

        // Second window - should have fresh limits
        assertTrue(rateLimiter.tryAcquire(2), "Should acquire 2 permits in second window");
        assertFalse(rateLimiter.tryAcquire(), "Should be at limit in second window");

        // Wait for third window
        Thread.sleep(1100);

        // Third window - should have fresh limits again
        assertTrue(rateLimiter.tryAcquire(), "Should acquire permit in third window");
    }

    @Test
    void rateLimiter_ZeroPermits_NoEffect() {
        // When & Then - Acquiring 0 permits should always succeed and not affect the counter
        assertTrue(rateLimiter.tryAcquire(0), "Acquiring 0 permits should succeed");
        assertTrue(rateLimiter.tryAcquire(2), "Should still be able to acquire full limit after 0-permit request");
        assertFalse(rateLimiter.tryAcquire(), "Should be at limit after acquiring 2 permits");
    }
}
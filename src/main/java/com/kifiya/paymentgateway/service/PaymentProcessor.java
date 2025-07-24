package com.kifiya.paymentgateway.service;

import com.kifiya.paymentgateway.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessor.class);
    
    private final PaymentService paymentService;
    private final RateLimiter rateLimiter;
    
    public PaymentProcessor(PaymentService paymentService, RateLimiter rateLimiter) {
        this.paymentService = paymentService;
        this.rateLimiter = rateLimiter;
    }
    
    @Scheduled(fixedDelay = 5000) // Process every 5 seconds
    public void processPendingPayments() {
        List<Payment> pendingPayments = paymentService.getPendingPayments();
        
        if (!pendingPayments.isEmpty()) {
            logger.info("Found {} pending payments to process", pendingPayments.size());
            
            for (Payment payment : pendingPayments) {
                // Check rate limit before processing
                if (rateLimiter.tryAcquire()) {
                    logger.info("Processing payment: {}", payment.getId());
                    paymentService.processPaymentWithProvider(payment.getId());
                } else {
                    logger.debug("Rate limit exceeded, payment {} will be processed later", payment.getId());
                    break; // Stop processing if rate limit is hit
                }
            }
        }
    }
    
    // Method to trigger immediate processing (for testing)
    public void processPaymentImmediately(java.util.UUID paymentId) {
        if (rateLimiter.tryAcquire()) {
            paymentService.processPaymentWithProvider(paymentId);
        } else {
            logger.info("Rate limit exceeded, payment {} queued for later processing", paymentId);
        }
    }
}
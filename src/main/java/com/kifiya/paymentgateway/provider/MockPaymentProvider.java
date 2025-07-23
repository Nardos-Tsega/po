package com.kifiya.paymentgateway.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MockPaymentProvider implements PaymentProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(MockPaymentProvider.class);
    private static final AtomicLong transactionCounter = new AtomicLong(0);
    private final Random random = new Random();
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        logger.info("MockProvider processing payment: {} for amount: {} {}", 
                   request.transactionId(), request.amount(), request.currency());
        
        // Simulate processing time (50-200ms)
        try {
            Thread.sleep(50 + random.nextInt(150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PaymentResult(false, null, "Processing interrupted", true);
        }
        
        // Simulate different outcomes based on probability
        int outcome = random.nextInt(100);
        
        if (outcome < 5) { // 5% transient failures (retryable)
            logger.warn("Transient failure for payment: {}", request.transactionId());
            return new PaymentResult(false, null, "Temporary service unavailable", true);
        } else if (outcome < 8) { // 3% permanent failures (not retryable)
            logger.error("Permanent failure for payment: {}", request.transactionId());
            return new PaymentResult(false, null, "Card declined", false);
        } else { // 92% success
            String providerTxId = "MOCK_TXN_" + transactionCounter.incrementAndGet();
            logger.info("Payment successful: {} -> {}", request.transactionId(), providerTxId);
            return new PaymentResult(true, providerTxId, null, false);
        }
    }
    
    @Override
    public String getProviderName() {
        return "MockPaymentProvider";
    }
}
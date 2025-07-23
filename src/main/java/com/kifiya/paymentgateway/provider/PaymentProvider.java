package com.kifiya.paymentgateway.provider;

import java.math.BigDecimal;

public interface PaymentProvider {
    
    PaymentResult processPayment(PaymentRequest request);
    
    String getProviderName();
    
    record PaymentRequest(
        String transactionId,
        BigDecimal amount,
        String currency,
        String merchantId,
        String customerId,
        String description
    ) {}
    
    record PaymentResult(
        boolean success,
        String providerTransactionId,
        String errorMessage,
        boolean isRetryable
    ) {}
}
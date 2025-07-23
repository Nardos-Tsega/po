package com.kifiya.paymentgateway.service;

import com.kifiya.paymentgateway.domain.Payment;
import com.kifiya.paymentgateway.domain.PaymentStatus;
import com.kifiya.paymentgateway.exception.DuplicatePaymentException;
import com.kifiya.paymentgateway.exception.PaymentNotFoundException;
import com.kifiya.paymentgateway.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final int MAX_RETRY_COUNT = 3;
    
    private final PaymentRepository paymentRepository;
    
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    @Transactional
    public Payment createPayment(String idempotencyKey, BigDecimal amount, String currency,
                                String merchantId, String customerId, String description) {
        
        // Check for duplicate
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            logger.warn("Duplicate payment attempt with idempotency key: {}", idempotencyKey);
            throw new DuplicatePaymentException("Payment with idempotency key already exists: " + idempotencyKey);
        }
        
        Payment payment = new Payment(idempotencyKey, amount, currency, merchantId, customerId, description);
        payment = paymentRepository.save(payment);
        
        logger.info("Created payment: {} with idempotency key: {}", payment.getId(), idempotencyKey);
        return payment;
    }
    
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
    }
    
    public Payment getPaymentByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found with idempotency key: " + idempotencyKey));
    }
    
    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatusAndRetryCountLessThanOrderByCreatedAt(
            PaymentStatus.PENDING, MAX_RETRY_COUNT);
    }
    
    public List<Payment> getStaleProcessingPayments() {
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(5);
        return paymentRepository.findStalePayments(PaymentStatus.PROCESSING, staleThreshold);
    }
}
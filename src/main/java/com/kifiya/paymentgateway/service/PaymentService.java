package com.kifiya.paymentgateway.service;

import com.kifiya.paymentgateway.domain.Payment;
import com.kifiya.paymentgateway.domain.PaymentStatus;
import com.kifiya.paymentgateway.exception.DuplicatePaymentException;
import com.kifiya.paymentgateway.exception.PaymentNotFoundException;
import com.kifiya.paymentgateway.provider.PaymentProvider;
import com.kifiya.paymentgateway.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final int MAX_RETRY_COUNT = 3;
    
    private final PaymentRepository paymentRepository;
    private final MeterRegistry meterRegistry;
    private final PaymentProvider paymentProvider; // Added PaymentProvider
    
    // Metrics
    private final Counter paymentsCreated;
    private final Counter paymentsCompleted;
    private final Counter paymentsFailed;
    private final Counter duplicatesRejected;
    private final Timer paymentProcessingTime;
    private final AtomicLong pendingPaymentsCount = new AtomicLong(0);
    
    // Updated constructor to include PaymentProvider
    public PaymentService(PaymentRepository paymentRepository, 
                         MeterRegistry meterRegistry,
                         PaymentProvider paymentProvider) {
        this.paymentRepository = paymentRepository;
        this.meterRegistry = meterRegistry;
        this.paymentProvider = paymentProvider;
        
        // Initialize counters
        this.paymentsCreated = Counter.builder("payments.created")
            .description("Number of payments created")
            .tag("service", "payment-gateway")
            .register(meterRegistry);
            
        this.paymentsCompleted = Counter.builder("payments.completed")
            .description("Number of payments completed successfully")
            .tag("service", "payment-gateway")
            .register(meterRegistry);
            
        this.paymentsFailed = Counter.builder("payments.failed")
            .description("Number of payments that failed permanently")
            .tag("service", "payment-gateway")
            .register(meterRegistry);
            
        this.duplicatesRejected = Counter.builder("payments.duplicates_rejected")
            .description("Number of duplicate payment attempts rejected")
            .tag("service", "payment-gateway")
            .register(meterRegistry);
            
        this.paymentProcessingTime = Timer.builder("payments.processing_time")
            .description("Time taken to process payments")
            .tag("service", "payment-gateway")
            .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("payments.pending_count", this, PaymentService::getPendingPaymentsCountAsDouble)
            .description("Current number of pending payments")
            .tag("service", "payment-gateway")
            .register(meterRegistry);
            
        Gauge.builder("payments.success_rate", this, PaymentService::getSuccessRate)
            .description("Payment success rate percentage")
            .tag("service", "payment-gateway")
            .register(meterRegistry);
    }
    
    @Transactional
    public Payment createPayment(String idempotencyKey, BigDecimal amount, String currency,
                                String merchantId, String customerId, String description) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Check for duplicate
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                logger.warn("Duplicate payment attempt with idempotency key: {}", idempotencyKey);
                duplicatesRejected.increment();
                throw new DuplicatePaymentException("Payment with idempotency key already exists: " + idempotencyKey);
            }
            
            Payment payment = new Payment(idempotencyKey, amount, currency, merchantId, customerId, description);
            payment = paymentRepository.save(payment);
            
            // Record metrics
            paymentsCreated.increment();
            pendingPaymentsCount.incrementAndGet();
            recordPaymentByAmount(amount);
            recordPaymentByCurrency(currency);
            
            logger.info("Created payment: {} with idempotency key: {}", payment.getId(), idempotencyKey);
            return payment;
            
        } finally {
            sample.stop(Timer.builder("payments.creation_time")
                .description("Time to create a payment")
                .register(meterRegistry));
        }
    }
    
    public Payment getPayment(UUID paymentId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        } finally {
            sample.stop(Timer.builder("payments.retrieval_time")
                .description("Time to retrieve a payment")
                .register(meterRegistry));
        }
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
    
    // NEW METHOD: Process payment with provider
    @Transactional
    public void processPaymentWithProvider(UUID paymentId) {
        Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);
        if (optionalPayment.isEmpty()) {
            logger.error("Payment not found: {}", paymentId);
            return;
        }
        
        Payment payment = optionalPayment.get();
        
        if (payment.getStatus() != PaymentStatus.PENDING) {
            logger.warn("Payment {} is not in PENDING status: {}", paymentId, payment.getStatus());
            return;
        }
        
        // Update status to PROCESSING
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Create provider request
            PaymentProvider.PaymentRequest providerRequest = new PaymentProvider.PaymentRequest(
                payment.getId().toString(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMerchantId(),
                payment.getCustomerId(),
                payment.getDescription()
            );
            
            // Process with provider
            PaymentProvider.PaymentResult result = paymentProvider.processPayment(providerRequest);
            
            if (result.success()) {
                // Payment succeeded
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setProviderTransactionId(result.providerTransactionId());
                payment.setCompletedAt(LocalDateTime.now());
                payment.setUpdatedAt(LocalDateTime.now());
                
                logger.info("Payment {} completed successfully with provider transaction: {}", 
                           paymentId, result.providerTransactionId());
                
                // Record metrics
                recordPaymentCompleted(payment);
                
            } else {
                // Payment failed
                payment.setFailureReason(result.errorMessage());
                payment.setRetryCount(payment.getRetryCount() + 1);
                payment.setUpdatedAt(LocalDateTime.now());
                
                if (result.isRetryable() && payment.getRetryCount() < MAX_RETRY_COUNT) {
                    // Mark for retry
                    payment.setStatus(PaymentStatus.PENDING);
                    logger.warn("Payment {} failed but will retry: {} (attempt {})", 
                               paymentId, result.errorMessage(), payment.getRetryCount());
                } else {
                    // Permanent failure
                    payment.setStatus(PaymentStatus.FAILED);
                    logger.error("Payment {} permanently failed: {}", 
                                paymentId, result.errorMessage());
                    
                    // Record metrics
                    recordPaymentFailed(payment);
                }
            }
            
            paymentRepository.save(payment);
            
        } catch (Exception e) {
            logger.error("Unexpected error processing payment {}: ", paymentId, e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Internal processing error");
            payment.setUpdatedAt(LocalDateTime.now());
            recordPaymentFailed(payment);
            paymentRepository.save(payment);
        } finally {
            sample.stop(paymentProcessingTime);
        }
    }
    
    // Metric calculation methods
    private long getPendingPaymentsCount() {
        return paymentRepository.countByStatus(PaymentStatus.PENDING);
    }
    
    private double getPendingPaymentsCountAsDouble() {
        return (double) getPendingPaymentsCount();
    }
    
    private double getSuccessRate() {
        double total = paymentsCreated.count();
        if (total == 0) return 0.0;
        
        double successful = paymentsCompleted.count();
        return (successful / total) * 100.0;
    }
    
    private void recordPaymentByAmount(BigDecimal amount) {
        String amountRange = getAmountRange(amount);
        Counter.builder("payments.by_amount_range")
            .description("Payments grouped by amount range")
            .tag("range", amountRange)
            .register(meterRegistry)
            .increment();
    }
    
    private void recordPaymentByCurrency(String currency) {
        Counter.builder("payments.by_currency")
            .description("Payments grouped by currency")
            .tag("currency", currency)
            .register(meterRegistry)
            .increment();
    }
    
    private String getAmountRange(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(10)) <= 0) return "0-10";
        if (amount.compareTo(BigDecimal.valueOf(50)) <= 0) return "11-50";
        if (amount.compareTo(BigDecimal.valueOf(100)) <= 0) return "51-100";
        if (amount.compareTo(BigDecimal.valueOf(500)) <= 0) return "101-500";
        if (amount.compareTo(BigDecimal.valueOf(1000)) <= 0) return "501-1000";
        return "1000+";
    }
    
    // Method to be called when payment completes
    public void recordPaymentCompleted(Payment payment) {
        paymentsCompleted.increment();
        pendingPaymentsCount.decrementAndGet();
        
        // Record processing time if available
        if (payment.getCreatedAt() != null && payment.getCompletedAt() != null) {
            long processingTimeMs = java.time.Duration.between(
                payment.getCreatedAt(), 
                payment.getCompletedAt()
            ).toMillis();
            
            Timer.builder("payments.total_processing_time")
                .description("Total time from creation to completion")
                .register(meterRegistry)
                .record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
    
    // Method to be called when payment fails
    public void recordPaymentFailed(Payment payment) {
        paymentsFailed.increment();
        pendingPaymentsCount.decrementAndGet();
        
        Counter.builder("payments.failed_by_reason")
            .description("Failed payments grouped by failure reason")
            .tag("reason", payment.getFailureReason() != null ? payment.getFailureReason() : "unknown")
            .register(meterRegistry)
            .increment();
    }
}
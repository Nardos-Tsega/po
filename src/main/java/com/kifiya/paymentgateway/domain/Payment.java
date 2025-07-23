package com.kifiya.paymentgateway.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    
    @Id
    private UUID id;
    
    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "merchant_id", nullable = false)
    private String merchantId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "description")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    
    @Column(name = "provider_transaction_id")
    private String providerTransactionId;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Default constructor
    public Payment() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }
    
    // Constructor with parameters
    public Payment(String idempotencyKey, BigDecimal amount, String currency, 
                   String merchantId, String customerId, String description) {
        this();
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.currency = currency;
        this.merchantId = merchantId;
        this.customerId = customerId;
        this.description = description;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { 
        this.status = status; 
        this.updatedAt = LocalDateTime.now();
        if (status == PaymentStatus.COMPLETED || status == PaymentStatus.FAILED) {
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String providerTransactionId) { 
        this.providerTransactionId = providerTransactionId; 
    }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
package com.kifiya.paymentgateway.repository;

import com.kifiya.paymentgateway.domain.Payment;
import com.kifiya.paymentgateway.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    List<Payment> findByStatusAndRetryCountLessThanOrderByCreatedAt(
        PaymentStatus status, Integer maxRetryCount);
    
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.updatedAt < :before")
    List<Payment> findStalePayments(@Param("status") PaymentStatus status, 
                                   @Param("before") LocalDateTime before);
    
    // Add this method for metrics
    long countByStatus(PaymentStatus status);
}
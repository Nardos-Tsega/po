package com.kifiya.paymentgateway.repository;

import com.kifiya.paymentgateway.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAt();
}
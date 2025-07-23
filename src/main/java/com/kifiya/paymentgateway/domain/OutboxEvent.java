package com.kifiya.paymentgateway.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    
    @Id
    private UUID id;
    
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;
    
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    public OutboxEvent() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }
    
    public OutboxEvent(UUID aggregateId, String eventType, String eventData) {
        this();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getEventData() { return eventData; }
    public Boolean getProcessed() { return processed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    
    public void setId(UUID id) { this.id = id; }
    public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEventData(String eventData) { this.eventData = eventData; }
    public void setProcessed(Boolean processed) { 
        this.processed = processed; 
        if (processed) {
            this.processedAt = LocalDateTime.now();
        }
    }
}
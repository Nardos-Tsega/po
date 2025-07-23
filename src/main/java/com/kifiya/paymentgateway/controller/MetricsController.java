// File: src/main/java/com/kifiya/paymentgateway/controller/MetricsController.java
package com.kifiya.paymentgateway.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {
    
    private final MeterRegistry meterRegistry;
    
    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @GetMapping("/dashboard")
    public Map<String, Object> getMetricsDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Basic Counters
        Counter created = meterRegistry.find("payments.created").counter();
        Counter duplicates = meterRegistry.find("payments.duplicates_rejected").counter();
        
        dashboard.put("payments_created", created != null ? created.count() : 0);
        dashboard.put("duplicates_rejected", duplicates != null ? duplicates.count() : 0);
        
        // Gauges
        Gauge pendingCount = meterRegistry.find("payments.pending_count").gauge();
        Gauge successRate = meterRegistry.find("payments.success_rate").gauge();
        
        dashboard.put("pending_payments", pendingCount != null ? pendingCount.value() : 0);
        dashboard.put("success_rate_percentage", successRate != null ? String.format("%.2f%%", successRate.value()) : "0.00%");
        
        // Currency breakdown
        Map<String, Double> currencyStats = new HashMap<>();
        meterRegistry.find("payments.by_currency").counters().forEach(counter -> {
            String currency = counter.getId().getTag("currency");
            if (currency != null) {
                currencyStats.put(currency, counter.count());
            }
        });
        dashboard.put("payments_by_currency", currencyStats);
        
        // Amount range breakdown
        Map<String, Double> amountStats = new HashMap<>();
        meterRegistry.find("payments.by_amount_range").counters().forEach(counter -> {
            String range = counter.getId().getTag("range");
            if (range != null) {
                amountStats.put(range, counter.count());
            }
        });
        dashboard.put("payments_by_amount_range", amountStats);
        
        // Timing metrics
        Timer creationTimer = meterRegistry.find("payments.creation_time").timer();
        if (creationTimer != null && creationTimer.count() > 0) {
            dashboard.put("avg_creation_time_ms", String.format("%.2f ms", creationTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)));
            dashboard.put("max_creation_time_ms", String.format("%.2f ms", creationTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS)));
        }
        
        return dashboard;
    }
}
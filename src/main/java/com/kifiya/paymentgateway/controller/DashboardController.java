package com.kifiya.paymentgateway.controller;

import com.kifiya.paymentgateway.domain.Payment;
import com.kifiya.paymentgateway.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {
    
    private final PaymentRepository paymentRepository;
    private final MeterRegistry meterRegistry; 
    
    public DashboardController(PaymentRepository paymentRepository, MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.meterRegistry = meterRegistry;
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get recent payments
        List<Payment> recentPayments = paymentRepository.findAll(
            PageRequest.of(0, 20, Sort.by("createdAt").descending())
        ).getContent();
        
        // Get payment statistics
        long totalPayments = paymentRepository.count();
        
        model.addAttribute("recentPayments", recentPayments);
        model.addAttribute("totalPayments", totalPayments);
        
        return "dashboard";
    }

    @GetMapping("/dashboard/metrics") 
    public String metricsPage(Model model) {
        // Get metrics data
        Counter created = meterRegistry.find("payments.created").counter();
        Counter duplicates = meterRegistry.find("payments.duplicates_rejected").counter();
        Gauge pendingCount = meterRegistry.find("payments.pending_count").gauge();
        Gauge successRate = meterRegistry.find("payments.success_rate").gauge();
        
        model.addAttribute("paymentsCreated", created != null ? created.count() : 0);
        model.addAttribute("duplicatesRejected", duplicates != null ? duplicates.count() : 0);
        model.addAttribute("pendingPayments", pendingCount != null ? pendingCount.value() : 0);
        model.addAttribute("successRate", successRate != null ? successRate.value() : 0);
        
        return "metrics-dashboard";
    }
}
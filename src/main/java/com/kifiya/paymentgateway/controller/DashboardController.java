package com.kifiya.paymentgateway.controller;

import com.kifiya.paymentgateway.domain.Payment;
import com.kifiya.paymentgateway.repository.PaymentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {
    
    private final PaymentRepository paymentRepository;
    
    public DashboardController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
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
}
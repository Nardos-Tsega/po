package com.kifiya.paymentgateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HelloController {
    
    @GetMapping("/")
    public String hello() {
        return "Hello World! Payment Gateway is running! 🚀";
    }
    
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "message", "Payment Gateway Hello World",
            "timestamp", LocalDateTime.now(),
            "status", "OK"
        );
    }
}
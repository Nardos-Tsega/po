package com.kifiya.paymentgateway.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class MockPaymentProviderTest {

    private MockPaymentProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockPaymentProvider();
    }

    @Test
    void processPayment_ReturnsValidResult() {
        // Given
        PaymentProvider.PaymentRequest request = new PaymentProvider.PaymentRequest(
            "test-txn-1",
            new BigDecimal("99.99"),
            "USD",
            "merchant_123",
            "customer_456",
            "Test payment"
        );

        // When
        PaymentProvider.PaymentResult result = provider.processPayment(request);

        // Then
        assertNotNull(result, "Payment result should not be null");
        assertNotNull(result.success(), "Success flag should not be null");
        
        if (result.success()) {
            assertNotNull(result.providerTransactionId(), "Transaction ID should be present for successful payments");
            assertTrue(result.providerTransactionId().startsWith("MOCK_TXN_"), "Transaction ID should have correct prefix");
            assertNull(result.errorMessage(), "Error message should be null for successful payments");
            assertFalse(result.isRetryable(), "Successful payments should not be retryable");
        } else {
            assertNull(result.providerTransactionId(), "Transaction ID should be null for failed payments");
            assertNotNull(result.errorMessage(), "Error message should be present for failed payments");
            // isRetryable can be true or false for failures
        }
    }

    @Test
    void getProviderName_ReturnsCorrectName() {
        // When
        String providerName = provider.getProviderName();

        // Then
        assertEquals("MockPaymentProvider", providerName, "Provider name should match expected value");
    }

    @Test
    void processPayment_HandlesDifferentAmounts() {
        // Test with different payment amounts
        BigDecimal[] testAmounts = {
            new BigDecimal("1.00"),
            new BigDecimal("99.99"), 
            new BigDecimal("500.00"),
            new BigDecimal("1000.00")
        };

        for (BigDecimal amount : testAmounts) {
            // Given
            PaymentProvider.PaymentRequest request = new PaymentProvider.PaymentRequest(
                "test-amount-" + amount,
                amount,
                "USD",
                "merchant_test",
                "customer_test",
                "Amount test: $" + amount
            );

            // When
            PaymentProvider.PaymentResult result = provider.processPayment(request);

            // Then
            assertNotNull(result, "Result should not be null for amount: " + amount);
            assertNotNull(result.success(), "Success flag should not be null for amount: " + amount);
        }
    }

    @Test
    void processPayment_HandlesDifferentCurrencies() {
        // Test with different currencies
        String[] currencies = {"USD", "EUR", "GBP", "JPY"};

        for (String currency : currencies) {
            // Given
            PaymentProvider.PaymentRequest request = new PaymentProvider.PaymentRequest(
                "test-currency-" + currency,
                new BigDecimal("99.99"),
                currency,
                "merchant_test",
                "customer_test",
                "Currency test: " + currency
            );

            // When
            PaymentProvider.PaymentResult result = provider.processPayment(request);

            // Then
            assertNotNull(result, "Result should not be null for currency: " + currency);
            assertNotNull(result.success(), "Success flag should not be null for currency: " + currency);
        }
    }

    @RepeatedTest(20)
    void processPayment_StatisticalDistribution() {
        // Given
        PaymentProvider.PaymentRequest request = new PaymentProvider.PaymentRequest(
            "test-stats-" + System.nanoTime(),
            new BigDecimal("99.99"),
            "USD",
            "merchant_stats",
            "customer_stats",
            "Statistical distribution test"
        );

        // When
        PaymentProvider.PaymentResult result = provider.processPayment(request);

        // Then - Just verify it doesn't crash and returns valid results
        assertNotNull(result, "Result should never be null");
        assertNotNull(result.success(), "Success flag should never be null");
        
        // Verify the result structure is always consistent
        if (result.success()) {
            assertNotNull(result.providerTransactionId(), "Successful payments must have transaction ID");
            assertNull(result.errorMessage(), "Successful payments should not have error message");
        } else {
            assertNull(result.providerTransactionId(), "Failed payments should not have transaction ID");
            assertNotNull(result.errorMessage(), "Failed payments must have error message");
        }
    }

    @Test
    void processPayment_SimulatesRealisticLatency() {
        // Given
        PaymentProvider.PaymentRequest request = new PaymentProvider.PaymentRequest(
            "test-latency",
            new BigDecimal("99.99"),
            "USD",
            "merchant_latency",
            "customer_latency",
            "Latency simulation test"
        );

        // When
        long startTime = System.currentTimeMillis();
        PaymentProvider.PaymentResult result = provider.processPayment(request);
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(processingTime >= 50, "Processing should take at least 50ms (simulated latency)");
        assertTrue(processingTime <= 300, "Processing should not take more than 300ms (reasonable upper bound)");
    }
}
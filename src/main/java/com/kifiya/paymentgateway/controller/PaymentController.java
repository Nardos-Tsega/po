package com.kifiya.paymentgateway.controller;

import com.kifiya.paymentgateway.domain.Payment;
import com.kifiya.paymentgateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Payment Processing", description = "APIs for managing payment transactions with idempotency support and comprehensive validation")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @Operation(
        summary = "Create a new payment",
        description = """
            Creates a new payment transaction with comprehensive idempotency support. 
            
            **Key Features:**
            - Prevents duplicate transactions using idempotency keys
            - Validates all input parameters
            - Returns detailed payment information
            - Supports multiple currencies
            
            **Idempotency:** If a payment with the same idempotency key already exists, 
            the API returns a 409 Conflict error instead of creating a duplicate.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Payment created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class),
                examples = @ExampleObject(
                    name = "Successful Payment Creation",
                    value = """
                    {
                      "id": "9a068d61-de42-4dd0-9f8e-af895e601c22",
                      "idempotencyKey": "payment_2024_001",
                      "amount": 99.99,
                      "currency": "USD",
                      "merchantId": "merchant_123",
                      "customerId": "customer_456",
                      "description": "Premium subscription payment",
                      "status": "PENDING",
                      "providerTransactionId": null,
                      "failureReason": null,
                      "retryCount": 0,
                      "createdAt": "2024-01-15T10:30:00.123456",
                      "updatedAt": "2024-01-15T10:30:00.123456",
                      "completedAt": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Payment with idempotency key already exists",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Duplicate Payment Error",
                    value = """
                    {
                      "code": "DUPLICATE_PAYMENT",
                      "message": "Payment with idempotency key already exists: payment_2024_001",
                      "timestamp": "2024-01-15T10:30:00.123456"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = """
                    {
                      "code": "VALIDATION_ERROR",
                      "message": "Request validation failed",
                      "fieldErrors": {
                        "amount": "Amount must be greater than 0",
                        "currency": "Currency must be 3 characters",
                        "merchantId": "must not be blank"
                      },
                      "timestamp": "2024-01-15T10:30:00.123456"
                    }
                    """
                )
            )
        )
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Payment creation details",
                content = @Content(
                    schema = @Schema(implementation = CreatePaymentRequest.class),
                    examples = {
                        @ExampleObject(
                            name = "Basic Payment",
                            description = "A simple payment example",
                            value = """
                            {
                              "amount": 99.99,
                              "currency": "USD",
                              "merchantId": "merchant_123",
                              "customerId": "customer_456",
                              "description": "Premium subscription payment"
                            }
                            """
                        ),
                        @ExampleObject(
                            name = "Large Payment",
                            description = "High-value payment example",
                            value = """
                            {
                              "amount": 2500.00,
                              "currency": "EUR",
                              "merchantId": "enterprise_merchant",
                              "customerId": "vip_customer",
                              "description": "Enterprise software license"
                            }
                            """
                        ),
                        @ExampleObject(
                            name = "International Payment",
                            description = "Multi-currency payment example",
                            value = """
                            {
                              "amount": 150.75,
                              "currency": "GBP",
                              "merchantId": "uk_retailer",
                              "customerId": "international_buyer",
                              "description": "International purchase"
                            }
                            """
                        )
                    }
                )
            )
            CreatePaymentRequest request,
            
            @Parameter(
                description = """
                    Unique identifier to prevent duplicate payments. This key should be:
                    - Unique for each payment attempt
                    - Consistent for retries of the same payment
                    - Maximum 255 characters
                    - Recommended format: 'payment_{timestamp}_{sequence}' or similar
                    """,
                example = "payment_2024_001",
                required = true
            )
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        
        logger.info("Creating payment with idempotency key: {}", idempotencyKey);
        
        Payment payment = paymentService.createPayment(
            idempotencyKey,
            request.amount(),
            request.currency(),
            request.merchantId(),
            request.customerId(),
            request.description()
        );
        
        PaymentResponse response = PaymentResponse.from(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
        summary = "Get payment by ID",
        description = """
            Retrieves a specific payment by its unique identifier.
            
            **Use Cases:**
            - Check payment status after creation
            - Retrieve payment details for reconciliation
            - Monitor payment processing progress
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Payment found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Payment not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "code": "PAYMENT_NOT_FOUND",
                      "message": "Payment not found: 9a068d61-de42-4dd0-9f8e-af895e601c22",
                      "timestamp": "2024-01-15T10:30:00.123456"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(
                description = "Unique payment identifier (UUID)",
                example = "9a068d61-de42-4dd0-9f8e-af895e601c22",
                required = true
            )
            @PathVariable UUID paymentId) {
        
        logger.info("Retrieving payment: {}", paymentId);
        
        Payment payment = paymentService.getPayment(paymentId);
        PaymentResponse response = PaymentResponse.from(payment);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get payment by idempotency key",
        description = """
            Retrieves a payment using its idempotency key. This is useful for:
            
            **Common Scenarios:**
            - Checking if a payment was already created with a specific idempotency key
            - Retrieving payment details when you only have the idempotency key
            - Implementing client-side duplicate detection
            
            **Best Practices:**
            - Use this endpoint to check payment status before creating a new payment
            - Store idempotency keys on the client side for reference
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Payment found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Payment not found with the given idempotency key",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "code": "PAYMENT_NOT_FOUND",
                      "message": "Payment not found with idempotency key: payment_2024_001",
                      "timestamp": "2024-01-15T10:30:00.123456"
                    }
                    """
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<PaymentResponse> getPaymentByIdempotencyKey(
            @Parameter(
                description = "The idempotency key used when creating the payment",
                example = "payment_2024_001",
                required = true
            )
            @RequestParam("idempotency_key") String idempotencyKey) {
        
        logger.info("Retrieving payment by idempotency key: {}", idempotencyKey);
        
        Payment payment = paymentService.getPaymentByIdempotencyKey(idempotencyKey);
        PaymentResponse response = PaymentResponse.from(payment);
        return ResponseEntity.ok(response);
    }
    
    // DTOs with enhanced documentation
    @Schema(description = "Request object for creating a new payment")
    public record CreatePaymentRequest(
        @Schema(
            description = "Payment amount in the specified currency",
            example = "99.99",
            minimum = "0.01"
        )
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,
        
        @Schema(
            description = "Three-letter ISO currency code",
            example = "USD",
            pattern = "^[A-Z]{3}$"
        )
        @NotBlank @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        String currency,
        
        @Schema(
            description = "Unique identifier for the merchant",
            example = "merchant_123",
            maxLength = 100
        )
        @NotBlank @Size(max = 100)
        String merchantId,
        
        @Schema(
            description = "Unique identifier for the customer",
            example = "customer_456", 
            maxLength = 100
        )
        @NotBlank @Size(max = 100)
        String customerId,
        
        @Schema(
            description = "Optional description of the payment",
            example = "Premium subscription payment",
            maxLength = 500
        )
        @Size(max = 500)
        String description
    ) {}
    
    @Schema(description = "Payment response containing all payment details")
    public record PaymentResponse(
        @Schema(description = "Unique payment identifier", example = "9a068d61-de42-4dd0-9f8e-af895e601c22")
        UUID id,
        
        @Schema(description = "Idempotency key used for this payment", example = "payment_2024_001")
        String idempotencyKey,
        
        @Schema(description = "Payment amount", example = "99.99")
        BigDecimal amount,
        
        @Schema(description = "Payment currency", example = "USD")
        String currency,
        
        @Schema(description = "Merchant identifier", example = "merchant_123")
        String merchantId,
        
        @Schema(description = "Customer identifier", example = "customer_456")
        String customerId,
        
        @Schema(description = "Payment description", example = "Premium subscription payment")
        String description,
        
        @Schema(
            description = "Current payment status", 
            example = "PENDING",
            allowableValues = {"PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"}
        )
        String status,
        
        @Schema(
            description = "Transaction ID from the payment provider (null if not processed yet)",
            example = "TXN_12345",
            nullable = true
        )
        String providerTransactionId,
        
        @Schema(
            description = "Reason for payment failure (null if not failed)",
            example = "Insufficient funds",
            nullable = true
        )
        String failureReason,
        
        @Schema(description = "Number of processing retry attempts", example = "0")
        Integer retryCount,
        
        @Schema(description = "Payment creation timestamp", example = "2024-01-15T10:30:00.123456")
        LocalDateTime createdAt,
        
        @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00.123456")
        LocalDateTime updatedAt,
        
        @Schema(
            description = "Payment completion timestamp (null if not completed)",
            example = "2024-01-15T10:32:15.654321",
            nullable = true
        )
        LocalDateTime completedAt
    ) {
        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                payment.getId(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMerchantId(),
                payment.getCustomerId(),
                payment.getDescription(),
                payment.getStatus().toString(),
                payment.getProviderTransactionId(),
                payment.getFailureReason(),
                payment.getRetryCount(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getCompletedAt()
            );
        }
    }
}
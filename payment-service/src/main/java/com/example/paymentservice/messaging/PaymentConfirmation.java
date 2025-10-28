package com.example.paymentservice.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmation {
    private String paymentId;
    private String transactionId;
    private String orderId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String gatewayResponse;
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    public enum PaymentStatus {
        SUCCESS, FAILED, PENDING, CANCELLED
    }
}
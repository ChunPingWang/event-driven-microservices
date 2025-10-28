package com.example.paymentservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付查詢結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQueryResult {
    private String paymentId;
    private String transactionId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String gatewayResponse;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
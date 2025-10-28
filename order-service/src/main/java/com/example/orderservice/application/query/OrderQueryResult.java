package com.example.orderservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 訂單查詢結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQueryResult {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
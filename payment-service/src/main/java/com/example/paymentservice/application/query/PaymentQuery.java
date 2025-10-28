package com.example.paymentservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付查詢
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQuery {
    private String paymentId;
    private String transactionId;
    private String orderId;
}
package com.example.paymentservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付列表查詢
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentListQuery {
    private String customerId;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int page;
    private int size;
}
package com.example.orderservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 訂單列表查詢
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListQuery {
    private String customerId;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int page;
    private int size;
}
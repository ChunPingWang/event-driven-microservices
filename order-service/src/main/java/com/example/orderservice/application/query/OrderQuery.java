package com.example.orderservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 訂單查詢
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQuery {
    private String orderId;
}
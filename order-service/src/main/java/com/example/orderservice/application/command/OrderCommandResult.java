package com.example.orderservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 訂單命令執行結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommandResult {
    private String orderId;
    private String transactionId;
    private String status;
    private boolean success;
    private String message;
    
    public static OrderCommandResult success(String orderId, String transactionId) {
        return OrderCommandResult.builder()
            .orderId(orderId)
            .transactionId(transactionId)
            .status("PAYMENT_PENDING")
            .success(true)
            .message("Order created successfully and payment request sent")
            .build();
    }
    
    public static OrderCommandResult failure(String message) {
        return OrderCommandResult.builder()
            .success(false)
            .message(message)
            .build();
    }
}
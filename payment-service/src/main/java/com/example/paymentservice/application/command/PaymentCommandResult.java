package com.example.paymentservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付命令執行結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCommandResult {
    private String paymentId;
    private String transactionId;
    private String orderId;
    private String status;
    private boolean success;
    private String message;
    private String gatewayResponse;
    
    public static PaymentCommandResult success(String paymentId, String transactionId, 
                                             String orderId, String gatewayResponse) {
        return PaymentCommandResult.builder()
            .paymentId(paymentId)
            .transactionId(transactionId)
            .orderId(orderId)
            .status("SUCCESS")
            .success(true)
            .message("Payment processed successfully")
            .gatewayResponse(gatewayResponse)
            .build();
    }
    
    public static PaymentCommandResult failure(String transactionId, String orderId, 
                                             String message, String gatewayResponse) {
        return PaymentCommandResult.builder()
            .transactionId(transactionId)
            .orderId(orderId)
            .status("FAILED")
            .success(false)
            .message(message)
            .gatewayResponse(gatewayResponse)
            .build();
    }
}
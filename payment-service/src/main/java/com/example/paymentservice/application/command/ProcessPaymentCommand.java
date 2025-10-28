package com.example.paymentservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 處理支付命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentCommand {
    private String transactionId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private CreditCardInfo creditCard;
    private BillingAddress billingAddress;
    private String merchantId;
    private String description;
    private LocalDateTime timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditCardInfo {
        private String cardNumber;
        private String expiryDate;
        private String cvv;
        private String cardHolderName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingAddress {
        private String street;
        private String city;
        private String postalCode;
        private String country;
    }
}
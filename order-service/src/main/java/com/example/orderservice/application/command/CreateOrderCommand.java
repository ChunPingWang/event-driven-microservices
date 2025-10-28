package com.example.orderservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 創建訂單命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private CreditCardInfo creditCard;
    private BillingAddress billingAddress;
    private String merchantId;
    private String description;
    
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
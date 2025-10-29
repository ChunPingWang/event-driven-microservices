package com.example.orderservice.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 創建訂單請求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotBlank(message = "Customer ID is required")
    @JsonProperty("customerId")
    private String customerId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter code")
    @JsonProperty("currency")
    private String currency;
    
    @NotNull(message = "Credit card information is required")
    @Valid
    @JsonProperty("creditCard")
    private CreditCardInfo creditCard;
    
    @NotNull(message = "Billing address is required")
    @Valid
    @JsonProperty("billingAddress")
    private BillingAddress billingAddress;
    
    @NotBlank(message = "Merchant ID is required")
    @JsonProperty("merchantId")
    private String merchantId;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditCardInfo {
        
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13-19 digits")
        @JsonProperty("cardNumber")
        private String cardNumber;
        
        @NotBlank(message = "Expiry date is required")
        @Pattern(regexp = "^(0[1-9]|1[0-2])/([0-9]{2})$", message = "Expiry date must be in MM/YY format")
        @JsonProperty("expiryDate")
        private String expiryDate;
        
        @NotBlank(message = "CVV is required")
        @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3-4 digits")
        @JsonProperty("cvv")
        private String cvv;
        
        @NotBlank(message = "Card holder name is required")
        @Size(min = 2, max = 100, message = "Card holder name must be between 2 and 100 characters")
        @JsonProperty("cardHolderName")
        private String cardHolderName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingAddress {
        
        @NotBlank(message = "Street is required")
        @Size(max = 200, message = "Street cannot exceed 200 characters")
        @JsonProperty("street")
        private String street;
        
        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        @JsonProperty("city")
        private String city;
        
        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^[A-Za-z0-9\\s-]{3,10}$", message = "Invalid postal code format")
        @JsonProperty("postalCode")
        private String postalCode;
        
        @NotBlank(message = "Country is required")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a valid 2-letter code")
        @JsonProperty("country")
        private String country;
    }
}
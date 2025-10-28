package com.example.paymentservice.domain.payment.service;

import com.example.paymentservice.domain.payment.valueobject.CreditCard;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 信用卡驗證服務實現
 */
@Service
public class CreditCardValidatorImpl implements CreditCardValidator {
    
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9\\s\\-]{13,19}$");
    
    // 模擬黑名單信用卡號碼（實際應用中應該從數據庫或外部服務獲取）
    private static final Set<String> BLACKLISTED_CARDS = Set.of(
        "**** **** **** 0000",
        "**** **** **** 1111",
        "**** **** **** 9999"
    );
    
    @Override
    public ValidationResult validate(CreditCard creditCard) {
        if (creditCard == null) {
            return ValidationResult.failure("Credit card cannot be null");
        }
        
        List<String> errors = new ArrayList<>();
        
        // 驗證信用卡號碼格式
        if (!isValidCardNumber(creditCard.getCardNumber())) {
            errors.add("Invalid card number format");
        }
        
        // 檢查是否在黑名單中
        if (isBlacklisted(creditCard.getCardNumber())) {
            errors.add("Credit card is blacklisted");
        }
        
        // 檢查是否過期
        if (creditCard.isExpired()) {
            errors.add("Credit card has expired");
        }
        
        // 驗證持卡人姓名
        if (creditCard.getCardHolderName() == null || creditCard.getCardHolderName().trim().length() < 2) {
            errors.add("Invalid card holder name");
        }
        
        // 驗證CVV格式
        if (creditCard.getCvv() == null || !creditCard.getCvv().matches("^[0-9]{3,4}$")) {
            errors.add("Invalid CVV format");
        }
        
        // 檢查信用卡品牌是否支持
        String brand = creditCard.getBrand();
        if ("UNKNOWN".equals(brand)) {
            errors.add("Unsupported credit card brand");
        }
        
        if (errors.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(errors);
        }
    }
    
    @Override
    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return false;
        }
        
        // 移除空格和連字符
        String cleanCardNumber = cardNumber.replaceAll("[\\s\\-]", "");
        
        // 檢查格式
        if (!cleanCardNumber.matches("^[0-9]{13,19}$")) {
            return false;
        }
        
        // Luhn 算法驗證
        return isValidLuhn(cleanCardNumber);
    }
    
    @Override
    public boolean isBlacklisted(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        
        return BLACKLISTED_CARDS.contains(cardNumber);
    }
    
    /**
     * Luhn 算法驗證信用卡號碼
     * @param cardNumber 清理後的信用卡號碼
     * @return true 如果通過Luhn驗證
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (digit < 0 || digit > 9) {
                return false;
            }
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }
}
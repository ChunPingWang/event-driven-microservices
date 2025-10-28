package com.example.paymentservice.domain.payment.valueobject;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 信用卡值對象
 */
@Embeddable
public class CreditCard {
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{13,19}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3,4}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$");
    
    private String cardNumber;
    private String originalCardNumber; // Store for brand detection
    private String expiryDate;
    private String cvv;
    private String cardHolderName;
    
    // JPA 需要的默認構造函數
    protected CreditCard() {}
    
    public CreditCard(String cardNumber, String expiryDate, String cvv, String cardHolderName) {
        validateCardNumber(cardNumber);
        validateExpiryDate(expiryDate);
        validateCvv(cvv);
        validateCardHolderName(cardHolderName);
        
        this.originalCardNumber = cardNumber.replaceAll("\\s+", ""); // Store clean original
        this.cardNumber = maskCardNumber(cardNumber);
        this.expiryDate = expiryDate;
        this.cvv = cvv; // 在實際應用中，CVV不應該存儲
        this.cardHolderName = cardHolderName.trim().toUpperCase();
    }
    
    /**
     * 驗證信用卡號碼
     * @param cardNumber 信用卡號碼
     */
    private void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Card number cannot be null or empty");
        }
        
        String cleanCardNumber = cardNumber.replaceAll("\\s+", "");
        
        if (!CARD_NUMBER_PATTERN.matcher(cleanCardNumber).matches()) {
            throw new IllegalArgumentException("Invalid card number format");
        }
        
        if (!isValidLuhn(cleanCardNumber)) {
            throw new IllegalArgumentException("Invalid card number - failed Luhn check");
        }
    }
    
    /**
     * 驗證到期日期
     * @param expiryDate 到期日期 (MM/YY 格式)
     */
    private void validateExpiryDate(String expiryDate) {
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Expiry date cannot be null or empty");
        }
        
        if (!EXPIRY_PATTERN.matcher(expiryDate).matches()) {
            throw new IllegalArgumentException("Invalid expiry date format. Expected MM/YY");
        }
        
        try {
            String[] parts = expiryDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = 2000 + Integer.parseInt(parts[1]);
            
            YearMonth expiryYearMonth = YearMonth.of(year, month);
            YearMonth currentYearMonth = YearMonth.now();
            
            if (expiryYearMonth.isBefore(currentYearMonth)) {
                throw new IllegalArgumentException("Card has expired");
            }
            
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid expiry date", e);
        }
    }
    
    /**
     * 驗證CVV
     * @param cvv CVV碼
     */
    private void validateCvv(String cvv) {
        if (cvv == null || cvv.trim().isEmpty()) {
            throw new IllegalArgumentException("CVV cannot be null or empty");
        }
        
        if (!CVV_PATTERN.matcher(cvv).matches()) {
            throw new IllegalArgumentException("Invalid CVV format");
        }
    }
    
    /**
     * 驗證持卡人姓名
     * @param cardHolderName 持卡人姓名
     */
    private void validateCardHolderName(String cardHolderName) {
        if (cardHolderName == null || cardHolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Card holder name cannot be null or empty");
        }
        
        if (cardHolderName.trim().length() < 2) {
            throw new IllegalArgumentException("Card holder name too short");
        }
        
        if (cardHolderName.trim().length() > 50) {
            throw new IllegalArgumentException("Card holder name too long");
        }
    }
    
    /**
     * Luhn 算法驗證信用卡號碼
     * @param cardNumber 信用卡號碼
     * @return true 如果通過Luhn驗證
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
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
    
    /**
     * 遮罩信用卡號碼，只顯示最後4位
     * @param cardNumber 原始信用卡號碼
     * @return 遮罩後的信用卡號碼
     */
    private String maskCardNumber(String cardNumber) {
        String cleanCardNumber = cardNumber.replaceAll("\\s+", "");
        if (cleanCardNumber.length() < 4) {
            return "****";
        }
        
        String lastFour = cleanCardNumber.substring(cleanCardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
    
    /**
     * 檢查信用卡是否已過期
     * @return true 如果信用卡已過期
     */
    public boolean isExpired() {
        try {
            String[] parts = expiryDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = 2000 + Integer.parseInt(parts[1]);
            
            YearMonth expiryYearMonth = YearMonth.of(year, month);
            return expiryYearMonth.isBefore(YearMonth.now());
            
        } catch (Exception e) {
            return true; // 如果解析失敗，視為過期
        }
    }
    
    /**
     * 獲取信用卡品牌
     * @return 信用卡品牌
     */
    public String getBrand() {
        if (originalCardNumber.startsWith("4")) {
            return "VISA";
        } else if (originalCardNumber.startsWith("5") || originalCardNumber.startsWith("2")) {
            return "MASTERCARD";
        } else if (originalCardNumber.startsWith("3")) {
            return "AMEX";
        } else {
            return "UNKNOWN";
        }
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public String getExpiryDate() {
        return expiryDate;
    }
    
    public String getCvv() {
        return cvv;
    }
    
    public String getCardHolderName() {
        return cardHolderName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditCard that = (CreditCard) o;
        return Objects.equals(cardNumber, that.cardNumber) &&
               Objects.equals(expiryDate, that.expiryDate) &&
               Objects.equals(cardHolderName, that.cardHolderName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, expiryDate, cardHolderName);
    }
    
    @Override
    public String toString() {
        return String.format("CreditCard{cardNumber='%s', expiryDate='%s', cardHolderName='%s'}", 
            cardNumber, expiryDate, cardHolderName);
    }
}
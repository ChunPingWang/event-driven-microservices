package com.example.paymentservice.domain.payment.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 驗證結果值對象
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errorMessages;
    
    private ValidationResult(boolean valid, List<String> errorMessages) {
        this.valid = valid;
        this.errorMessages = errorMessages != null ? 
            new ArrayList<>(errorMessages) : new ArrayList<>();
    }
    
    /**
     * 創建成功的驗證結果
     * @return 成功的驗證結果
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }
    
    /**
     * 創建失敗的驗證結果
     * @param errorMessage 錯誤信息
     * @return 失敗的驗證結果
     */
    public static ValidationResult failure(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        return new ValidationResult(false, Collections.singletonList(errorMessage));
    }
    
    /**
     * 創建失敗的驗證結果
     * @param errorMessages 錯誤信息列表
     * @return 失敗的驗證結果
     */
    public static ValidationResult failure(List<String> errorMessages) {
        if (errorMessages == null || errorMessages.isEmpty()) {
            throw new IllegalArgumentException("Error messages cannot be null or empty");
        }
        return new ValidationResult(false, errorMessages);
    }
    
    /**
     * 檢查驗證是否成功
     * @return true 如果驗證成功
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * 檢查驗證是否失敗
     * @return true 如果驗證失敗
     */
    public boolean isInvalid() {
        return !valid;
    }
    
    /**
     * 獲取錯誤信息列表
     * @return 錯誤信息列表（只讀）
     */
    public List<String> getErrorMessages() {
        return Collections.unmodifiableList(errorMessages);
    }
    
    /**
     * 獲取第一個錯誤信息
     * @return 第一個錯誤信息，如果沒有錯誤則返回null
     */
    public String getErrorMessage() {
        return errorMessages.isEmpty() ? null : errorMessages.get(0);
    }
    
    /**
     * 合併另一個驗證結果
     * @param other 另一個驗證結果
     * @return 合併後的驗證結果
     */
    public ValidationResult merge(ValidationResult other) {
        if (other == null) {
            return this;
        }
        
        if (this.valid && other.valid) {
            return ValidationResult.success();
        }
        
        List<String> allErrors = new ArrayList<>(this.errorMessages);
        allErrors.addAll(other.errorMessages);
        
        return ValidationResult.failure(allErrors);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid && Objects.equals(errorMessages, that.errorMessages);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errorMessages);
    }
    
    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult{valid=true}";
        } else {
            return String.format("ValidationResult{valid=false, errors=%s}", errorMessages);
        }
    }
}
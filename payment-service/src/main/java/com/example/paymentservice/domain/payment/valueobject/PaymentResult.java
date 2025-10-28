package com.example.paymentservice.domain.payment.valueobject;

import java.util.Objects;

/**
 * 支付結果值對象
 */
public class PaymentResult {
    private final boolean success;
    private final String paymentId;
    private final String errorMessage;
    
    private PaymentResult(boolean success, String paymentId, String errorMessage) {
        this.success = success;
        this.paymentId = paymentId;
        this.errorMessage = errorMessage;
    }
    
    /**
     * 創建成功的支付結果
     * @param paymentId 支付ID
     * @return 成功的支付結果
     */
    public static PaymentResult success(String paymentId) {
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty for successful result");
        }
        return new PaymentResult(true, paymentId, null);
    }
    
    /**
     * 創建失敗的支付結果
     * @param errorMessage 錯誤信息
     * @return 失敗的支付結果
     */
    public static PaymentResult failed(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty for failed result");
        }
        return new PaymentResult(false, null, errorMessage);
    }
    
    /**
     * 檢查支付是否成功
     * @return true 如果支付成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 檢查支付是否失敗
     * @return true 如果支付失敗
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * 獲取支付ID（僅在成功時有值）
     * @return 支付ID
     */
    public String getPaymentId() {
        return paymentId;
    }
    
    /**
     * 獲取錯誤信息（僅在失敗時有值）
     * @return 錯誤信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentResult that = (PaymentResult) o;
        return success == that.success &&
               Objects.equals(paymentId, that.paymentId) &&
               Objects.equals(errorMessage, that.errorMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, paymentId, errorMessage);
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("PaymentResult{success=true, paymentId='%s'}", paymentId);
        } else {
            return String.format("PaymentResult{success=false, errorMessage='%s'}", errorMessage);
        }
    }
}
package com.example.paymentservice.domain.payment.valueobject;

/**
 * 支付狀態枚舉
 */
public enum PaymentStatus {
    /**
     * 處理中 - 支付正在處理
     */
    PROCESSING,
    
    /**
     * 成功 - 支付成功完成
     */
    SUCCESS,
    
    /**
     * 失敗 - 支付處理失敗
     */
    FAILED,
    
    /**
     * 已退款 - 支付已退款
     */
    REFUNDED;
    
    /**
     * 檢查是否為最終狀態
     * @return true 如果是最終狀態（不可再變更）
     */
    public boolean isFinalStatus() {
        return this == SUCCESS || this == FAILED || this == REFUNDED;
    }
    
    /**
     * 檢查是否可以退款
     * @return true 如果可以退款
     */
    public boolean canRefund() {
        return this == SUCCESS;
    }
    
    /**
     * 檢查支付是否成功
     * @return true 如果支付成功
     */
    public boolean isSuccessful() {
        return this == SUCCESS;
    }
    
    /**
     * 檢查支付是否失敗
     * @return true 如果支付失敗
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}
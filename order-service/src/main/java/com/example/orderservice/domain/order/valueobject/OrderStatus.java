package com.example.orderservice.domain.order.valueobject;

/**
 * 訂單狀態枚舉
 */
public enum OrderStatus {
    /**
     * 已創建 - 訂單剛創建，尚未開始支付流程
     */
    CREATED,
    
    /**
     * 支付處理中 - 支付請求已發送，等待支付確認
     */
    PAYMENT_PENDING,
    
    /**
     * 支付已確認 - 支付成功完成
     */
    PAYMENT_CONFIRMED,
    
    /**
     * 支付失敗 - 支付處理失敗
     */
    PAYMENT_FAILED,
    
    /**
     * 已取消 - 訂單被取消
     */
    CANCELLED;
    
    /**
     * 檢查是否可以請求支付
     * @return true 如果可以請求支付
     */
    public boolean canRequestPayment() {
        return this == CREATED || this == PAYMENT_FAILED;
    }
    
    /**
     * 檢查是否可以確認支付
     * @return true 如果可以確認支付
     */
    public boolean canConfirmPayment() {
        return this == PAYMENT_PENDING;
    }
    
    /**
     * 檢查是否可以標記支付失敗
     * @return true 如果可以標記支付失敗
     */
    public boolean canFailPayment() {
        return this == PAYMENT_PENDING;
    }
    
    /**
     * 檢查是否可以取消訂單
     * @return true 如果可以取消訂單
     */
    public boolean canCancel() {
        return this == CREATED || this == PAYMENT_FAILED;
    }
    
    /**
     * 檢查訂單是否已完成
     * @return true 如果訂單已完成（成功或失敗）
     */
    public boolean isCompleted() {
        return this == PAYMENT_CONFIRMED || this == PAYMENT_FAILED || this == CANCELLED;
    }
}
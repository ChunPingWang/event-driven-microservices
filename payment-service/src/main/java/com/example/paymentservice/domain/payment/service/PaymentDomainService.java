package com.example.paymentservice.domain.payment.service;

import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.valueobject.PaymentResult;

/**
 * 支付領域服務接口
 */
public interface PaymentDomainService {
    
    /**
     * 處理支付
     * @param payment 支付聚合
     * @param gatewayResponse 支付網關響應
     * @return 支付結果
     */
    PaymentResult processPayment(Payment payment, String gatewayResponse);
    
    /**
     * 檢查支付是否可以重試
     * @param payment 支付聚合
     * @return true 如果可以重試
     */
    boolean canRetryPayment(Payment payment);
    
    /**
     * 驗證支付請求
     * @param payment 支付聚合
     * @return 驗證結果
     */
    ValidationResult validatePaymentRequest(Payment payment);
    
    /**
     * 計算支付手續費
     * @param payment 支付聚合
     * @return 手續費金額
     */
    java.math.BigDecimal calculateProcessingFee(Payment payment);
}
package com.example.paymentservice.domain.payment.service;

import com.example.paymentservice.domain.payment.valueobject.CreditCard;

/**
 * 信用卡驗證服務接口
 */
public interface CreditCardValidator {
    
    /**
     * 驗證信用卡
     * @param creditCard 信用卡信息
     * @return 驗證結果
     */
    ValidationResult validate(CreditCard creditCard);
    
    /**
     * 驗證信用卡號碼
     * @param cardNumber 信用卡號碼
     * @return true 如果號碼有效
     */
    boolean isValidCardNumber(String cardNumber);
    
    /**
     * 檢查信用卡是否被列入黑名單
     * @param cardNumber 信用卡號碼
     * @return true 如果在黑名單中
     */
    boolean isBlacklisted(String cardNumber);
}
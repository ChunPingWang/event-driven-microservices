package com.example.paymentservice.domain.payment.service;

import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.valueobject.PaymentResult;
import com.example.paymentservice.domain.payment.valueobject.PaymentStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 支付領域服務實現
 */
@Service
public class PaymentDomainServiceImpl implements PaymentDomainService {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final BigDecimal PROCESSING_FEE_RATE = new BigDecimal("0.029"); // 2.9%
    private static final BigDecimal MIN_PROCESSING_FEE = new BigDecimal("0.30"); // 最低手續費
    
    private final CreditCardValidator creditCardValidator;
    
    public PaymentDomainServiceImpl(CreditCardValidator creditCardValidator) {
        this.creditCardValidator = creditCardValidator;
    }
    
    @Override
    public PaymentResult processPayment(Payment payment, String gatewayResponse) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }
        
        // 首先驗證支付請求
        ValidationResult validationResult = validatePaymentRequest(payment);
        if (validationResult.isInvalid()) {
            return PaymentResult.failed("Payment validation failed: " + validationResult.getErrorMessage());
        }
        
        // 處理支付
        return payment.process(gatewayResponse);
    }
    
    @Override
    public boolean canRetryPayment(Payment payment) {
        if (payment == null) {
            return false;
        }
        
        // 只有失敗的支付才能重試，且不能超過最大重試次數
        return payment.getStatus() == PaymentStatus.FAILED && payment.canRetry();
    }
    
    @Override
    public ValidationResult validatePaymentRequest(Payment payment) {
        if (payment == null) {
            return ValidationResult.failure("Payment cannot be null");
        }
        
        // 驗證支付狀態
        if (payment.getStatus().isFinalStatus() && payment.getStatus() != PaymentStatus.PROCESSING) {
            return ValidationResult.failure("Payment is in final status and cannot be processed");
        }
        
        // 驗證金額
        if (payment.getAmount() == null || payment.getAmount().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.failure("Payment amount must be positive");
        }
        
        // 驗證金額上限（例如：單筆不超過 $10,000）
        BigDecimal maxAmount = new BigDecimal("10000.00");
        if (payment.getAmount().getAmount().compareTo(maxAmount) > 0) {
            return ValidationResult.failure("Payment amount exceeds maximum limit");
        }
        
        // 驗證信用卡
        ValidationResult cardValidation = creditCardValidator.validate(payment.getCreditCard());
        if (cardValidation.isInvalid()) {
            return cardValidation;
        }
        
        // 驗證訂單ID和交易ID
        if (payment.getOrderId() == null || payment.getOrderId().trim().isEmpty()) {
            return ValidationResult.failure("Order ID cannot be null or empty");
        }
        
        if (payment.getTransactionId() == null || payment.getTransactionId().trim().isEmpty()) {
            return ValidationResult.failure("Transaction ID cannot be null or empty");
        }
        
        return ValidationResult.success();
    }
    
    @Override
    public BigDecimal calculateProcessingFee(Payment payment) {
        if (payment == null || payment.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal amount = payment.getAmount().getAmount();
        BigDecimal fee = amount.multiply(PROCESSING_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        
        // 確保手續費不低於最低金額
        if (fee.compareTo(MIN_PROCESSING_FEE) < 0) {
            fee = MIN_PROCESSING_FEE;
        }
        
        return fee;
    }
    
    /**
     * 檢查支付金額是否在允許範圍內
     * @param amount 支付金額
     * @return true 如果金額在允許範圍內
     */
    private boolean isAmountWithinLimits(BigDecimal amount) {
        BigDecimal minAmount = new BigDecimal("0.50"); // 最低 $0.50
        BigDecimal maxAmount = new BigDecimal("10000.00"); // 最高 $10,000
        
        return amount.compareTo(minAmount) >= 0 && amount.compareTo(maxAmount) <= 0;
    }
    
    /**
     * 檢查貨幣是否支持
     * @param currency 貨幣代碼
     * @return true 如果支持該貨幣
     */
    private boolean isSupportedCurrency(String currency) {
        // 支持的貨幣列表
        return "USD".equals(currency) || "TWD".equals(currency) || "EUR".equals(currency);
    }
}
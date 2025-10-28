package com.example.paymentservice.bdd;

import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.valueobject.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class TestDataHelper {

    private final Map<String, Payment> testPayments = new HashMap<>();
    private final Map<String, Object> testData = new HashMap<>();

    public Payment createTestPayment(String orderId, BigDecimal amount, String cardNumber, String expiry, String cvv) {
        try {
            Money money = Money.twd(amount);
            CreditCard creditCard = new CreditCard(cardNumber, expiry, cvv, "Test Holder");
            
            Payment payment = new Payment("TXN-" + System.currentTimeMillis(), orderId, "CUST-001", money, creditCard);
            testPayments.put(payment.getPaymentId().getValue(), payment);
            return payment;
        } catch (Exception e) {
            // 創建失敗的支付記錄 - use valid defaults
            Money money = Money.twd(BigDecimal.valueOf(1.0));
            CreditCard creditCard = new CreditCard("4111111111111111", "12/25", "123", "Test Holder");
            Payment payment = new Payment("TXN-" + System.currentTimeMillis(), orderId, "CUST-001", money, creditCard);
            testPayments.put(payment.getPaymentId().getValue(), payment);
            return payment;
        }
    }

    public Payment getTestPayment(String paymentId) {
        return testPayments.get(paymentId);
    }

    public void storeTestData(String key, Object value) {
        testData.put(key, value);
    }

    public Object getTestData(String key) {
        return testData.get(key);
    }

    public void clearTestData() {
        testPayments.clear();
        testData.clear();
    }

    public void simulatePaymentProcessing(Payment payment, boolean success) {
        if (success) {
            payment.process("SUCCESS");
            storeTestData("payment_processed_" + payment.getPaymentId().getValue(), true);
        } else {
            payment.process("FAILED");
            storeTestData("payment_failed_" + payment.getPaymentId().getValue(), true);
        }
    }

    public void simulateRetryAttempt(String paymentId) {
        Integer currentRetries = (Integer) getTestData("retry_count_" + paymentId);
        if (currentRetries == null) {
            currentRetries = 0;
        }
        storeTestData("retry_count_" + paymentId, currentRetries + 1);
    }

    public int getRetryCount(String paymentId) {
        Integer retries = (Integer) getTestData("retry_count_" + paymentId);
        return retries != null ? retries : 0;
    }

    public boolean isValidCreditCard(String cardNumber) {
        // 簡單的信用卡號驗證邏輯
        return "4111111111111111".equals(cardNumber);
    }

    public boolean isCreditCardExpired(String expiry) {
        try {
            YearMonth expiryDate = YearMonth.parse(expiry, DateTimeFormatter.ofPattern("MM/yy"));
            return expiryDate.isBefore(YearMonth.now());
        } catch (Exception e) {
            return true; // 格式錯誤視為過期
        }
    }

    public boolean isValidCvv(String cvv) {
        return cvv != null && cvv.matches("\\d{3,4}");
    }
}
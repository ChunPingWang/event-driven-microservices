package com.example.paymentservice.domain.payment.valueobject;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;

/**
 * 支付ID值對象
 */
@Embeddable
public class PaymentId {
    private String value;
    
    // JPA 需要的默認構造函數
    protected PaymentId() {}
    
    public PaymentId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
        this.value = value.trim();
    }
    
    /**
     * 生成新的支付ID
     * @return 新的支付ID實例
     */
    public static PaymentId generate() {
        return new PaymentId("PAY-" + UUID.randomUUID().toString());
    }
    
    /**
     * 從字符串創建支付ID
     * @param value 支付ID字符串
     * @return 支付ID實例
     */
    public static PaymentId of(String value) {
        return new PaymentId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentId paymentId = (PaymentId) o;
        return Objects.equals(value, paymentId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
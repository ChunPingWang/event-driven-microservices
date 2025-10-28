package com.example.orderservice.domain.order.valueobject;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;

/**
 * 訂單ID值對象
 */
@Embeddable
public class OrderId {
    private String value;
    
    // JPA 需要的默認構造函數
    protected OrderId() {}
    
    public OrderId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        this.value = value.trim();
    }
    
    /**
     * 生成新的訂單ID
     * @return 新的訂單ID實例
     */
    public static OrderId generate() {
        return new OrderId("ORDER-" + UUID.randomUUID().toString());
    }
    
    /**
     * 從字符串創建訂單ID
     * @param value 訂單ID字符串
     * @return 訂單ID實例
     */
    public static OrderId of(String value) {
        return new OrderId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
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
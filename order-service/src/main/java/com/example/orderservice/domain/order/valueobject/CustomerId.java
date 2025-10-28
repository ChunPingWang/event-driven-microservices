package com.example.orderservice.domain.order.valueobject;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * 客戶ID值對象
 */
@Embeddable
public class CustomerId {
    private String value;
    
    // JPA 需要的默認構造函數
    protected CustomerId() {}
    
    public CustomerId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        this.value = value.trim();
    }
    
    /**
     * 從字符串創建客戶ID
     * @param value 客戶ID字符串
     * @return 客戶ID實例
     */
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerId that = (CustomerId) o;
        return Objects.equals(value, that.value);
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
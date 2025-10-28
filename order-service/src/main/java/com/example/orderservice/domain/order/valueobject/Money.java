package com.example.orderservice.domain.order.valueobject;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 金額值對象
 */
@Embeddable
public class Money {
    private BigDecimal amount;
    private String currency;
    
    // JPA 需要的默認構造函數
    protected Money() {}
    
    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency.getCurrencyCode();
    }
    
    public Money(BigDecimal amount, String currencyCode) {
        this(amount, Currency.getInstance(currencyCode));
    }
    
    /**
     * 私有構造函數，用於內部創建零金額（僅用於計算結果）
     */
    private Money(BigDecimal amount, String currencyCode, boolean allowZero) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currencyCode == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (!allowZero && amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currencyCode;
    }
    
    /**
     * 創建金額實例
     * @param amount 金額
     * @param currencyCode 貨幣代碼
     * @return 金額實例
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, currencyCode);
    }
    
    /**
     * 創建美元金額
     * @param amount 金額
     * @return 美元金額實例
     */
    public static Money usd(BigDecimal amount) {
        return new Money(amount, "USD");
    }
    
    /**
     * 創建台幣金額
     * @param amount 金額
     * @return 台幣金額實例
     */
    public static Money twd(BigDecimal amount) {
        return new Money(amount, "TWD");
    }
    
    /**
     * 金額相加
     * @param other 另一個金額
     * @return 相加後的金額
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * 金額相減
     * @param other 另一個金額
     * @return 相減後的金額
     */
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result cannot be negative");
        }
        return new Money(result, this.currency, true); // Allow zero for calculation results
    }
    
    /**
     * 檢查是否為零
     * @return true 如果金額為零
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * 檢查是否大於另一個金額
     * @param other 另一個金額
     * @return true 如果當前金額大於另一個金額
     */
    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public Currency getCurrencyInstance() {
        return Currency.getInstance(currency);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
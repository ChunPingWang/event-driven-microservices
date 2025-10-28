package com.example.paymentservice.domain.payment.event;

import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import java.math.BigDecimal;

/**
 * 支付處理完成事件 - 當支付處理成功時發布
 */
public class PaymentProcessedEvent extends DomainEvent {
    private final PaymentId paymentId;
    private final String orderId;
    private final String transactionId;
    private final BigDecimal amount;
    private final String currency;
    
    public PaymentProcessedEvent(PaymentId paymentId, String orderId, String transactionId, 
                               BigDecimal amount, String currency) {
        super();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
    }
    
    public PaymentId getPaymentId() {
        return paymentId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public String getCurrency() {
        return currency;
    }
}
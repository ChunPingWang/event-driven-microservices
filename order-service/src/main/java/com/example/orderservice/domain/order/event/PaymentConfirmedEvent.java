package com.example.orderservice.domain.order.event;

import com.example.orderservice.domain.shared.DomainEvent;
import com.example.orderservice.domain.order.valueobject.OrderId;

/**
 * 支付確認事件 - 當支付成功確認時發布
 */
public class PaymentConfirmedEvent extends DomainEvent {
    private final OrderId orderId;
    private final String transactionId;
    private final String paymentId;
    
    public PaymentConfirmedEvent(OrderId orderId, String transactionId, String paymentId) {
        super();
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.paymentId = paymentId;
    }
    
    public OrderId getOrderId() {
        return orderId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
}
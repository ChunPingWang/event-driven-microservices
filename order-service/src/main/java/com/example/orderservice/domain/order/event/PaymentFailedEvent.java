package com.example.orderservice.domain.order.event;

import com.example.orderservice.domain.shared.DomainEvent;
import com.example.orderservice.domain.order.valueobject.OrderId;

/**
 * 支付失敗事件 - 當支付處理失敗時發布
 */
public class PaymentFailedEvent extends DomainEvent {
    private final OrderId orderId;
    private final String transactionId;
    private final String reason;
    
    public PaymentFailedEvent(OrderId orderId, String transactionId, String reason) {
        super();
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.reason = reason;
    }
    
    public OrderId getOrderId() {
        return orderId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getReason() {
        return reason;
    }
}
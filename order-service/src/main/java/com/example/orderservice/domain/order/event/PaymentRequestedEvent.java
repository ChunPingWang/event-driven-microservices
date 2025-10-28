package com.example.orderservice.domain.order.event;

import com.example.orderservice.domain.shared.DomainEvent;
import com.example.orderservice.domain.order.valueobject.OrderId;

/**
 * 支付請求事件 - 當訂單請求支付時發布
 */
public class PaymentRequestedEvent extends DomainEvent {
    private final OrderId orderId;
    private final String transactionId;
    private final String customerId;
    
    public PaymentRequestedEvent(OrderId orderId, String transactionId, String customerId) {
        super();
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.customerId = customerId;
    }
    
    public OrderId getOrderId() {
        return orderId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
}
package com.example.paymentservice.domain.payment.event;

import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;

/**
 * 支付失敗事件 - 當支付處理失敗時發布
 */
public class PaymentFailedEvent extends DomainEvent {
    private final PaymentId paymentId;
    private final String orderId;
    private final String transactionId;
    private final String errorMessage;
    
    public PaymentFailedEvent(PaymentId paymentId, String orderId, String transactionId, String errorMessage) {
        super();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
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
    
    public String getErrorMessage() {
        return errorMessage;
    }
}
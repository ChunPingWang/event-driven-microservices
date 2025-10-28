package com.example.orderservice.domain.order;

import com.example.orderservice.domain.shared.AggregateRoot;
import com.example.orderservice.domain.order.event.PaymentConfirmedEvent;
import com.example.orderservice.domain.order.event.PaymentFailedEvent;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 訂單聚合根
 */
@Entity
@Table(name = "orders")
public class Order extends AggregateRoot {
    
    @EmbeddedId
    private OrderId orderId;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "customer_id"))
    private CustomerId customerId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    // JPA 需要的默認構造函數
    protected Order() {}
    
    /**
     * 創建新訂單
     * @param customerId 客戶ID
     * @param amount 訂單金額
     */
    public Order(CustomerId customerId, Money amount) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        this.orderId = OrderId.generate();
        this.customerId = customerId;
        this.amount = amount;
        this.status = OrderStatus.CREATED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 請求支付
     * @param transactionId 交易ID
     */
    public void requestPayment(String transactionId) {
        if (!status.canRequestPayment()) {
            throw new IllegalStateException(
                String.format("Cannot request payment for order in status: %s", status));
        }
        
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        
        this.transactionId = transactionId;
        this.status = OrderStatus.PAYMENT_PENDING;
        this.updatedAt = LocalDateTime.now();
        
        // 發布領域事件
        addDomainEvent(new PaymentRequestedEvent(this.orderId, transactionId, this.customerId.getValue()));
    }
    
    /**
     * 確認支付
     * @param paymentId 支付ID
     */
    public void confirmPayment(String paymentId) {
        if (!status.canConfirmPayment()) {
            throw new IllegalStateException(
                String.format("Cannot confirm payment for order in status: %s", status));
        }
        
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
        
        this.status = OrderStatus.PAYMENT_CONFIRMED;
        this.updatedAt = LocalDateTime.now();
        
        // 發布領域事件
        addDomainEvent(new PaymentConfirmedEvent(this.orderId, this.transactionId, paymentId));
    }
    
    /**
     * 標記支付失敗
     * @param reason 失敗原因
     */
    public void failPayment(String reason) {
        if (!status.canFailPayment()) {
            throw new IllegalStateException(
                String.format("Cannot fail payment for order in status: %s", status));
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure reason cannot be null or empty");
        }
        
        this.status = OrderStatus.PAYMENT_FAILED;
        this.updatedAt = LocalDateTime.now();
        
        // 發布領域事件
        addDomainEvent(new PaymentFailedEvent(this.orderId, this.transactionId, reason));
    }
    
    /**
     * 取消訂單
     */
    public void cancel() {
        if (!status.canCancel()) {
            throw new IllegalStateException(
                String.format("Cannot cancel order in status: %s", status));
        }
        
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 檢查訂單是否可以重試支付
     * @return true 如果可以重試支付
     */
    public boolean canRetryPayment() {
        return status == OrderStatus.PAYMENT_FAILED;
    }
    
    /**
     * 重試支付
     * @param newTransactionId 新的交易ID
     */
    public void retryPayment(String newTransactionId) {
        if (!canRetryPayment()) {
            throw new IllegalStateException(
                String.format("Cannot retry payment for order in status: %s", status));
        }
        
        requestPayment(newTransactionId);
    }
    
    // Getters
    public OrderId getOrderId() {
        return orderId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    @Override
    public String toString() {
        return String.format("Order{orderId=%s, customerId=%s, amount=%s, status=%s}", 
            orderId, customerId, amount, status);
    }
}
package com.example.paymentservice.domain.payment;

import com.example.paymentservice.domain.shared.AggregateRoot;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.valueobject.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 支付聚合根
 */
@Entity
@Table(name = "payment_records")
public class Payment extends AggregateRoot {
    
    @EmbeddedId
    private PaymentId paymentId;
    
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;
    
    @Column(name = "order_id", nullable = false)
    private String orderId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money amount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "cardNumber", column = @Column(name = "card_number")),
        @AttributeOverride(name = "expiryDate", column = @Column(name = "expiry_date")),
        @AttributeOverride(name = "cvv", column = @Column(name = "cvv")),
        @AttributeOverride(name = "cardHolderName", column = @Column(name = "card_holder_name"))
    })
    private CreditCard creditCard;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;
    
    @Column(name = "gateway_response")
    private String gatewayResponse;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    // JPA 需要的默認構造函數
    protected Payment() {}
    
    /**
     * 創建新的支付記錄
     * @param transactionId 交易ID
     * @param orderId 訂單ID
     * @param customerId 客戶ID
     * @param amount 支付金額
     * @param creditCard 信用卡信息
     */
    public Payment(String transactionId, String orderId, String customerId, 
                  Money amount, CreditCard creditCard) {
        validateConstructorParameters(transactionId, orderId, customerId, amount, creditCard);
        
        this.paymentId = PaymentId.generate();
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.creditCard = creditCard;
        this.status = PaymentStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * 驗證構造函數參數
     */
    private void validateConstructorParameters(String transactionId, String orderId, 
                                             String customerId, Money amount, CreditCard creditCard) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (creditCard == null) {
            throw new IllegalArgumentException("Credit card cannot be null");
        }
    }
    
    /**
     * 處理支付
     * @param gatewayResponse 支付網關響應
     * @return 支付結果
     */
    public PaymentResult process(String gatewayResponse) {
        if (status.isFinalStatus()) {
            throw new IllegalStateException(
                String.format("Cannot process payment in final status: %s", status));
        }
        
        // 驗證信用卡是否過期
        if (creditCard.isExpired()) {
            return failPayment("Credit card has expired", gatewayResponse);
        }
        
        // 模擬支付處理邏輯
        if (isPaymentSuccessful(gatewayResponse)) {
            return completePayment(gatewayResponse);
        } else {
            return failPayment("Payment processing failed", gatewayResponse);
        }
    }
    
    /**
     * 完成支付
     * @param gatewayResponse 支付網關響應
     * @return 成功的支付結果
     */
    private PaymentResult completePayment(String gatewayResponse) {
        this.status = PaymentStatus.SUCCESS;
        this.gatewayResponse = gatewayResponse;
        this.processedAt = LocalDateTime.now();
        
        // 發布領域事件
        addDomainEvent(new PaymentProcessedEvent(
            this.paymentId, 
            this.orderId, 
            this.transactionId,
            this.amount.getAmount(),
            this.amount.getCurrency()
        ));
        
        return PaymentResult.success(this.paymentId.getValue());
    }
    
    /**
     * 支付失敗
     * @param errorMessage 錯誤信息
     * @param gatewayResponse 支付網關響應
     * @return 失敗的支付結果
     */
    private PaymentResult failPayment(String errorMessage, String gatewayResponse) {
        this.status = PaymentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.gatewayResponse = gatewayResponse;
        this.processedAt = LocalDateTime.now();
        
        // 發布領域事件
        addDomainEvent(new PaymentFailedEvent(
            this.paymentId, 
            this.orderId, 
            this.transactionId, 
            errorMessage
        ));
        
        return PaymentResult.failed(errorMessage);
    }
    
    /**
     * 模擬判斷支付是否成功
     * @param gatewayResponse 支付網關響應
     * @return true 如果支付成功
     */
    private boolean isPaymentSuccessful(String gatewayResponse) {
        // 簡單的模擬邏輯：如果響應包含"SUCCESS"則認為成功
        return gatewayResponse != null && gatewayResponse.contains("SUCCESS");
    }
    
    /**
     * 退款
     * @param refundReason 退款原因
     */
    public void refund(String refundReason) {
        if (!status.canRefund()) {
            throw new IllegalStateException(
                String.format("Cannot refund payment in status: %s", status));
        }
        
        if (refundReason == null || refundReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Refund reason cannot be null or empty");
        }
        
        this.status = PaymentStatus.REFUNDED;
        this.errorMessage = refundReason;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 檢查支付是否可以重試
     * @return true 如果可以重試
     */
    public boolean canRetry() {
        return status == PaymentStatus.FAILED;
    }
    
    // Getters
    public PaymentId getPaymentId() {
        return paymentId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public CreditCard getCreditCard() {
        return creditCard;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public String getGatewayResponse() {
        return gatewayResponse;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    @Override
    public String toString() {
        return String.format("Payment{paymentId=%s, transactionId='%s', orderId='%s', amount=%s, status=%s}", 
            paymentId, transactionId, orderId, amount, status);
    }
}
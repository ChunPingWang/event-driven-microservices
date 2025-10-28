package com.example.paymentservice.infrastructure.messaging;

import com.example.paymentservice.application.service.PaymentApplicationService;
import com.example.paymentservice.messaging.PaymentRequest;
import com.example.logging.annotation.LogMessageEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 支付請求監聽器 (基礎設施層適配器)
 */
@Component
public class PaymentRequestListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentRequestListener.class);
    
    private final PaymentApplicationService paymentApplicationService;
    private final ObjectMapper objectMapper;
    
    public PaymentRequestListener(PaymentApplicationService paymentApplicationService, ObjectMapper objectMapper) {
        this.paymentApplicationService = paymentApplicationService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 監聽支付請求消息
     */
    @RabbitListener(queues = "${messaging.payment.request.queue:payment.request.queue}")
    @LogMessageEvent(eventType = "PAYMENT_REQUEST_RECEIVED", logPayload = true)
    public void handlePaymentRequest(@Payload String messageBody,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                   @Header(value = "orderId", required = false) String orderId,
                                   @Header(value = "transactionId", required = false) String transactionId,
                                   @Header(value = "customerId", required = false) String customerId,
                                   Message message) {
        
        logger.info("Received payment request message: orderId={}, transactionId={}, customerId={}", 
            orderId, transactionId, customerId);
        
        try {
            // 反序列化消息
            PaymentRequest paymentRequest = objectMapper.readValue(messageBody, PaymentRequest.class);
            
            // 驗證消息內容
            validatePaymentRequest(paymentRequest);
            
            // 處理支付請求
            processPaymentRequest(paymentRequest);
            
            logger.info("Successfully processed payment request: transactionId={}, orderId={}, amount={}", 
                paymentRequest.getTransactionId(), paymentRequest.getOrderId(), paymentRequest.getAmount());
            
        } catch (Exception e) {
            logger.error("Failed to process payment request: orderId={}, transactionId={}, error={}", 
                orderId, transactionId, e.getMessage(), e);
            
            // 根據錯誤類型決定是否重試
            if (shouldRetry(e)) {
                throw new MessageProcessingException("Retryable error processing payment request", e);
            } else {
                logger.error("Non-retryable error, message will be discarded: orderId={}", orderId);
                // 可以選擇發送錯誤響應或記錄到錯誤表
                sendErrorResponse(orderId, transactionId, e.getMessage());
            }
        }
    }
    
    /**
     * 處理支付請求
     */
    private void processPaymentRequest(PaymentRequest paymentRequest) {
        try {
            // 調用應用服務處理支付
            paymentApplicationService.processPayment(
                paymentRequest.getTransactionId(),
                paymentRequest.getOrderId(),
                paymentRequest.getCustomerId(),
                paymentRequest.getAmount(),
                paymentRequest.getCurrency(),
                convertCreditCard(paymentRequest.getCreditCard()),
                paymentRequest.getDescription()
            );
            
        } catch (Exception e) {
            logger.error("Payment processing failed: transactionId={}, orderId={}, error={}", 
                paymentRequest.getTransactionId(), paymentRequest.getOrderId(), e.getMessage(), e);
            throw e; // 重新拋出異常以觸發重試或錯誤處理
        }
    }
    
    /**
     * 轉換信用卡信息
     */
    private com.example.paymentservice.domain.payment.valueobject.CreditCard convertCreditCard(
            PaymentRequest.CreditCardInfo creditCardInfo) {
        
        if (creditCardInfo == null) {
            throw new IllegalArgumentException("Credit card information cannot be null");
        }
        
        return new com.example.paymentservice.domain.payment.valueobject.CreditCard(
            creditCardInfo.getCardNumber(),
            creditCardInfo.getExpiryDate(),
            creditCardInfo.getCvv(),
            creditCardInfo.getCardHolderName()
        );
    }
    
    /**
     * 驗證支付請求消息
     */
    private void validatePaymentRequest(PaymentRequest paymentRequest) {
        if (paymentRequest == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        
        if (paymentRequest.getTransactionId() == null || paymentRequest.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        
        if (paymentRequest.getOrderId() == null || paymentRequest.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        
        if (paymentRequest.getCustomerId() == null || paymentRequest.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        if (paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (paymentRequest.getCurrency() == null || paymentRequest.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        
        if (paymentRequest.getCreditCard() == null) {
            throw new IllegalArgumentException("Credit card information cannot be null");
        }
        
        // 驗證信用卡信息
        validateCreditCardInfo(paymentRequest.getCreditCard());
    }
    
    /**
     * 驗證信用卡信息
     */
    private void validateCreditCardInfo(PaymentRequest.CreditCardInfo creditCard) {
        if (creditCard.getCardNumber() == null || creditCard.getCardNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Card number cannot be null or empty");
        }
        
        if (creditCard.getExpiryDate() == null || creditCard.getExpiryDate().trim().isEmpty()) {
            throw new IllegalArgumentException("Expiry date cannot be null or empty");
        }
        
        if (creditCard.getCvv() == null || creditCard.getCvv().trim().isEmpty()) {
            throw new IllegalArgumentException("CVV cannot be null or empty");
        }
        
        if (creditCard.getCardHolderName() == null || creditCard.getCardHolderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Card holder name cannot be null or empty");
        }
    }
    
    /**
     * 發送錯誤響應
     */
    private void sendErrorResponse(String orderId, String transactionId, String errorMessage) {
        try {
            // 創建錯誤響應並發送
            paymentApplicationService.sendPaymentFailureNotification(orderId, transactionId, errorMessage);
            
        } catch (Exception e) {
            logger.error("Failed to send error response: orderId={}, transactionId={}", 
                orderId, transactionId, e);
        }
    }
    
    /**
     * 判斷是否應該重試
     */
    private boolean shouldRetry(Exception e) {
        // 業務邏輯錯誤不重試
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return false;
        }
        
        // 序列化錯誤不重試
        if (e instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            return false;
        }
        
        // 信用卡驗證錯誤不重試
        if (e.getMessage() != null && e.getMessage().contains("Invalid credit card")) {
            return false;
        }
        
        // 其他錯誤可以重試（如數據庫連接錯誤、網絡錯誤等）
        return true;
    }
    
    /**
     * 消息處理異常
     */
    public static class MessageProcessingException extends RuntimeException {
        public MessageProcessingException(String message) {
            super(message);
        }
        
        public MessageProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
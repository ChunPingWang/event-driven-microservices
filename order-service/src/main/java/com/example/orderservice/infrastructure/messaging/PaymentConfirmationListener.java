package com.example.orderservice.infrastructure.messaging;

import com.example.orderservice.application.service.OrderApplicationService;
import com.example.orderservice.messaging.PaymentConfirmation;
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
 * 支付確認監聽器 (基礎設施層適配器)
 */
@Component
public class PaymentConfirmationListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentConfirmationListener.class);
    
    private final OrderApplicationService orderApplicationService;
    private final ObjectMapper objectMapper;
    
    public PaymentConfirmationListener(OrderApplicationService orderApplicationService, ObjectMapper objectMapper) {
        this.orderApplicationService = orderApplicationService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 監聽支付確認消息
     */
    @RabbitListener(queues = "${messaging.payment.confirmation.queue:payment.confirmation.queue}")
    @LogMessageEvent(eventType = "PAYMENT_CONFIRMATION_RECEIVED", logPayload = true)
    public void handlePaymentConfirmation(@Payload String messageBody,
                                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                        @Header(value = "orderId", required = false) String orderId,
                                        @Header(value = "transactionId", required = false) String transactionId,
                                        Message message) {
        
        logger.info("Received payment confirmation message: orderId={}, transactionId={}", orderId, transactionId);
        
        try {
            // 反序列化消息
            PaymentConfirmation confirmation = objectMapper.readValue(messageBody, PaymentConfirmation.class);
            
            // 驗證消息內容
            validatePaymentConfirmation(confirmation);
            
            // 處理支付確認
            processPaymentConfirmation(confirmation);
            
            logger.info("Successfully processed payment confirmation: paymentId={}, orderId={}, status={}", 
                confirmation.getPaymentId(), confirmation.getOrderId(), confirmation.getStatus());
            
        } catch (Exception e) {
            logger.error("Failed to process payment confirmation: orderId={}, transactionId={}, error={}", 
                orderId, transactionId, e.getMessage(), e);
            
            // 根據錯誤類型決定是否重試
            if (shouldRetry(e)) {
                throw new MessageProcessingException("Retryable error processing payment confirmation", e);
            } else {
                logger.error("Non-retryable error, message will be discarded: orderId={}", orderId);
                // 可以選擇發送到死信隊列或記錄到錯誤表
            }
        }
    }
    
    /**
     * 處理支付確認
     */
    private void processPaymentConfirmation(PaymentConfirmation confirmation) {
        switch (confirmation.getStatus()) {
            case SUCCESS:
                orderApplicationService.handlePaymentSuccess(
                    confirmation.getOrderId(), 
                    confirmation.getPaymentId(),
                    confirmation.getTransactionId()
                );
                break;
                
            case FAILED:
                orderApplicationService.handlePaymentFailure(
                    confirmation.getOrderId(),
                    confirmation.getTransactionId(),
                    confirmation.getErrorMessage()
                );
                break;
                
            case PENDING:
                logger.info("Payment is still pending: orderId={}, paymentId={}", 
                    confirmation.getOrderId(), confirmation.getPaymentId());
                // 可能需要設置定時器檢查狀態
                break;
                
            case CANCELLED:
                orderApplicationService.handlePaymentCancellation(
                    confirmation.getOrderId(),
                    confirmation.getTransactionId()
                );
                break;
                
            default:
                logger.warn("Unknown payment status: {}, orderId={}", 
                    confirmation.getStatus(), confirmation.getOrderId());
                throw new IllegalArgumentException("Unknown payment status: " + confirmation.getStatus());
        }
    }
    
    /**
     * 驗證支付確認消息
     */
    private void validatePaymentConfirmation(PaymentConfirmation confirmation) {
        if (confirmation == null) {
            throw new IllegalArgumentException("Payment confirmation cannot be null");
        }
        
        if (confirmation.getOrderId() == null || confirmation.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        
        if (confirmation.getTransactionId() == null || confirmation.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        
        if (confirmation.getStatus() == null) {
            throw new IllegalArgumentException("Payment status cannot be null");
        }
        
        // 對於成功的支付，支付ID不能為空
        if (confirmation.getStatus() == PaymentConfirmation.PaymentStatus.SUCCESS) {
            if (confirmation.getPaymentId() == null || confirmation.getPaymentId().trim().isEmpty()) {
                throw new IllegalArgumentException("Payment ID cannot be null or empty for successful payment");
            }
        }
        
        // 對於失敗的支付，錯誤信息不能為空
        if (confirmation.getStatus() == PaymentConfirmation.PaymentStatus.FAILED) {
            if (confirmation.getErrorMessage() == null || confirmation.getErrorMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("Error message cannot be null or empty for failed payment");
            }
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
        
        // 其他錯誤可以重試
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
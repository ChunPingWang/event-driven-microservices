package com.example.paymentservice.infrastructure.messaging;

import com.example.paymentservice.application.port.out.PaymentConfirmationPublisher;
import com.example.paymentservice.messaging.PaymentConfirmation;
import com.example.logging.annotation.LogMessageEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付確認發布器實現 (基礎設施層適配器)
 */
@Component
public class PaymentConfirmationPublisherImpl implements PaymentConfirmationPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentConfirmationPublisherImpl.class);
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${messaging.payment.confirmation.exchange:payment.exchange}")
    private String paymentExchange;
    
    @Value("${messaging.payment.confirmation.routing-key:payment.confirmation}")
    private String paymentConfirmationRoutingKey;
    
    public PaymentConfirmationPublisherImpl(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @LogMessageEvent(eventType = "PAYMENT_CONFIRMATION_PUBLISHED", logPayload = true)
    public void publishPaymentConfirmation(PaymentConfirmation confirmation) {
        if (confirmation == null) {
            throw new IllegalArgumentException("Payment confirmation cannot be null");
        }
        
        try {
            // 設置處理時間
            if (confirmation.getProcessedAt() == null) {
                confirmation.setProcessedAt(LocalDateTime.now());
            }
            
            // 序列化消息
            String messageBody = objectMapper.writeValueAsString(confirmation);
            
            // 發布消息
            publishMessage(messageBody, confirmation);
            
            logger.info("Published payment confirmation: paymentId={}, orderId={}, status={}", 
                confirmation.getPaymentId(), confirmation.getOrderId(), confirmation.getStatus());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize payment confirmation: paymentId={}", 
                confirmation.getPaymentId(), e);
            throw new MessagePublishingException("Failed to serialize payment confirmation", e);
        } catch (Exception e) {
            logger.error("Failed to publish payment confirmation: paymentId={}", 
                confirmation.getPaymentId(), e);
            throw new MessagePublishingException("Failed to publish payment confirmation", e);
        }
    }
    
    @Override
    @LogMessageEvent(eventType = "PAYMENT_CONFIRMATION_PUBLISHED_FROM_JSON", logPayload = true)
    public void publishPaymentConfirmation(String confirmationJson) {
        if (confirmationJson == null || confirmationJson.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment confirmation JSON cannot be null or empty");
        }
        
        try {
            // 解析JSON以獲取基本信息用於日誌
            PaymentConfirmation confirmation = objectMapper.readValue(confirmationJson, PaymentConfirmation.class);
            
            // 發布消息
            publishMessage(confirmationJson, confirmation);
            
            logger.info("Published payment confirmation from JSON: paymentId={}, orderId={}, status={}", 
                confirmation.getPaymentId(), confirmation.getOrderId(), confirmation.getStatus());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse payment confirmation JSON: {}", confirmationJson, e);
            throw new MessagePublishingException("Failed to parse payment confirmation JSON", e);
        } catch (Exception e) {
            logger.error("Failed to publish payment confirmation from JSON: {}", confirmationJson, e);
            throw new MessagePublishingException("Failed to publish payment confirmation from JSON", e);
        }
    }
    
    /**
     * 發布消息的通用方法
     */
    private void publishMessage(String messageBody, PaymentConfirmation confirmation) {
        try {
            // 創建消息屬性
            MessageProperties properties = createMessageProperties(confirmation);
            
            // 創建消息
            Message message = new Message(messageBody.getBytes(), properties);
            
            // 發送消息
            rabbitTemplate.send(paymentExchange, paymentConfirmationRoutingKey, message);
            
        } catch (Exception e) {
            throw new MessagePublishingException("Failed to publish message", e);
        }
    }
    
    /**
     * 創建消息屬性
     */
    private MessageProperties createMessageProperties(PaymentConfirmation confirmation) {
        MessageProperties properties = new MessageProperties();
        
        // 基本屬性
        properties.setContentType("application/json");
        properties.setContentEncoding("UTF-8");
        properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE); // 持久化
        
        // 消息ID和相關ID
        properties.setMessageId(UUID.randomUUID().toString());
        properties.setCorrelationId(confirmation.getTransactionId());
        
        // 時間戳
        LocalDateTime processedAt = confirmation.getProcessedAt() != null ? 
            confirmation.getProcessedAt() : LocalDateTime.now();
        properties.setTimestamp(java.util.Date.from(
            processedAt.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        
        // 自定義頭部
        properties.setHeader("eventType", "PaymentConfirmation");
        properties.setHeader("paymentId", confirmation.getPaymentId());
        properties.setHeader("orderId", confirmation.getOrderId());
        properties.setHeader("transactionId", confirmation.getTransactionId());
        properties.setHeader("status", confirmation.getStatus().toString());
        properties.setHeader("source", "payment-service");
        properties.setHeader("version", "1.0");
        
        if (confirmation.getAmount() != null) {
            properties.setHeader("amount", confirmation.getAmount().toString());
        }
        if (confirmation.getCurrency() != null) {
            properties.setHeader("currency", confirmation.getCurrency());
        }
        
        // 設置過期時間 (30分鐘)
        properties.setExpiration("1800000");
        
        // 根據狀態設置優先級
        if (confirmation.getStatus() == PaymentConfirmation.PaymentStatus.FAILED) {
            properties.setPriority(5); // 失敗消息優先級較高
        } else {
            properties.setPriority(1); // 正常優先級
        }
        
        return properties;
    }
    
    /**
     * 消息發布異常
     */
    public static class MessagePublishingException extends RuntimeException {
        public MessagePublishingException(String message) {
            super(message);
        }
        
        public MessagePublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
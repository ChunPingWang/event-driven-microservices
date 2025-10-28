package com.example.orderservice.infrastructure.messaging;

import com.example.orderservice.application.port.out.PaymentRequestPublisher;
import com.example.orderservice.messaging.PaymentRequest;
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
 * 支付請求發布器實現 (基礎設施層適配器)
 */
@Component
public class PaymentRequestPublisherImpl implements PaymentRequestPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentRequestPublisherImpl.class);
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${messaging.payment.request.exchange:payment.exchange}")
    private String paymentExchange;
    
    @Value("${messaging.payment.request.routing-key:payment.request}")
    private String paymentRequestRoutingKey;
    
    public PaymentRequestPublisherImpl(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @LogMessageEvent(eventType = "PAYMENT_REQUEST_PUBLISHED", logPayload = true)
    public void publishPaymentRequest(PaymentRequest paymentRequest) {
        if (paymentRequest == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        
        try {
            // 設置時間戳
            if (paymentRequest.getTimestamp() == null) {
                paymentRequest.setTimestamp(LocalDateTime.now());
            }
            
            // 序列化消息
            String messageBody = objectMapper.writeValueAsString(paymentRequest);
            
            // 創建消息屬性
            MessageProperties properties = createMessageProperties(paymentRequest);
            
            // 創建消息
            Message message = new Message(messageBody.getBytes(), properties);
            
            // 發送消息
            rabbitTemplate.send(paymentExchange, paymentRequestRoutingKey, message);
            
            logger.info("Published payment request: transactionId={}, orderId={}, amount={}", 
                paymentRequest.getTransactionId(), paymentRequest.getOrderId(), paymentRequest.getAmount());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize payment request: transactionId={}", 
                paymentRequest.getTransactionId(), e);
            throw new MessagePublishingException("Failed to serialize payment request", e);
        } catch (Exception e) {
            logger.error("Failed to publish payment request: transactionId={}", 
                paymentRequest.getTransactionId(), e);
            throw new MessagePublishingException("Failed to publish payment request", e);
        }
    }
    
    /**
     * 創建消息屬性
     */
    private MessageProperties createMessageProperties(PaymentRequest paymentRequest) {
        MessageProperties properties = new MessageProperties();
        
        // 基本屬性
        properties.setContentType("application/json");
        properties.setContentEncoding("UTF-8");
        properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE); // 持久化
        
        // 消息ID和相關ID
        properties.setMessageId(UUID.randomUUID().toString());
        properties.setCorrelationId(paymentRequest.getTransactionId());
        
        // 時間戳
        properties.setTimestamp(java.util.Date.from(
            paymentRequest.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        
        // 自定義頭部
        properties.setHeader("eventType", "PaymentRequest");
        properties.setHeader("orderId", paymentRequest.getOrderId());
        properties.setHeader("customerId", paymentRequest.getCustomerId());
        properties.setHeader("amount", paymentRequest.getAmount().toString());
        properties.setHeader("currency", paymentRequest.getCurrency());
        properties.setHeader("source", "order-service");
        properties.setHeader("version", "1.0");
        
        // 設置過期時間 (30分鐘)
        properties.setExpiration("1800000");
        
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
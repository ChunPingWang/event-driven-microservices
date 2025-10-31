package com.example.orderservice.domain.event.handler;

import com.example.logging.annotation.LogMessageEvent;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付請求事件處理器 - 處理支付請求事件的業務邏輯
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestedEventHandler {

    private final DomainEventPublisher domainEventPublisher;

    /**
     * 處理支付請求事件
     * @param event 支付請求事件
     */
    @LogMessageEvent(eventType = "PAYMENT_REQUESTED_EVENT_PROCESSING", logPayload = true)
    @Transactional
    public void handle(PaymentRequestedEvent event) {
        try {
            log.info("Processing PaymentRequestedEvent for order: {}, transaction: {}", 
                event.getOrderId().getValue(), event.getTransactionId());

            // 驗證事件數據
            validateEvent(event);

            // 發布事件到消息系統
            domainEventPublisher.publishEvent(event);

            log.info("Successfully processed PaymentRequestedEvent for order: {}", 
                event.getOrderId().getValue());

        } catch (Exception e) {
            log.error("Failed to process PaymentRequestedEvent for order: {}, transaction: {}", 
                event.getOrderId().getValue(), event.getTransactionId(), e);
            
            // 重新拋出異常以觸發事務回滾
            throw new PaymentRequestedEventProcessingException(
                "Failed to process PaymentRequestedEvent for order: " + event.getOrderId().getValue(), e);
        }
    }

    /**
     * 驗證支付請求事件
     */
    private void validateEvent(PaymentRequestedEvent event) {
        if (event.getOrderId() == null || event.getOrderId().getValue() == null || event.getOrderId().getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (event.getTransactionId() == null || event.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }

        if (event.getCustomerId() == null || event.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }

        log.debug("PaymentRequestedEvent validation passed for order: {}", event.getOrderId().getValue());
    }

    /**
     * 支付請求事件處理異常
     */
    public static class PaymentRequestedEventProcessingException extends RuntimeException {
        public PaymentRequestedEventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
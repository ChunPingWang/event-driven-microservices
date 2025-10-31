package com.example.paymentservice.domain.event.handler;

import com.example.logging.annotation.LogMessageEvent;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付處理完成事件處理器 - 處理支付處理完成事件的業務邏輯
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessedEventHandler {

    private final DomainEventPublisher domainEventPublisher;

    /**
     * 處理支付處理完成事件
     * @param event 支付處理完成事件
     */
    @LogMessageEvent(eventType = "PAYMENT_PROCESSED_EVENT_PROCESSING", logPayload = true)
    @Transactional
    public void handle(PaymentProcessedEvent event) {
        try {
            log.info("Processing PaymentProcessedEvent for payment: {}, order: {}, transaction: {}", 
                event.getPaymentId().getValue(), event.getOrderId(), event.getTransactionId());

            // 驗證事件數據
            validateEvent(event);

            // 發布事件到Outbox系統
            domainEventPublisher.publishEvent(event);

            log.info("Successfully processed PaymentProcessedEvent for payment: {}, order: {}", 
                event.getPaymentId().getValue(), event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process PaymentProcessedEvent for payment: {}, order: {}, transaction: {}", 
                event.getPaymentId().getValue(), event.getOrderId(), event.getTransactionId(), e);
            
            // 重新拋出異常以觸發事務回滾
            throw new PaymentProcessedEventProcessingException(
                "Failed to process PaymentProcessedEvent for payment: " + event.getPaymentId().getValue(), e);
        }
    }

    /**
     * 驗證支付處理完成事件
     */
    private void validateEvent(PaymentProcessedEvent event) {
        if (event.getPaymentId() == null || event.getPaymentId().getValue() == null || event.getPaymentId().getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }

        if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (event.getTransactionId() == null || event.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }

        if (event.getAmount() == null || event.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (event.getCurrency() == null || event.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }

        log.debug("PaymentProcessedEvent validation passed for payment: {}", event.getPaymentId().getValue());
    }

    /**
     * 支付處理完成事件處理異常
     */
    public static class PaymentProcessedEventProcessingException extends RuntimeException {
        public PaymentProcessedEventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
package com.example.paymentservice.domain.event.handler;

import com.example.logging.annotation.LogMessageEvent;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付失敗事件處理器 - 處理支付失敗事件的業務邏輯
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedEventHandler {

    private final DomainEventPublisher domainEventPublisher;

    /**
     * 處理支付失敗事件
     * @param event 支付失敗事件
     */
    @LogMessageEvent(eventType = "PAYMENT_FAILED_EVENT_PROCESSING", logPayload = true)
    @Transactional
    public void handle(PaymentFailedEvent event) {
        try {
            log.info("Processing PaymentFailedEvent for payment: {}, order: {}, transaction: {}", 
                event.getPaymentId().getValue(), event.getOrderId(), event.getTransactionId());

            // 驗證事件數據
            validateEvent(event);

            // 發布事件到Outbox系統
            domainEventPublisher.publishEvent(event);

            log.warn("Successfully processed PaymentFailedEvent for payment: {}, order: {}, error: {}", 
                event.getPaymentId().getValue(), event.getOrderId(), event.getErrorMessage());

        } catch (Exception e) {
            log.error("Failed to process PaymentFailedEvent for payment: {}, order: {}, transaction: {}", 
                event.getPaymentId().getValue(), event.getOrderId(), event.getTransactionId(), e);
            
            // 重新拋出異常以觸發事務回滾
            throw new PaymentFailedEventProcessingException(
                "Failed to process PaymentFailedEvent for payment: " + event.getPaymentId().getValue(), e);
        }
    }

    /**
     * 驗證支付失敗事件
     */
    private void validateEvent(PaymentFailedEvent event) {
        if (event.getPaymentId() == null || event.getPaymentId().getValue() == null || event.getPaymentId().getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }

        if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (event.getTransactionId() == null || event.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }

        if (event.getErrorMessage() == null || event.getErrorMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }

        log.debug("PaymentFailedEvent validation passed for payment: {}", event.getPaymentId().getValue());
    }

    /**
     * 支付失敗事件處理異常
     */
    public static class PaymentFailedEventProcessingException extends RuntimeException {
        public PaymentFailedEventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
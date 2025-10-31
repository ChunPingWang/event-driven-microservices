package com.example.paymentservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.infrastructure.outbox.OutboxService;
import com.example.paymentservice.infrastructure.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付服務領域事件發布器 - 負責將領域事件轉換為Outbox事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final OutboxService outboxService;

    /**
     * 發布領域事件列表
     * @param events 領域事件列表
     */
    @LogMessageEvent(eventType = "DOMAIN_EVENTS_PUBLISHED", logPayload = true)
    public void publishEvents(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        log.info("Publishing {} domain events to outbox", events.size());
        
        for (DomainEvent event : events) {
            try {
                publishEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish domain event to outbox: {}", event.getEventType(), e);
                throw new DomainEventPublishingException("Failed to publish domain event: " + event.getEventType(), e);
            }
        }
    }

    /**
     * 發布單個領域事件
     * @param event 領域事件
     */
    @LogMessageEvent(eventType = "DOMAIN_EVENT_PUBLISHED", logPayload = true)
    public void publishEvent(DomainEvent event) {
        log.debug("Publishing domain event to outbox: {} with ID: {}", event.getEventType(), event.getEventId());
        
        try {
            if (event instanceof PaymentProcessedEvent paymentProcessedEvent) {
                handlePaymentProcessedEvent(paymentProcessedEvent);
            } else if (event instanceof PaymentFailedEvent paymentFailedEvent) {
                handlePaymentFailedEvent(paymentFailedEvent);
            } else {
                log.warn("Unknown domain event type: {}", event.getEventType());
                throw new UnsupportedDomainEventException("Unsupported domain event type: " + event.getEventType());
            }
        } catch (UnsupportedDomainEventException e) {
            // Re-throw UnsupportedDomainEventException as-is
            throw e;
        } catch (Exception e) {
            log.error("Failed to publish domain event to outbox: {}", event.getEventType(), e);
            throw new DomainEventPublishingException("Failed to publish domain event: " + event.getEventType(), e);
        }
    }

    /**
     * 處理支付處理完成事件
     */
    @LogMessageEvent(eventType = "PAYMENT_PROCESSED_EVENT_HANDLED", logPayload = true)
    private void handlePaymentProcessedEvent(PaymentProcessedEvent event) {
        log.info("Handling PaymentProcessedEvent for payment: {}, order: {}", 
            event.getPaymentId().getValue(), event.getOrderId());
        
        // 使用OutboxService保存領域事件
        outboxService.saveEvent(event, event.getPaymentId().getValue(), "Payment");
        
        log.info("PaymentProcessedEvent successfully converted and saved to outbox for payment: {}", 
            event.getPaymentId().getValue());
    }

    /**
     * 處理支付失敗事件
     */
    @LogMessageEvent(eventType = "PAYMENT_FAILED_EVENT_HANDLED", logPayload = true)
    private void handlePaymentFailedEvent(PaymentFailedEvent event) {
        log.info("Handling PaymentFailedEvent for payment: {}, order: {}", 
            event.getPaymentId().getValue(), event.getOrderId());
        
        // 使用OutboxService保存領域事件
        outboxService.saveEvent(event, event.getPaymentId().getValue(), "Payment");
        
        log.warn("PaymentFailedEvent successfully converted and saved to outbox for payment: {}, error: {}", 
            event.getPaymentId().getValue(), event.getErrorMessage());
    }

    /**
     * 領域事件發布異常
     */
    public static class DomainEventPublishingException extends RuntimeException {
        public DomainEventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 不支持的領域事件異常
     */
    public static class UnsupportedDomainEventException extends RuntimeException {
        public UnsupportedDomainEventException(String message) {
            super(message);
        }
    }
}
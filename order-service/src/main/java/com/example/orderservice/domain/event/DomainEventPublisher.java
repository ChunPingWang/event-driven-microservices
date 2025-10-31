package com.example.orderservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import com.example.orderservice.domain.shared.DomainEvent;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.event.PaymentConfirmedEvent;
import com.example.orderservice.domain.order.event.PaymentFailedEvent;
import com.example.orderservice.messaging.MessagePublisher;
import com.example.orderservice.messaging.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 領域事件發布器 - 負責將領域事件轉換為消息並發布
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final MessagePublisher messagePublisher;
    private final DomainEventToMessageConverter eventConverter;

    /**
     * 發布領域事件列表
     * @param events 領域事件列表
     */
    @LogMessageEvent(eventType = "DOMAIN_EVENTS_PUBLISHED", logPayload = true)
    public void publishEvents(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        log.info("Publishing {} domain events", events.size());
        
        for (DomainEvent event : events) {
            try {
                publishEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish domain event: {}", event.getEventType(), e);
                // 在實際應用中，這裡可能需要將失敗的事件存儲到重試隊列
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
        log.debug("Publishing domain event: {} with ID: {}", event.getEventType(), event.getEventId());
        
        try {
            if (event instanceof PaymentRequestedEvent paymentRequestedEvent) {
                handlePaymentRequestedEvent(paymentRequestedEvent);
            } else if (event instanceof PaymentConfirmedEvent paymentConfirmedEvent) {
                handlePaymentConfirmedEvent(paymentConfirmedEvent);
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
            log.error("Failed to publish domain event: {}", event.getEventType(), e);
            throw new DomainEventPublishingException("Failed to publish domain event: " + event.getEventType(), e);
        }
    }

    /**
     * 處理支付請求事件
     */
    @LogMessageEvent(eventType = "PAYMENT_REQUESTED_EVENT_HANDLED", logPayload = true)
    private void handlePaymentRequestedEvent(PaymentRequestedEvent event) {
        log.info("Handling PaymentRequestedEvent for order: {}, transaction: {}", 
            event.getOrderId().getValue(), event.getTransactionId());
        
        // 將領域事件轉換為支付請求消息
        PaymentRequest paymentRequest = eventConverter.convertToPaymentRequest(event);
        
        // 發布支付請求消息
        messagePublisher.publishPaymentRequest(paymentRequest);
        
        log.info("PaymentRequestedEvent successfully converted and published for order: {}", 
            event.getOrderId().getValue());
    }

    /**
     * 處理支付確認事件
     */
    @LogMessageEvent(eventType = "PAYMENT_CONFIRMED_EVENT_HANDLED", logPayload = true)
    private void handlePaymentConfirmedEvent(PaymentConfirmedEvent event) {
        log.info("Handling PaymentConfirmedEvent for order: {}, transaction: {}", 
            event.getOrderId().getValue(), event.getTransactionId());
        
        // 支付確認事件通常不需要發送外部消息，因為它是響應外部消息的結果
        // 但可以記錄日誌或觸發其他內部處理
        log.info("Payment confirmed for order: {}, payment: {}", 
            event.getOrderId().getValue(), event.getPaymentId());
    }

    /**
     * 處理支付失敗事件
     */
    @LogMessageEvent(eventType = "PAYMENT_FAILED_EVENT_HANDLED", logPayload = true)
    private void handlePaymentFailedEvent(PaymentFailedEvent event) {
        log.info("Handling PaymentFailedEvent for order: {}, transaction: {}", 
            event.getOrderId().getValue(), event.getTransactionId());
        
        // 支付失敗時可能需要觸發重試機制或通知相關系統
        log.warn("Payment failed for order: {}, reason: {}", 
            event.getOrderId().getValue(), event.getReason());
        
        // 這裡可以觸發重試邏輯或發送失敗通知
        // 暫時只記錄日誌
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
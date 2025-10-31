package com.example.orderservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import com.example.orderservice.domain.shared.AggregateRoot;
import com.example.orderservice.domain.shared.DomainEvent;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.event.handler.PaymentRequestedEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 領域事件調度器 - 負責調度和處理聚合根產生的領域事件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventDispatcher {

    private final PaymentRequestedEventHandler paymentRequestedEventHandler;

    /**
     * 處理聚合根的所有領域事件
     * @param aggregateRoot 聚合根
     */
    @LogMessageEvent(eventType = "DOMAIN_EVENTS_DISPATCHED", logPayload = true)
    @Transactional
    public void dispatchEvents(AggregateRoot aggregateRoot) {
        if (aggregateRoot == null || !aggregateRoot.hasDomainEvents()) {
            return;
        }

        List<DomainEvent> events = aggregateRoot.getDomainEvents();
        log.info("Dispatching {} domain events from aggregate", events.size());

        try {
            for (DomainEvent event : events) {
                dispatchEvent(event);
            }

            // 清除已處理的事件
            aggregateRoot.clearDomainEvents();
            
            log.info("Successfully dispatched {} domain events", events.size());

        } catch (Exception e) {
            log.error("Failed to dispatch domain events", e);
            throw new DomainEventDispatchingException("Failed to dispatch domain events", e);
        }
    }

    /**
     * 調度單個領域事件
     * @param event 領域事件
     */
    @LogMessageEvent(eventType = "DOMAIN_EVENT_DISPATCHED", logPayload = true)
    private void dispatchEvent(DomainEvent event) {
        log.debug("Dispatching domain event: {} with ID: {}", event.getEventType(), event.getEventId());

        if (event instanceof PaymentRequestedEvent paymentRequestedEvent) {
            log.info("Dispatching PaymentRequestedEvent for order: {}", 
                paymentRequestedEvent.getOrderId().getValue());
            paymentRequestedEventHandler.handle(paymentRequestedEvent);
        } else {
            log.warn("No handler found for domain event type: {}", event.getEventType());
            throw new UnsupportedDomainEventException("No handler found for domain event type: " + event.getEventType());
        }
    }

    /**
     * 領域事件調度異常
     */
    public static class DomainEventDispatchingException extends RuntimeException {
        public DomainEventDispatchingException(String message, Throwable cause) {
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
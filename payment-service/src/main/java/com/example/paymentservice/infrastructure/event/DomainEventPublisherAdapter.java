package com.example.paymentservice.infrastructure.event;

import com.example.paymentservice.application.port.out.DomainEventPublisher;
import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.domain.event.DomainEventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付服務領域事件發布器適配器 - 實現出站端口接口，將領域事件調度到相應的處理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisherAdapter implements DomainEventPublisher {

    private final com.example.paymentservice.domain.event.DomainEventPublisher domainEventPublisher;

    @Override
    public void publishEvents(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("No domain events to publish");
            return;
        }

        log.info("Publishing {} domain events through payment service adapter", events.size());
        
        try {
            // 使用領域事件發布器發布事件
            domainEventPublisher.publishEvents(events);
            
            log.info("Successfully published {} domain events", events.size());
            
        } catch (Exception e) {
            log.error("Failed to publish domain events through payment service adapter", e);
            throw new DomainEventPublishingException("Failed to publish domain events", e);
        }
    }

    /**
     * 領域事件發布異常
     */
    public static class DomainEventPublishingException extends RuntimeException {
        public DomainEventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
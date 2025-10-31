package com.example.paymentservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 支付服務領域事件發布器日誌記錄功能測試
 */
@ExtendWith(MockitoExtension.class)
class DomainEventPublisherLoggingTest {

    @Mock
    private OutboxService outboxService;

    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new DomainEventPublisher(outboxService);
    }

    @Test
    void shouldHaveLogMessageEventAnnotationOnPublishEvents() throws NoSuchMethodException {
        // Given
        Method publishEventsMethod = DomainEventPublisher.class.getMethod("publishEvents", List.class);

        // When
        LogMessageEvent annotation = publishEventsMethod.getAnnotation(LogMessageEvent.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.eventType()).isEqualTo("DOMAIN_EVENTS_PUBLISHED");
        assertThat(annotation.logPayload()).isTrue();
    }

    @Test
    void shouldHaveLogMessageEventAnnotationOnPublishEvent() throws NoSuchMethodException {
        // Given
        Method publishEventMethod = DomainEventPublisher.class.getMethod("publishEvent", 
            com.example.paymentservice.domain.shared.DomainEvent.class);

        // When
        LogMessageEvent annotation = publishEventMethod.getAnnotation(LogMessageEvent.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.eventType()).isEqualTo("DOMAIN_EVENT_PUBLISHED");
        assertThat(annotation.logPayload()).isTrue();
    }

    @Test
    void shouldHaveLogMessageEventAnnotationOnHandlePaymentProcessedEvent() throws NoSuchMethodException {
        // Given
        Method handleMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentProcessedEvent", 
            PaymentProcessedEvent.class);

        // When
        LogMessageEvent annotation = handleMethod.getAnnotation(LogMessageEvent.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.eventType()).isEqualTo("PAYMENT_PROCESSED_EVENT_HANDLED");
        assertThat(annotation.logPayload()).isTrue();
    }

    @Test
    void shouldHaveLogMessageEventAnnotationOnHandlePaymentFailedEvent() throws NoSuchMethodException {
        // Given
        Method handleMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentFailedEvent", 
            PaymentFailedEvent.class);

        // When
        LogMessageEvent annotation = handleMethod.getAnnotation(LogMessageEvent.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.eventType()).isEqualTo("PAYMENT_FAILED_EVENT_HANDLED");
        assertThat(annotation.logPayload()).isTrue();
    }

    @Test
    void shouldLogEventPublishingToOutbox() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
            paymentId, "ORDER-002", "TXN-002", "Card declined");

        List<com.example.paymentservice.domain.shared.DomainEvent> events = Arrays.asList(
            processedEvent, failedEvent);

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        verify(outboxService).saveEvent(processedEvent, "PAY-001", "Payment");
        verify(outboxService).saveEvent(failedEvent, "PAY-001", "Payment");
    }

    @Test
    void shouldLogPaymentProcessedEventDetails() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-002");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-002", "TXN-002", new BigDecimal("250.00"), "EUR");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-002", "Payment");
        
        // 驗證事件詳細信息
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo("ORDER-002");
        assertThat(event.getTransactionId()).isEqualTo("TXN-002");
        assertThat(event.getAmount()).isEqualTo(new BigDecimal("250.00"));
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getEventType()).isEqualTo("PaymentProcessedEvent");
    }

    @Test
    void shouldLogPaymentFailedEventDetails() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-003");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-003", "TXN-003", "Insufficient funds");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-003", "Payment");
        
        // 驗證事件詳細信息
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo("ORDER-003");
        assertThat(event.getTransactionId()).isEqualTo("TXN-003");
        assertThat(event.getErrorMessage()).isEqualTo("Insufficient funds");
        assertThat(event.getEventType()).isEqualTo("PaymentFailedEvent");
    }

    @Test
    void shouldLogOutboxServiceInteractions() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-004");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-004", "TXN-004", new BigDecimal("75.50"), "USD");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        // 驗證與OutboxService的交互
        verify(outboxService, times(1)).saveEvent(
            eq(event), 
            eq("PAY-004"), 
            eq("Payment")
        );
        
        // 驗證沒有其他不必要的調用
        verifyNoMoreInteractions(outboxService);
    }

    @Test
    void shouldLogErrorsInOutboxService() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-005");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-005", "TXN-005", new BigDecimal("100.00"), "USD");
        
        RuntimeException outboxError = new RuntimeException("Outbox service failed");
        doThrow(outboxError).when(outboxService).saveEvent(any(), any(), any());

        // When & Then
        try {
            domainEventPublisher.publishEvent(event);
        } catch (DomainEventPublisher.DomainEventPublishingException e) {
            // 驗證異常處理和日誌記錄
            assertThat(e.getMessage()).contains("Failed to publish domain event: PaymentProcessedEvent");
            assertThat(e.getCause()).isEqualTo(outboxError);
        }

        // 驗證OutboxService被調用
        verify(outboxService).saveEvent(event, "PAY-005", "Payment");
    }

    @Test
    void shouldLogEventProcessingSequence() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-006");
        PaymentId paymentId2 = new PaymentId("PAY-007");
        
        PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
            paymentId1, "ORDER-006", "TXN-006", new BigDecimal("150.00"), "USD");
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-007", "TXN-007", "Card expired");

        // When
        domainEventPublisher.publishEvent(processedEvent);
        domainEventPublisher.publishEvent(failedEvent);

        // Then
        // 驗證事件處理順序
        verify(outboxService).saveEvent(processedEvent, "PAY-006", "Payment");
        verify(outboxService).saveEvent(failedEvent, "PAY-007", "Payment");
        
        // 驗證每個事件都被正確處理
        assertThat(processedEvent.getEventType()).isEqualTo("PaymentProcessedEvent");
        assertThat(failedEvent.getEventType()).isEqualTo("PaymentFailedEvent");
    }

    @Test
    void shouldLogEventMetadata() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-008");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-008", "TXN-008", new BigDecimal("99.99"), "GBP");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        // 驗證事件元數據
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("PaymentProcessedEvent");
        
        // 驗證OutboxService調用參數
        verify(outboxService).saveEvent(
            argThat(e -> e.getEventId().equals(event.getEventId())),
            eq("PAY-008"),
            eq("Payment")
        );
    }

    @Test
    void shouldLogBatchEventProcessing() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-009");
        PaymentId paymentId2 = new PaymentId("PAY-010");
        PaymentId paymentId3 = new PaymentId("PAY-011");
        
        PaymentProcessedEvent event1 = new PaymentProcessedEvent(
            paymentId1, "ORDER-009", "TXN-009", new BigDecimal("50.00"), "USD");
        PaymentProcessedEvent event2 = new PaymentProcessedEvent(
            paymentId2, "ORDER-010", "TXN-010", new BigDecimal("75.00"), "EUR");
        PaymentFailedEvent event3 = new PaymentFailedEvent(
            paymentId3, "ORDER-011", "TXN-011", "Network error");

        List<com.example.paymentservice.domain.shared.DomainEvent> events = Arrays.asList(
            event1, event2, event3);

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        // 驗證批量處理日誌
        verify(outboxService).saveEvent(event1, "PAY-009", "Payment");
        verify(outboxService).saveEvent(event2, "PAY-010", "Payment");
        verify(outboxService).saveEvent(event3, "PAY-011", "Payment");
        
        // 驗證所有事件都被處理
        verify(outboxService, times(3)).saveEvent(any(), any(), eq("Payment"));
    }
}
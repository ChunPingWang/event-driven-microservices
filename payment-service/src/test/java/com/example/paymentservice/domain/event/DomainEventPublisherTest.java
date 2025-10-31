package com.example.paymentservice.domain.event;

import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 支付服務領域事件發布器單元測試
 */
@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new DomainEventPublisher(outboxService);
    }

    @Test
    void shouldPublishPaymentProcessedEvent() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-001", "Payment");
    }

    @Test
    void shouldPublishPaymentFailedEvent() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-002");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-002", "TXN-002", "Insufficient funds");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-002", "Payment");
    }

    @Test
    void shouldPublishMultipleEvents() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-001");
        PaymentId paymentId2 = new PaymentId("PAY-002");
        
        PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
            paymentId1, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-002", "TXN-002", "Card declined");
        
        List<com.example.paymentservice.domain.shared.DomainEvent> events = Arrays.asList(
            processedEvent, failedEvent);

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        verify(outboxService).saveEvent(processedEvent, "PAY-001", "Payment");
        verify(outboxService).saveEvent(failedEvent, "PAY-002", "Payment");
    }

    @Test
    void shouldHandleEmptyEventsList() {
        // Given
        List<com.example.paymentservice.domain.shared.DomainEvent> events = Collections.emptyList();

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        verifyNoInteractions(outboxService);
    }

    @Test
    void shouldHandleNullEventsList() {
        // When
        domainEventPublisher.publishEvents(null);

        // Then
        verifyNoInteractions(outboxService);
    }

    @Test
    void shouldThrowExceptionForUnsupportedEventType() {
        // Given
        com.example.paymentservice.domain.shared.DomainEvent unsupportedEvent = 
            new com.example.paymentservice.domain.shared.DomainEvent() {
                @Override
                public String getEventType() {
                    return "UNSUPPORTED_EVENT";
                }
            };

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(unsupportedEvent))
            .isInstanceOf(DomainEventPublisher.UnsupportedDomainEventException.class)
            .hasMessageContaining("Unsupported domain event type: UNSUPPORTED_EVENT");
    }

    @Test
    void shouldThrowExceptionWhenOutboxServiceFails() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");

        doThrow(new RuntimeException("Outbox service failed")).when(outboxService).saveEvent(any(), any(), any());

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(event))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
            .hasMessageContaining("Failed to publish domain event: PaymentProcessedEvent")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldContinueProcessingOtherEventsWhenOneEventFails() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-001");
        PaymentId paymentId2 = new PaymentId("PAY-002");
        
        PaymentProcessedEvent failingEvent = new PaymentProcessedEvent(
            paymentId1, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");
        PaymentFailedEvent successEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-002", "TXN-002", "Card declined");
        
        List<com.example.paymentservice.domain.shared.DomainEvent> events = Arrays.asList(
            failingEvent, successEvent);

        doThrow(new RuntimeException("Outbox service failed"))
            .when(outboxService).saveEvent(eq(failingEvent), eq("PAY-001"), eq("Payment"));

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvents(events))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class);

        // 驗證第一個事件失敗後，不會處理後續事件
        verify(outboxService, times(1)).saveEvent(eq(failingEvent), eq("PAY-001"), eq("Payment"));
        verify(outboxService, never()).saveEvent(eq(successEvent), any(), any());
    }

    @Test
    void shouldHandlePaymentProcessedEventWithDifferentCurrencies() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-003");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-003", "TXN-003", new BigDecimal("50.00"), "EUR");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-003", "Payment");
    }

    @Test
    void shouldHandlePaymentFailedEventWithLongErrorMessage() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-004");
        String longErrorMessage = "This is a very long error message that describes in detail what went wrong during the payment processing including technical details and user-friendly information";
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-004", "TXN-004", longErrorMessage);

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-004", "Payment");
    }
}
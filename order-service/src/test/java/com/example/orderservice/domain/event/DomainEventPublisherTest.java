package com.example.orderservice.domain.event;

import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.event.PaymentConfirmedEvent;
import com.example.orderservice.domain.order.event.PaymentFailedEvent;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.messaging.MessagePublisher;
import com.example.orderservice.messaging.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 領域事件發布器單元測試
 */
@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private DomainEventToMessageConverter eventConverter;

    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new DomainEventPublisher(messagePublisher, eventConverter);
    }

    @Test
    void shouldPublishPaymentRequestedEvent() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .transactionId("TXN-001")
            .orderId("ORDER-001")
            .customerId("CUSTOMER-001")
            .build();

        when(eventConverter.convertToPaymentRequest(event)).thenReturn(paymentRequest);

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(eventConverter).convertToPaymentRequest(event);
        verify(messagePublisher).publishPaymentRequest(paymentRequest);
    }

    @Test
    void shouldPublishPaymentConfirmedEvent() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(orderId, "TXN-001", "PAY-001");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        // PaymentConfirmedEvent 不需要發送外部消息，只記錄日誌
        verifyNoInteractions(messagePublisher);
        verifyNoInteractions(eventConverter);
    }

    @Test
    void shouldPublishPaymentFailedEvent() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentFailedEvent event = new PaymentFailedEvent(orderId, "TXN-001", "Insufficient funds");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        // PaymentFailedEvent 不需要發送外部消息，只記錄日誌
        verifyNoInteractions(messagePublisher);
        verifyNoInteractions(eventConverter);
    }

    @Test
    void shouldPublishMultipleEvents() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent paymentRequestedEvent = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");
        PaymentConfirmedEvent paymentConfirmedEvent = new PaymentConfirmedEvent(orderId, "TXN-001", "PAY-001");
        
        List<com.example.orderservice.domain.shared.DomainEvent> events = Arrays.asList(
            paymentRequestedEvent, paymentConfirmedEvent);

        PaymentRequest paymentRequest = PaymentRequest.builder()
            .transactionId("TXN-001")
            .orderId("ORDER-001")
            .customerId("CUSTOMER-001")
            .build();

        when(eventConverter.convertToPaymentRequest(paymentRequestedEvent)).thenReturn(paymentRequest);

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        verify(eventConverter).convertToPaymentRequest(paymentRequestedEvent);
        verify(messagePublisher).publishPaymentRequest(paymentRequest);
    }

    @Test
    void shouldHandleEmptyEventsList() {
        // Given
        List<com.example.orderservice.domain.shared.DomainEvent> events = Collections.emptyList();

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        verifyNoInteractions(messagePublisher);
        verifyNoInteractions(eventConverter);
    }

    @Test
    void shouldHandleNullEventsList() {
        // When
        domainEventPublisher.publishEvents(null);

        // Then
        verifyNoInteractions(messagePublisher);
        verifyNoInteractions(eventConverter);
    }

    @Test
    void shouldThrowExceptionForUnsupportedEventType() {
        // Given
        com.example.orderservice.domain.shared.DomainEvent unsupportedEvent = 
            new com.example.orderservice.domain.shared.DomainEvent() {
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
    void shouldThrowExceptionWhenMessagePublisherFails() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .transactionId("TXN-001")
            .orderId("ORDER-001")
            .customerId("CUSTOMER-001")
            .build();

        when(eventConverter.convertToPaymentRequest(event)).thenReturn(paymentRequest);
        doThrow(new RuntimeException("Message publishing failed")).when(messagePublisher).publishPaymentRequest(any());

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(event))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
            .hasMessageContaining("Failed to publish domain event: PaymentRequestedEvent")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldThrowExceptionWhenEventConverterFails() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");

        when(eventConverter.convertToPaymentRequest(event))
            .thenThrow(new RuntimeException("Event conversion failed"));

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(event))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
            .hasMessageContaining("Failed to publish domain event: PaymentRequestedEvent")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldContinueProcessingOtherEventsWhenOneEventFails() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent failingEvent = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");
        PaymentConfirmedEvent successEvent = new PaymentConfirmedEvent(orderId, "TXN-002", "PAY-001");
        
        List<com.example.orderservice.domain.shared.DomainEvent> events = Arrays.asList(
            failingEvent, successEvent);

        when(eventConverter.convertToPaymentRequest(failingEvent))
            .thenThrow(new RuntimeException("Event conversion failed"));

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvents(events))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class);

        // 驗證第一個事件失敗後，不會處理後續事件
        verify(eventConverter, times(1)).convertToPaymentRequest(failingEvent);
        verifyNoMoreInteractions(eventConverter);
        verifyNoInteractions(messagePublisher);
    }
}
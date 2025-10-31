package com.example.paymentservice.domain.event.handler;

import com.example.paymentservice.domain.event.DomainEventPublisher;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 支付處理完成事件處理器單元測試
 */
@ExtendWith(MockitoExtension.class)
class PaymentProcessedEventHandlerTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private PaymentProcessedEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new PaymentProcessedEventHandler(domainEventPublisher);
    }

    @Test
    void shouldHandleValidPaymentProcessedEvent() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");

        // When
        eventHandler.handle(event);

        // Then
        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldThrowExceptionWhenPaymentIdIsNull() {
        // Given
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            null, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(NullPointerException.class);

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentIdValueIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD") {
            @Override
            public PaymentId getPaymentId() {
                return new PaymentId("PAY-001") {
                    @Override
                    public String getValue() {
                        return null;
                    }
                };
            }
        };

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Payment ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentIdValueIsEmpty() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD") {
            @Override
            public PaymentId getPaymentId() {
                return new PaymentId("PAY-001") {
                    @Override
                    public String getValue() {
                        return "";
                    }
                };
            }
        };

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Payment ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, null, "TXN-001", new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Order ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsEmpty() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "", "TXN-001", new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Order ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", null, new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdIsEmpty() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "", new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", null, "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Amount must be positive");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsZero() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", BigDecimal.ZERO, "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Amount must be positive");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("-10.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Amount must be positive");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenCurrencyIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), null);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Currency cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenCurrencyIsEmpty() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Currency cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenDomainEventPublisherFails() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");

        doThrow(new RuntimeException("Publisher failed")).when(domainEventPublisher).publishEvent(event);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasMessageContaining("Failed to process PaymentProcessedEvent for payment: PAY-001")
            .hasCauseInstanceOf(RuntimeException.class);

        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldHandleEventWithDifferentCurrencies() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-002");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-002", "TXN-002", new BigDecimal("50.00"), "EUR");

        // When
        eventHandler.handle(event);

        // Then
        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldHandleEventWithLargeAmount() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-003");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-003", "TXN-003", new BigDecimal("9999.99"), "USD");

        // When
        eventHandler.handle(event);

        // Then
        verify(domainEventPublisher).publishEvent(event);
    }
}
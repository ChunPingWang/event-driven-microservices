package com.example.paymentservice.domain.event.handler;

import com.example.paymentservice.domain.event.DomainEventPublisher;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 支付失敗事件處理器單元測試
 */
@ExtendWith(MockitoExtension.class)
class PaymentFailedEventHandlerTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private PaymentFailedEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new PaymentFailedEventHandler(domainEventPublisher);
    }

    @Test
    void shouldHandleValidPaymentFailedEvent() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-001", "TXN-001", "Insufficient funds");

        // When
        eventHandler.handle(event);

        // Then
        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldThrowExceptionWhenPaymentIdIsNull() {
        // Given
        PaymentFailedEvent event = new PaymentFailedEvent(
            null, "ORDER-001", "TXN-001", "Card declined");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(NullPointerException.class);

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentIdValueIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-001", "TXN-001", "Card declined") {
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
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Payment ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentIdValueIsEmpty() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-001", "TXN-001", "Card declined") {
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
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Payment ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, null, "TXN-001", "Card declined");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Order ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsEmpty() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "", "TXN-001", "Card declined");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Order ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-001", null, "Card declined");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdIsEmpty() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-001", "", "Card declined");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenErrorMessageIsNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-001", "TXN-001", null);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Error message cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenErrorMessageIsEmpty() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-001", "TXN-001", "");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Error message cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenDomainEventPublisherFails() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-001", "TXN-001", "Card declined");

        doThrow(new RuntimeException("Publisher failed")).when(domainEventPublisher).publishEvent(event);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentFailedEventHandler.PaymentFailedEventProcessingException.class)
            .hasMessageContaining("Failed to process PaymentFailedEvent for payment: PAY-001")
            .hasCauseInstanceOf(RuntimeException.class);

        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldHandleEventWithLongErrorMessage() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-002");
        String longErrorMessage = "This is a very long error message that describes in detail what went wrong during the payment processing including technical details and user-friendly information that might be useful for debugging purposes";
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-002", "TXN-002", longErrorMessage);

        // When
        eventHandler.handle(event);

        // Then
        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldHandleEventWithDifferentErrorTypes() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-003");
        PaymentId paymentId2 = new PaymentId("PAY-004");
        PaymentId paymentId3 = new PaymentId("PAY-005");

        PaymentFailedEvent insufficientFundsEvent = new PaymentFailedEvent(
            paymentId1, "ORDER-003", "TXN-003", "Insufficient funds");
        PaymentFailedEvent cardDeclinedEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-004", "TXN-004", "Card declined");
        PaymentFailedEvent expiredCardEvent = new PaymentFailedEvent(
            paymentId3, "ORDER-005", "TXN-005", "Card expired");

        // When
        eventHandler.handle(insufficientFundsEvent);
        eventHandler.handle(cardDeclinedEvent);
        eventHandler.handle(expiredCardEvent);

        // Then
        verify(domainEventPublisher).publishEvent(insufficientFundsEvent);
        verify(domainEventPublisher).publishEvent(cardDeclinedEvent);
        verify(domainEventPublisher).publishEvent(expiredCardEvent);
    }

    @Test
    void shouldHandleEventWithSpecialCharactersInErrorMessage() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-006");
        String errorMessageWithSpecialChars = "Payment failed: 信用卡驗證失敗 - 特殊字符測試 @#$%^&*()";
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-006", "TXN-006", errorMessageWithSpecialChars);

        // When
        eventHandler.handle(event);

        // Then
        verify(domainEventPublisher).publishEvent(event);
    }
}
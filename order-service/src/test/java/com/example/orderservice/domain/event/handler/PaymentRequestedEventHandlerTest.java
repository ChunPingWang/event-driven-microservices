package com.example.orderservice.domain.event.handler;

import com.example.orderservice.domain.event.DomainEventPublisher;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.valueobject.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 支付請求事件處理器單元測試
 */
@ExtendWith(MockitoExtension.class)
class PaymentRequestedEventHandlerTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private PaymentRequestedEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new PaymentRequestedEventHandler(domainEventPublisher);
    }

    @Test
    void shouldHandleValidPaymentRequestedEvent() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");

        // When
        eventHandler.handle(event);

        // Then
        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsNull() {
        // Given
        PaymentRequestedEvent event = new PaymentRequestedEvent(null, "TXN-001", "CUSTOMER-001");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(NullPointerException.class);

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdValueIsNull() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        // 創建一個模擬的事件，其中OrderId的getValue()返回null
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001") {
            @Override
            public OrderId getOrderId() {
                return new OrderId("ORDER-001") {
                    @Override
                    public String getValue() {
                        return null;
                    }
                };
            }
        };

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Order ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdValueIsEmpty() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001") {
            @Override
            public OrderId getOrderId() {
                return new OrderId("ORDER-001") {
                    @Override
                    public String getValue() {
                        return "";
                    }
                };
            }
        };

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Order ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdIsNull() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, null, "CUSTOMER-001");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdIsEmpty() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "", "CUSTOMER-001");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenCustomerIdIsNull() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", null);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Customer ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenCustomerIdIsEmpty() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Customer ID cannot be null or empty");

        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenDomainEventPublisherFails() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");

        doThrow(new RuntimeException("Publisher failed")).when(domainEventPublisher).publishEvent(event);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasMessageContaining("Failed to process PaymentRequestedEvent for order: ORDER-001")
            .hasCauseInstanceOf(RuntimeException.class);

        verify(domainEventPublisher).publishEvent(event);
    }
}
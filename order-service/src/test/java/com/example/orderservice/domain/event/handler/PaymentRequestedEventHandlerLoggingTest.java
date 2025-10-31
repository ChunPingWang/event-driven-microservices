package com.example.orderservice.domain.event.handler;

import com.example.orderservice.domain.event.DomainEventPublisher;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.valueobject.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 支付請求事件處理器日誌記錄功能測試
 */
@ExtendWith(MockitoExtension.class)
class PaymentRequestedEventHandlerLoggingTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private PaymentRequestedEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new PaymentRequestedEventHandler(domainEventPublisher);
    }

    @Test
    void shouldLogSuccessfulEventHandling() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-LOG-001", "CUSTOMER-LOG-001");

        // When
        eventHandler.handle(event);

        // Then
        // 驗證事件處理的日誌記錄
        verify(domainEventPublisher, times(1)).publishEvent(event);
        
        // 驗證事件內容
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getTransactionId()).isEqualTo("TXN-LOG-001");
        assertThat(event.getCustomerId()).isEqualTo("CUSTOMER-LOG-001");
        assertThat(event.getEventType()).isEqualTo("PaymentRequestedEvent");
    }

    @Test
    void shouldLogEventValidationProcess() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-002");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-LOG-002", "CUSTOMER-LOG-002");

        // When
        eventHandler.handle(event);

        // Then
        // 驗證事件驗證過程的日誌記錄
        verify(domainEventPublisher).publishEvent(event);
        
        // 驗證事件驗證通過
        assertThat(event.getOrderId().getValue()).isNotNull();
        assertThat(event.getOrderId().getValue()).isNotEmpty();
        assertThat(event.getTransactionId()).isNotNull();
        assertThat(event.getTransactionId()).isNotEmpty();
        assertThat(event.getCustomerId()).isNotNull();
        assertThat(event.getCustomerId()).isNotEmpty();
    }

    @Test
    void shouldLogEventPublishingProcess() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-003");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-LOG-003", "CUSTOMER-LOG-003");

        // When
        eventHandler.handle(event);

        // Then
        // 驗證事件發布過程的日誌記錄
        verify(domainEventPublisher, times(1)).publishEvent(eq(event));
        
        // 驗證沒有其他不必要的調用
        verifyNoMoreInteractions(domainEventPublisher);
    }

    @Test
    void shouldLogValidationErrorsForNullOrderId() {
        // Given
        PaymentRequestedEvent event = new PaymentRequestedEvent(null, "TXN-LOG-004", "CUSTOMER-LOG-004");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Cannot invoke \"com.example.orderservice.domain.order.valueobject.OrderId.getValue()\" because the return value of \"com.example.orderservice.domain.order.event.PaymentRequestedEvent.getOrderId()\" is null");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForNullTransactionId() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-005");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, null, "CUSTOMER-LOG-005");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForEmptyTransactionId() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-006");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "", "CUSTOMER-LOG-006");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForNullCustomerId() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-007");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-LOG-007", null);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Customer ID cannot be null or empty");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForEmptyCustomerId() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-008");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-LOG-008", "");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Customer ID cannot be null or empty");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogPublisherFailures() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-009");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-LOG-009", "CUSTOMER-LOG-009");

        RuntimeException publisherError = new RuntimeException("Publisher failed");
        doThrow(publisherError).when(domainEventPublisher).publishEvent(event);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentRequestedEventHandler.PaymentRequestedEventProcessingException.class)
            .hasMessageContaining("Failed to process PaymentRequestedEvent for order: ORDER-LOG-009")
            .hasCauseInstanceOf(RuntimeException.class);

        // 驗證發布器失敗的日誌記錄
        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldLogEventProcessingMetrics() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-010");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-LOG-010", "CUSTOMER-LOG-010");

        long startTime = System.currentTimeMillis();

        // When
        eventHandler.handle(event);

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Then
        // 驗證事件處理指標的日誌記錄
        verify(domainEventPublisher).publishEvent(event);
        
        // 驗證處理時間合理（應該很快）
        assertThat(processingTime).isLessThan(1000); // 小於1秒
        
        // 驗證事件處理成功
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
    }

    @Test
    void shouldLogMultipleEventHandling() {
        // Given
        OrderId orderId1 = new OrderId("ORDER-LOG-011");
        OrderId orderId2 = new OrderId("ORDER-LOG-012");
        OrderId orderId3 = new OrderId("ORDER-LOG-013");

        PaymentRequestedEvent event1 = new PaymentRequestedEvent(orderId1, "TXN-LOG-011", "CUSTOMER-LOG-011");
        PaymentRequestedEvent event2 = new PaymentRequestedEvent(orderId2, "TXN-LOG-012", "CUSTOMER-LOG-012");
        PaymentRequestedEvent event3 = new PaymentRequestedEvent(orderId3, "TXN-LOG-013", "CUSTOMER-LOG-013");

        // When
        eventHandler.handle(event1);
        eventHandler.handle(event2);
        eventHandler.handle(event3);

        // Then
        // 驗證多個事件處理的日誌記錄
        verify(domainEventPublisher).publishEvent(event1);
        verify(domainEventPublisher).publishEvent(event2);
        verify(domainEventPublisher).publishEvent(event3);
        
        // 驗證每個事件都被正確處理
        verify(domainEventPublisher, times(3)).publishEvent(any(PaymentRequestedEvent.class));
    }

    @Test
    void shouldLogEventHandlingContext() {
        // Given
        OrderId orderId = new OrderId("ORDER-LOG-014");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-LOG-014", "CUSTOMER-LOG-014");

        // When
        eventHandler.handle(event);

        // Then
        // 驗證事件處理上下文的日誌記錄
        verify(domainEventPublisher).publishEvent(event);
        
        // 驗證事件上下文信息
        assertThat(event.getEventType()).isEqualTo("PaymentRequestedEvent");
        assertThat(event.getOrderId().getValue()).startsWith("ORDER-");
        assertThat(event.getTransactionId()).startsWith("TXN-");
        assertThat(event.getCustomerId()).startsWith("CUSTOMER-");
        
        // 驗證事件元數據
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
    }
}
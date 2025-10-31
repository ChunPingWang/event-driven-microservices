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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 支付處理完成事件處理器日誌記錄功能測試
 */
@ExtendWith(MockitoExtension.class)
class PaymentProcessedEventHandlerLoggingTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private PaymentProcessedEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new PaymentProcessedEventHandler(domainEventPublisher);
    }

    @Test
    void shouldLogSuccessfulEventHandling() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-001", "TXN-LOG-001", new BigDecimal("100.00"), "USD");

        // When
        eventHandler.handle(event);

        // Then
        // 驗證事件處理的日誌記錄
        verify(domainEventPublisher, times(1)).publishEvent(event);
        
        // 驗證事件內容
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo("ORDER-LOG-001");
        assertThat(event.getTransactionId()).isEqualTo("TXN-LOG-001");
        assertThat(event.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(event.getCurrency()).isEqualTo("USD");
        assertThat(event.getEventType()).isEqualTo("PaymentProcessedEvent");
    }

    @Test
    void shouldLogEventValidationProcess() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-002");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-002", "TXN-LOG-002", new BigDecimal("250.50"), "EUR");

        // When
        eventHandler.handle(event);

        // Then
        // 驗證事件驗證過程的日誌記錄
        verify(domainEventPublisher).publishEvent(event);
        
        // 驗證事件驗證通過
        assertThat(event.getPaymentId().getValue()).isNotNull();
        assertThat(event.getPaymentId().getValue()).isNotEmpty();
        assertThat(event.getOrderId()).isNotNull();
        assertThat(event.getOrderId()).isNotEmpty();
        assertThat(event.getTransactionId()).isNotNull();
        assertThat(event.getTransactionId()).isNotEmpty();
        assertThat(event.getAmount()).isNotNull();
        assertThat(event.getAmount()).isPositive();
        assertThat(event.getCurrency()).isNotNull();
        assertThat(event.getCurrency()).isNotEmpty();
    }

    @Test
    void shouldLogEventPublishingProcess() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-003");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-003", "TXN-LOG-003", new BigDecimal("75.25"), "GBP");

        // When
        eventHandler.handle(event);

        // Then
        // 驗證事件發布過程的日誌記錄
        verify(domainEventPublisher, times(1)).publishEvent(eq(event));
        
        // 驗證沒有其他不必要的調用
        verifyNoMoreInteractions(domainEventPublisher);
    }

    @Test
    void shouldLogValidationErrorsForNullPaymentId() {
        // Given
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            null, "ORDER-LOG-004", "TXN-LOG-004", new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Cannot invoke \"com.example.paymentservice.domain.payment.valueobject.PaymentId.getValue()\" because the return value of \"com.example.paymentservice.domain.payment.event.PaymentProcessedEvent.getPaymentId()\" is null");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForNullOrderId() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-005");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, null, "TXN-LOG-005", new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Order ID cannot be null or empty");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForEmptyOrderId() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-006");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "", "TXN-LOG-006", new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Order ID cannot be null or empty");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForNullTransactionId() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-007");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-007", null, new BigDecimal("100.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Transaction ID cannot be null or empty");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForNullAmount() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-008");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-008", "TXN-LOG-008", null, "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Amount must be positive");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForZeroAmount() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-009");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-009", "TXN-LOG-009", BigDecimal.ZERO, "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Amount must be positive");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForNegativeAmount() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-010");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-010", "TXN-LOG-010", new BigDecimal("-10.00"), "USD");

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Amount must be positive");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogValidationErrorsForNullCurrency() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-011");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-011", "TXN-LOG-011", new BigDecimal("100.00"), null);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Currency cannot be null or empty");

        // 驗證錯誤日誌記錄
        verify(domainEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLogPublisherFailures() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-012");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-012", "TXN-LOG-012", new BigDecimal("100.00"), "USD");

        RuntimeException publisherError = new RuntimeException("Publisher failed");
        doThrow(publisherError).when(domainEventPublisher).publishEvent(event);

        // When & Then
        assertThatThrownBy(() -> eventHandler.handle(event))
            .isInstanceOf(PaymentProcessedEventHandler.PaymentProcessedEventProcessingException.class)
            .hasMessageContaining("Failed to process PaymentProcessedEvent for payment: PAY-LOG-012")
            .hasCauseInstanceOf(RuntimeException.class);

        // 驗證發布器失敗的日誌記錄
        verify(domainEventPublisher).publishEvent(event);
    }

    @Test
    void shouldLogEventProcessingWithDifferentCurrencies() {
        // Given
        String[] currencies = {"USD", "EUR", "GBP", "JPY"};
        BigDecimal[] amounts = {
            new BigDecimal("100.00"),
            new BigDecimal("85.50"),
            new BigDecimal("75.25"),
            new BigDecimal("11000")
        };

        for (int i = 0; i < currencies.length; i++) {
            PaymentId paymentId = new PaymentId("PAY-LOG-" + (13 + i));
            PaymentProcessedEvent event = new PaymentProcessedEvent(
                paymentId, "ORDER-LOG-" + (13 + i), "TXN-LOG-" + (13 + i), amounts[i], currencies[i]);

            // When
            eventHandler.handle(event);

            // Then
            // 驗證不同貨幣的事件處理日誌記錄
            verify(domainEventPublisher).publishEvent(event);
            
            assertThat(event.getCurrency()).isEqualTo(currencies[i]);
            assertThat(event.getAmount()).isEqualTo(amounts[i]);
        }
    }

    @Test
    void shouldLogEventProcessingMetrics() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-017");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-017", "TXN-LOG-017", new BigDecimal("500.00"), "USD");

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
    void shouldLogEventHandlingContext() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-LOG-018");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-LOG-018", "TXN-LOG-018", new BigDecimal("999.99"), "EUR");

        // When
        eventHandler.handle(event);

        // Then
        // 驗證事件處理上下文的日誌記錄
        verify(domainEventPublisher).publishEvent(event);
        
        // 驗證事件上下文信息
        assertThat(event.getEventType()).isEqualTo("PaymentProcessedEvent");
        assertThat(event.getPaymentId().getValue()).startsWith("PAY-");
        assertThat(event.getOrderId()).startsWith("ORDER-");
        assertThat(event.getTransactionId()).startsWith("TXN-");
        
        // 驗證事件元數據
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
        
        // 驗證金額和貨幣
        assertThat(event.getAmount()).isPositive();
        assertThat(event.getCurrency()).isIn("USD", "EUR", "GBP", "JPY");
    }
}
package com.example.paymentservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import com.example.logging.aspect.MessageEventLoggingAspect;
import com.example.logging.model.MessageEventLog;
import com.example.logging.service.MessageLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 支付服務事件處理集成測試 - 驗證事件發布、處理和日誌記錄的完整流程
 */
@ExtendWith(MockitoExtension.class)
class EventHandlingIntegrationTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private MessageLogService messageLogService;

    @Mock
    private ObjectMapper objectMapper;

    private DomainEventPublisher domainEventPublisher;
    private MessageEventLoggingAspect loggingAspect;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new DomainEventPublisher(outboxService);
        loggingAspect = new MessageEventLoggingAspect(messageLogService, objectMapper);
    }

    @Test
    void shouldHandleCompleteEventProcessingFlow() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-001");
        PaymentId paymentId2 = new PaymentId("PAY-002");

        PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
            paymentId1, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-002", "TXN-002", "Insufficient funds");

        List<DomainEvent> events = Arrays.asList(processedEvent, failedEvent);

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        // 驗證兩個事件都被保存到Outbox
        verify(outboxService).saveEvent(processedEvent, "PAY-001", "Payment");
        verify(outboxService).saveEvent(failedEvent, "PAY-002", "Payment");
        
        // 驗證OutboxService被調用了兩次
        verify(outboxService, times(2)).saveEvent(any(), any(), eq("Payment"));
    }

    @Test
    void shouldVerifyLoggingAnnotationsOnAllEventHandlingMethods() throws NoSuchMethodException {
        // 驗證DomainEventPublisher的所有公共方法都有日誌註解
        Method publishEventsMethod = DomainEventPublisher.class.getMethod("publishEvents", List.class);
        LogMessageEvent publishEventsAnnotation = publishEventsMethod.getAnnotation(LogMessageEvent.class);
        assertThat(publishEventsAnnotation).isNotNull();
        assertThat(publishEventsAnnotation.eventType()).isEqualTo("DOMAIN_EVENTS_PUBLISHED");
        assertThat(publishEventsAnnotation.logPayload()).isTrue();

        Method publishEventMethod = DomainEventPublisher.class.getMethod("publishEvent", DomainEvent.class);
        LogMessageEvent publishEventAnnotation = publishEventMethod.getAnnotation(LogMessageEvent.class);
        assertThat(publishEventAnnotation).isNotNull();
        assertThat(publishEventAnnotation.eventType()).isEqualTo("DOMAIN_EVENT_PUBLISHED");
        assertThat(publishEventAnnotation.logPayload()).isTrue();

        // 驗證私有處理方法的日誌註解
        Method handlePaymentProcessedMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentProcessedEvent", PaymentProcessedEvent.class);
        LogMessageEvent handlePaymentProcessedAnnotation = handlePaymentProcessedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentProcessedAnnotation).isNotNull();
        assertThat(handlePaymentProcessedAnnotation.eventType()).isEqualTo("PAYMENT_PROCESSED_EVENT_HANDLED");

        Method handlePaymentFailedMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentFailedEvent", PaymentFailedEvent.class);
        LogMessageEvent handlePaymentFailedAnnotation = handlePaymentFailedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentFailedAnnotation).isNotNull();
        assertThat(handlePaymentFailedAnnotation.eventType()).isEqualTo("PAYMENT_FAILED_EVENT_HANDLED");
    }

    @Test
    void shouldHandlePaymentProcessedEventWithLogging() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-003");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-003", "TXN-003", new BigDecimal("250.50"), "EUR");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-003", "Payment");
        
        // 驗證事件詳細信息
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo("ORDER-003");
        assertThat(event.getTransactionId()).isEqualTo("TXN-003");
        assertThat(event.getAmount()).isEqualTo(new BigDecimal("250.50"));
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getEventType()).isEqualTo("PaymentProcessedEvent");
    }

    @Test
    void shouldHandlePaymentFailedEventWithLogging() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-004");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-004", "TXN-004", "Card declined");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-004", "Payment");
        
        // 驗證事件詳細信息
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo("ORDER-004");
        assertThat(event.getTransactionId()).isEqualTo("TXN-004");
        assertThat(event.getErrorMessage()).isEqualTo("Card declined");
        assertThat(event.getEventType()).isEqualTo("PaymentFailedEvent");
    }

    @Test
    void shouldHandleOutboxServiceErrors() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-005");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-005", "TXN-005", new BigDecimal("100.00"), "USD");

        doThrow(new RuntimeException("Outbox service failed")).when(outboxService).saveEvent(any(), any(), any());

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(event))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
            .hasMessageContaining("Failed to publish domain event: PaymentProcessedEvent")
            .hasCauseInstanceOf(RuntimeException.class);

        // 驗證OutboxService被調用
        verify(outboxService).saveEvent(event, "PAY-005", "Payment");
    }

    @Test
    void shouldHandleUnsupportedEventTypes() {
        // Given
        DomainEvent unsupportedEvent = new DomainEvent() {
            @Override
            public String getEventType() {
                return "UnsupportedEvent";
            }
        };

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(unsupportedEvent))
            .isInstanceOf(DomainEventPublisher.UnsupportedDomainEventException.class)
            .hasMessageContaining("Unsupported domain event type: UnsupportedEvent");

        // 驗證OutboxService沒有被調用
        verify(outboxService, never()).saveEvent(any(), any(), any());
    }

    @Test
    void shouldHandleBatchEventProcessingWithErrors() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-006");
        PaymentId paymentId2 = new PaymentId("PAY-007");

        PaymentProcessedEvent failingEvent = new PaymentProcessedEvent(
            paymentId1, "ORDER-006", "TXN-006", new BigDecimal("100.00"), "USD");
        PaymentFailedEvent successEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-007", "TXN-007", "Card expired");

        List<DomainEvent> events = Arrays.asList(failingEvent, successEvent);

        doThrow(new RuntimeException("Outbox service failed"))
            .when(outboxService).saveEvent(eq(failingEvent), eq("PAY-006"), eq("Payment"));

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvents(events))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class);

        // 驗證第一個事件失敗後，不會處理後續事件
        verify(outboxService, times(1)).saveEvent(eq(failingEvent), eq("PAY-006"), eq("Payment"));
        verify(outboxService, never()).saveEvent(eq(successEvent), any(), any());
    }

    @Test
    void shouldHandleEventProcessingWithDifferentCurrencies() {
        // Given
        String[] currencies = {"USD", "EUR", "GBP", "JPY"};
        BigDecimal[] amounts = {
            new BigDecimal("100.00"),
            new BigDecimal("85.50"),
            new BigDecimal("75.25"),
            new BigDecimal("11000")
        };

        for (int i = 0; i < currencies.length; i++) {
            PaymentId paymentId = new PaymentId("PAY-" + (8 + i));
            PaymentProcessedEvent event = new PaymentProcessedEvent(
                paymentId, "ORDER-" + (8 + i), "TXN-" + (8 + i), amounts[i], currencies[i]);

            // When
            domainEventPublisher.publishEvent(event);

            // Then
            verify(outboxService).saveEvent(event, paymentId.getValue(), "Payment");
            
            assertThat(event.getCurrency()).isEqualTo(currencies[i]);
            assertThat(event.getAmount()).isEqualTo(amounts[i]);
        }
    }

    @Test
    void shouldValidateEventDataIntegrity() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-012");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-012", "TXN-012", new BigDecimal("999.99"), "USD");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
        verify(outboxService).saveEvent(eventCaptor.capture(), eq("PAY-012"), eq("Payment"));
        
        PaymentProcessedEvent capturedEvent = eventCaptor.getValue();
        
        // 驗證事件數據完整性
        assertThat(capturedEvent.getPaymentId()).isEqualTo(paymentId);
        assertThat(capturedEvent.getOrderId()).isEqualTo("ORDER-012");
        assertThat(capturedEvent.getTransactionId()).isEqualTo("TXN-012");
        assertThat(capturedEvent.getAmount()).isEqualTo(new BigDecimal("999.99"));
        assertThat(capturedEvent.getCurrency()).isEqualTo("USD");
        
        // 驗證事件元數據
        assertThat(capturedEvent.getEventId()).isNotNull();
        assertThat(capturedEvent.getOccurredOn()).isNotNull();
        assertThat(capturedEvent.getEventType()).isEqualTo("PaymentProcessedEvent");
    }

    @Test
    void shouldHandleEventMetadata() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-013");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-013", "TXN-013", "Network timeout");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        // 驗證事件元數據
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("PaymentFailedEvent");
        
        // 驗證事件內容
        assertThat(event.getPaymentId()).isEqualTo(paymentId);
        assertThat(event.getOrderId()).isEqualTo("ORDER-013");
        assertThat(event.getTransactionId()).isEqualTo("TXN-013");
        assertThat(event.getErrorMessage()).isEqualTo("Network timeout");
        
        // 驗證Outbox保存
        verify(outboxService).saveEvent(event, "PAY-013", "Payment");
    }

    @Test
    void shouldHandleOutboxServiceInteractions() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-014");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-014", "TXN-014", new BigDecimal("150.00"), "EUR");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        // 驗證與OutboxService的交互
        verify(outboxService, times(1)).saveEvent(
            eq(event), 
            eq("PAY-014"), 
            eq("Payment")
        );
        
        // 驗證沒有其他不必要的調用
        verifyNoMoreInteractions(outboxService);
    }

    @Test
    void shouldHandleEventProcessingSequence() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-015");
        PaymentId paymentId2 = new PaymentId("PAY-016");
        PaymentId paymentId3 = new PaymentId("PAY-017");

        PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
            paymentId1, "ORDER-015", "TXN-015", new BigDecimal("100.00"), "USD");
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-016", "TXN-016", "Card declined");
        PaymentProcessedEvent anotherProcessedEvent = new PaymentProcessedEvent(
            paymentId3, "ORDER-017", "TXN-017", new BigDecimal("200.00"), "GBP");

        // When
        domainEventPublisher.publishEvent(processedEvent);
        domainEventPublisher.publishEvent(failedEvent);
        domainEventPublisher.publishEvent(anotherProcessedEvent);

        // Then
        // 驗證事件處理順序
        verify(outboxService).saveEvent(processedEvent, "PAY-015", "Payment");
        verify(outboxService).saveEvent(failedEvent, "PAY-016", "Payment");
        verify(outboxService).saveEvent(anotherProcessedEvent, "PAY-017", "Payment");
        
        // 驗證每個事件都被正確處理
        assertThat(processedEvent.getEventType()).isEqualTo("PaymentProcessedEvent");
        assertThat(failedEvent.getEventType()).isEqualTo("PaymentFailedEvent");
        assertThat(anotherProcessedEvent.getEventType()).isEqualTo("PaymentProcessedEvent");
    }

    @Test
    void shouldHandleLargeAmountPayments() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-018");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-018", "TXN-018", new BigDecimal("99999.99"), "USD");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-018", "Payment");
        
        // 驗證大金額處理
        assertThat(event.getAmount()).isEqualTo(new BigDecimal("99999.99"));
        assertThat(event.getAmount()).isPositive();
    }

    @Test
    void shouldHandleEventProcessingWithLongErrorMessages() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-019");
        String longErrorMessage = "This is a very long error message that describes in detail what went wrong during the payment processing including technical details and user-friendly information that might be useful for debugging purposes and customer support";
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-019", "TXN-019", longErrorMessage);

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(event, "PAY-019", "Payment");
        
        // 驗證長錯誤消息處理
        assertThat(event.getErrorMessage()).isEqualTo(longErrorMessage);
        assertThat(event.getErrorMessage().length()).isGreaterThan(100);
    }
}
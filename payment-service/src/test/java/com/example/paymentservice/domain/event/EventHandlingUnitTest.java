package com.example.paymentservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.domain.shared.DomainEvent;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 支付服務事件處理單元測試 - 專注於測試事件發布和處理邏輯、事件到Outbox的轉換、驗證日誌記錄功能
 * 
 * 此測試類實現任務 8.3 的要求：
 * - 測試事件發布和處理邏輯
 * - 測試事件到消息的轉換（通過Outbox Pattern）
 * - 驗證日誌記錄功能
 */
@ExtendWith(MockitoExtension.class)
class EventHandlingUnitTest {

    @Mock
    private OutboxService outboxService;

    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new DomainEventPublisher(outboxService);
    }

    // ========== 測試事件發布和處理邏輯 ==========

    @Test
    void shouldPublishPaymentProcessedEventCorrectly() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-001");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-001", "TXN-001", new BigDecimal("100.00"), "USD");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService, times(1)).saveEvent(event, "PAY-001", "Payment");
    }

    @Test
    void shouldPublishPaymentFailedEventCorrectly() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-002");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-002", "TXN-002", "Insufficient funds");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService, times(1)).saveEvent(event, "PAY-002", "Payment");
    }

    @Test
    void shouldPublishMultipleEventsInSequence() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-003");
        PaymentId paymentId2 = new PaymentId("PAY-004");
        
        PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
            paymentId1, "ORDER-003", "TXN-003", new BigDecimal("150.00"), "EUR");
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-004", "TXN-004", "Card declined");

        List<DomainEvent> events = Arrays.asList(processedEvent, failedEvent);

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        verify(outboxService).saveEvent(processedEvent, "PAY-003", "Payment");
        verify(outboxService).saveEvent(failedEvent, "PAY-004", "Payment");
        verify(outboxService, times(2)).saveEvent(any(), any(), eq("Payment"));
    }

    @Test
    void shouldHandleEventProcessingFailures() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-005");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-005", "TXN-005", new BigDecimal("200.00"), "USD");

        doThrow(new RuntimeException("Outbox service failed")).when(outboxService).saveEvent(any(), any(), any());

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(event))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
            .hasMessageContaining("Failed to publish domain event: PaymentProcessedEvent")
            .hasCauseInstanceOf(RuntimeException.class);

        verify(outboxService).saveEvent(event, "PAY-005", "Payment");
    }

    @Test
    void shouldHandleUnsupportedEventTypes() {
        // Given
        DomainEvent unsupportedEvent = new DomainEvent() {
            @Override
            public String getEventType() {
                return "UnsupportedPaymentEvent";
            }
        };

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(unsupportedEvent))
            .isInstanceOf(DomainEventPublisher.UnsupportedDomainEventException.class)
            .hasMessageContaining("Unsupported domain event type: UnsupportedPaymentEvent");

        verify(outboxService, never()).saveEvent(any(), any(), any());
    }

    @Test
    void shouldStopProcessingOnFirstEventFailure() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-006");
        PaymentId paymentId2 = new PaymentId("PAY-007");
        
        PaymentProcessedEvent firstEvent = new PaymentProcessedEvent(
            paymentId1, "ORDER-006", "TXN-006", new BigDecimal("100.00"), "USD");
        PaymentFailedEvent secondEvent = new PaymentFailedEvent(
            paymentId2, "ORDER-007", "TXN-007", "Card expired");

        List<DomainEvent> events = Arrays.asList(firstEvent, secondEvent);

        doThrow(new RuntimeException("Outbox service failed"))
            .when(outboxService).saveEvent(eq(firstEvent), eq("PAY-006"), eq("Payment"));

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvents(events))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class);

        // 第一個事件應該被嘗試處理，第二個不應該
        verify(outboxService, times(1)).saveEvent(eq(firstEvent), eq("PAY-006"), eq("Payment"));
        verify(outboxService, never()).saveEvent(eq(secondEvent), any(), any());
    }

    @Test
    void shouldHandleEmptyEventsList() {
        // Given
        List<DomainEvent> emptyEvents = Arrays.asList();

        // When
        domainEventPublisher.publishEvents(emptyEvents);

        // Then
        verify(outboxService, never()).saveEvent(any(), any(), any());
    }

    @Test
    void shouldHandleNullEventsList() {
        // When
        domainEventPublisher.publishEvents(null);

        // Then
        verify(outboxService, never()).saveEvent(any(), any(), any());
    }

    // ========== 測試事件到消息的轉換（通過Outbox Pattern）==========

    @Test
    void shouldConvertPaymentProcessedEventToOutboxCorrectly() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-008");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-008", "TXN-008", new BigDecimal("250.00"), "GBP");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(
            argThat(e -> {
                PaymentProcessedEvent savedEvent = (PaymentProcessedEvent) e;
                return savedEvent.getPaymentId().equals(paymentId) &&
                       savedEvent.getOrderId().equals("ORDER-008") &&
                       savedEvent.getTransactionId().equals("TXN-008") &&
                       savedEvent.getAmount().equals(new BigDecimal("250.00")) &&
                       savedEvent.getCurrency().equals("GBP");
            }),
            eq("PAY-008"),
            eq("Payment")
        );
    }

    @Test
    void shouldConvertPaymentFailedEventToOutboxCorrectly() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-009");
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, "ORDER-009", "TXN-009", "Network timeout");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(
            argThat(e -> {
                PaymentFailedEvent savedEvent = (PaymentFailedEvent) e;
                return savedEvent.getPaymentId().equals(paymentId) &&
                       savedEvent.getOrderId().equals("ORDER-009") &&
                       savedEvent.getTransactionId().equals("TXN-009") &&
                       savedEvent.getErrorMessage().equals("Network timeout");
            }),
            eq("PAY-009"),
            eq("Payment")
        );
    }

    @Test
    void shouldHandleEventConversionWithDifferentCurrencies() {
        // Given
        PaymentId paymentIdUSD = new PaymentId("PAY-USD");
        PaymentId paymentIdEUR = new PaymentId("PAY-EUR");
        PaymentId paymentIdGBP = new PaymentId("PAY-GBP");

        PaymentProcessedEvent eventUSD = new PaymentProcessedEvent(
            paymentIdUSD, "ORDER-USD", "TXN-USD", new BigDecimal("100.00"), "USD");
        PaymentProcessedEvent eventEUR = new PaymentProcessedEvent(
            paymentIdEUR, "ORDER-EUR", "TXN-EUR", new BigDecimal("85.00"), "EUR");
        PaymentProcessedEvent eventGBP = new PaymentProcessedEvent(
            paymentIdGBP, "ORDER-GBP", "TXN-GBP", new BigDecimal("75.00"), "GBP");

        // When
        domainEventPublisher.publishEvent(eventUSD);
        domainEventPublisher.publishEvent(eventEUR);
        domainEventPublisher.publishEvent(eventGBP);

        // Then
        verify(outboxService).saveEvent(eventUSD, "PAY-USD", "Payment");
        verify(outboxService).saveEvent(eventEUR, "PAY-EUR", "Payment");
        verify(outboxService).saveEvent(eventGBP, "PAY-GBP", "Payment");
        verify(outboxService, times(3)).saveEvent(any(), any(), eq("Payment"));
    }

    @Test
    void shouldPreserveEventMetadataDuringConversion() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-010");
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            paymentId, "ORDER-010", "TXN-010", new BigDecimal("99.99"), "USD");

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(outboxService).saveEvent(
            argThat(e -> {
                assertThat(e.getEventId()).isNotNull();
                assertThat(e.getOccurredOn()).isNotNull();
                assertThat(e.getEventType()).isEqualTo("PaymentProcessedEvent");
                return true;
            }),
            eq("PAY-010"),
            eq("Payment")
        );
    }

    // ========== 驗證日誌記錄功能 ==========

    @Test
    void shouldHaveLoggingAnnotationsOnAllPublicMethods() throws NoSuchMethodException {
        // Given
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;

        // When & Then - 驗證公共方法的日誌註解
        Method publishEventsMethod = publisherClass.getMethod("publishEvents", List.class);
        LogMessageEvent publishEventsAnnotation = publishEventsMethod.getAnnotation(LogMessageEvent.class);
        assertThat(publishEventsAnnotation).isNotNull();
        assertThat(publishEventsAnnotation.eventType()).isEqualTo("DOMAIN_EVENTS_PUBLISHED");
        assertThat(publishEventsAnnotation.logPayload()).isTrue();

        Method publishEventMethod = publisherClass.getMethod("publishEvent", DomainEvent.class);
        LogMessageEvent publishEventAnnotation = publishEventMethod.getAnnotation(LogMessageEvent.class);
        assertThat(publishEventAnnotation).isNotNull();
        assertThat(publishEventAnnotation.eventType()).isEqualTo("DOMAIN_EVENT_PUBLISHED");
        assertThat(publishEventAnnotation.logPayload()).isTrue();
    }

    @Test
    void shouldHaveLoggingAnnotationsOnAllEventHandlerMethods() throws NoSuchMethodException {
        // Given
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;

        // When & Then - 驗證私有事件處理方法的日誌註解
        Method handlePaymentProcessedMethod = publisherClass.getDeclaredMethod("handlePaymentProcessedEvent", PaymentProcessedEvent.class);
        LogMessageEvent handlePaymentProcessedAnnotation = handlePaymentProcessedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentProcessedAnnotation).isNotNull();
        assertThat(handlePaymentProcessedAnnotation.eventType()).isEqualTo("PAYMENT_PROCESSED_EVENT_HANDLED");
        assertThat(handlePaymentProcessedAnnotation.logPayload()).isTrue();

        Method handlePaymentFailedMethod = publisherClass.getDeclaredMethod("handlePaymentFailedEvent", PaymentFailedEvent.class);
        LogMessageEvent handlePaymentFailedAnnotation = handlePaymentFailedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentFailedAnnotation).isNotNull();
        assertThat(handlePaymentFailedAnnotation.eventType()).isEqualTo("PAYMENT_FAILED_EVENT_HANDLED");
        assertThat(handlePaymentFailedAnnotation.logPayload()).isTrue();
    }

    @Test
    void shouldUseConsistentLoggingEventTypeNaming() throws NoSuchMethodException {
        // Given
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        Method[] methods = publisherClass.getDeclaredMethods();

        // When & Then
        for (Method method : methods) {
            LogMessageEvent annotation = method.getAnnotation(LogMessageEvent.class);
            if (annotation != null) {
                String eventType = annotation.eventType();
                
                // 驗證事件類型命名約定
                assertThat(eventType).isUpperCase();
                assertThat(eventType).contains("_");
                assertThat(eventType).matches("^[A-Z_]+$");
                
                // 驗證特定的命名模式
                if (method.getName().startsWith("handle")) {
                    assertThat(eventType).endsWith("_HANDLED");
                } else if (method.getName().startsWith("publish")) {
                    assertThat(eventType).endsWith("_PUBLISHED");
                }
            }
        }
    }

    @Test
    void shouldEnablePayloadLoggingForAllEventHandlingMethods() throws NoSuchMethodException {
        // Given
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        Method[] methods = publisherClass.getDeclaredMethods();

        // When & Then
        for (Method method : methods) {
            LogMessageEvent annotation = method.getAnnotation(LogMessageEvent.class);
            if (annotation != null) {
                assertThat(annotation.logPayload())
                    .as("Method %s should have logPayload=true", method.getName())
                    .isTrue();
            }
        }
    }

    @Test
    void shouldHaveUniqueEventTypesForEachMethod() throws NoSuchMethodException {
        // Given
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        Method[] methods = publisherClass.getDeclaredMethods();

        // When
        List<String> eventTypes = Arrays.stream(methods)
            .filter(method -> method.getAnnotation(LogMessageEvent.class) != null)
            .map(method -> method.getAnnotation(LogMessageEvent.class).eventType())
            .toList();

        // Then
        assertThat(eventTypes).doesNotHaveDuplicates();
        assertThat(eventTypes).hasSize(4); // 預期有4個帶註解的方法
    }

    @Test
    void shouldValidateLoggingAnnotationConfiguration() {
        // Given
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        Method[] methods = publisherClass.getDeclaredMethods();

        // When & Then
        int annotatedMethodCount = 0;
        for (Method method : methods) {
            LogMessageEvent annotation = method.getAnnotation(LogMessageEvent.class);
            if (annotation != null) {
                annotatedMethodCount++;
                
                // 驗證註解配置的完整性
                assertThat(annotation.eventType()).isNotNull();
                assertThat(annotation.eventType()).isNotEmpty();
                assertThat(annotation.eventType()).doesNotContainIgnoringCase("null");
                assertThat(annotation.logPayload()).isTrue();
            }
        }
        
        // 確保有足夠的方法被註解
        assertThat(annotatedMethodCount).isGreaterThanOrEqualTo(4);
    }

    @Test
    void shouldValidateOutboxSpecificLoggingConfiguration() throws NoSuchMethodException {
        // Given
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;

        // When & Then - 驗證Outbox相關的事件處理方法
        Method[] methods = publisherClass.getDeclaredMethods();
        boolean hasPaymentProcessedHandler = false;
        boolean hasPaymentFailedHandler = false;

        for (Method method : methods) {
            LogMessageEvent annotation = method.getAnnotation(LogMessageEvent.class);
            if (annotation != null) {
                if (annotation.eventType().equals("PAYMENT_PROCESSED_EVENT_HANDLED")) {
                    hasPaymentProcessedHandler = true;
                }
                if (annotation.eventType().equals("PAYMENT_FAILED_EVENT_HANDLED")) {
                    hasPaymentFailedHandler = true;
                }
            }
        }

        // 確保支付相關的事件處理器都有日誌記錄
        assertThat(hasPaymentProcessedHandler).isTrue();
        assertThat(hasPaymentFailedHandler).isTrue();
    }

    @Test
    void shouldValidateEventProcessingLoggingCoverage() {
        // Given
        Class<DomainEventPublisher> publisherClass = DomainEventPublisher.class;
        Method[] allMethods = publisherClass.getDeclaredMethods();

        // When & Then
        int totalEventHandlingMethods = 0;
        int loggedEventHandlingMethods = 0;

        for (Method method : allMethods) {
            // 識別事件處理方法
            if (method.getName().startsWith("handle") || method.getName().startsWith("publish")) {
                totalEventHandlingMethods++;

                if (method.getAnnotation(LogMessageEvent.class) != null) {
                    loggedEventHandlingMethods++;
                }
            }
        }

        // 確保所有事件處理方法都有日誌記錄
        assertThat(loggedEventHandlingMethods).isEqualTo(totalEventHandlingMethods);
        assertThat(totalEventHandlingMethods).isGreaterThan(0);
    }
}
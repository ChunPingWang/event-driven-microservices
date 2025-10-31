package com.example.orderservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import com.example.logging.model.MessageEventLog;
import com.example.logging.service.MessageLogService;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.event.PaymentConfirmedEvent;
import com.example.orderservice.domain.order.event.PaymentFailedEvent;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.messaging.MessagePublisher;
import com.example.orderservice.messaging.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 領域事件發布器日誌記錄功能測試
 */
@ExtendWith(MockitoExtension.class)
class DomainEventPublisherLoggingTest {

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private DomainEventToMessageConverter eventConverter;

    @Mock
    private MessageLogService messageLogService;

    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new DomainEventPublisher(messagePublisher, eventConverter);
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
            com.example.orderservice.domain.shared.DomainEvent.class);

        // When
        LogMessageEvent annotation = publishEventMethod.getAnnotation(LogMessageEvent.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.eventType()).isEqualTo("DOMAIN_EVENT_PUBLISHED");
        assertThat(annotation.logPayload()).isTrue();
    }

    @Test
    void shouldHaveLogMessageEventAnnotationOnHandlePaymentRequestedEvent() throws NoSuchMethodException {
        // Given
        Method handleMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentRequestedEvent", 
            PaymentRequestedEvent.class);

        // When
        LogMessageEvent annotation = handleMethod.getAnnotation(LogMessageEvent.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.eventType()).isEqualTo("PAYMENT_REQUESTED_EVENT_HANDLED");
        assertThat(annotation.logPayload()).isTrue();
    }

    @Test
    void shouldHaveLogMessageEventAnnotationOnHandlePaymentConfirmedEvent() throws NoSuchMethodException {
        // Given
        Method handleMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentConfirmedEvent", 
            PaymentConfirmedEvent.class);

        // When
        LogMessageEvent annotation = handleMethod.getAnnotation(LogMessageEvent.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.eventType()).isEqualTo("PAYMENT_CONFIRMED_EVENT_HANDLED");
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
    void shouldLogEventPublishingWithCorrectEventTypes() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent paymentRequestedEvent = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");
        PaymentConfirmedEvent paymentConfirmedEvent = new PaymentConfirmedEvent(orderId, "TXN-001", "PAY-001");
        PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(orderId, "TXN-001", "Payment failed");

        List<com.example.orderservice.domain.shared.DomainEvent> events = Arrays.asList(
            paymentRequestedEvent, paymentConfirmedEvent, paymentFailedEvent);

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
        
        // 驗證所有事件類型都被正確處理
        // PaymentRequestedEvent 應該觸發消息發送
        // PaymentConfirmedEvent 和 PaymentFailedEvent 只記錄日誌
    }

    @Test
    void shouldLogEventProcessingDetails() {
        // Given
        OrderId orderId = new OrderId("ORDER-002");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-002", "CUSTOMER-002");
        
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .transactionId("TXN-002")
            .orderId("ORDER-002")
            .customerId("CUSTOMER-002")
            .build();

        when(eventConverter.convertToPaymentRequest(event)).thenReturn(paymentRequest);

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(eventConverter).convertToPaymentRequest(event);
        verify(messagePublisher).publishPaymentRequest(paymentRequest);
        
        // 驗證事件處理的詳細信息
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getTransactionId()).isEqualTo("TXN-002");
        assertThat(event.getCustomerId()).isEqualTo("CUSTOMER-002");
        assertThat(event.getEventType()).isEqualTo("PaymentRequestedEvent");
    }

    @Test
    void shouldLogEventConversionProcess() {
        // Given
        OrderId orderId = new OrderId("ORDER-003");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-003", "CUSTOMER-003");
        
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .transactionId("TXN-003")
            .orderId("ORDER-003")
            .customerId("CUSTOMER-003")
            .build();

        when(eventConverter.convertToPaymentRequest(event)).thenReturn(paymentRequest);

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        // 驗證事件轉換過程
        verify(eventConverter, times(1)).convertToPaymentRequest(event);
        verify(messagePublisher, times(1)).publishPaymentRequest(paymentRequest);
        
        // 驗證轉換後的消息內容
        assertThat(paymentRequest.getTransactionId()).isEqualTo("TXN-003");
        assertThat(paymentRequest.getOrderId()).isEqualTo("ORDER-003");
        assertThat(paymentRequest.getCustomerId()).isEqualTo("CUSTOMER-003");
    }

    @Test
    void shouldLogErrorsInEventProcessing() {
        // Given
        OrderId orderId = new OrderId("ORDER-004");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-004", "CUSTOMER-004");
        
        RuntimeException conversionError = new RuntimeException("Event conversion failed");
        when(eventConverter.convertToPaymentRequest(event)).thenThrow(conversionError);

        // When & Then
        try {
            domainEventPublisher.publishEvent(event);
        } catch (DomainEventPublisher.DomainEventPublishingException e) {
            // 驗證異常處理和日誌記錄
            assertThat(e.getMessage()).contains("Failed to publish domain event: PaymentRequestedEvent");
            assertThat(e.getCause()).isEqualTo(conversionError);
        }

        // 驗證錯誤情況下的調用
        verify(eventConverter).convertToPaymentRequest(event);
        verify(messagePublisher, never()).publishPaymentRequest(any());
    }

    @Test
    void shouldLogMessagePublishingErrors() {
        // Given
        OrderId orderId = new OrderId("ORDER-005");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-005", "CUSTOMER-005");
        
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .transactionId("TXN-005")
            .orderId("ORDER-005")
            .customerId("CUSTOMER-005")
            .build();

        when(eventConverter.convertToPaymentRequest(event)).thenReturn(paymentRequest);
        
        RuntimeException publishingError = new RuntimeException("Message publishing failed");
        doThrow(publishingError).when(messagePublisher).publishPaymentRequest(paymentRequest);

        // When & Then
        try {
            domainEventPublisher.publishEvent(event);
        } catch (DomainEventPublisher.DomainEventPublishingException e) {
            // 驗證異常處理和日誌記錄
            assertThat(e.getMessage()).contains("Failed to publish domain event: PaymentRequestedEvent");
            assertThat(e.getCause()).isEqualTo(publishingError);
        }

        // 驗證調用順序
        verify(eventConverter).convertToPaymentRequest(event);
        verify(messagePublisher).publishPaymentRequest(paymentRequest);
    }

    @Test
    void shouldLogDifferentEventTypesCorrectly() {
        // Given
        OrderId orderId = new OrderId("ORDER-006");
        
        PaymentConfirmedEvent confirmedEvent = new PaymentConfirmedEvent(orderId, "TXN-006", "PAY-006");
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(orderId, "TXN-007", "Insufficient funds");

        // When
        domainEventPublisher.publishEvent(confirmedEvent);
        domainEventPublisher.publishEvent(failedEvent);

        // Then
        // 這些事件不應該觸發外部消息發送，只記錄日誌
        verify(eventConverter, never()).convertToPaymentRequest(any());
        verify(messagePublisher, never()).publishPaymentRequest(any());
        
        // 驗證事件類型
        assertThat(confirmedEvent.getEventType()).isEqualTo("PaymentConfirmedEvent");
        assertThat(failedEvent.getEventType()).isEqualTo("PaymentFailedEvent");
    }
}
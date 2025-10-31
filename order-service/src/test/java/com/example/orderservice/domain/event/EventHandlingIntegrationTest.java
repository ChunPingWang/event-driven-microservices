package com.example.orderservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
import com.example.logging.aspect.MessageEventLoggingAspect;
import com.example.logging.model.MessageEventLog;
import com.example.logging.service.MessageLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.event.PaymentConfirmedEvent;
import com.example.orderservice.domain.order.event.PaymentFailedEvent;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.shared.DomainEvent;
import com.example.orderservice.infrastructure.repository.OrderRepositoryImpl;
import com.example.orderservice.messaging.MessagePublisher;
import com.example.orderservice.messaging.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 事件處理集成測試 - 驗證事件發布、處理和日誌記錄的完整流程
 */
@ExtendWith(MockitoExtension.class)
class EventHandlingIntegrationTest {

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private OrderRepositoryImpl orderRepository;

    @Mock
    private MessageLogService messageLogService;

    @Mock
    private ObjectMapper objectMapper;

    private DomainEventPublisher domainEventPublisher;
    private DomainEventToMessageConverter eventConverter;
    private MessageEventLoggingAspect loggingAspect;

    @BeforeEach
    void setUp() {
        eventConverter = new DomainEventToMessageConverter(orderRepository);
        domainEventPublisher = new DomainEventPublisher(messagePublisher, eventConverter);
        loggingAspect = new MessageEventLoggingAspect(messageLogService, objectMapper);
    }

    @Test
    void shouldHandleCompleteEventProcessingFlow() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-001");
        Money amount = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent paymentRequestedEvent = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");
        PaymentConfirmedEvent paymentConfirmedEvent = new PaymentConfirmedEvent(orderId, "TXN-001", "PAY-001");
        PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(orderId, "TXN-002", "Payment failed");

        List<DomainEvent> events = Arrays.asList(
            paymentRequestedEvent, paymentConfirmedEvent, paymentFailedEvent);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        // 驗證PaymentRequestedEvent觸發了消息發送
        ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(messagePublisher).publishPaymentRequest(paymentRequestCaptor.capture());
        
        PaymentRequest capturedRequest = paymentRequestCaptor.getValue();
        assertThat(capturedRequest.getTransactionId()).isEqualTo("TXN-001");
        assertThat(capturedRequest.getOrderId()).isEqualTo(orderId.getValue());
        assertThat(capturedRequest.getCustomerId()).isEqualTo("CUSTOMER-001");
        assertThat(capturedRequest.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(capturedRequest.getCurrency()).isEqualTo("USD");

        // 驗證PaymentConfirmedEvent和PaymentFailedEvent沒有觸發外部消息
        verify(messagePublisher, times(1)).publishPaymentRequest(any());
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
        Method handlePaymentRequestedMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentRequestedEvent", PaymentRequestedEvent.class);
        LogMessageEvent handlePaymentRequestedAnnotation = handlePaymentRequestedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentRequestedAnnotation).isNotNull();
        assertThat(handlePaymentRequestedAnnotation.eventType()).isEqualTo("PAYMENT_REQUESTED_EVENT_HANDLED");

        Method handlePaymentConfirmedMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentConfirmedEvent", PaymentConfirmedEvent.class);
        LogMessageEvent handlePaymentConfirmedAnnotation = handlePaymentConfirmedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentConfirmedAnnotation).isNotNull();
        assertThat(handlePaymentConfirmedAnnotation.eventType()).isEqualTo("PAYMENT_CONFIRMED_EVENT_HANDLED");

        Method handlePaymentFailedMethod = DomainEventPublisher.class.getDeclaredMethod("handlePaymentFailedEvent", PaymentFailedEvent.class);
        LogMessageEvent handlePaymentFailedAnnotation = handlePaymentFailedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentFailedAnnotation).isNotNull();
        assertThat(handlePaymentFailedAnnotation.eventType()).isEqualTo("PAYMENT_FAILED_EVENT_HANDLED");
    }

    @Test
    void shouldHandleEventConversionWithLogging() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-002");
        Money amount = new Money(new BigDecimal("250.00"), Currency.getInstance("EUR"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-002", "CUSTOMER-002");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = eventConverter.convertToPaymentRequest(event);

        // Then
        assertThat(paymentRequest).isNotNull();
        assertThat(paymentRequest.getTransactionId()).isEqualTo("TXN-002");
        assertThat(paymentRequest.getOrderId()).isEqualTo(orderId.getValue());
        assertThat(paymentRequest.getCustomerId()).isEqualTo("CUSTOMER-002");
        assertThat(paymentRequest.getAmount()).isEqualTo(new BigDecimal("250.00"));
        assertThat(paymentRequest.getCurrency()).isEqualTo("EUR");

        // 驗證轉換過程中的數據完整性
        assertThat(paymentRequest.getMerchantId()).isEqualTo("MERCHANT_001");
        assertThat(paymentRequest.getDescription()).contains(orderId.getValue());
        assertThat(paymentRequest.getCreditCard()).isNotNull();
        assertThat(paymentRequest.getBillingAddress()).isNotNull();
        assertThat(paymentRequest.getTimestamp()).isNotNull();
    }

    @Test
    void shouldHandleEventProcessingErrors() {
        // Given
        OrderId orderId = new OrderId("NONEXISTENT-ORDER");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-003", "CUSTOMER-003");

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(event))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
            .hasMessageContaining("Failed to publish domain event: PaymentRequestedEvent")
            .hasCauseInstanceOf(IllegalStateException.class);

        // 驗證沒有消息被發送
        verify(messagePublisher, never()).publishPaymentRequest(any());
    }

    @Test
    void shouldHandleMessagePublisherErrors() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-004");
        Money amount = new Money(new BigDecimal("75.00"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-004", "CUSTOMER-004");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("Message publishing failed")).when(messagePublisher).publishPaymentRequest(any());

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(event))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
            .hasMessageContaining("Failed to publish domain event: PaymentRequestedEvent")
            .hasCauseInstanceOf(RuntimeException.class);

        // 驗證消息發送被嘗試
        verify(messagePublisher).publishPaymentRequest(any());
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

        // 驗證沒有消息被發送
        verify(messagePublisher, never()).publishPaymentRequest(any());
    }

    @Test
    void shouldHandleBatchEventProcessingWithMixedResults() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-005");
        Money amount = new Money(new BigDecimal("150.00"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent validEvent = new PaymentRequestedEvent(orderId, "TXN-005", "CUSTOMER-005");
        PaymentConfirmedEvent confirmedEvent = new PaymentConfirmedEvent(orderId, "TXN-005", "PAY-005");
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(orderId, "TXN-006", "Card declined");

        List<DomainEvent> events = Arrays.asList(validEvent, confirmedEvent, failedEvent);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        // 只有PaymentRequestedEvent應該觸發消息發送
        verify(messagePublisher, times(1)).publishPaymentRequest(any());
        
        // 驗證所有事件都被處理（沒有異常拋出）
        ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(messagePublisher).publishPaymentRequest(paymentRequestCaptor.capture());
        
        PaymentRequest capturedRequest = paymentRequestCaptor.getValue();
        assertThat(capturedRequest.getTransactionId()).isEqualTo("TXN-005");
        assertThat(capturedRequest.getOrderId()).isEqualTo(orderId.getValue());
    }

    @Test
    void shouldValidateEventDataIntegrity() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-006");
        Money amount = new Money(new BigDecimal("999.99"), Currency.getInstance("GBP"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-006", "CUSTOMER-006");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(messagePublisher).publishPaymentRequest(paymentRequestCaptor.capture());
        
        PaymentRequest capturedRequest = paymentRequestCaptor.getValue();
        
        // 驗證事件數據完整性
        assertThat(capturedRequest.getTransactionId()).isEqualTo(event.getTransactionId());
        assertThat(capturedRequest.getOrderId()).isEqualTo(event.getOrderId().getValue());
        assertThat(capturedRequest.getCustomerId()).isEqualTo(event.getCustomerId());
        
        // 驗證從訂單獲取的數據
        assertThat(capturedRequest.getAmount()).isEqualTo(order.getAmount().getAmount());
        assertThat(capturedRequest.getCurrency()).isEqualTo(order.getAmount().getCurrency());
        
        // 驗證生成的數據
        assertThat(capturedRequest.getMerchantId()).isNotNull();
        assertThat(capturedRequest.getDescription()).isNotNull();
        assertThat(capturedRequest.getCreditCard()).isNotNull();
        assertThat(capturedRequest.getBillingAddress()).isNotNull();
        assertThat(capturedRequest.getTimestamp()).isNotNull();
    }

    @Test
    void shouldHandleEventMetadata() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-007");
        Money amount = new Money(new BigDecimal("50.00"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-007", "CUSTOMER-007");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        // 驗證事件元數據
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("PaymentRequestedEvent");
        
        // 驗證事件內容
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getTransactionId()).isEqualTo("TXN-007");
        assertThat(event.getCustomerId()).isEqualTo("CUSTOMER-007");
        
        // 驗證消息發送
        verify(messagePublisher).publishPaymentRequest(any());
    }
}
package com.example.orderservice.domain.event;

import com.example.logging.annotation.LogMessageEvent;
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
 * 事件處理單元測試 - 專注於測試事件發布和處理邏輯、事件到消息的轉換、驗證日誌記錄功能
 * 
 * 此測試類實現任務 8.3 的要求：
 * - 測試事件發布和處理邏輯
 * - 測試事件到消息的轉換
 * - 驗證日誌記錄功能
 */
@ExtendWith(MockitoExtension.class)
class EventHandlingUnitTest {

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private OrderRepositoryImpl orderRepository;

    private DomainEventPublisher domainEventPublisher;
    private DomainEventToMessageConverter eventConverter;

    @BeforeEach
    void setUp() {
        eventConverter = new DomainEventToMessageConverter(orderRepository);
        domainEventPublisher = new DomainEventPublisher(messagePublisher, eventConverter);
    }

    // ========== 測試事件發布和處理邏輯 ==========

    @Test
    void shouldPublishSingleEventCorrectly() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-001");
        Money amount = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        domainEventPublisher.publishEvent(event);

        // Then
        verify(messagePublisher, times(1)).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldPublishMultipleEventsInSequence() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-002");
        Money amount = new Money(new BigDecimal("200.00"), Currency.getInstance("EUR"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent paymentRequestedEvent = new PaymentRequestedEvent(orderId, "TXN-002", "CUSTOMER-002");
        PaymentConfirmedEvent paymentConfirmedEvent = new PaymentConfirmedEvent(orderId, "TXN-002", "PAY-002");
        PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(orderId, "TXN-003", "Insufficient funds");

        List<DomainEvent> events = Arrays.asList(paymentRequestedEvent, paymentConfirmedEvent, paymentFailedEvent);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        domainEventPublisher.publishEvents(events);

        // Then
        // 只有PaymentRequestedEvent應該觸發消息發送
        verify(messagePublisher, times(1)).publishPaymentRequest(any(PaymentRequest.class));
        
        // 驗證事件處理邏輯
        ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(messagePublisher).publishPaymentRequest(paymentRequestCaptor.capture());
        
        PaymentRequest capturedRequest = paymentRequestCaptor.getValue();
        assertThat(capturedRequest.getTransactionId()).isEqualTo("TXN-002");
        assertThat(capturedRequest.getOrderId()).isEqualTo(orderId.getValue());
    }

    @Test
    void shouldHandleEventProcessingFailures() {
        // Given
        OrderId orderId = new OrderId("INVALID-ORDER");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-004", "CUSTOMER-004");

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(event))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class)
            .hasMessageContaining("Failed to publish domain event: PaymentRequestedEvent");

        verify(messagePublisher, never()).publishPaymentRequest(any());
    }

    @Test
    void shouldHandleUnsupportedEventTypes() {
        // Given
        DomainEvent unsupportedEvent = new DomainEvent() {
            @Override
            public String getEventType() {
                return "UnsupportedEventType";
            }
        };

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvent(unsupportedEvent))
            .isInstanceOf(DomainEventPublisher.UnsupportedDomainEventException.class)
            .hasMessageContaining("Unsupported domain event type: UnsupportedEventType");

        verify(messagePublisher, never()).publishPaymentRequest(any());
    }

    @Test
    void shouldStopProcessingOnFirstEventFailure() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-005");
        Money amount = new Money(new BigDecimal("300.00"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent validEvent = new PaymentRequestedEvent(orderId, "TXN-005", "CUSTOMER-005");
        PaymentRequestedEvent invalidEvent = new PaymentRequestedEvent(new OrderId("INVALID"), "TXN-006", "CUSTOMER-006");

        List<DomainEvent> events = Arrays.asList(validEvent, invalidEvent);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.findById(new OrderId("INVALID"))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> domainEventPublisher.publishEvents(events))
            .isInstanceOf(DomainEventPublisher.DomainEventPublishingException.class);

        // 第一個事件應該被處理
        verify(messagePublisher, times(1)).publishPaymentRequest(any());
    }

    // ========== 測試事件到消息的轉換 ==========

    @Test
    void shouldConvertPaymentRequestedEventToPaymentRequestCorrectly() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-006");
        Money amount = new Money(new BigDecimal("150.00"), Currency.getInstance("GBP"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-007", "CUSTOMER-006");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = eventConverter.convertToPaymentRequest(event);

        // Then
        assertThat(paymentRequest).isNotNull();
        assertThat(paymentRequest.getTransactionId()).isEqualTo("TXN-007");
        assertThat(paymentRequest.getOrderId()).isEqualTo(orderId.getValue());
        assertThat(paymentRequest.getCustomerId()).isEqualTo("CUSTOMER-006");
        assertThat(paymentRequest.getAmount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(paymentRequest.getCurrency()).isEqualTo("GBP");
    }

    @Test
    void shouldIncludeAllRequiredFieldsInConvertedMessage() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-007");
        Money amount = new Money(new BigDecimal("75.50"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-008", "CUSTOMER-007");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = eventConverter.convertToPaymentRequest(event);

        // Then - 驗證所有必需字段
        assertThat(paymentRequest.getTransactionId()).isNotNull();
        assertThat(paymentRequest.getOrderId()).isNotNull();
        assertThat(paymentRequest.getCustomerId()).isNotNull();
        assertThat(paymentRequest.getAmount()).isNotNull();
        assertThat(paymentRequest.getCurrency()).isNotNull();
        assertThat(paymentRequest.getMerchantId()).isNotNull();
        assertThat(paymentRequest.getDescription()).isNotNull();
        assertThat(paymentRequest.getTimestamp()).isNotNull();
        assertThat(paymentRequest.getCreditCard()).isNotNull();
        assertThat(paymentRequest.getBillingAddress()).isNotNull();
    }

    @Test
    void shouldHandleConversionWithDifferentCurrencies() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-008");
        Money amountUSD = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        Money amountEUR = new Money(new BigDecimal("85.00"), Currency.getInstance("EUR"));
        Money amountGBP = new Money(new BigDecimal("75.00"), Currency.getInstance("GBP"));

        Order orderUSD = new Order(customerId, amountUSD);
        Order orderEUR = new Order(customerId, amountEUR);
        Order orderGBP = new Order(customerId, amountGBP);

        PaymentRequestedEvent eventUSD = new PaymentRequestedEvent(orderUSD.getOrderId(), "TXN-USD", "CUSTOMER-008");
        PaymentRequestedEvent eventEUR = new PaymentRequestedEvent(orderEUR.getOrderId(), "TXN-EUR", "CUSTOMER-008");
        PaymentRequestedEvent eventGBP = new PaymentRequestedEvent(orderGBP.getOrderId(), "TXN-GBP", "CUSTOMER-008");

        when(orderRepository.findById(orderUSD.getOrderId())).thenReturn(Optional.of(orderUSD));
        when(orderRepository.findById(orderEUR.getOrderId())).thenReturn(Optional.of(orderEUR));
        when(orderRepository.findById(orderGBP.getOrderId())).thenReturn(Optional.of(orderGBP));

        // When
        PaymentRequest requestUSD = eventConverter.convertToPaymentRequest(eventUSD);
        PaymentRequest requestEUR = eventConverter.convertToPaymentRequest(eventEUR);
        PaymentRequest requestGBP = eventConverter.convertToPaymentRequest(eventGBP);

        // Then
        assertThat(requestUSD.getCurrency()).isEqualTo("USD");
        assertThat(requestEUR.getCurrency()).isEqualTo("EUR");
        assertThat(requestGBP.getCurrency()).isEqualTo("GBP");
        
        assertThat(requestUSD.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(requestEUR.getAmount()).isEqualTo(new BigDecimal("85.00"));
        assertThat(requestGBP.getAmount()).isEqualTo(new BigDecimal("75.00"));
    }

    @Test
    void shouldFailConversionWhenOrderNotFound() {
        // Given
        OrderId nonExistentOrderId = new OrderId("NON-EXISTENT");
        PaymentRequestedEvent event = new PaymentRequestedEvent(nonExistentOrderId, "TXN-009", "CUSTOMER-009");

        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventConverter.convertToPaymentRequest(event))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Order not found for PaymentRequestedEvent: NON-EXISTENT");
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
        Method handlePaymentRequestedMethod = publisherClass.getDeclaredMethod("handlePaymentRequestedEvent", PaymentRequestedEvent.class);
        LogMessageEvent handlePaymentRequestedAnnotation = handlePaymentRequestedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentRequestedAnnotation).isNotNull();
        assertThat(handlePaymentRequestedAnnotation.eventType()).isEqualTo("PAYMENT_REQUESTED_EVENT_HANDLED");
        assertThat(handlePaymentRequestedAnnotation.logPayload()).isTrue();

        Method handlePaymentConfirmedMethod = publisherClass.getDeclaredMethod("handlePaymentConfirmedEvent", PaymentConfirmedEvent.class);
        LogMessageEvent handlePaymentConfirmedAnnotation = handlePaymentConfirmedMethod.getAnnotation(LogMessageEvent.class);
        assertThat(handlePaymentConfirmedAnnotation).isNotNull();
        assertThat(handlePaymentConfirmedAnnotation.eventType()).isEqualTo("PAYMENT_CONFIRMED_EVENT_HANDLED");
        assertThat(handlePaymentConfirmedAnnotation.logPayload()).isTrue();

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
        assertThat(eventTypes).hasSize(5); // 預期有5個帶註解的方法
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
        assertThat(annotatedMethodCount).isGreaterThanOrEqualTo(5);
    }
}
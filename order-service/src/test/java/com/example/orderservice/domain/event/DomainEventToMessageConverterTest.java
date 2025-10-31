package com.example.orderservice.domain.event;

import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.infrastructure.repository.OrderRepositoryImpl;
import com.example.orderservice.messaging.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 領域事件到消息轉換器單元測試
 */
@ExtendWith(MockitoExtension.class)
class DomainEventToMessageConverterTest {

    @Mock
    private OrderRepositoryImpl orderRepository;

    private DomainEventToMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DomainEventToMessageConverter(orderRepository);
    }

    @Test
    void shouldConvertPaymentRequestedEventToPaymentRequest() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-001");
        Money amount = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId(); // Use the generated OrderId from the order

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        // Then
        assertThat(paymentRequest).isNotNull();
        assertThat(paymentRequest.getTransactionId()).isEqualTo("TXN-001");
        assertThat(paymentRequest.getOrderId()).isEqualTo(orderId.getValue());
        assertThat(paymentRequest.getCustomerId()).isEqualTo("CUSTOMER-001");
        assertThat(paymentRequest.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(paymentRequest.getCurrency()).isEqualTo("USD");
        assertThat(paymentRequest.getMerchantId()).isEqualTo("MERCHANT_001");
        assertThat(paymentRequest.getDescription()).isEqualTo("Payment for order " + orderId.getValue());
        assertThat(paymentRequest.getTimestamp()).isNotNull();

        // 驗證信用卡信息
        assertThat(paymentRequest.getCreditCard()).isNotNull();
        assertThat(paymentRequest.getCreditCard().getCardNumber()).isEqualTo("**** **** **** 1234");
        assertThat(paymentRequest.getCreditCard().getExpiryDate()).isEqualTo("12/25");
        assertThat(paymentRequest.getCreditCard().getCvv()).isEqualTo("***");
        assertThat(paymentRequest.getCreditCard().getCardHolderName()).isEqualTo("Customer CUSTOMER-001");

        // 驗證帳單地址
        assertThat(paymentRequest.getBillingAddress()).isNotNull();
        assertThat(paymentRequest.getBillingAddress().getStreet()).isEqualTo("123 Main Street");
        assertThat(paymentRequest.getBillingAddress().getCity()).isEqualTo("Taipei");
        assertThat(paymentRequest.getBillingAddress().getPostalCode()).isEqualTo("10001");
        assertThat(paymentRequest.getBillingAddress().getCountry()).isEqualTo("TW");
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        OrderId orderId = new OrderId("ORDER-001");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> converter.convertToPaymentRequest(event))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Order not found for PaymentRequestedEvent: ORDER-001");
    }

    @Test
    void shouldHandleDifferentCurrencies() {
        // Given
        OrderId orderId = new OrderId("ORDER-002");
        CustomerId customerId = new CustomerId("CUSTOMER-002");
        Money amount = new Money(new BigDecimal("50.00"), Currency.getInstance("EUR"));
        
        Order order = new Order(customerId, amount);

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-002", "CUSTOMER-002");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        // Then
        assertThat(paymentRequest.getAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(paymentRequest.getCurrency()).isEqualTo("EUR");
        assertThat(paymentRequest.getCreditCard().getCardHolderName()).isEqualTo("Customer CUSTOMER-002");
    }

    @Test
    void shouldHandleLargeAmounts() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-003");
        Money amount = new Money(new BigDecimal("9999.99"), Currency.getInstance("USD"));
        
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId(); // Use the generated OrderId from the order

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-003", "CUSTOMER-003");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        // Then
        assertThat(paymentRequest.getAmount()).isEqualTo(new BigDecimal("9999.99"));
        assertThat(paymentRequest.getDescription()).isEqualTo("Payment for order " + orderId.getValue());
    }

    @Test
    void shouldGenerateUniqueTimestamps() throws InterruptedException {
        // Given
        OrderId orderId1 = new OrderId("ORDER-004");
        OrderId orderId2 = new OrderId("ORDER-005");
        CustomerId customerId = new CustomerId("CUSTOMER-004");
        Money amount = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        
        Order order1 = new Order(customerId, amount);

        Order order2 = new Order(customerId, amount);

        PaymentRequestedEvent event1 = new PaymentRequestedEvent(orderId1, "TXN-004", "CUSTOMER-004");
        PaymentRequestedEvent event2 = new PaymentRequestedEvent(orderId2, "TXN-005", "CUSTOMER-004");

        when(orderRepository.findById(orderId1)).thenReturn(Optional.of(order1));
        when(orderRepository.findById(orderId2)).thenReturn(Optional.of(order2));

        // When
        PaymentRequest paymentRequest1 = converter.convertToPaymentRequest(event1);
        Thread.sleep(1); // 確保時間戳不同
        PaymentRequest paymentRequest2 = converter.convertToPaymentRequest(event2);

        // Then
        assertThat(paymentRequest1.getTimestamp()).isNotEqualTo(paymentRequest2.getTimestamp());
        assertThat(paymentRequest1.getTransactionId()).isEqualTo("TXN-004");
        assertThat(paymentRequest2.getTransactionId()).isEqualTo("TXN-005");
    }
}
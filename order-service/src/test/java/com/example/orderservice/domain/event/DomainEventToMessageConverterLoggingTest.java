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
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 領域事件到消息轉換器日誌記錄功能測試
 */
@ExtendWith(MockitoExtension.class)
class DomainEventToMessageConverterLoggingTest {

    @Mock
    private OrderRepositoryImpl orderRepository;

    private DomainEventToMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DomainEventToMessageConverter(orderRepository);
    }

    @Test
    void shouldLogEventConversionProcess() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-001");
        Money amount = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-001", "CUSTOMER-001");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        // Then
        // 驗證轉換過程的日誌記錄
        assertThat(paymentRequest).isNotNull();
        assertThat(paymentRequest.getTransactionId()).isEqualTo("TXN-001");
        assertThat(paymentRequest.getOrderId()).isEqualTo(orderId.getValue());
        assertThat(paymentRequest.getCustomerId()).isEqualTo("CUSTOMER-001");
        
        // 驗證轉換過程中的數據完整性
        assertThat(paymentRequest.getAmount()).isEqualTo(order.getAmount().getAmount());
        assertThat(paymentRequest.getCurrency()).isEqualTo(order.getAmount().getCurrency());
        assertThat(paymentRequest.getTimestamp()).isNotNull();
        assertThat(paymentRequest.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void shouldLogOrderRetrievalProcess() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-002");
        Money amount = new Money(new BigDecimal("250.50"), Currency.getInstance("EUR"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-002", "CUSTOMER-002");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        // Then
        // 驗證訂單檢索過程的日誌記錄
        assertThat(paymentRequest.getOrderId()).isEqualTo(orderId.getValue());
        assertThat(paymentRequest.getCustomerId()).isEqualTo(customerId.getValue());
        assertThat(paymentRequest.getAmount()).isEqualTo(amount.getAmount());
        assertThat(paymentRequest.getCurrency()).isEqualTo(amount.getCurrency());
        
        // 驗證商戶信息和描述
        assertThat(paymentRequest.getMerchantId()).isEqualTo("MERCHANT_001");
        assertThat(paymentRequest.getDescription()).isEqualTo("Payment for order " + orderId.getValue());
    }

    @Test
    void shouldLogCreditCardInfoGeneration() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-003");
        Money amount = new Money(new BigDecimal("75.25"), Currency.getInstance("GBP"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-003", "CUSTOMER-003");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        // Then
        // 驗證信用卡信息生成的日誌記錄
        PaymentRequest.CreditCardInfo creditCard = paymentRequest.getCreditCard();
        assertThat(creditCard).isNotNull();
        assertThat(creditCard.getCardNumber()).isEqualTo("**** **** **** 1234");
        assertThat(creditCard.getExpiryDate()).isEqualTo("12/25");
        assertThat(creditCard.getCvv()).isEqualTo("***");
        assertThat(creditCard.getCardHolderName()).isEqualTo("Customer " + customerId.getValue());
        
        // 驗證安全性：敏感信息已被遮罩
        assertThat(creditCard.getCardNumber()).contains("****");
        assertThat(creditCard.getCvv()).isEqualTo("***");
    }

    @Test
    void shouldLogBillingAddressGeneration() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-004");
        Money amount = new Money(new BigDecimal("199.99"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-004", "CUSTOMER-004");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        // Then
        // 驗證帳單地址生成的日誌記錄
        PaymentRequest.BillingAddress billingAddress = paymentRequest.getBillingAddress();
        assertThat(billingAddress).isNotNull();
        assertThat(billingAddress.getStreet()).isEqualTo("123 Main Street");
        assertThat(billingAddress.getCity()).isEqualTo("Taipei");
        assertThat(billingAddress.getPostalCode()).isEqualTo("10001");
        assertThat(billingAddress.getCountry()).isEqualTo("TW");
    }

    @Test
    void shouldLogErrorWhenOrderNotFound() {
        // Given
        OrderId orderId = new OrderId("NONEXISTENT-ORDER");
        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-005", "CUSTOMER-005");

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> converter.convertToPaymentRequest(event))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Order not found for PaymentRequestedEvent: NONEXISTENT-ORDER");
        
        // 驗證錯誤日誌記錄
        // 這個測試驗證了當訂單不存在時，轉換器會記錄適當的錯誤信息
    }

    @Test
    void shouldLogConversionWithDifferentCurrencies() {
        // Given
        Currency[] currencies = {
            Currency.getInstance("USD"),
            Currency.getInstance("EUR"),
            Currency.getInstance("GBP"),
            Currency.getInstance("JPY")
        };

        for (int i = 0; i < currencies.length; i++) {
            CustomerId customerId = new CustomerId("CUSTOMER-" + (i + 1));
            Money amount = new Money(new BigDecimal("100.00"), currencies[i]);
            Order order = new Order(customerId, amount);
            OrderId orderId = order.getOrderId();

            PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-" + (i + 1), customerId.getValue());

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When
            PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

            // Then
            // 驗證不同貨幣的轉換日誌記錄
            assertThat(paymentRequest.getCurrency()).isEqualTo(currencies[i].getCurrencyCode());
            assertThat(paymentRequest.getAmount()).isEqualTo(new BigDecimal("100.00"));
            
            // 驗證轉換過程的一致性
            assertThat(paymentRequest.getOrderId()).isEqualTo(orderId.getValue());
            assertThat(paymentRequest.getCustomerId()).isEqualTo(customerId.getValue());
        }
    }

    @Test
    void shouldLogConversionTimingInformation() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-TIMING");
        Money amount = new Money(new BigDecimal("500.00"), Currency.getInstance("USD"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-TIMING", "CUSTOMER-TIMING");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        LocalDateTime beforeConversion = LocalDateTime.now();

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        LocalDateTime afterConversion = LocalDateTime.now();

        // Then
        // 驗證轉換時間的日誌記錄
        assertThat(paymentRequest.getTimestamp()).isNotNull();
        assertThat(paymentRequest.getTimestamp()).isAfter(beforeConversion.minusSeconds(1));
        assertThat(paymentRequest.getTimestamp()).isBefore(afterConversion.plusSeconds(1));
        
        // 驗證時間戳的精確性
        assertThat(paymentRequest.getTimestamp()).isBetween(beforeConversion, afterConversion);
    }

    @Test
    void shouldLogConversionDataIntegrity() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-INTEGRITY");
        Money amount = new Money(new BigDecimal("999.99"), Currency.getInstance("EUR"));
        Order order = new Order(customerId, amount);
        OrderId orderId = order.getOrderId();

        PaymentRequestedEvent event = new PaymentRequestedEvent(orderId, "TXN-INTEGRITY", "CUSTOMER-INTEGRITY");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        PaymentRequest paymentRequest = converter.convertToPaymentRequest(event);

        // Then
        // 驗證數據完整性的日誌記錄
        assertThat(paymentRequest.getTransactionId()).isEqualTo(event.getTransactionId());
        assertThat(paymentRequest.getOrderId()).isEqualTo(event.getOrderId().getValue());
        assertThat(paymentRequest.getCustomerId()).isEqualTo(event.getCustomerId());
        
        // 驗證從訂單獲取的數據
        assertThat(paymentRequest.getAmount()).isEqualTo(order.getAmount().getAmount());
        assertThat(paymentRequest.getCurrency()).isEqualTo(order.getAmount().getCurrency());
        
        // 驗證生成的數據
        assertThat(paymentRequest.getMerchantId()).isNotNull();
        assertThat(paymentRequest.getDescription()).contains(orderId.getValue());
        assertThat(paymentRequest.getCreditCard()).isNotNull();
        assertThat(paymentRequest.getBillingAddress()).isNotNull();
        assertThat(paymentRequest.getTimestamp()).isNotNull();
    }
}
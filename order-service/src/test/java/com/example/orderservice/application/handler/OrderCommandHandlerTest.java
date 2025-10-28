package com.example.orderservice.application.handler;

import com.example.orderservice.application.command.CreateOrderCommand;
import com.example.orderservice.application.command.OrderCommandResult;
import com.example.orderservice.application.port.out.DomainEventPublisher;
import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.application.port.out.PaymentRequestPublisher;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.shared.DomainEvent;
import com.example.orderservice.messaging.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 訂單命令處理器單元測試
 */
@ExtendWith(MockitoExtension.class)
class OrderCommandHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRequestPublisher paymentRequestPublisher;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private OrderCommandHandler orderCommandHandler;

    private CreateOrderCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = CreateOrderCommand.builder()
            .customerId("CUST-001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .creditCard(CreateOrderCommand.CreditCardInfo.builder()
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("John Doe")
                .build())
            .billingAddress(CreateOrderCommand.BillingAddress.builder()
                .street("123 Main St")
                .city("New York")
                .postalCode("10001")
                .country("US")
                .build())
            .merchantId("MERCHANT-001")
            .description("Test order")
            .build();
    }

    @Test
    void shouldHandleCreateOrderCommandSuccessfully() {
        // When
        OrderCommandResult result = orderCommandHandler.handle(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isNotNull();
        assertThat(result.getTransactionId()).isNotNull();
        assertThat(result.getTransactionId()).startsWith("TXN-");
        assertThat(result.getStatus()).isEqualTo("PAYMENT_PENDING");
        assertThat(result.getMessage()).isEqualTo("Order created successfully and payment request sent");

        // 驗證訂單保存
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getCustomerId().getValue()).isEqualTo("CUST-001");
        assertThat(savedOrder.getAmount().getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(savedOrder.getAmount().getCurrency()).isEqualTo("USD");

        // 驗證領域事件發布
        verify(domainEventPublisher).publishEvents(any(List.class));

        // 驗證支付請求發布
        ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentRequestPublisher).publishPaymentRequest(paymentRequestCaptor.capture());
        
        PaymentRequest publishedRequest = paymentRequestCaptor.getValue();
        assertThat(publishedRequest.getOrderId()).isEqualTo(savedOrder.getOrderId().getValue());
        assertThat(publishedRequest.getCustomerId()).isEqualTo("CUST-001");
        assertThat(publishedRequest.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(publishedRequest.getCurrency()).isEqualTo("USD");
        assertThat(publishedRequest.getCreditCard().getCardNumber()).isEqualTo("4111111111111111");
        assertThat(publishedRequest.getMerchantId()).isEqualTo("MERCHANT-001");
        assertThat(publishedRequest.getDescription()).isEqualTo("Test order");
    }

    @Test
    void shouldReturnFailureWhenCustomerIdIsNull() {
        // Given
        CreateOrderCommand invalidCommand = CreateOrderCommand.builder()
            .customerId(null)
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Customer ID is required");

        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldReturnFailureWhenCustomerIdIsEmpty() {
        // Given
        CreateOrderCommand invalidCommand = CreateOrderCommand.builder()
            .customerId("")
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Customer ID is required");

        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldReturnFailureWhenAmountIsNull() {
        // Given
        CreateOrderCommand invalidCommand = CreateOrderCommand.builder()
            .customerId(validCommand.getCustomerId())
            .amount(null)
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Amount must be positive");

        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldReturnFailureWhenAmountIsNegative() {
        // Given
        CreateOrderCommand invalidCommand = CreateOrderCommand.builder()
            .customerId(validCommand.getCustomerId())
            .amount(new BigDecimal("-10.00"))
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Amount must be positive");

        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldReturnFailureWhenAmountIsZero() {
        // Given
        CreateOrderCommand invalidCommand = CreateOrderCommand.builder()
            .customerId(validCommand.getCustomerId())
            .amount(BigDecimal.ZERO)
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Amount must be positive");

        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldReturnFailureWhenCurrencyIsNull() {
        // Given
        CreateOrderCommand invalidCommand = CreateOrderCommand.builder()
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(null)
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Currency is required");

        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldReturnFailureWhenCreditCardIsNull() {
        // Given
        CreateOrderCommand invalidCommand = CreateOrderCommand.builder()
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(null)
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Credit card information is required");

        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldReturnFailureWhenCardNumberIsNull() {
        // Given
        CreateOrderCommand invalidCommand = CreateOrderCommand.builder()
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(CreateOrderCommand.CreditCardInfo.builder()
                .cardNumber(null)
                .expiryDate(validCommand.getCreditCard().getExpiryDate())
                .cvv(validCommand.getCreditCard().getCvv())
                .cardHolderName(validCommand.getCreditCard().getCardHolderName())
                .build())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Card number is required");

        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldHandleCommandWithoutBillingAddress() {
        // Given
        CreateOrderCommand commandWithoutBilling = CreateOrderCommand.builder()
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(null)
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .build();

        // When
        OrderCommandResult result = orderCommandHandler.handle(commandWithoutBilling);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // 驗證支付請求中的帳單地址為空
        ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentRequestPublisher).publishPaymentRequest(paymentRequestCaptor.capture());
        
        PaymentRequest publishedRequest = paymentRequestCaptor.getValue();
        assertThat(publishedRequest.getBillingAddress()).isNull();
    }

    @Test
    void shouldGenerateUniqueTransactionIds() {
        // When
        OrderCommandResult result1 = orderCommandHandler.handle(validCommand);
        OrderCommandResult result2 = orderCommandHandler.handle(validCommand);

        // Then
        assertThat(result1.getTransactionId()).isNotEqualTo(result2.getTransactionId());
        assertThat(result1.getTransactionId()).startsWith("TXN-");
        assertThat(result2.getTransactionId()).startsWith("TXN-");
    }

    @Test
    void shouldPublishPaymentRequestedEvent() {
        // When
        OrderCommandResult result = orderCommandHandler.handle(validCommand);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(domainEventPublisher).publishEvents(any(List.class));
    }
}
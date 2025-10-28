package com.example.paymentservice.application.handler;

import com.example.paymentservice.application.command.ProcessPaymentCommand;
import com.example.paymentservice.application.command.PaymentCommandResult;
import com.example.paymentservice.application.port.out.DomainEventPublisher;
import com.example.paymentservice.application.port.out.PaymentConfirmationPublisher;
import com.example.paymentservice.application.port.out.PaymentGateway;
import com.example.paymentservice.application.port.out.PaymentRepository;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.event.PaymentFailedEvent;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.messaging.PaymentConfirmation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 支付命令處理器單元測試
 */
@ExtendWith(MockitoExtension.class)
class PaymentCommandHandlerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentConfirmationPublisher confirmationPublisher;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private PaymentCommandHandler paymentCommandHandler;

    private ProcessPaymentCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = ProcessPaymentCommand.builder()
            .transactionId("TXN-001")
            .orderId("ORDER-001")
            .customerId("CUST-001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .creditCard(ProcessPaymentCommand.CreditCardInfo.builder()
                .cardNumber("4111111111111111")
                .expiryDate("12/30")
                .cvv("123")
                .cardHolderName("John Doe")
                .build())
            .billingAddress(ProcessPaymentCommand.BillingAddress.builder()
                .street("123 Main St")
                .city("New York")
                .postalCode("10001")
                .country("US")
                .build())
            .merchantId("MERCHANT-001")
            .description("Test payment")
            .timestamp(LocalDateTime.now())
            .build();
    }

    @Test
    void shouldHandleProcessPaymentCommandSuccessfully() {
        // Given
        String gatewayResponse = "Gateway response: SUCCESS";
        when(paymentGateway.processPayment(any(), any(), any(), eq("MERCHANT-001")))
            .thenReturn(gatewayResponse);

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPaymentId()).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isEqualTo("Payment processed successfully");
        assertThat(result.getGatewayResponse()).isEqualTo(gatewayResponse);

        // 驗證支付記錄保存
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getTransactionId()).isEqualTo("TXN-001");
        assertThat(savedPayment.getOrderId()).isEqualTo("ORDER-001");
        assertThat(savedPayment.getCustomerId()).isEqualTo("CUST-001");
        assertThat(savedPayment.getAmount().getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(savedPayment.getAmount().getCurrency()).isEqualTo("USD");

        // 驗證支付網關調用
        verify(paymentGateway).processPayment(
            eq("TXN-001"), 
            any(), 
            any(), 
            eq("MERCHANT-001")
        );

        // 驗證領域事件發布
        verify(domainEventPublisher).publishEvents(any(List.class));

        // 驗證支付確認發布
        ArgumentCaptor<PaymentConfirmation> confirmationCaptor = ArgumentCaptor.forClass(PaymentConfirmation.class);
        verify(confirmationPublisher).publishPaymentConfirmation(confirmationCaptor.capture());
        
        PaymentConfirmation publishedConfirmation = confirmationCaptor.getValue();
        assertThat(publishedConfirmation.getTransactionId()).isEqualTo("TXN-001");
        assertThat(publishedConfirmation.getOrderId()).isEqualTo("ORDER-001");
        assertThat(publishedConfirmation.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(publishedConfirmation.getCurrency()).isEqualTo("USD");
        assertThat(publishedConfirmation.getStatus()).isEqualTo(PaymentConfirmation.PaymentStatus.SUCCESS);
        assertThat(publishedConfirmation.getGatewayResponse()).isEqualTo(gatewayResponse);
        assertThat(publishedConfirmation.getErrorMessage()).isNull();
    }

    @Test
    void shouldHandlePaymentFailureFromGateway() {
        // Given
        String gatewayResponse = "Gateway response: DECLINED";
        when(paymentGateway.processPayment(any(), any(), any(), eq("MERCHANT-001")))
            .thenReturn(gatewayResponse);

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).contains("Payment processing failed");
        assertThat(result.getGatewayResponse()).isEqualTo(gatewayResponse);

        // 驗證支付記錄保存
        verify(paymentRepository).save(any(Payment.class));

        // 驗證領域事件發布
        verify(domainEventPublisher).publishEvents(any(List.class));

        // 驗證支付確認發布
        ArgumentCaptor<PaymentConfirmation> confirmationCaptor = ArgumentCaptor.forClass(PaymentConfirmation.class);
        verify(confirmationPublisher).publishPaymentConfirmation(confirmationCaptor.capture());
        
        PaymentConfirmation publishedConfirmation = confirmationCaptor.getValue();
        assertThat(publishedConfirmation.getStatus()).isEqualTo(PaymentConfirmation.PaymentStatus.FAILED);
        assertThat(publishedConfirmation.getErrorMessage()).isNotNull();
    }

    @Test
    void shouldReturnFailureWhenTransactionIdIsNull() {
        // Given
        ProcessPaymentCommand invalidCommand = ProcessPaymentCommand.builder()
            .transactionId(null)
            .orderId(validCommand.getOrderId())
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .timestamp(validCommand.getTimestamp())
            .build();

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Transaction ID is required");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentGateway, never()).processPayment(any(), any(), any(), any());
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldReturnFailureWhenOrderIdIsNull() {
        // Given
        ProcessPaymentCommand invalidCommand = ProcessPaymentCommand.builder()
            .transactionId(validCommand.getTransactionId())
            .orderId(null)
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .timestamp(validCommand.getTimestamp())
            .build();

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Order ID is required");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentGateway, never()).processPayment(any(), any(), any(), any());
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldReturnFailureWhenCustomerIdIsNull() {
        // Given
        ProcessPaymentCommand invalidCommand = ProcessPaymentCommand.builder()
            .transactionId(validCommand.getTransactionId())
            .orderId(validCommand.getOrderId())
            .customerId(null)
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .timestamp(validCommand.getTimestamp())
            .build();

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Customer ID is required");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentGateway, never()).processPayment(any(), any(), any(), any());
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldReturnFailureWhenAmountIsNull() {
        // Given
        ProcessPaymentCommand invalidCommand = ProcessPaymentCommand.builder()
            .transactionId(validCommand.getTransactionId())
            .orderId(validCommand.getOrderId())
            .customerId(validCommand.getCustomerId())
            .amount(null)
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .timestamp(validCommand.getTimestamp())
            .build();

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Amount must be positive");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentGateway, never()).processPayment(any(), any(), any(), any());
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldReturnFailureWhenAmountIsNegative() {
        // Given
        ProcessPaymentCommand invalidCommand = ProcessPaymentCommand.builder()
            .transactionId(validCommand.getTransactionId())
            .orderId(validCommand.getOrderId())
            .customerId(validCommand.getCustomerId())
            .amount(new BigDecimal("-10.00"))
            .currency(validCommand.getCurrency())
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .timestamp(validCommand.getTimestamp())
            .build();

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Amount must be positive");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentGateway, never()).processPayment(any(), any(), any(), any());
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldReturnFailureWhenCurrencyIsNull() {
        // Given
        ProcessPaymentCommand invalidCommand = ProcessPaymentCommand.builder()
            .transactionId(validCommand.getTransactionId())
            .orderId(validCommand.getOrderId())
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(null)
            .creditCard(validCommand.getCreditCard())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .timestamp(validCommand.getTimestamp())
            .build();

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Currency is required");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentGateway, never()).processPayment(any(), any(), any(), any());
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldReturnFailureWhenCreditCardIsNull() {
        // Given
        ProcessPaymentCommand invalidCommand = ProcessPaymentCommand.builder()
            .transactionId(validCommand.getTransactionId())
            .orderId(validCommand.getOrderId())
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(null)
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .timestamp(validCommand.getTimestamp())
            .build();

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Credit card information is required");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentGateway, never()).processPayment(any(), any(), any(), any());
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldReturnFailureWhenCardNumberIsNull() {
        // Given
        ProcessPaymentCommand invalidCommand = ProcessPaymentCommand.builder()
            .transactionId(validCommand.getTransactionId())
            .orderId(validCommand.getOrderId())
            .customerId(validCommand.getCustomerId())
            .amount(validCommand.getAmount())
            .currency(validCommand.getCurrency())
            .creditCard(ProcessPaymentCommand.CreditCardInfo.builder()
                .cardNumber(null)
                .expiryDate(validCommand.getCreditCard().getExpiryDate())
                .cvv(validCommand.getCreditCard().getCvv())
                .cardHolderName(validCommand.getCreditCard().getCardHolderName())
                .build())
            .billingAddress(validCommand.getBillingAddress())
            .merchantId(validCommand.getMerchantId())
            .description(validCommand.getDescription())
            .timestamp(validCommand.getTimestamp())
            .build();

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(invalidCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Card number is required");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentGateway, never()).processPayment(any(), any(), any(), any());
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldHandleGatewayException() {
        // Given
        when(paymentGateway.processPayment(any(), any(), any(), eq("MERCHANT-001")))
            .thenThrow(new RuntimeException("Gateway connection failed"));

        // When
        PaymentCommandResult result = paymentCommandHandler.handle(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getMessage()).contains("Payment processing failed");

        // 驗證沒有保存支付記錄或發布事件
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
        verify(confirmationPublisher, never()).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldPublishCorrectDomainEventForSuccessfulPayment() {
        // Given
        String gatewayResponse = "Gateway response: SUCCESS";
        when(paymentGateway.processPayment(any(), any(), any(), eq("MERCHANT-001")))
            .thenReturn(gatewayResponse);

        // When
        paymentCommandHandler.handle(validCommand);

        // Then
        verify(paymentRepository).save(any(Payment.class));
        verify(domainEventPublisher).publishEvents(any(List.class));
        verify(confirmationPublisher).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }

    @Test
    void shouldPublishCorrectDomainEventForFailedPayment() {
        // Given
        String gatewayResponse = "Gateway response: DECLINED";
        when(paymentGateway.processPayment(any(), any(), any(), eq("MERCHANT-001")))
            .thenReturn(gatewayResponse);

        // When
        paymentCommandHandler.handle(validCommand);

        // Then
        verify(paymentRepository).save(any(Payment.class));
        verify(domainEventPublisher).publishEvents(any(List.class));
        verify(confirmationPublisher).publishPaymentConfirmation(any(PaymentConfirmation.class));
    }
}
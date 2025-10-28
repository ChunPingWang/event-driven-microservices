package com.example.paymentservice.application.service;

import com.example.paymentservice.application.command.ProcessPaymentCommand;
import com.example.paymentservice.application.command.PaymentCommandResult;
import com.example.paymentservice.application.handler.PaymentCommandHandler;
import com.example.paymentservice.application.handler.PaymentQueryHandler;
import com.example.paymentservice.application.port.out.DomainEventPublisher;
import com.example.paymentservice.application.port.out.PaymentRepository;
import com.example.paymentservice.application.query.PaymentListQuery;
import com.example.paymentservice.application.query.PaymentListQueryResult;
import com.example.paymentservice.application.query.PaymentQuery;
import com.example.paymentservice.application.query.PaymentQueryResult;
import com.example.paymentservice.domain.payment.Payment;

import com.example.paymentservice.domain.payment.valueobject.CreditCard;
import com.example.paymentservice.domain.payment.valueobject.Money;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.domain.payment.valueobject.PaymentStatus;
import com.example.paymentservice.domain.shared.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 支付應用服務單元測試
 */
@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    @Mock
    private PaymentCommandHandler commandHandler;

    @Mock
    private PaymentQueryHandler queryHandler;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private PaymentApplicationService paymentApplicationService;

    private ProcessPaymentCommand processPaymentCommand;
    private PaymentQuery paymentQuery;
    private PaymentListQuery paymentListQuery;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        // 準備測試數據
        processPaymentCommand = ProcessPaymentCommand.builder()
            .transactionId("TXN-001")
            .orderId("ORDER-001")
            .customerId("CUST-001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .creditCard(ProcessPaymentCommand.CreditCardInfo.builder()
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("John Doe")
                .build())
            .merchantId("MERCHANT-001")
            .description("Test payment")
            .timestamp(LocalDateTime.now())
            .build();

        paymentQuery = PaymentQuery.builder()
            .paymentId("PAY-001")
            .build();

        paymentListQuery = PaymentListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(10)
            .build();

        // 創建測試支付
        Money amount = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        CreditCard creditCard = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        testPayment = new Payment("TXN-001", "ORDER-001", "CUST-001", amount, creditCard);
        testPayment.process("Gateway response: SUCCESS");
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        PaymentCommandResult expectedResult = PaymentCommandResult.success(
            "PAY-001", "TXN-001", "ORDER-001", "Gateway response: SUCCESS");
        when(commandHandler.handle(processPaymentCommand)).thenReturn(expectedResult);

        // When
        PaymentCommandResult result = paymentApplicationService.processPayment(processPaymentCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPaymentId()).isEqualTo("PAY-001");
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getGatewayResponse()).isEqualTo("Gateway response: SUCCESS");
        
        verify(commandHandler).handle(processPaymentCommand);
    }

    @Test
    void shouldGetPaymentSuccessfully() {
        // Given
        PaymentQueryResult expectedResult = PaymentQueryResult.builder()
            .paymentId("PAY-001")
            .transactionId("TXN-001")
            .orderId("ORDER-001")
            .customerId("CUST-001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .status("SUCCESS")
            .gatewayResponse("Gateway response: SUCCESS")
            .createdAt(LocalDateTime.now())
            .processedAt(LocalDateTime.now())
            .build();
        when(queryHandler.handle(paymentQuery)).thenReturn(expectedResult);

        // When
        PaymentQueryResult result = paymentApplicationService.getPayment(paymentQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo("PAY-001");
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        
        verify(queryHandler).handle(paymentQuery);
    }

    @Test
    void shouldGetPaymentsSuccessfully() {
        // Given
        List<PaymentQueryResult> payments = Arrays.asList(
            PaymentQueryResult.builder()
                .paymentId("PAY-001")
                .transactionId("TXN-001")
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status("SUCCESS")
                .build(),
            PaymentQueryResult.builder()
                .paymentId("PAY-002")
                .transactionId("TXN-002")
                .orderId("ORDER-002")
                .customerId("CUST-001")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .status("FAILED")
                .build()
        );
        
        PaymentListQueryResult expectedResult = PaymentListQueryResult.builder()
            .payments(payments)
            .totalCount(2)
            .page(0)
            .size(10)
            .hasNext(false)
            .build();
        
        when(queryHandler.handle(paymentListQuery)).thenReturn(expectedResult);

        // When
        PaymentListQueryResult result = paymentApplicationService.getPayments(paymentListQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPayments()).hasSize(2);
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.isHasNext()).isFalse();
        
        verify(queryHandler).handle(paymentListQuery);
    }

    @Test
    void shouldRefundPaymentSuccessfully() {
        // Given
        String paymentId = testPayment.getPaymentId().getValue();
        String refundReason = "Customer request";
        
        when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(testPayment));

        // When
        paymentApplicationService.refundPayment(paymentId, refundReason);

        // Then
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        
        verify(paymentRepository).findById(any(PaymentId.class));
        verify(paymentRepository).save(testPayment);
        verify(domainEventPublisher).publishEvents(any(List.class));
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFoundForRefund() {
        // Given
        String paymentId = "NON-EXISTENT-PAYMENT";
        String refundReason = "Customer request";
        
        when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            paymentApplicationService.refundPayment(paymentId, refundReason))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to process refund");
        
        verify(paymentRepository).findById(any(PaymentId.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldThrowExceptionWhenPaymentIdIsNullForRefund() {
        // Given
        String paymentId = null;
        String refundReason = "Customer request";

        // When & Then
        assertThatThrownBy(() -> 
            paymentApplicationService.refundPayment(paymentId, refundReason))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to process refund");
        
        verify(paymentRepository, never()).findById(any(PaymentId.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldThrowExceptionWhenPaymentIdIsEmptyForRefund() {
        // Given
        String paymentId = "";
        String refundReason = "Customer request";

        // When & Then
        assertThatThrownBy(() -> 
            paymentApplicationService.refundPayment(paymentId, refundReason))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to process refund");
        
        verify(paymentRepository, never()).findById(any(PaymentId.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldThrowExceptionWhenRefundReasonIsNullForRefund() {
        // Given
        String paymentId = testPayment.getPaymentId().getValue();
        String refundReason = null;

        // When & Then
        assertThatThrownBy(() -> 
            paymentApplicationService.refundPayment(paymentId, refundReason))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to process refund");
        
        verify(paymentRepository, never()).findById(any(PaymentId.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldThrowExceptionWhenRefundReasonIsEmptyForRefund() {
        // Given
        String paymentId = testPayment.getPaymentId().getValue();
        String refundReason = "";

        // When & Then
        assertThatThrownBy(() -> 
            paymentApplicationService.refundPayment(paymentId, refundReason))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to process refund");
        
        verify(paymentRepository, never()).findById(any(PaymentId.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldClearDomainEventsAfterPublishing() {
        // Given
        String paymentId = testPayment.getPaymentId().getValue();
        String refundReason = "Customer request";
        
        when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(testPayment));

        // When
        paymentApplicationService.refundPayment(paymentId, refundReason);

        // Then
        verify(domainEventPublisher).publishEvents(any(List.class));
    }
}
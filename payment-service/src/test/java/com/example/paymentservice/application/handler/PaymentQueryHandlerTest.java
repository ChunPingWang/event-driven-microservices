package com.example.paymentservice.application.handler;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付查詢處理器單元測試
 */
@ExtendWith(MockitoExtension.class)
class PaymentQueryHandlerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentQueryHandler paymentQueryHandler;

    private Payment testPayment1;
    private Payment testPayment2;

    @BeforeEach
    void setUp() {
        // 創建測試支付 - 使用未來的到期日期
        Money amount1 = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        CreditCard creditCard1 = new CreditCard("4111111111111111", "12/30", "123", "John Doe");
        testPayment1 = new Payment("TXN-001", "ORDER-001", "CUST-001", amount1, creditCard1);
        testPayment1.process("Gateway response: SUCCESS");

        Money amount2 = new Money(new BigDecimal("200.00"), Currency.getInstance("USD"));
        CreditCard creditCard2 = new CreditCard("4000000000000002", "01/31", "456", "Jane Smith");
        testPayment2 = new Payment("TXN-002", "ORDER-002", "CUST-001", amount2, creditCard2);
        testPayment2.process("Gateway response: DECLINED");
    }

    @Test
    void shouldHandlePaymentQueryByPaymentId() {
        // Given
        PaymentQuery query = PaymentQuery.builder()
            .paymentId(testPayment1.getPaymentId().getValue())
            .build();
        
        when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.of(testPayment1));

        // When
        PaymentQueryResult result = paymentQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(testPayment1.getPaymentId().getValue());
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getGatewayResponse()).isEqualTo("Gateway response: SUCCESS");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getProcessedAt()).isNotNull();

        verify(paymentRepository).findById(any(PaymentId.class));
    }

    @Test
    void shouldHandlePaymentQueryByTransactionId() {
        // Given
        PaymentQuery query = PaymentQuery.builder()
            .transactionId("TXN-001")
            .build();
        
        when(paymentRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(testPayment1));

        // When
        PaymentQueryResult result = paymentQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");

        verify(paymentRepository).findByTransactionId("TXN-001");
    }

    @Test
    void shouldHandlePaymentQueryByOrderId() {
        // Given
        PaymentQuery query = PaymentQuery.builder()
            .orderId("ORDER-001")
            .build();
        
        when(paymentRepository.findByOrderId("ORDER-001")).thenReturn(Optional.of(testPayment1));

        // When
        PaymentQueryResult result = paymentQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");

        verify(paymentRepository).findByOrderId("ORDER-001");
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        PaymentQuery query = PaymentQuery.builder()
            .paymentId("NON-EXISTENT-PAYMENT")
            .transactionId("NON-EXISTENT-TXN")
            .orderId("NON-EXISTENT-ORDER")
            .build();
        
        when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());
        when(paymentRepository.findByTransactionId(any())).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentQueryHandler.handle(query))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Payment not found");

        verify(paymentRepository).findById(any(PaymentId.class));
        verify(paymentRepository).findByTransactionId("NON-EXISTENT-TXN");
        verify(paymentRepository).findByOrderId("NON-EXISTENT-ORDER");
    }

    @Test
    void shouldHandlePaymentListQueryByCustomerId() {
        // Given
        PaymentListQuery query = PaymentListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(10)
            .build();
        
        List<Payment> payments = Arrays.asList(testPayment1, testPayment2);
        when(paymentRepository.findByCustomerId("CUST-001", 0, 10)).thenReturn(payments);
        when(paymentRepository.countPayments("CUST-001", null, null, null)).thenReturn(2);

        // When
        PaymentListQueryResult result = paymentQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPayments()).hasSize(2);
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.isHasNext()).isFalse();

        // 驗證第一個支付
        PaymentQueryResult firstPayment = result.getPayments().get(0);
        assertThat(firstPayment.getPaymentId()).isEqualTo(testPayment1.getPaymentId().getValue());
        assertThat(firstPayment.getCustomerId()).isEqualTo("CUST-001");
        assertThat(firstPayment.getStatus()).isEqualTo("SUCCESS");

        // 驗證第二個支付
        PaymentQueryResult secondPayment = result.getPayments().get(1);
        assertThat(secondPayment.getPaymentId()).isEqualTo(testPayment2.getPaymentId().getValue());
        assertThat(secondPayment.getCustomerId()).isEqualTo("CUST-001");
        assertThat(secondPayment.getStatus()).isEqualTo("FAILED");

        verify(paymentRepository).findByCustomerId("CUST-001", 0, 10);
        verify(paymentRepository).countPayments("CUST-001", null, null, null);
    }

    @Test
    void shouldHandlePaymentListQueryByStatus() {
        // Given
        PaymentListQuery query = PaymentListQuery.builder()
            .status("SUCCESS")
            .page(0)
            .size(10)
            .build();
        
        List<Payment> payments = Arrays.asList(testPayment1);
        when(paymentRepository.findByStatus(PaymentStatus.SUCCESS, 0, 10)).thenReturn(payments);
        when(paymentRepository.countPayments(null, PaymentStatus.SUCCESS, null, null)).thenReturn(1);

        // When
        PaymentListQueryResult result = paymentQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPayments()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.isHasNext()).isFalse();

        PaymentQueryResult paymentResult = result.getPayments().get(0);
        assertThat(paymentResult.getStatus()).isEqualTo("SUCCESS");

        verify(paymentRepository).findByStatus(PaymentStatus.SUCCESS, 0, 10);
        verify(paymentRepository).countPayments(null, PaymentStatus.SUCCESS, null, null);
    }

    @Test
    void shouldHandlePaymentListQueryByDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        PaymentListQuery query = PaymentListQuery.builder()
            .startDate(startDate)
            .endDate(endDate)
            .page(0)
            .size(10)
            .build();
        
        List<Payment> payments = Arrays.asList(testPayment1, testPayment2);
        when(paymentRepository.findByDateRange(startDate, endDate, 0, 10)).thenReturn(payments);
        when(paymentRepository.countPayments(null, null, startDate, endDate)).thenReturn(2);

        // When
        PaymentListQueryResult result = paymentQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPayments()).hasSize(2);
        assertThat(result.getTotalCount()).isEqualTo(2);

        verify(paymentRepository).findByDateRange(startDate, endDate, 0, 10);
        verify(paymentRepository).countPayments(null, null, startDate, endDate);
    }

    @Test
    void shouldCalculateHasNextCorrectly() {
        // Given
        PaymentListQuery query = PaymentListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(1)
            .build();
        
        List<Payment> payments = Arrays.asList(testPayment1);
        when(paymentRepository.findByCustomerId("CUST-001", 0, 1)).thenReturn(payments);
        when(paymentRepository.countPayments("CUST-001", null, null, null)).thenReturn(2);

        // When
        PaymentListQueryResult result = paymentQueryHandler.handle(query);

        // Then
        assertThat(result.isHasNext()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenPageIsNegative() {
        // Given
        PaymentListQuery query = PaymentListQuery.builder()
            .customerId("CUST-001")
            .page(-1)
            .size(10)
            .build();

        // When & Then
        assertThatThrownBy(() -> paymentQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page number cannot be negative");
    }

    @Test
    void shouldThrowExceptionWhenSizeIsZero() {
        // Given
        PaymentListQuery query = PaymentListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(0)
            .build();

        // When & Then
        assertThatThrownBy(() -> paymentQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size must be positive");
    }

    @Test
    void shouldThrowExceptionWhenSizeIsNegative() {
        // Given
        PaymentListQuery query = PaymentListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(-1)
            .build();

        // When & Then
        assertThatThrownBy(() -> paymentQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size must be positive");
    }

    @Test
    void shouldThrowExceptionWhenSizeExceedsLimit() {
        // Given
        PaymentListQuery query = PaymentListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(101)
            .build();

        // When & Then
        assertThatThrownBy(() -> paymentQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size cannot exceed 100");
    }

    @Test
    void shouldThrowExceptionWhenNoCriteriaProvided() {
        // Given
        PaymentListQuery query = PaymentListQuery.builder()
            .page(0)
            .size(10)
            .build();

        // When & Then
        assertThatThrownBy(() -> paymentQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one query criteria must be provided");
    }
}
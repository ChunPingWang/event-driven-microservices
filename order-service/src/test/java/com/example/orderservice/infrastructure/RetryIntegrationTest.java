package com.example.orderservice.infrastructure;

import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.application.port.out.PaymentRequestPublisher;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import com.example.orderservice.infrastructure.retry.*;
import com.example.orderservice.messaging.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 重試機制集成測試 - 測試支付重試邏輯和數據庫操作
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PaymentRetryService.class})
@TestPropertySource(properties = {
    "payment.retry.max-attempts=3",
    "payment.retry.base-delay-minutes=1",
    "payment.retry.timeout-minutes=30",
    "payment.retry.batch-size=10"
})
class RetryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orderdb_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PaymentRetryService paymentRetryService;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private PaymentRequestPublisher paymentRequestPublisher;

    @Autowired
    private RetryHistoryRepository retryHistoryRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @BeforeEach
    void setUp() {
        retryHistoryRepository.deleteAll();
        reset(orderRepository, paymentRequestPublisher);
    }

    @Test
    @Transactional
    void shouldProcessPaymentRetrySuccessfully() {
        // Given
        Order failedOrder = createFailedOrder();
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_FAILED, 0, 10))
                .thenReturn(List.of(failedOrder));
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10))
                .thenReturn(List.of());

        // When
        paymentRetryService.processPaymentRetries();

        // Then
        verify(orderRepository).save(failedOrder);
        verify(paymentRequestPublisher).publishPaymentRequest(any(PaymentRequest.class));
        
        // 驗證重試歷史已記錄
        Optional<RetryHistory> retryHistory = retryHistoryRepository.findByOrderId(failedOrder.getOrderId().getValue());
        assertThat(retryHistory).isPresent();
        assertThat(retryHistory.get().getAttemptCount()).isEqualTo(1);
        assertThat(retryHistory.get().getNextRetryAt()).isNotNull();
    }

    @Test
    @Transactional
    void shouldProcessTimeoutOrderRetry() {
        // Given
        Order timeoutOrder = createTimeoutOrder();
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_FAILED, 0, 10))
                .thenReturn(List.of());
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10))
                .thenReturn(List.of(timeoutOrder));

        // When
        paymentRetryService.processPaymentRetries();

        // Then
        verify(orderRepository).save(timeoutOrder);
        verify(paymentRequestPublisher).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    @Transactional
    void shouldSkipRetryWhenNotTimeReached() {
        // Given
        Order failedOrder = createFailedOrder();
        
        // 創建重試歷史，設置下次重試時間為未來
        RetryHistory retryHistory = new RetryHistory(failedOrder.getOrderId().getValue(), failedOrder.getTransactionId());
        retryHistory.recordAttempt(UUID.randomUUID().toString(), LocalDateTime.now().plusMinutes(30));
        retryHistoryRepository.save(retryHistory);
        
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_FAILED, 0, 10))
                .thenReturn(List.of(failedOrder));
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10))
                .thenReturn(List.of());

        // When
        paymentRetryService.processPaymentRetries();

        // Then
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    @Transactional
    void shouldMarkOrderAsFinallyFailedAfterMaxRetries() {
        // Given
        Order failedOrder = createFailedOrder();
        
        // 創建已達到最大重試次數的重試歷史
        RetryHistory retryHistory = new RetryHistory(failedOrder.getOrderId().getValue(), failedOrder.getTransactionId());
        for (int i = 0; i < 3; i++) {
            retryHistory.recordAttempt(UUID.randomUUID().toString(), LocalDateTime.now().minusMinutes(1));
        }
        retryHistoryRepository.save(retryHistory);
        
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_FAILED, 0, 10))
                .thenReturn(List.of(failedOrder));
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10))
                .thenReturn(List.of());

        // When
        paymentRetryService.processPaymentRetries();

        // Then
        verify(orderRepository).save(failedOrder);
        verify(paymentRequestPublisher, never()).publishPaymentRequest(any(PaymentRequest.class));
        
        // 驗證重試歷史已標記為最終失敗
        Optional<RetryHistory> updatedHistory = retryHistoryRepository.findByOrderId(failedOrder.getOrderId().getValue());
        assertThat(updatedHistory).isPresent();
        assertThat(updatedHistory.get().getStatus()).isEqualTo(RetryHistory.RetryStatus.FINALLY_FAILED);
        assertThat(updatedHistory.get().getFinalFailureReason()).isEqualTo("Maximum retry attempts exceeded");
    }

    @Test
    @Transactional
    void shouldHandleMultipleOrdersInBatch() {
        // Given
        Order failedOrder1 = createFailedOrder();
        Order failedOrder2 = createFailedOrder();
        Order timeoutOrder = createTimeoutOrder();
        
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_FAILED, 0, 10))
                .thenReturn(List.of(failedOrder1, failedOrder2));
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10))
                .thenReturn(List.of(timeoutOrder));

        // When
        paymentRetryService.processPaymentRetries();

        // Then
        verify(orderRepository, times(3)).save(any(Order.class));
        verify(paymentRequestPublisher, times(3)).publishPaymentRequest(any(PaymentRequest.class));
        
        // 驗證所有訂單的重試歷史都已記錄
        assertThat(retryHistoryRepository.findByOrderId(failedOrder1.getOrderId().getValue())).isPresent();
        assertThat(retryHistoryRepository.findByOrderId(failedOrder2.getOrderId().getValue())).isPresent();
        assertThat(retryHistoryRepository.findByOrderId(timeoutOrder.getOrderId().getValue())).isPresent();
    }

    @Test
    @Transactional
    void shouldHandleManualRetry() {
        // Given
        Order failedOrder = createFailedOrder();
        String orderId = failedOrder.getOrderId().getValue();
        
        when(orderRepository.findById(failedOrder.getOrderId()))
                .thenReturn(Optional.of(failedOrder));

        // When
        PaymentRetryService.RetryResult result = paymentRetryService.manualRetry(orderId);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentRetryService.RetryResult.RetryStatus.SUCCESS);
        verify(orderRepository).save(failedOrder);
        verify(paymentRequestPublisher).publishPaymentRequest(any(PaymentRequest.class));
    }

    @Test
    void shouldThrowExceptionForNullOrderId() {
        // When & Then
        assertThatThrownBy(() -> paymentRetryService.manualRetry(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order ID cannot be null or empty");
    }

    @Test
    void shouldReturnFailedResultForNonExistentOrder() {
        // Given
        String nonExistentOrderId = "non-existent-order";
        when(orderRepository.findById(any(OrderId.class)))
                .thenReturn(Optional.empty());

        // When
        PaymentRetryService.RetryResult result = paymentRetryService.manualRetry(nonExistentOrderId);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentRetryService.RetryResult.RetryStatus.FAILED);
        assertThat(result.getMessage()).contains("Order not found");
    }

    @Test
    @Transactional
    void shouldGetRetryStatistics() {
        // Given
        // 創建一些重試歷史記錄
        RetryHistory successHistory = new RetryHistory("order-1", "tx-1");
        successHistory.recordAttempt("tx-1-retry", LocalDateTime.now());
        successHistory.markAsSuccessful();
        retryHistoryRepository.save(successHistory);
        
        RetryHistory failedHistory = new RetryHistory("order-2", "tx-2");
        failedHistory.recordAttempt("tx-2-retry", LocalDateTime.now());
        failedHistory.markAsFinallyFailed("Max retries exceeded");
        retryHistoryRepository.save(failedHistory);
        
        RetryHistory pendingHistory = new RetryHistory("order-3", "tx-3");
        pendingHistory.recordAttempt("tx-3-retry", LocalDateTime.now().plusMinutes(10));
        retryHistoryRepository.save(pendingHistory);

        // When
        RetryStatistics stats = paymentRetryService.getRetryStatistics();

        // Then
        assertThat(stats.getTotalCount()).isEqualTo(3);
        assertThat(stats.getSuccessfulCount()).isEqualTo(1);
        assertThat(stats.getFinallyFailedCount()).isEqualTo(1);
        assertThat(stats.getPendingCount()).isEqualTo(1);
    }

    @Test
    @Transactional
    void shouldHandleRetryServiceException() {
        // Given
        Order failedOrder = createFailedOrder();
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_FAILED, 0, 10))
                .thenReturn(List.of(failedOrder));
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10))
                .thenReturn(List.of());
        
        // 模擬發布消息時發生異常
        doThrow(new RuntimeException("Message publishing failed"))
                .when(paymentRequestPublisher).publishPaymentRequest(any(PaymentRequest.class));

        // When
        paymentRetryService.processPaymentRetries();

        // Then
        // 驗證即使發生異常，重試歷史仍然被記錄
        Optional<RetryHistory> retryHistory = retryHistoryRepository.findByOrderId(failedOrder.getOrderId().getValue());
        assertThat(retryHistory).isPresent();
    }

    @Test
    @Transactional
    void shouldCreatePaymentRequestWithCorrectData() {
        // Given
        Order failedOrder = createFailedOrder();
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_FAILED, 0, 10))
                .thenReturn(List.of(failedOrder));
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10))
                .thenReturn(List.of());

        // When
        paymentRetryService.processPaymentRetries();

        // Then
        verify(paymentRequestPublisher).publishPaymentRequest(argThat(request -> {
            assertThat(request.getOrderId()).isEqualTo(failedOrder.getOrderId().getValue());
            assertThat(request.getCustomerId()).isEqualTo(failedOrder.getCustomerId().getValue());
            assertThat(request.getAmount()).isEqualTo(failedOrder.getAmount().getAmount());
            assertThat(request.getCurrency()).isEqualTo(failedOrder.getAmount().getCurrency());
            assertThat(request.getTransactionId()).isNotEqualTo(failedOrder.getTransactionId()); // 新的交易ID
            assertThat(request.getDescription()).contains("retry");
            return true;
        }));
    }

    private Order createFailedOrder() {
        Order order = new Order(
                new CustomerId("customer-123"),
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD"))
        );
        order.requestPayment(UUID.randomUUID().toString());
        order.failPayment("Payment failed");
        
        return order;
    }

    private Order createTimeoutOrder() {
        Order order = new Order(
                new CustomerId("customer-456"),
                new Money(new BigDecimal("200.00"), Currency.getInstance("USD"))
        );
        order.requestPayment(UUID.randomUUID().toString());
        
        return order;
    }
}
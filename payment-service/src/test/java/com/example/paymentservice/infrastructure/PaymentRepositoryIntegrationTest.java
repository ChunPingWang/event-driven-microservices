package com.example.paymentservice.infrastructure;

import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.valueobject.*;
import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.infrastructure.outbox.OutboxEvent;
import com.example.paymentservice.infrastructure.outbox.OutboxRepository;
import com.example.paymentservice.infrastructure.outbox.OutboxService;
import com.example.paymentservice.infrastructure.repository.PaymentRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

/**
 * 支付倉儲集成測試 - 測試數據庫操作、事務處理和 Outbox Pattern
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PaymentRepositoryImpl.class, OutboxService.class, ObjectMapper.class})
class PaymentRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("paymentdb_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PaymentRepositoryImpl paymentRepository;

    @Autowired
    private PaymentRepositoryImpl.PaymentJpaRepository jpaRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxService outboxService;

    private Payment testPayment;

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
        jpaRepository.deleteAll();
        outboxRepository.deleteAll();
        testPayment = createTestPayment();
    }

    @Test
    void shouldSaveAndRetrievePayment() {
        // When
        paymentRepository.save(testPayment);

        // Then
        Optional<Payment> retrieved = paymentRepository.findById(testPayment.getPaymentId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPaymentId()).isEqualTo(testPayment.getPaymentId());
        assertThat(retrieved.get().getOrderId()).isEqualTo(testPayment.getOrderId());
        assertThat(retrieved.get().getAmount()).isEqualTo(testPayment.getAmount());
        assertThat(retrieved.get().getStatus()).isEqualTo(testPayment.getStatus());
    }

    @Test
    void shouldFindPaymentByTransactionId() {
        // Given
        paymentRepository.save(testPayment);

        // When
        Optional<Payment> found = paymentRepository.findByTransactionId(testPayment.getTransactionId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentId()).isEqualTo(testPayment.getPaymentId());
    }

    @Test
    void shouldFindPaymentByOrderId() {
        // Given
        paymentRepository.save(testPayment);

        // When
        Optional<Payment> found = paymentRepository.findByOrderId(testPayment.getOrderId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentId()).isEqualTo(testPayment.getPaymentId());
    }

    @Test
    void shouldFindPaymentsByCustomerId() {
        // Given
        String customerId = "customer-123";
        Payment payment1 = createPaymentForCustomer(customerId);
        Payment payment2 = createPaymentForCustomer(customerId);
        Payment payment3 = createPaymentForCustomer("other-customer");

        paymentRepository.save(payment1);
        paymentRepository.save(payment2);
        paymentRepository.save(payment3);

        // When
        List<Payment> customerPayments = paymentRepository.findByCustomerId(customerId, 0, 10);

        // Then
        assertThat(customerPayments).hasSize(2);
        assertThat(customerPayments).allMatch(payment -> 
            payment.getCustomerId().equals(customerId));
    }

    @Test
    void shouldFindPaymentsByStatus() {
        // Given
        Payment processingPayment1 = createPaymentWithStatus(PaymentStatus.PROCESSING);
        Payment processingPayment2 = createPaymentWithStatus(PaymentStatus.PROCESSING);
        Payment successPayment = createPaymentWithStatus(PaymentStatus.SUCCESS);

        paymentRepository.save(processingPayment1);
        paymentRepository.save(processingPayment2);
        paymentRepository.save(successPayment);

        // When
        List<Payment> processingPayments = paymentRepository.findByStatus(PaymentStatus.PROCESSING, 0, 10);

        // Then
        assertThat(processingPayments).hasSize(2);
        assertThat(processingPayments).allMatch(payment -> 
            payment.getStatus() == PaymentStatus.PROCESSING);
    }

    @Test
    @Transactional
    void shouldSaveEventToOutbox() {
        // Given
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            testPayment.getPaymentId(),
            testPayment.getOrderId(),
            testPayment.getTransactionId(),
            testPayment.getAmount().getAmount(),
            testPayment.getAmount().getCurrency()
        );

        // When
        paymentRepository.saveEvent(event, testPayment.getPaymentId().getValue(), "Payment");

        // Then
        List<OutboxEvent> outboxEvents = outboxRepository.findUnprocessedEvents(10);
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getEventType()).isEqualTo("PaymentProcessedEvent");
        assertThat(outboxEvents.get(0).getAggregateId()).isEqualTo(testPayment.getPaymentId().getValue());
        assertThat(outboxEvents.get(0).getAggregateType()).isEqualTo("Payment");
        assertThat(outboxEvents.get(0).isProcessed()).isFalse();
    }

    @Test
    void shouldCountPaymentsWithFilters() {
        // Given
        String customerId = "customer-123";
        Payment payment1 = createPaymentForCustomer(customerId);
        Payment payment2 = createPaymentForCustomer(customerId);
        // Process payment2 to success
        payment2.process("SUCCESS");

        paymentRepository.save(payment1);
        paymentRepository.save(payment2);

        // When
        int totalCount = paymentRepository.countPayments(customerId, null, null, null);
        int processingCount = paymentRepository.countPayments(customerId, PaymentStatus.PROCESSING, null, null);
        int successCount = paymentRepository.countPayments(customerId, PaymentStatus.SUCCESS, null, null);

        // Then
        assertThat(totalCount).isEqualTo(2);
        assertThat(processingCount).isEqualTo(1);
        assertThat(successCount).isEqualTo(1);
    }

    @Test
    void shouldHandlePaginationCorrectly() {
        // Given
        String customerId = "customer-123";
        for (int i = 0; i < 15; i++) {
            paymentRepository.save(createPaymentForCustomer(customerId));
        }

        // When
        List<Payment> page1 = paymentRepository.findByCustomerId(customerId, 0, 10);
        List<Payment> page2 = paymentRepository.findByCustomerId(customerId, 1, 10);

        // Then
        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(5);
    }

    @Test
    void shouldThrowExceptionForNullPayment() {
        // When & Then
        assertThatThrownBy(() -> paymentRepository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullDomainEvent() {
        // When & Then
        assertThatThrownBy(() -> paymentRepository.saveEvent(null, "payment-123", "Payment"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Domain event cannot be null");
    }

    @Test
    void shouldThrowExceptionForInvalidPagination() {
        // When & Then
        assertThatThrownBy(() -> paymentRepository.findByCustomerId("customer-123", -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page number cannot be negative");

        assertThatThrownBy(() -> paymentRepository.findByCustomerId("customer-123", 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be positive");

        assertThatThrownBy(() -> paymentRepository.findByCustomerId("customer-123", 0, 1001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size cannot exceed 1000");
    }

    @Test
    @Transactional
    void shouldRollbackTransactionOnFailure() {
        // Given
        Payment payment = createTestPayment();

        // When - 模擬事務失敗
        try {
            paymentRepository.save(payment);
            // 模擬在保存後發生異常
            throw new RuntimeException("Simulated transaction failure");
        } catch (RuntimeException e) {
            // 預期的異常
        }

        // Then - 驗證數據未被保存（由於事務回滾）
        Optional<Payment> retrieved = paymentRepository.findById(payment.getPaymentId());
        assertThat(retrieved).isEmpty();
    }

    private Payment createTestPayment() {
        return new Payment(
            UUID.randomUUID().toString(),
            "order-123",
            "customer-123",
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            createTestCreditCard()
        );
    }

    private Payment createPaymentForCustomer(String customerId) {
        return new Payment(
            UUID.randomUUID().toString(),
            "order-" + UUID.randomUUID().toString(),
            customerId,
            new Money(new BigDecimal("100.00"), Currency.getInstance("USD")),
            createTestCreditCard()
        );
    }

    private Payment createPaymentWithStatus(PaymentStatus status) {
        Payment payment = createTestPayment();
        if (status == PaymentStatus.SUCCESS) {
            payment.process("SUCCESS");
        } else if (status == PaymentStatus.FAILED) {
            payment.process("FAILED");
        }
        return payment;
    }

    private CreditCard createTestCreditCard() {
        return new CreditCard(
            "4111111111111111",
            "12/25",
            "123",
            "John Doe"
        );
    }
}
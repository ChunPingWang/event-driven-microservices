package com.example.paymentservice.infrastructure;

import com.example.paymentservice.domain.payment.event.PaymentProcessedEvent;
import com.example.paymentservice.domain.payment.valueobject.Money;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.infrastructure.outbox.*;
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
 * Outbox Pattern 集成測試 - 測試發件箱功能和事務一致性
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OutboxService.class, ObjectMapper.class})
class OutboxIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("paymentdb_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

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
        outboxRepository.deleteAll();
    }

    @Test
    @Transactional
    void shouldSaveEventToOutbox() {
        // Given
        PaymentProcessedEvent event = createTestEvent();
        String aggregateId = "payment-123";
        String aggregateType = "Payment";

        // When
        outboxService.saveEvent(event, aggregateId, aggregateType);

        // Then
        List<OutboxEvent> events = outboxRepository.findUnprocessedEvents(10);
        assertThat(events).hasSize(1);
        
        OutboxEvent savedEvent = events.get(0);
        assertThat(savedEvent.getEventType()).isEqualTo("PaymentProcessedEvent");
        assertThat(savedEvent.getAggregateId()).isEqualTo(aggregateId);
        assertThat(savedEvent.getAggregateType()).isEqualTo(aggregateType);
        assertThat(savedEvent.isProcessed()).isFalse();
        assertThat(savedEvent.getRetryCount()).isEqualTo(0);
        assertThat(savedEvent.getCreatedAt()).isNotNull();
        assertThat(savedEvent.getPayload()).isNotNull();
        assertThat(savedEvent.getHeaders()).isNotNull();
    }

    @Test
    @Transactional
    void shouldGetUnprocessedEvents() {
        // Given
        saveTestEvents(5, false);
        saveTestEvents(3, true); // 已處理的事件

        // When
        List<OutboxEvent> unprocessedEvents = outboxService.getUnprocessedEvents(10);

        // Then
        assertThat(unprocessedEvents).hasSize(5);
        assertThat(unprocessedEvents).allMatch(event -> !event.isProcessed());
    }

    @Test
    @Transactional
    void shouldGetRetryableEvents() {
        // Given
        int maxRetries = 3;
        
        // 創建可重試的事件（重試次數 < maxRetries）
        OutboxEvent retryableEvent1 = createOutboxEvent();
        retryableEvent1.recordFailure("First failure");
        outboxRepository.save(retryableEvent1);
        
        OutboxEvent retryableEvent2 = createOutboxEvent();
        retryableEvent2.recordFailure("First failure");
        retryableEvent2.recordFailure("Second failure");
        outboxRepository.save(retryableEvent2);
        
        // 創建不可重試的事件（重試次數 >= maxRetries）
        OutboxEvent nonRetryableEvent = createOutboxEvent();
        for (int i = 0; i < maxRetries; i++) {
            nonRetryableEvent.recordFailure("Failure " + (i + 1));
        }
        outboxRepository.save(nonRetryableEvent);

        // When
        List<OutboxEvent> retryableEvents = outboxService.getRetryableEvents(maxRetries, 10);

        // Then
        assertThat(retryableEvents).hasSize(2);
        assertThat(retryableEvents).allMatch(event -> 
            !event.isProcessed() && event.getRetryCount() < maxRetries);
    }

    @Test
    @Transactional
    void shouldMarkEventAsProcessed() {
        // Given
        OutboxEvent event = createOutboxEvent();
        outboxRepository.save(event);
        String eventId = event.getEventId();

        // When
        outboxService.markEventAsProcessed(eventId);

        // Then
        Optional<OutboxEvent> updatedEvent = outboxRepository.findById(eventId);
        assertThat(updatedEvent).isPresent();
        assertThat(updatedEvent.get().isProcessed()).isTrue();
        assertThat(updatedEvent.get().getProcessedAt()).isNotNull();
    }

    @Test
    @Transactional
    void shouldRecordEventFailure() {
        // Given
        OutboxEvent event = createOutboxEvent();
        outboxRepository.save(event);
        String eventId = event.getEventId();
        String errorMessage = "Processing failed";

        // When
        outboxService.recordEventFailure(eventId, errorMessage);

        // Then
        Optional<OutboxEvent> updatedEvent = outboxRepository.findById(eventId);
        assertThat(updatedEvent).isPresent();
        assertThat(updatedEvent.get().getRetryCount()).isEqualTo(1);
        assertThat(updatedEvent.get().getLastError()).isEqualTo(errorMessage);
        assertThat(updatedEvent.get().getLastError()).isNotNull();
    }

    @Test
    @Transactional
    void shouldCleanupProcessedEvents() {
        // Given
        int olderThanHours = 1;
        
        // 創建舊的已處理事件
        OutboxEvent oldProcessedEvent = createOutboxEvent();
        oldProcessedEvent.markAsProcessed();
        // 模擬舊事件（通過反射設置創建時間為2小時前）
        try {
            java.lang.reflect.Field createdAtField = OutboxEvent.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(oldProcessedEvent, LocalDateTime.now().minusHours(2));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
        outboxRepository.save(oldProcessedEvent);
        
        // 創建新的已處理事件
        OutboxEvent newProcessedEvent = createOutboxEvent();
        newProcessedEvent.markAsProcessed();
        outboxRepository.save(newProcessedEvent);
        
        // 創建未處理事件
        OutboxEvent unprocessedEvent = createOutboxEvent();
        outboxRepository.save(unprocessedEvent);

        // When
        int deletedCount = outboxService.cleanupProcessedEvents(olderThanHours);

        // Then
        assertThat(deletedCount).isEqualTo(1);
        
        List<OutboxEvent> remainingEvents = outboxRepository.findAll();
        assertThat(remainingEvents).hasSize(2);
        assertThat(remainingEvents).noneMatch(event -> 
            event.getEventId().equals(oldProcessedEvent.getEventId()));
    }

    @Test
    @Transactional
    void shouldCleanupExpiredFailedEvents() {
        // Given
        int olderThanHours = 1;
        int maxRetries = 3;
        
        // 創建過期的失敗事件
        OutboxEvent expiredFailedEvent = createOutboxEvent();
        for (int i = 0; i < maxRetries; i++) {
            expiredFailedEvent.recordFailure("Failure " + (i + 1));
        }
        try {
            java.lang.reflect.Field createdAtField = OutboxEvent.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(expiredFailedEvent, LocalDateTime.now().minusHours(2));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
        outboxRepository.save(expiredFailedEvent);
        
        // 創建新的失敗事件
        OutboxEvent newFailedEvent = createOutboxEvent();
        for (int i = 0; i < maxRetries; i++) {
            newFailedEvent.recordFailure("Failure " + (i + 1));
        }
        outboxRepository.save(newFailedEvent);
        
        // 創建可重試的事件
        OutboxEvent retryableEvent = createOutboxEvent();
        retryableEvent.recordFailure("First failure");
        try {
            java.lang.reflect.Field createdAtField = OutboxEvent.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(retryableEvent, LocalDateTime.now().minusHours(2));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
        outboxRepository.save(retryableEvent);

        // When
        int deletedCount = outboxService.cleanupExpiredFailedEvents(olderThanHours, maxRetries);

        // Then
        assertThat(deletedCount).isEqualTo(1);
        
        List<OutboxEvent> remainingEvents = outboxRepository.findAll();
        assertThat(remainingEvents).hasSize(2);
        assertThat(remainingEvents).noneMatch(event -> 
            event.getEventId().equals(expiredFailedEvent.getEventId()));
    }

    @Test
    @Transactional
    void shouldGetStatistics() {
        // Given
        int maxRetries = 3;
        
        // 創建未處理事件
        saveTestEvents(5, false);
        
        // 創建已處理事件
        saveTestEvents(3, true);
        
        // 創建失敗事件
        OutboxEvent failedEvent = createOutboxEvent();
        for (int i = 0; i < maxRetries; i++) {
            failedEvent.recordFailure("Failure " + (i + 1));
        }
        outboxRepository.save(failedEvent);

        // When
        OutboxService.OutboxStatistics stats = outboxService.getStatistics(maxRetries);

        // Then
        assertThat(stats.getTotalEvents()).isEqualTo(9);
        assertThat(stats.getUnprocessedEvents()).isEqualTo(6); // 5 unprocessed + 1 failed
        assertThat(stats.getFailedEvents()).isEqualTo(1);
        assertThat(stats.getProcessedEvents()).isEqualTo(3);
    }

    @Test
    void shouldThrowExceptionForNullDomainEvent() {
        // When & Then
        assertThatThrownBy(() -> outboxService.saveEvent(null, "payment-123", "Payment"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Domain event cannot be null");
    }

    @Test
    void shouldThrowExceptionForInvalidBatchSize() {
        // When & Then
        assertThatThrownBy(() -> outboxService.getUnprocessedEvents(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch size must be between 1 and 1000");

        assertThatThrownBy(() -> outboxService.getUnprocessedEvents(1001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch size must be between 1 and 1000");
    }

    @Test
    void shouldThrowExceptionForNonExistentEvent() {
        // Given
        String nonExistentEventId = UUID.randomUUID().toString();

        // When & Then
        assertThatThrownBy(() -> outboxService.markEventAsProcessed(nonExistentEventId))
                .isInstanceOf(OutboxService.OutboxException.class)
                .hasMessage("Outbox event not found: " + nonExistentEventId);
    }

    @Test
    @Transactional
    void shouldHandleTransactionRollback() {
        // Given
        PaymentProcessedEvent event = createTestEvent();
        String aggregateId = "payment-123";

        // When - 模擬事務回滾
        try {
            outboxService.saveEvent(event, aggregateId, "Payment");
            // 模擬異常導致事務回滾
            throw new RuntimeException("Simulated transaction failure");
        } catch (RuntimeException e) {
            // 預期的異常
        }

        // Then - 驗證事件未被保存
        List<OutboxEvent> events = outboxRepository.findUnprocessedEvents(10);
        assertThat(events).isEmpty();
    }

    @Test
    @Transactional
    void shouldHandleMultipleFailures() {
        // Given
        OutboxEvent event = createOutboxEvent();
        outboxRepository.save(event);
        String eventId = event.getEventId();

        // When - 記錄多次失敗
        outboxService.recordEventFailure(eventId, "First failure");
        outboxService.recordEventFailure(eventId, "Second failure");
        outboxService.recordEventFailure(eventId, "Third failure");

        // Then
        Optional<OutboxEvent> updatedEvent = outboxRepository.findById(eventId);
        assertThat(updatedEvent).isPresent();
        assertThat(updatedEvent.get().getRetryCount()).isEqualTo(3);
        assertThat(updatedEvent.get().getLastError()).isEqualTo("Third failure");
    }

    private PaymentProcessedEvent createTestEvent() {
        return new PaymentProcessedEvent(
            PaymentId.of("payment-123"),
            "order-456",
            "tx-123",
            new BigDecimal("100.00"),
            "USD"
        );
    }

    private OutboxEvent createOutboxEvent() {
        PaymentProcessedEvent event = createTestEvent();
        return new OutboxEvent(
            "PaymentProcessedEvent",
            "payment-" + UUID.randomUUID().toString(),
            "Payment",
            "{\"paymentId\":\"payment-123\"}",
            "{\"eventType\":\"PaymentProcessedEvent\"}"
        );
    }

    private void saveTestEvents(int count, boolean processed) {
        for (int i = 0; i < count; i++) {
            OutboxEvent event = createOutboxEvent();
            if (processed) {
                event.markAsProcessed();
            }
            outboxRepository.save(event);
        }
    }
}
package com.example.logging.service.impl;

import com.example.logging.model.MessageEventLog;
import com.example.logging.repository.MessageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 消息日誌服務實現測試
 */
@ExtendWith(MockitoExtension.class)
class MessageLogServiceImplTest {

    @Mock
    private MessageLogRepository repository;

    private MessageLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MessageLogServiceImpl(repository);
    }

    @Test
    void shouldSaveMessageLog() {
        // Given
        MessageEventLog messageLog = MessageEventLog.builder()
                .messageId("test-message-id")
                .eventType("PAYMENT_PROCESSED")
                .className("PaymentHandler")
                .methodName("handlePayment")
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        service.saveMessageLog(messageLog);

        // Then
        verify(repository).save(messageLog);
    }

    @Test
    void shouldFindByEventType() {
        // Given
        String eventType = "ORDER_CREATED";
        List<MessageEventLog> expectedLogs = Arrays.asList(
                createTestLog("1", eventType),
                createTestLog("2", eventType)
        );
        when(repository.findByEventType(eventType)).thenReturn(expectedLogs);

        // When
        List<MessageEventLog> result = service.findByEventType(eventType);

        // Then
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findByEventType(eventType);
    }

    @Test
    void shouldFindByTimeRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now();
        List<MessageEventLog> expectedLogs = Arrays.asList(
                createTestLog("1", "EVENT1"),
                createTestLog("2", "EVENT2")
        );
        when(repository.findByTimeRange(start, end)).thenReturn(expectedLogs);

        // When
        List<MessageEventLog> result = service.findByTimeRange(start, end);

        // Then
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findByTimeRange(start, end);
    }

    @Test
    void shouldFindByStatus() {
        // Given
        String status = "ERROR";
        List<MessageEventLog> expectedLogs = Arrays.asList(
                createTestLog("1", "EVENT1"),
                createTestLog("2", "EVENT2")
        );
        when(repository.findByStatus(status)).thenReturn(expectedLogs);

        // When
        List<MessageEventLog> result = service.findByStatus(status);

        // Then
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findByStatus(status);
    }

    @Test
    void shouldFindSlowMessages() {
        // Given
        Long threshold = 3000L;
        List<MessageEventLog> expectedLogs = Arrays.asList(
                createTestLog("1", "SLOW_EVENT1"),
                createTestLog("2", "SLOW_EVENT2")
        );
        when(repository.findSlowMessages(threshold)).thenReturn(expectedLogs);

        // When
        List<MessageEventLog> result = service.findSlowMessages(threshold);

        // Then
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findSlowMessages(threshold);
    }

    @Test
    void shouldFindByMessageId() {
        // Given
        String messageId = "test-message-id";
        MessageEventLog expectedLog = createTestLog(messageId, "TEST_EVENT");
        when(repository.findById(messageId)).thenReturn(java.util.Optional.of(expectedLog));

        // When
        MessageEventLog result = service.findByMessageId(messageId);

        // Then
        assertThat(result).isEqualTo(expectedLog);
        verify(repository).findById(messageId);
    }

    private MessageEventLog createTestLog(String messageId, String eventType) {
        return MessageEventLog.builder()
                .messageId(messageId)
                .eventType(eventType)
                .className("TestHandler")
                .methodName("handleEvent")
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
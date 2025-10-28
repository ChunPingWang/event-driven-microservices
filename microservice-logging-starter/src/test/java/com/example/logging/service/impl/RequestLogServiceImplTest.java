package com.example.logging.service.impl;

import com.example.logging.model.ApiRequestLog;
import com.example.logging.repository.RequestLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 請求日誌服務實現測試
 */
@ExtendWith(MockitoExtension.class)
class RequestLogServiceImplTest {

    @Mock
    private RequestLogRepository repository;

    private RequestLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RequestLogServiceImpl(repository);
    }

    @Test
    void shouldSaveRequestLog() {
        // Given
        ApiRequestLog requestLog = ApiRequestLog.builder()
                .requestId("test-request-id")
                .operation("TEST_OPERATION")
                .className("TestClass")
                .methodName("testMethod")
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // When
        service.saveRequestLog(requestLog);

        // Then
        verify(repository).save(requestLog);
    }

    @Test
    void shouldFindByOperation() {
        // Given
        String operation = "CREATE_ORDER";
        List<ApiRequestLog> expectedLogs = Arrays.asList(
                createTestLog("1", operation),
                createTestLog("2", operation)
        );
        when(repository.findByOperation(operation)).thenReturn(expectedLogs);

        // When
        List<ApiRequestLog> result = service.findByOperation(operation);

        // Then
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findByOperation(operation);
    }

    @Test
    void shouldFindByTimeRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now();
        List<ApiRequestLog> expectedLogs = Arrays.asList(
                createTestLog("1", "OP1"),
                createTestLog("2", "OP2")
        );
        when(repository.findByTimeRange(start, end)).thenReturn(expectedLogs);

        // When
        List<ApiRequestLog> result = service.findByTimeRange(start, end);

        // Then
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findByTimeRange(start, end);
    }

    @Test
    void shouldFindByStatus() {
        // Given
        String status = "ERROR";
        List<ApiRequestLog> expectedLogs = Arrays.asList(
                createTestLog("1", "OP1"),
                createTestLog("2", "OP2")
        );
        when(repository.findByStatus(status)).thenReturn(expectedLogs);

        // When
        List<ApiRequestLog> result = service.findByStatus(status);

        // Then
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findByStatus(status);
    }

    @Test
    void shouldFindSlowRequests() {
        // Given
        Long threshold = 5000L;
        List<ApiRequestLog> expectedLogs = Arrays.asList(
                createTestLog("1", "SLOW_OP1"),
                createTestLog("2", "SLOW_OP2")
        );
        when(repository.findSlowRequests(threshold)).thenReturn(expectedLogs);

        // When
        List<ApiRequestLog> result = service.findSlowRequests(threshold);

        // Then
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findSlowRequests(threshold);
    }

    @Test
    void shouldFindByRequestId() {
        // Given
        String requestId = "test-request-id";
        ApiRequestLog expectedLog = createTestLog(requestId, "TEST_OP");
        when(repository.findById(requestId)).thenReturn(java.util.Optional.of(expectedLog));

        // When
        ApiRequestLog result = service.findByRequestId(requestId);

        // Then
        assertThat(result).isEqualTo(expectedLog);
        verify(repository).findById(requestId);
    }

    private ApiRequestLog createTestLog(String requestId, String operation) {
        return ApiRequestLog.builder()
                .requestId(requestId)
                .operation(operation)
                .className("TestClass")
                .methodName("testMethod")
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
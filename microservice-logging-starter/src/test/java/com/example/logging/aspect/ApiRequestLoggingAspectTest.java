package com.example.logging.aspect;

import com.example.logging.annotation.LogApiRequest;
import com.example.logging.model.ApiRequestLog;
import com.example.logging.service.RequestLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * API 請求日誌記錄切面測試
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiRequestLoggingAspectTest {

    @Mock
    private RequestLogService requestLogService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private LogApiRequest logApiRequest;

    private ApiRequestLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new ApiRequestLoggingAspect(requestLogService, objectMapper);
    }

    @Test
    void shouldLogSuccessfulApiRequest() throws Throwable {
        // Given
        String methodName = "testMethod";
        String className = "TestClass";
        Object[] args = {"arg1", "arg2"};
        String expectedResult = "success";
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(expectedResult);
        
        when(logApiRequest.operation()).thenReturn("");
        when(logApiRequest.logRequest()).thenReturn(true);
        when(logApiRequest.logResponse()).thenReturn(true);
        when(logApiRequest.logExecutionTime()).thenReturn(true);
        when(logApiRequest.maxPayloadLength()).thenReturn(10000);
        
        when(objectMapper.writeValueAsString(args)).thenReturn("[\"arg1\",\"arg2\"]");
        when(objectMapper.writeValueAsString(expectedResult)).thenReturn("\"success\"");

        // When
        Object result = aspect.logApiRequest(joinPoint, logApiRequest);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        
        ArgumentCaptor<ApiRequestLog> logCaptor = ArgumentCaptor.forClass(ApiRequestLog.class);
        verify(requestLogService).saveRequestLog(logCaptor.capture());
        
        ApiRequestLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getOperation()).isEqualTo(methodName);
        assertThat(capturedLog.getClassName()).isEqualTo(className);
        assertThat(capturedLog.getMethodName()).isEqualTo(methodName);
        assertThat(capturedLog.getStatus()).isEqualTo("SUCCESS");
        assertThat(capturedLog.getRequestPayload()).isEqualTo("[\"arg1\",\"arg2\"]");
        assertThat(capturedLog.getResponsePayload()).isEqualTo("\"success\"");
        assertThat(capturedLog.getExecutionTimeMs()).isNotNull();
        assertThat(capturedLog.getRequestId()).isNotNull();
        assertThat(capturedLog.getTimestamp()).isNotNull();
    }

    @Test
    void shouldLogFailedApiRequest() throws Throwable {
        // Given
        String methodName = "testMethod";
        RuntimeException exception = new RuntimeException("Test error");
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenThrow(exception);
        
        when(logApiRequest.operation()).thenReturn("");
        when(logApiRequest.logRequest()).thenReturn(true);
        when(logApiRequest.logExecutionTime()).thenReturn(true);
        when(logApiRequest.maxPayloadLength()).thenReturn(10000);
        
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // When & Then
        assertThatThrownBy(() -> aspect.logApiRequest(joinPoint, logApiRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Test error");
        
        ArgumentCaptor<ApiRequestLog> logCaptor = ArgumentCaptor.forClass(ApiRequestLog.class);
        verify(requestLogService).saveRequestLog(logCaptor.capture());
        
        ApiRequestLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getStatus()).isEqualTo("ERROR");
        assertThat(capturedLog.getErrorMessage()).isEqualTo("Test error");
        assertThat(capturedLog.getExecutionTimeMs()).isNotNull();
    }

    @Test
    void shouldUseCustomOperationName() throws Throwable {
        // Given
        String customOperation = "CUSTOM_OPERATION";
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");
        
        when(logApiRequest.operation()).thenReturn(customOperation);
        when(logApiRequest.logRequest()).thenReturn(false);
        when(logApiRequest.logResponse()).thenReturn(false);
        when(logApiRequest.logExecutionTime()).thenReturn(false);

        // When
        aspect.logApiRequest(joinPoint, logApiRequest);

        // Then
        ArgumentCaptor<ApiRequestLog> logCaptor = ArgumentCaptor.forClass(ApiRequestLog.class);
        verify(requestLogService).saveRequestLog(logCaptor.capture());
        
        ApiRequestLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getOperation()).isEqualTo(customOperation);
        assertThat(capturedLog.getRequestPayload()).isNull();
        assertThat(capturedLog.getResponsePayload()).isNull();
        assertThat(capturedLog.getExecutionTimeMs()).isNull();
    }

    @Test
    void shouldHandleSerializationError() throws Throwable {
        // Given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg"});
        when(joinPoint.proceed()).thenReturn("result");
        
        when(logApiRequest.operation()).thenReturn("");
        when(logApiRequest.logRequest()).thenReturn(true);
        when(logApiRequest.logResponse()).thenReturn(true);
        when(logApiRequest.logExecutionTime()).thenReturn(true);
        when(logApiRequest.maxPayloadLength()).thenReturn(10000);
        
        when(objectMapper.writeValueAsString(any()))
            .thenThrow(new RuntimeException("Serialization error"));

        // When
        aspect.logApiRequest(joinPoint, logApiRequest);

        // Then
        ArgumentCaptor<ApiRequestLog> logCaptor = ArgumentCaptor.forClass(ApiRequestLog.class);
        verify(requestLogService).saveRequestLog(logCaptor.capture());
        
        ApiRequestLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getRequestPayload()).contains("Failed to serialize");
        assertThat(capturedLog.getResponsePayload()).contains("Failed to serialize");
    }

    @Test
    void shouldTruncateLongPayload() throws Throwable {
        // Given
        String longPayload = "x".repeat(15000);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg"});
        when(joinPoint.proceed()).thenReturn("result");
        
        when(logApiRequest.operation()).thenReturn("");
        when(logApiRequest.logRequest()).thenReturn(true);
        when(logApiRequest.logResponse()).thenReturn(false);
        when(logApiRequest.logExecutionTime()).thenReturn(false);
        when(logApiRequest.maxPayloadLength()).thenReturn(1000);
        
        when(objectMapper.writeValueAsString(any())).thenReturn(longPayload);

        // When
        aspect.logApiRequest(joinPoint, logApiRequest);

        // Then
        ArgumentCaptor<ApiRequestLog> logCaptor = ArgumentCaptor.forClass(ApiRequestLog.class);
        verify(requestLogService).saveRequestLog(logCaptor.capture());
        
        ApiRequestLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getRequestPayload()).contains("... [truncated]");
        assertThat(capturedLog.getRequestPayload().length()).isLessThanOrEqualTo(1020); // Allow some flexibility
    }

    private static class TestClass {
        // Test helper class
    }
}
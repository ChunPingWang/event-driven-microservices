package com.example.logging.aspect;

import com.example.logging.annotation.LogMessageEvent;
import com.example.logging.model.MessageEventLog;
import com.example.logging.service.MessageLogService;
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
 * 消息事件日誌記錄切面測試
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageEventLoggingAspectTest {

    @Mock
    private MessageLogService messageLogService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private LogMessageEvent logMessageEvent;

    private MessageEventLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new MessageEventLoggingAspect(messageLogService, objectMapper);
    }

    @Test
    void shouldLogSuccessfulMessageEvent() throws Throwable {
        // Given
        String methodName = "handleMessage";
        String className = "MessageHandler";
        Object[] args = {"message1", "message2"};
        String expectedResult = "processed";
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new MessageHandler());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(expectedResult);
        
        when(logMessageEvent.eventType()).thenReturn("");
        when(logMessageEvent.logPayload()).thenReturn(true);
        when(logMessageEvent.logHeaders()).thenReturn(false);
        when(logMessageEvent.logProcessingTime()).thenReturn(true);
        when(logMessageEvent.maxPayloadLength()).thenReturn(10000);
        
        when(objectMapper.writeValueAsString(args)).thenReturn("[\"message1\",\"message2\"]");

        // When
        Object result = aspect.logMessageEvent(joinPoint, logMessageEvent);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        
        ArgumentCaptor<MessageEventLog> logCaptor = ArgumentCaptor.forClass(MessageEventLog.class);
        verify(messageLogService).saveMessageLog(logCaptor.capture());
        
        MessageEventLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getEventType()).isEqualTo(methodName);
        assertThat(capturedLog.getClassName()).isEqualTo(className);
        assertThat(capturedLog.getMethodName()).isEqualTo(methodName);
        assertThat(capturedLog.getStatus()).isEqualTo("SUCCESS");
        assertThat(capturedLog.getPayload()).isEqualTo("[\"message1\",\"message2\"]");
        assertThat(capturedLog.getProcessingTimeMs()).isNotNull();
        assertThat(capturedLog.getMessageId()).isNotNull();
        assertThat(capturedLog.getTimestamp()).isNotNull();
    }

    @Test
    void shouldLogFailedMessageEvent() throws Throwable {
        // Given
        String methodName = "handleMessage";
        RuntimeException exception = new RuntimeException("Processing failed");
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new MessageHandler());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenThrow(exception);
        
        when(logMessageEvent.eventType()).thenReturn("");
        when(logMessageEvent.logPayload()).thenReturn(true);
        when(logMessageEvent.logProcessingTime()).thenReturn(true);
        when(logMessageEvent.maxPayloadLength()).thenReturn(10000);
        
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // When & Then
        assertThatThrownBy(() -> aspect.logMessageEvent(joinPoint, logMessageEvent))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Processing failed");
        
        ArgumentCaptor<MessageEventLog> logCaptor = ArgumentCaptor.forClass(MessageEventLog.class);
        verify(messageLogService).saveMessageLog(logCaptor.capture());
        
        MessageEventLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getStatus()).isEqualTo("ERROR");
        assertThat(capturedLog.getErrorMessage()).isEqualTo("Processing failed");
        assertThat(capturedLog.getProcessingTimeMs()).isNotNull();
    }

    @Test
    void shouldUseCustomEventType() throws Throwable {
        // Given
        String customEventType = "PAYMENT_PROCESSED";
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("handlePayment");
        when(joinPoint.getTarget()).thenReturn(new MessageHandler());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("result");
        
        when(logMessageEvent.eventType()).thenReturn(customEventType);
        when(logMessageEvent.logPayload()).thenReturn(false);
        when(logMessageEvent.logHeaders()).thenReturn(false);
        when(logMessageEvent.logProcessingTime()).thenReturn(false);

        // When
        aspect.logMessageEvent(joinPoint, logMessageEvent);

        // Then
        ArgumentCaptor<MessageEventLog> logCaptor = ArgumentCaptor.forClass(MessageEventLog.class);
        verify(messageLogService).saveMessageLog(logCaptor.capture());
        
        MessageEventLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getEventType()).isEqualTo(customEventType);
        assertThat(capturedLog.getPayload()).isNull();
        assertThat(capturedLog.getHeaders()).isNull();
        assertThat(capturedLog.getProcessingTimeMs()).isNull();
    }

    @Test
    void shouldLogHeaders() throws Throwable {
        // Given
        MessageWithHeaders messageWithHeaders = new MessageWithHeaders();
        Object[] args = {messageWithHeaders, "otherArg"};
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("handleMessage");
        when(joinPoint.getTarget()).thenReturn(new MessageHandler());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("result");
        
        when(logMessageEvent.eventType()).thenReturn("");
        when(logMessageEvent.logPayload()).thenReturn(false);
        when(logMessageEvent.logHeaders()).thenReturn(true);
        when(logMessageEvent.logProcessingTime()).thenReturn(false);
        
        when(objectMapper.writeValueAsString(messageWithHeaders)).thenReturn("{\"headers\":{\"key\":\"value\"}}");

        // When
        aspect.logMessageEvent(joinPoint, logMessageEvent);

        // Then
        ArgumentCaptor<MessageEventLog> logCaptor = ArgumentCaptor.forClass(MessageEventLog.class);
        verify(messageLogService).saveMessageLog(logCaptor.capture());
        
        MessageEventLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getHeaders()).isEqualTo("{\"headers\":{\"key\":\"value\"}}");
    }

    @Test
    void shouldHandleHeadersNotFound() throws Throwable {
        // Given
        Object[] args = {"simpleString", 123};
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("handleMessage");
        when(joinPoint.getTarget()).thenReturn(new MessageHandler());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("result");
        
        when(logMessageEvent.eventType()).thenReturn("");
        when(logMessageEvent.logPayload()).thenReturn(false);
        when(logMessageEvent.logHeaders()).thenReturn(true);
        when(logMessageEvent.logProcessingTime()).thenReturn(false);

        // When
        aspect.logMessageEvent(joinPoint, logMessageEvent);

        // Then
        ArgumentCaptor<MessageEventLog> logCaptor = ArgumentCaptor.forClass(MessageEventLog.class);
        verify(messageLogService).saveMessageLog(logCaptor.capture());
        
        MessageEventLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getHeaders()).isEqualTo("No headers found");
    }

    @Test
    void shouldHandleSerializationError() throws Throwable {
        // Given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("handleMessage");
        when(joinPoint.getTarget()).thenReturn(new MessageHandler());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"message"});
        when(joinPoint.proceed()).thenReturn("result");
        
        when(logMessageEvent.eventType()).thenReturn("");
        when(logMessageEvent.logPayload()).thenReturn(true);
        when(logMessageEvent.logHeaders()).thenReturn(false);
        when(logMessageEvent.logProcessingTime()).thenReturn(true);
        when(logMessageEvent.maxPayloadLength()).thenReturn(10000);
        
        when(objectMapper.writeValueAsString(any()))
            .thenThrow(new RuntimeException("Serialization error"));

        // When
        aspect.logMessageEvent(joinPoint, logMessageEvent);

        // Then
        ArgumentCaptor<MessageEventLog> logCaptor = ArgumentCaptor.forClass(MessageEventLog.class);
        verify(messageLogService).saveMessageLog(logCaptor.capture());
        
        MessageEventLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getPayload()).contains("Failed to serialize");
    }

    @Test
    void shouldTruncateLongPayload() throws Throwable {
        // Given
        String longPayload = "x".repeat(15000);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("handleMessage");
        when(joinPoint.getTarget()).thenReturn(new MessageHandler());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"message"});
        when(joinPoint.proceed()).thenReturn("result");
        
        when(logMessageEvent.eventType()).thenReturn("");
        when(logMessageEvent.logPayload()).thenReturn(true);
        when(logMessageEvent.logHeaders()).thenReturn(false);
        when(logMessageEvent.logProcessingTime()).thenReturn(false);
        when(logMessageEvent.maxPayloadLength()).thenReturn(1000);
        
        when(objectMapper.writeValueAsString(any())).thenReturn(longPayload);

        // When
        aspect.logMessageEvent(joinPoint, logMessageEvent);

        // Then
        ArgumentCaptor<MessageEventLog> logCaptor = ArgumentCaptor.forClass(MessageEventLog.class);
        verify(messageLogService).saveMessageLog(logCaptor.capture());
        
        MessageEventLog capturedLog = logCaptor.getValue();
        assertThat(capturedLog.getPayload()).contains("... [truncated]");
        assertThat(capturedLog.getPayload().length()).isLessThanOrEqualTo(1020); // Allow some flexibility
    }

    private static class MessageHandler {
        // Test helper class
    }

    private static class MessageWithHeaders {
        // Test helper class that contains "header" in class name
    }
}
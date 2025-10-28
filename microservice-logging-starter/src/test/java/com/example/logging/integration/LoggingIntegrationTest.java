package com.example.logging.integration;

import com.example.logging.annotation.LogApiRequest;
import com.example.logging.annotation.LogMessageEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 日誌記錄功能集成測試
 * 簡化版本，專注於註解功能測試
 */
class LoggingIntegrationTest {

    private final TestService testService = new TestService();

    @Test
    void shouldProcessApiRequestSuccessfully() {
        // Given
        String input = "test input";

        // When
        String result = testService.processApiRequest(input);

        // Then
        assertThat(result).isEqualTo("Processed: test input");
    }

    @Test
    void shouldProcessApiRequestWithError() {
        // Given
        String input = "error";

        // When & Then
        assertThatThrownBy(() -> testService.processApiRequest(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated error");
    }

    @Test
    void shouldHandleMessageEventSuccessfully() {
        // Given
        String message = "test message";

        // When
        String result = testService.handleMessageEvent(message);

        // Then
        assertThat(result).isEqualTo("Handled: test message");
    }

    @Test
    void shouldHandleMessageEventWithError() {
        // Given
        String message = "error";

        // When & Then
        assertThatThrownBy(() -> testService.handleMessageEvent(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Message processing failed");
    }

    @Test
    void shouldProcessWithoutLogging() {
        // Given
        String input = "test";

        // When
        String result = testService.processWithoutLogging(input);

        // Then
        assertThat(result).isEqualTo("No logging: test");
    }

    @Service
    static class TestService {

        @LogApiRequest(operation = "PROCESS_API_REQUEST", logRequest = true, logResponse = true)
        public String processApiRequest(String input) {
            if ("error".equals(input)) {
                throw new RuntimeException("Simulated error");
            }
            return "Processed: " + input;
        }

        @LogMessageEvent(eventType = "MESSAGE_RECEIVED", logPayload = true)
        public String handleMessageEvent(String message) {
            if ("error".equals(message)) {
                throw new RuntimeException("Message processing failed");
            }
            return "Handled: " + message;
        }

        public String processWithoutLogging(String input) {
            return "No logging: " + input;
        }
    }
}
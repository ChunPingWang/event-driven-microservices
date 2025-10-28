package com.example.logging.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 微服務日誌記錄配置屬性測試
 */
@SpringBootTest(classes = MicroserviceLoggingPropertiesTest.TestConfiguration.class)
@TestPropertySource(properties = {
        "microservice.logging.enabled=false",
        "microservice.logging.log-request-payload=false",
        "microservice.logging.log-response-payload=false",
        "microservice.logging.log-message-payload=false",
        "microservice.logging.log-message-headers=true",
        "microservice.logging.max-payload-length=5000",
        "microservice.logging.exclude-patterns=com.example.test,com.example.ignore",
        "microservice.logging.log-execution-time=false",
        "microservice.logging.log-processing-time=false",
        "microservice.logging.slow-request-threshold=3000",
        "microservice.logging.slow-message-threshold=2000",
        "microservice.logging.async-logging=true",
        "microservice.logging.async-thread-pool-size=10"
})
class MicroserviceLoggingPropertiesTest {

    @Autowired
    private MicroserviceLoggingProperties properties;

    @Test
    void shouldBindPropertiesCorrectly() {
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isLogRequestPayload()).isFalse();
        assertThat(properties.isLogResponsePayload()).isFalse();
        assertThat(properties.isLogMessagePayload()).isFalse();
        assertThat(properties.isLogMessageHeaders()).isTrue();
        assertThat(properties.getMaxPayloadLength()).isEqualTo(5000);
        assertThat(properties.getExcludePatterns()).containsExactly("com.example.test", "com.example.ignore");
        assertThat(properties.isLogExecutionTime()).isFalse();
        assertThat(properties.isLogProcessingTime()).isFalse();
        assertThat(properties.getSlowRequestThreshold()).isEqualTo(3000);
        assertThat(properties.getSlowMessageThreshold()).isEqualTo(2000);
        assertThat(properties.isAsyncLogging()).isTrue();
        assertThat(properties.getAsyncThreadPoolSize()).isEqualTo(10);
    }

    @Test
    void shouldHaveDefaultValues() {
        MicroserviceLoggingProperties defaultProperties = new MicroserviceLoggingProperties();
        
        assertThat(defaultProperties.isEnabled()).isTrue();
        assertThat(defaultProperties.isLogRequestPayload()).isTrue();
        assertThat(defaultProperties.isLogResponsePayload()).isTrue();
        assertThat(defaultProperties.isLogMessagePayload()).isTrue();
        assertThat(defaultProperties.isLogMessageHeaders()).isFalse();
        assertThat(defaultProperties.getMaxPayloadLength()).isEqualTo(10000);
        assertThat(defaultProperties.getExcludePatterns()).isEmpty();
        assertThat(defaultProperties.isLogExecutionTime()).isTrue();
        assertThat(defaultProperties.isLogProcessingTime()).isTrue();
        assertThat(defaultProperties.getSlowRequestThreshold()).isEqualTo(5000);
        assertThat(defaultProperties.getSlowMessageThreshold()).isEqualTo(3000);
        assertThat(defaultProperties.isAsyncLogging()).isFalse();
        assertThat(defaultProperties.getAsyncThreadPoolSize()).isEqualTo(5);
    }

    @EnableConfigurationProperties(MicroserviceLoggingProperties.class)
    static class TestConfiguration {
    }
}
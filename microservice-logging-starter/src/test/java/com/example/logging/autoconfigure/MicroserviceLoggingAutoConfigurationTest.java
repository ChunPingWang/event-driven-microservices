package com.example.logging.autoconfigure;

import com.example.logging.config.MicroserviceLoggingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 微服務日誌記錄自動配置測試
 * 簡化版本，專注於核心配置功能測試
 */
@SpringBootTest(classes = MicroserviceLoggingAutoConfigurationTest.TestConfiguration.class)
@TestPropertySource(properties = {
        "microservice.logging.enabled=true",
        "microservice.logging.log-request-payload=false",
        "microservice.logging.max-payload-length=5000"
})
class MicroserviceLoggingAutoConfigurationTest {

    @Autowired
    private MicroserviceLoggingProperties properties;

    @Test
    void shouldLoadPropertiesCorrectly() {
        assertThat(properties).isNotNull();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isLogRequestPayload()).isFalse();
        assertThat(properties.getMaxPayloadLength()).isEqualTo(5000);
    }

    @Test
    void shouldHaveDefaultValues() {
        MicroserviceLoggingProperties defaultProperties = new MicroserviceLoggingProperties();
        
        assertThat(defaultProperties.isEnabled()).isTrue();
        assertThat(defaultProperties.isLogRequestPayload()).isTrue();
        assertThat(defaultProperties.isLogResponsePayload()).isTrue();
        assertThat(defaultProperties.getMaxPayloadLength()).isEqualTo(10000);
        assertThat(defaultProperties.getSlowRequestThreshold()).isEqualTo(5000);
    }

    @EnableConfigurationProperties(MicroserviceLoggingProperties.class)
    static class TestConfiguration {
    }
}
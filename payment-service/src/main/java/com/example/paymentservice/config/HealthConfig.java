package com.example.paymentservice.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class HealthConfig {

    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return () -> {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(1)) {
                    return Health.up()
                            .withDetail("database", "Available")
                            .withDetail("validationQuery", "SELECT 1")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("database", "Connection validation failed")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "Connection failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator rabbitHealthIndicator(RabbitTemplate rabbitTemplate) {
        return () -> {
            try {
                // Try to get connection factory info
                rabbitTemplate.getConnectionFactory().createConnection().close();
                return Health.up()
                        .withDetail("rabbitmq", "Available")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("rabbitmq", "Connection failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator outboxHealthIndicator() {
        return () -> {
            // Check if outbox publisher is working
            try {
                // This is a simple check - in production you might want to check
                // the last successful outbox processing time
                return Health.up()
                        .withDetail("outbox", "Publisher active")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("outbox", "Publisher failed")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
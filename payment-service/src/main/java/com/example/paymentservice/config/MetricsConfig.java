package com.example.paymentservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter paymentProcessingCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_processing_total")
                .description("Total number of payment processing attempts")
                .tag("service", "payment-service")
                .register(meterRegistry);
    }

    @Bean
    public Counter paymentProcessingFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_processing_failures_total")
                .description("Total number of payment processing failures")
                .tag("service", "payment-service")
                .register(meterRegistry);
    }

    @Bean
    public Counter paymentSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_success_total")
                .description("Total number of successful payments")
                .tag("service", "payment-service")
                .register(meterRegistry);
    }

    @Bean
    public Counter creditCardValidationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("credit_card_validation_total")
                .description("Total number of credit card validations")
                .tag("service", "payment-service")
                .register(meterRegistry);
    }

    @Bean
    public Counter outboxEventCounter(MeterRegistry meterRegistry) {
        return Counter.builder("outbox_events_published_total")
                .description("Total number of outbox events published")
                .tag("service", "payment-service")
                .register(meterRegistry);
    }

    @Bean
    public Timer paymentProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("payment_processing_duration_seconds")
                .description("Time taken to process payments")
                .tag("service", "payment-service")
                .register(meterRegistry);
    }

    @Bean
    public Timer creditCardValidationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("credit_card_validation_duration_seconds")
                .description("Time taken to validate credit cards")
                .tag("service", "payment-service")
                .register(meterRegistry);
    }
}
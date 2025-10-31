package com.example.orderservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter orderCreationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("order_creation_total")
                .description("Total number of order creation attempts")
                .tag("service", "order-service")
                .register(meterRegistry);
    }

    @Bean
    public Counter orderCreationFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("order_creation_failures_total")
                .description("Total number of order creation failures")
                .tag("service", "order-service")
                .register(meterRegistry);
    }

    @Bean
    public Counter paymentRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_request_sent_total")
                .description("Total number of payment requests sent")
                .tag("service", "order-service")
                .register(meterRegistry);
    }

    @Bean
    public Counter paymentConfirmationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_confirmation_received_total")
                .description("Total number of payment confirmations received")
                .tag("service", "order-service")
                .register(meterRegistry);
    }

    @Bean
    public Counter retryAttemptCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_retry_attempts_total")
                .description("Total number of payment retry attempts")
                .tag("service", "order-service")
                .register(meterRegistry);
    }

    @Bean
    public Timer orderProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("order_processing_duration_seconds")
                .description("Time taken to process orders")
                .tag("service", "order-service")
                .register(meterRegistry);
    }
}
package com.example.orderservice.messaging;

import com.example.logging.annotation.LogMessageEvent;
import com.example.orderservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @LogMessageEvent(eventType = "PAYMENT_REQUEST_SENT", logPayload = true)
    public void publishPaymentRequest(PaymentRequest request) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_REQUEST_ROUTING_KEY,
                request
            );
            log.info("Payment request sent for order: {}, transaction: {}", 
                request.getOrderId(), request.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to send payment request for order: {}, transaction: {}", 
                request.getOrderId(), request.getTransactionId(), e);
            throw new RuntimeException("Failed to publish payment request", e);
        }
    }

    @LogMessageEvent(eventType = "RETRY_SCHEDULED", logPayload = true)
    public void scheduleRetry(String orderId, int attemptNumber) {
        log.info("Scheduling retry for order: {}, attempt: {}", orderId, attemptNumber);
        // Implementation will be added in later tasks
    }
}
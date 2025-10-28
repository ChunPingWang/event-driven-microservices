package com.example.paymentservice.messaging;

import com.example.logging.annotation.LogMessageEvent;
import com.example.paymentservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @LogMessageEvent(eventType = "PAYMENT_CONFIRMATION_SENT", logPayload = true)
    public void publishPaymentConfirmation(PaymentConfirmation confirmation) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.PAYMENT_CONFIRMATION_ROUTING_KEY,
                confirmation
            );
            log.info("Payment confirmation sent for order: {}, status: {}", 
                confirmation.getOrderId(), confirmation.getStatus());
        } catch (Exception e) {
            log.error("Failed to send payment confirmation for order: {}", 
                confirmation.getOrderId(), e);
            throw new RuntimeException("Failed to publish payment confirmation", e);
        }
    }
}
package com.example.orderservice.messaging;

import com.example.logging.annotation.LogMessageEvent;
import com.example.orderservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConfirmationListener {

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_CONFIRMATION_QUEUE)
    @LogMessageEvent(eventType = "PAYMENT_CONFIRMATION_RECEIVED", logPayload = true)
    public void handlePaymentConfirmation(PaymentConfirmation confirmation) {
        try {
            log.info("Received payment confirmation for order: {}, status: {}", 
                confirmation.getOrderId(), confirmation.getStatus());
            
            // Implementation will be added in later tasks
            // This will call the order service to update order status
            
        } catch (Exception e) {
            log.error("Failed to process payment confirmation for order: {}", 
                confirmation.getOrderId(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}
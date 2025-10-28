package com.example.paymentservice.messaging;

import com.example.logging.annotation.LogMessageEvent;
import com.example.paymentservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestListener {

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REQUEST_QUEUE)
    @LogMessageEvent(eventType = "PAYMENT_REQUEST_RECEIVED", logPayload = true)
    public void handlePaymentRequest(PaymentRequest request) {
        try {
            log.info("Received payment request for order: {}, transaction: {}", 
                request.getOrderId(), request.getTransactionId());
            
            // Implementation will be added in later tasks
            // This will call the payment service to process the payment
            
        } catch (Exception e) {
            log.error("Failed to process payment request for order: {}, transaction: {}", 
                request.getOrderId(), request.getTransactionId(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}
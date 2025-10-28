package com.example.paymentservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    // Queue names
    public static final String PAYMENT_REQUEST_QUEUE = "payment.request.queue";
    public static final String PAYMENT_CONFIRMATION_QUEUE = "payment.confirmation.queue";
    public static final String PAYMENT_REQUEST_DLQ = "payment.request.dlq";
    public static final String PAYMENT_CONFIRMATION_DLQ = "payment.confirmation.dlq";
    
    // Routing keys
    public static final String PAYMENT_REQUEST_ROUTING_KEY = "payment.request";
    public static final String PAYMENT_CONFIRMATION_ROUTING_KEY = "payment.confirmation";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        return factory;
    }

    // Exchanges
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    // Dead Letter Exchange
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("payment.dlx", true, false);
    }

    // Payment Request Queue (Order Service sends to Payment Service)
    @Bean
    public Queue paymentRequestQueue() {
        return QueueBuilder.durable(PAYMENT_REQUEST_QUEUE)
                .withArgument("x-dead-letter-exchange", "payment.dlx")
                .withArgument("x-dead-letter-routing-key", "payment.request.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .withArgument("x-max-length", 10000)
                .build();
    }

    // Payment Confirmation Queue (Payment Service sends to Order Service)
    @Bean
    public Queue paymentConfirmationQueue() {
        return QueueBuilder.durable(PAYMENT_CONFIRMATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "payment.dlx")
                .withArgument("x-dead-letter-routing-key", "payment.confirmation.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    // Dead Letter Queues
    @Bean
    public Queue paymentRequestDLQ() {
        return QueueBuilder.durable(PAYMENT_REQUEST_DLQ).build();
    }

    @Bean
    public Queue paymentConfirmationDLQ() {
        return QueueBuilder.durable(PAYMENT_CONFIRMATION_DLQ).build();
    }

    // Bindings
    @Bean
    public Binding paymentRequestBinding() {
        return BindingBuilder
                .bind(paymentRequestQueue())
                .to(paymentExchange())
                .with(PAYMENT_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding paymentConfirmationBinding() {
        return BindingBuilder
                .bind(paymentConfirmationQueue())
                .to(orderExchange())
                .with(PAYMENT_CONFIRMATION_ROUTING_KEY);
    }

    // Dead Letter Bindings
    @Bean
    public Binding paymentRequestDLQBinding() {
        return BindingBuilder
                .bind(paymentRequestDLQ())
                .to(deadLetterExchange())
                .with("payment.request.failed");
    }

    @Bean
    public Binding paymentConfirmationDLQBinding() {
        return BindingBuilder
                .bind(paymentConfirmationDLQ())
                .to(deadLetterExchange())
                .with("payment.confirmation.failed");
    }
}
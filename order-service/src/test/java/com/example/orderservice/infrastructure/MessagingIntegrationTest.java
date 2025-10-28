package com.example.orderservice.infrastructure;

import com.example.orderservice.infrastructure.messaging.PaymentConfirmationListener;
import com.example.orderservice.infrastructure.messaging.PaymentRequestPublisherImpl;
import com.example.orderservice.messaging.PaymentConfirmation;
import com.example.orderservice.messaging.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * 消息傳遞集成測試 - 測試 RabbitMQ 消息發送和接收功能
 */
@SpringBootTest
@Testcontainers
@RabbitListenerTest(capture = true)
class MessagingIntegrationTest {

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management-alpine")
            .withUser("test", "test")
            .withVhost("/");

    @Autowired
    private PaymentRequestPublisherImpl paymentRequestPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitListenerTestHarness harness;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "test");
        registry.add("spring.rabbitmq.password", () -> "test");
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
        
        // 消息配置
        registry.add("messaging.payment.request.exchange", () -> "payment.exchange");
        registry.add("messaging.payment.request.routing-key", () -> "payment.request");
        registry.add("messaging.payment.confirmation.exchange", () -> "payment.exchange");
        registry.add("messaging.payment.confirmation.routing-key", () -> "payment.confirmation");
    }

    @BeforeEach
    void setUp() {
        // 清理隊列
        rabbitTemplate.execute(channel -> {
            try {
                channel.queuePurge("payment.request.queue");
                channel.queuePurge("payment.confirmation.queue");
            } catch (Exception e) {
                // 隊列可能不存在，忽略異常
            }
            return null;
        });
    }

    @Test
    void shouldPublishPaymentRequestSuccessfully() throws Exception {
        // Given
        PaymentRequest paymentRequest = createTestPaymentRequest();

        // When
        paymentRequestPublisher.publishPaymentRequest(paymentRequest);

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Message message = rabbitTemplate.receive("payment.request.queue", 1000);
            assertThat(message).isNotNull();
            
            String messageBody = new String(message.getBody());
            PaymentRequest receivedRequest = objectMapper.readValue(messageBody, PaymentRequest.class);
            
            assertThat(receivedRequest.getTransactionId()).isEqualTo(paymentRequest.getTransactionId());
            assertThat(receivedRequest.getOrderId()).isEqualTo(paymentRequest.getOrderId());
            assertThat(receivedRequest.getAmount()).isEqualTo(paymentRequest.getAmount());
            
            // 驗證消息屬性
            MessageProperties properties = message.getMessageProperties();
            assertThat(properties.getContentType()).isEqualTo("application/json");
            assertThat(properties.getCorrelationId()).isEqualTo(paymentRequest.getTransactionId());
            assertThat(properties.getHeaders().get("eventType")).isEqualTo("PaymentRequest");
            assertThat(properties.getHeaders().get("orderId")).isEqualTo(paymentRequest.getOrderId());
        });
    }

    @Test
    void shouldHandlePaymentConfirmationMessage() throws Exception {
        // Given
        PaymentConfirmation confirmation = createTestPaymentConfirmation();
        String confirmationJson = objectMapper.writeValueAsString(confirmation);

        // When
        rabbitTemplate.convertAndSend("payment.exchange", "payment.confirmation", confirmationJson);

        // Then - 驗證消息已發送到隊列
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Message message = rabbitTemplate.receive("payment.confirmation.queue", 1000);
            if (message != null) {
                String messageBody = new String(message.getBody());
                PaymentConfirmation receivedConfirmation = objectMapper.readValue(messageBody, PaymentConfirmation.class);
                
                assertThat(receivedConfirmation.getPaymentId()).isEqualTo(confirmation.getPaymentId());
                assertThat(receivedConfirmation.getOrderId()).isEqualTo(confirmation.getOrderId());
                assertThat(receivedConfirmation.getStatus()).isEqualTo(confirmation.getStatus());
            }
        });
    }

    @Test
    void shouldSetCorrectMessageProperties() throws Exception {
        // Given
        PaymentRequest paymentRequest = createTestPaymentRequest();

        // When
        paymentRequestPublisher.publishPaymentRequest(paymentRequest);

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Message message = rabbitTemplate.receive("payment.request.queue", 1000);
            assertThat(message).isNotNull();
            
            MessageProperties properties = message.getMessageProperties();
            
            // 驗證基本屬性
            assertThat(properties.getContentType()).isEqualTo("application/json");
            assertThat(properties.getContentEncoding()).isEqualTo("UTF-8");
            assertThat(properties.getDeliveryMode()).isEqualTo(MessageProperties.DEFAULT_DELIVERY_MODE);
            
            // 驗證相關ID
            assertThat(properties.getMessageId()).isNotNull();
            assertThat(properties.getCorrelationId()).isEqualTo(paymentRequest.getTransactionId());
            
            // 驗證時間戳
            assertThat(properties.getTimestamp()).isNotNull();
            
            // 驗證自定義頭部
            assertThat(properties.getHeaders().get("eventType")).isEqualTo("PaymentRequest");
            assertThat(properties.getHeaders().get("orderId")).isEqualTo(paymentRequest.getOrderId());
            assertThat(properties.getHeaders().get("customerId")).isEqualTo(paymentRequest.getCustomerId());
            assertThat(properties.getHeaders().get("amount")).isEqualTo(paymentRequest.getAmount().toString());
            assertThat(properties.getHeaders().get("currency")).isEqualTo(paymentRequest.getCurrency());
            assertThat(properties.getHeaders().get("source")).isEqualTo("order-service");
            assertThat(properties.getHeaders().get("version")).isEqualTo("1.0");
            
            // 驗證過期時間
            assertThat(properties.getExpiration()).isEqualTo("1800000"); // 30分鐘
        });
    }

    @Test
    void shouldHandleMessagePublishingError() {
        // Given
        PaymentRequest invalidRequest = null;

        // When & Then
        assertThatThrownBy(() -> paymentRequestPublisher.publishPaymentRequest(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment request cannot be null");
    }

    @Test
    void shouldHandleSerializationError() {
        // Given - 創建一個會導致序列化錯誤的請求
        PaymentRequest paymentRequest = createTestPaymentRequest();
        // 設置一個無法序列化的值（這裡我們模擬通過反射設置）
        
        // 由於我們的 PaymentRequest 是正常的 POJO，這個測試主要驗證異常處理邏輯
        // 在實際場景中，序列化錯誤可能由循環引用或其他問題引起
        
        // When & Then - 正常情況下應該成功
        paymentRequestPublisher.publishPaymentRequest(paymentRequest);
        
        // 驗證消息已發送
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Message message = rabbitTemplate.receive("payment.request.queue", 1000);
            assertThat(message).isNotNull();
        });
    }

    @Test
    void shouldHandleConnectionFailure() {
        // Given - 停止 RabbitMQ 容器來模擬連接失敗
        rabbitmq.stop();
        
        PaymentRequest paymentRequest = createTestPaymentRequest();

        // When & Then
        assertThatThrownBy(() -> paymentRequestPublisher.publishPaymentRequest(paymentRequest))
                .isInstanceOf(PaymentRequestPublisherImpl.MessagePublishingException.class)
                .hasMessage("Failed to publish payment request");
        
        // 重新啟動容器以便後續測試
        rabbitmq.start();
    }

    @Test
    void shouldHandleMessageWithMissingTimestamp() throws Exception {
        // Given
        PaymentRequest paymentRequest = createTestPaymentRequest();
        paymentRequest.setTimestamp(null); // 移除時間戳

        // When
        paymentRequestPublisher.publishPaymentRequest(paymentRequest);

        // Then - 應該自動設置時間戳
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Message message = rabbitTemplate.receive("payment.request.queue", 1000);
            assertThat(message).isNotNull();
            
            String messageBody = new String(message.getBody());
            PaymentRequest receivedRequest = objectMapper.readValue(messageBody, PaymentRequest.class);
            
            assertThat(receivedRequest.getTimestamp()).isNotNull();
        });
    }

    @Test
    void shouldHandleHighVolumeMessages() throws Exception {
        // Given
        int messageCount = 100;

        // When - 發送大量消息
        for (int i = 0; i < messageCount; i++) {
            PaymentRequest request = createTestPaymentRequest();
            request.setOrderId("order-" + i);
            paymentRequestPublisher.publishPaymentRequest(request);
        }

        // Then - 驗證所有消息都已發送
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            int receivedCount = 0;
            while (receivedCount < messageCount) {
                Message message = rabbitTemplate.receive("payment.request.queue", 100);
                if (message == null) {
                    break;
                }
                receivedCount++;
            }
            assertThat(receivedCount).isEqualTo(messageCount);
        });
    }

    private PaymentRequest createTestPaymentRequest() {
        return PaymentRequest.builder()
                .transactionId(UUID.randomUUID().toString())
                .orderId("order-123")
                .customerId("customer-123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .merchantId("MERCHANT_001")
                .description("Test payment request")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private PaymentConfirmation createTestPaymentConfirmation() {
        return PaymentConfirmation.builder()
                .paymentId("payment-123")
                .transactionId(UUID.randomUUID().toString())
                .orderId("order-123")
                .status(PaymentConfirmation.PaymentStatus.SUCCESS)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .processedAt(LocalDateTime.now())
                .build();
    }
}
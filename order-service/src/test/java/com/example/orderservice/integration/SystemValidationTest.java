package com.example.orderservice.integration;

import com.example.orderservice.domain.order.Order;
import com.example.orderservice.infrastructure.repository.OrderRepositoryImpl;
import com.example.orderservice.web.dto.CreateOrderRequest;
import com.example.orderservice.web.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class SystemValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orderdb")
            .withUsername("orderuser")
            .withPassword("orderpass");

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.12-management-alpine")
            .withUser("guest", "guest");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderRepositoryImpl orderRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Test
    void shouldValidateMessagePassingAndDataConsistency() throws Exception {
        // Given - Create order request
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("VALIDATION-001")
                .amount(new BigDecimal("150.00"))
                .currency("TWD")
                .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                        .cardNumber("4111111111111111")
                        .expiryDate("12/25")
                        .cvv("123")
                        .cardHolderName("Validation Test")
                        .build())
                .billingAddress(CreateOrderRequest.BillingAddress.builder()
                        .street("123 Test St")
                        .city("Test City")
                        .postalCode("12345")
                        .country("TW")
                        .build())
                .merchantId("MERCHANT-001")
                .description("Validation test order")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        // When - Create order
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        // Then - Validate order creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        String orderId = createResponse.getBody().getOrderId();

        // Validate database record
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                            "SELECT * FROM orders WHERE order_id = ?", orderId);
                    return !orders.isEmpty();
                });

        List<Map<String, Object>> orderRecords = jdbcTemplate.queryForList(
                "SELECT * FROM orders WHERE order_id = ?", orderId);
        assertThat(orderRecords).hasSize(1);
        
        Map<String, Object> orderRecord = orderRecords.get(0);
        assertThat(orderRecord.get("customer_id")).isEqualTo("VALIDATION-001");
        assertThat(orderRecord.get("amount")).isEqualTo(new BigDecimal("150.00"));
        assertThat(orderRecord.get("status")).isEqualTo("PENDING");

        // Validate payment request record
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> paymentRequests = jdbcTemplate.queryForList(
                            "SELECT * FROM payment_requests WHERE order_id = ?", orderId);
                    return !paymentRequests.isEmpty();
                });

        List<Map<String, Object>> paymentRequestRecords = jdbcTemplate.queryForList(
                "SELECT * FROM payment_requests WHERE order_id = ?", orderId);
        assertThat(paymentRequestRecords).hasSize(1);
        
        Map<String, Object> paymentRequestRecord = paymentRequestRecords.get(0);
        assertThat(paymentRequestRecord.get("order_id")).isEqualTo(orderId);
        assertThat(paymentRequestRecord.get("status")).isEqualTo("SENT");

        // Validate API request log
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> apiLogs = jdbcTemplate.queryForList(
                            "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER'");
                    return !apiLogs.isEmpty();
                });

        List<Map<String, Object>> apiLogRecords = jdbcTemplate.queryForList(
                "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' ORDER BY timestamp DESC LIMIT 1");
        assertThat(apiLogRecords).hasSize(1);
        
        Map<String, Object> apiLogRecord = apiLogRecords.get(0);
        assertThat(apiLogRecord.get("operation")).isEqualTo("CREATE_ORDER");
        assertThat(apiLogRecord.get("status")).isEqualTo("SUCCESS");
        assertThat(apiLogRecord.get("execution_time_ms")).isNotNull();

        // Validate message event log
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> messageLogs = jdbcTemplate.queryForList(
                            "SELECT * FROM message_event_logs WHERE event_type = 'PAYMENT_REQUEST_SENT'");
                    return !messageLogs.isEmpty();
                });

        List<Map<String, Object>> messageLogRecords = jdbcTemplate.queryForList(
                "SELECT * FROM message_event_logs WHERE event_type = 'PAYMENT_REQUEST_SENT' ORDER BY timestamp DESC LIMIT 1");
        assertThat(messageLogRecords).hasSize(1);
        
        Map<String, Object> messageLogRecord = messageLogRecords.get(0);
        assertThat(messageLogRecord.get("event_type")).isEqualTo("PAYMENT_REQUEST_SENT");
        assertThat(messageLogRecord.get("status")).isEqualTo("SUCCESS");
    }

    @Test
    void shouldValidateRetryMechanism() throws Exception {
        // Given - Create order that will trigger retry
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("RETRY-TEST-001")
                .amount(new BigDecimal("75.00"))
                .currency("TWD")
                .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                        .cardNumber("4111111111111111")
                        .expiryDate("12/25")
                        .cvv("123")
                        .cardHolderName("Retry Test")
                        .build())
                .billingAddress(CreateOrderRequest.BillingAddress.builder()
                        .street("123 Test St")
                        .city("Test City")
                        .postalCode("12345")
                        .country("TW")
                        .build())
                .merchantId("MERCHANT-001")
                .description("Retry test order")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        // When - Create order
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String orderId = createResponse.getBody().getOrderId();

        // Wait for initial payment request
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> paymentRequests = jdbcTemplate.queryForList(
                            "SELECT * FROM payment_requests WHERE order_id = ?", orderId);
                    return !paymentRequests.isEmpty();
                });

        // Simulate no payment confirmation received - check retry history
        await().atMost(60, TimeUnit.SECONDS) // Allow time for retry mechanism
                .until(() -> {
                    List<Map<String, Object>> retryHistory = jdbcTemplate.queryForList(
                            "SELECT * FROM retry_history WHERE order_id = ?", orderId);
                    return !retryHistory.isEmpty();
                });

        // Validate retry history records
        List<Map<String, Object>> retryRecords = jdbcTemplate.queryForList(
                "SELECT * FROM retry_history WHERE order_id = ? ORDER BY attempt_number", orderId);
        
        assertThat(retryRecords).isNotEmpty();
        
        // Check that retry attempts are recorded
        for (Map<String, Object> retryRecord : retryRecords) {
            assertThat(retryRecord.get("order_id")).isEqualTo(orderId);
            assertThat(retryRecord.get("attempt_number")).isNotNull();
            assertThat(retryRecord.get("attempted_at")).isNotNull();
        }
    }

    @Test
    void shouldValidateDataIntegrityUnderConcurrency() throws Exception {
        // Given - Multiple concurrent requests for the same customer
        String customerId = "CONCURRENT-CUSTOMER-001";
        int numberOfRequests = 3;
        Thread[] threads = new Thread[numberOfRequests];
        String[] orderIds = new String[numberOfRequests];

        // When - Create multiple orders concurrently
        for (int i = 0; i < numberOfRequests; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                            .customerId(customerId)
                            .amount(new BigDecimal("25.00"))
                            .currency("TWD")
                            .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                                    .cardNumber("4111111111111111")
                                    .expiryDate("12/25")
                                    .cvv("123")
                                    .cardHolderName("Concurrent Test " + index)
                                    .build())
                            .billingAddress(CreateOrderRequest.BillingAddress.builder()
                                    .street("123 Test St")
                                    .city("Test City")
                                    .postalCode("12345")
                                    .country("TW")
                                    .build())
                            .merchantId("MERCHANT-001")
                            .description("Concurrent test order")
                            .build();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

                    ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                            "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        orderIds[index] = response.getBody().getOrderId();
                    }
                } catch (Exception e) {
                    System.err.println("Error in concurrent request " + index + ": " + e.getMessage());
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000);
        }

        // Then - Validate data integrity
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                            "SELECT * FROM orders WHERE customer_id = ?", customerId);
                    return orders.size() >= numberOfRequests - 1; // Allow for one potential failure
                });

        // Validate all orders are properly recorded
        List<Map<String, Object>> customerOrders = jdbcTemplate.queryForList(
                "SELECT * FROM orders WHERE customer_id = ? ORDER BY created_at", customerId);
        
        assertThat(customerOrders.size()).isGreaterThanOrEqualTo(numberOfRequests - 1);

        // Validate each order has corresponding payment request
        for (Map<String, Object> order : customerOrders) {
            String orderId = (String) order.get("order_id");
            
            List<Map<String, Object>> paymentRequests = jdbcTemplate.queryForList(
                    "SELECT * FROM payment_requests WHERE order_id = ?", orderId);
            assertThat(paymentRequests).hasSize(1);
        }

        // Validate no duplicate processing
        List<Map<String, Object>> allPaymentRequests = jdbcTemplate.queryForList(
                "SELECT order_id, COUNT(*) as count FROM payment_requests WHERE order_id IN " +
                "(SELECT order_id FROM orders WHERE customer_id = ?) GROUP BY order_id HAVING COUNT(*) > 1",
                customerId);
        assertThat(allPaymentRequests).isEmpty(); // No duplicates should exist
    }

    @Test
    void shouldValidateLoggingFunctionality() throws Exception {
        // Given - Create order to trigger logging
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("LOGGING-TEST-001")
                .amount(new BigDecimal("99.99"))
                .currency("TWD")
                .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                        .cardNumber("4111111111111111")
                        .expiryDate("12/25")
                        .cvv("123")
                        .cardHolderName("Logging Test")
                        .build())
                .billingAddress(CreateOrderRequest.BillingAddress.builder()
                        .street("123 Test St")
                        .city("Test City")
                        .postalCode("12345")
                        .country("TW")
                        .build())
                .merchantId("MERCHANT-001")
                .description("Logging test order")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        // When - Create order
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Then - Validate API request logging
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> apiLogs = jdbcTemplate.queryForList(
                            "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' " +
                            "AND request_payload LIKE '%LOGGING-TEST-001%'");
                    return !apiLogs.isEmpty();
                });

        List<Map<String, Object>> apiLogs = jdbcTemplate.queryForList(
                "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' " +
                "AND request_payload LIKE '%LOGGING-TEST-001%' ORDER BY timestamp DESC LIMIT 1");
        
        assertThat(apiLogs).hasSize(1);
        Map<String, Object> apiLog = apiLogs.get(0);
        
        assertThat(apiLog.get("operation")).isEqualTo("CREATE_ORDER");
        assertThat(apiLog.get("class_name")).isEqualTo("OrderController");
        assertThat(apiLog.get("method_name")).isEqualTo("createOrder");
        assertThat(apiLog.get("status")).isEqualTo("SUCCESS");
        assertThat(apiLog.get("request_payload")).asString().contains("LOGGING-TEST-001");
        assertThat(apiLog.get("response_payload")).asString().contains("orderId");
        assertThat(apiLog.get("execution_time_ms")).isNotNull();

        // Validate message event logging
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> messageLogs = jdbcTemplate.queryForList(
                            "SELECT * FROM message_event_logs WHERE event_type = 'PAYMENT_REQUEST_SENT'");
                    return !messageLogs.isEmpty();
                });

        List<Map<String, Object>> messageLogs = jdbcTemplate.queryForList(
                "SELECT * FROM message_event_logs WHERE event_type = 'PAYMENT_REQUEST_SENT' " +
                "ORDER BY timestamp DESC LIMIT 1");
        
        assertThat(messageLogs).hasSize(1);
        Map<String, Object> messageLog = messageLogs.get(0);
        
        assertThat(messageLog.get("event_type")).isEqualTo("PAYMENT_REQUEST_SENT");
        assertThat(messageLog.get("status")).isEqualTo("SUCCESS");
        assertThat(messageLog.get("payload")).isNotNull();
    }
}
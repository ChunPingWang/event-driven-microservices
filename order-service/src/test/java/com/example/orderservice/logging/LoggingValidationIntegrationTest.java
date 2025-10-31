package com.example.orderservice.logging;

import com.example.orderservice.web.dto.CreateOrderRequest;
import com.example.orderservice.web.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class LoggingValidationIntegrationTest {

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
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        
        // Enable all logging features
        registry.add("microservice.logging.enabled", () -> "true");
        registry.add("microservice.logging.log-request-payload", () -> "true");
        registry.add("microservice.logging.log-response-payload", () -> "true");
        registry.add("microservice.logging.log-message-payload", () -> "true");
        registry.add("microservice.logging.log-message-headers", () -> "true");
    }

    @Test
    void shouldValidateApiRequestLogging() throws Exception {
        // Given - Create order request
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("API-LOG-TEST-001")
                .amount(new BigDecimal("123.45"))
                .currency("TWD")
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("API Log Test")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        LocalDateTime testStartTime = LocalDateTime.now();

        // When - Create order
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        // Then - Validate API request logging
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                            "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' " +
                            "AND request_payload LIKE '%API-LOG-TEST-001%'");
                    return !logs.isEmpty();
                });

        List<Map<String, Object>> apiLogs = jdbcTemplate.queryForList(
                "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' " +
                "AND request_payload LIKE '%API-LOG-TEST-001%' ORDER BY timestamp DESC LIMIT 1");

        assertThat(apiLogs).hasSize(1);
        Map<String, Object> apiLog = apiLogs.get(0);

        // Validate basic log fields
        assertThat(apiLog.get("request_id")).isNotNull();
        assertThat(apiLog.get("operation")).isEqualTo("CREATE_ORDER");
        assertThat(apiLog.get("class_name")).isEqualTo("OrderController");
        assertThat(apiLog.get("method_name")).isEqualTo("createOrder");
        assertThat(apiLog.get("status")).isEqualTo("SUCCESS");

        // Validate request payload logging
        String requestPayload = (String) apiLog.get("request_payload");
        assertThat(requestPayload).isNotNull();
        assertThat(requestPayload).contains("API-LOG-TEST-001");
        assertThat(requestPayload).contains("123.45");
        assertThat(requestPayload).contains("TWD");
        assertThat(requestPayload).contains("API Log Test");

        // Validate response payload logging
        String responsePayload = (String) apiLog.get("response_payload");
        assertThat(responsePayload).isNotNull();
        assertThat(responsePayload).contains("orderId");
        assertThat(responsePayload).contains("PENDING");

        // Validate execution time logging
        Long executionTime = (Long) apiLog.get("execution_time_ms");
        assertThat(executionTime).isNotNull();
        assertThat(executionTime).isGreaterThan(0L);
        assertThat(executionTime).isLessThan(5000L); // Should complete within 5 seconds

        // Validate timestamp
        LocalDateTime logTimestamp = ((java.sql.Timestamp) apiLog.get("timestamp")).toLocalDateTime();
        assertThat(logTimestamp).isAfter(testStartTime.minusMinutes(1));
        assertThat(logTimestamp).isBefore(LocalDateTime.now().plusMinutes(1));
    }

    @Test
    void shouldValidateMessageEventLogging() throws Exception {
        // Given - Create order to trigger message event
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("MSG-LOG-TEST-001")
                .amount(new BigDecimal("67.89"))
                .currency("TWD")
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("Message Log Test")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        LocalDateTime testStartTime = LocalDateTime.now();

        // When - Create order (this will trigger payment request message)
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Then - Validate message event logging
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                            "SELECT * FROM message_event_logs WHERE event_type = 'PAYMENT_REQUEST_SENT'");
                    return !logs.isEmpty();
                });

        List<Map<String, Object>> messageLogs = jdbcTemplate.queryForList(
                "SELECT * FROM message_event_logs WHERE event_type = 'PAYMENT_REQUEST_SENT' " +
                "ORDER BY timestamp DESC LIMIT 1");

        assertThat(messageLogs).hasSize(1);
        Map<String, Object> messageLog = messageLogs.get(0);

        // Validate basic message log fields
        assertThat(messageLog.get("message_id")).isNotNull();
        assertThat(messageLog.get("event_type")).isEqualTo("PAYMENT_REQUEST_SENT");
        assertThat(messageLog.get("class_name")).isNotNull();
        assertThat(messageLog.get("method_name")).isNotNull();
        assertThat(messageLog.get("status")).isEqualTo("SUCCESS");

        // Validate payload logging
        String payload = (String) messageLog.get("payload");
        assertThat(payload).isNotNull();
        assertThat(payload).contains("67.89");

        // Validate timestamp
        LocalDateTime logTimestamp = ((java.sql.Timestamp) messageLog.get("timestamp")).toLocalDateTime();
        assertThat(logTimestamp).isAfter(testStartTime.minusMinutes(1));
        assertThat(logTimestamp).isBefore(LocalDateTime.now().plusMinutes(1));
    }

    @Test
    void shouldValidateErrorLogging() throws Exception {
        // Given - Invalid order request (missing required fields)
        String invalidJson = "{\"customerId\":\"\",\"amount\":-1}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(invalidJson, headers);

        LocalDateTime testStartTime = LocalDateTime.now();

        // When - Send invalid request
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, String.class);

        // Then - Should get error response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Validate error logging
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                            "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' " +
                            "AND status = 'ERROR' AND timestamp > ?", 
                            java.sql.Timestamp.valueOf(testStartTime));
                    return !logs.isEmpty();
                });

        List<Map<String, Object>> errorLogs = jdbcTemplate.queryForList(
                "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' " +
                "AND status = 'ERROR' AND timestamp > ? ORDER BY timestamp DESC LIMIT 1",
                java.sql.Timestamp.valueOf(testStartTime));

        assertThat(errorLogs).hasSize(1);
        Map<String, Object> errorLog = errorLogs.get(0);

        // Validate error log fields
        assertThat(errorLog.get("status")).isEqualTo("ERROR");
        assertThat(errorLog.get("error_message")).isNotNull();
        assertThat(errorLog.get("execution_time_ms")).isNotNull();

        // Error should be logged with request payload
        String requestPayload = (String) errorLog.get("request_payload");
        assertThat(requestPayload).contains("customerId");
        assertThat(requestPayload).contains("amount");
    }

    @Test
    void shouldValidateLoggingConfiguration() throws Exception {
        // Test that logging configuration is properly applied
        
        // Check that logging tables exist
        List<Map<String, Object>> apiLogTable = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'api_request_logs' ORDER BY column_name");
        
        assertThat(apiLogTable).isNotEmpty();
        
        // Verify expected columns exist
        List<String> columnNames = apiLogTable.stream()
                .map(row -> (String) row.get("column_name"))
                .toList();
        
        assertThat(columnNames).contains(
                "request_id", "operation", "class_name", "method_name",
                "request_payload", "response_payload", "status", "error_message",
                "execution_time_ms", "timestamp"
        );

        List<Map<String, Object>> messageLogTable = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'message_event_logs' ORDER BY column_name");
        
        assertThat(messageLogTable).isNotEmpty();
        
        List<String> messageColumnNames = messageLogTable.stream()
                .map(row -> (String) row.get("column_name"))
                .toList();
        
        assertThat(messageColumnNames).contains(
                "message_id", "event_type", "class_name", "method_name",
                "payload", "headers", "status", "error_message", "timestamp"
        );
    }

    @Test
    void shouldValidateLoggingPerformance() throws Exception {
        // Given - Multiple requests to test logging performance
        int numberOfRequests = 10;
        LocalDateTime testStartTime = LocalDateTime.now();

        // When - Send multiple requests
        for (int i = 0; i < numberOfRequests; i++) {
            CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                    .customerId("PERF-TEST-" + String.format("%03d", i))
                    .amount(new BigDecimal("10.00"))
                    .currency("TWD")
                    .cardNumber("4111111111111111")
                    .expiryDate("12/25")
                    .cvv("123")
                    .cardHolderName("Performance Test " + i)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

            ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/orders", request, OrderResponse.class);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        // Then - Validate all requests are logged
        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                            "SELECT COUNT(*) as count FROM api_request_logs " +
                            "WHERE operation = 'CREATE_ORDER' AND timestamp > ?",
                            java.sql.Timestamp.valueOf(testStartTime));
                    return ((Number) logs.get(0).get("count")).intValue() >= numberOfRequests;
                });

        // Validate logging performance - all requests should be logged within reasonable time
        List<Map<String, Object>> performanceLogs = jdbcTemplate.queryForList(
                "SELECT execution_time_ms FROM api_request_logs " +
                "WHERE operation = 'CREATE_ORDER' AND timestamp > ? " +
                "ORDER BY timestamp DESC LIMIT ?",
                java.sql.Timestamp.valueOf(testStartTime), numberOfRequests);

        assertThat(performanceLogs).hasSize(numberOfRequests);

        // Check that logging doesn't significantly impact performance
        for (Map<String, Object> log : performanceLogs) {
            Long executionTime = (Long) log.get("execution_time_ms");
            assertThat(executionTime).isLessThan(2000L); // Should complete within 2 seconds
        }

        // Validate message event logs are also created
        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> messageLogs = jdbcTemplate.queryForList(
                            "SELECT COUNT(*) as count FROM message_event_logs " +
                            "WHERE event_type = 'PAYMENT_REQUEST_SENT' AND timestamp > ?",
                            java.sql.Timestamp.valueOf(testStartTime));
                    return ((Number) messageLogs.get(0).get("count")).intValue() >= numberOfRequests;
                });
    }

    @Test
    void shouldValidateLoggingDataIntegrity() throws Exception {
        // Given - Create order with special characters and large payload
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("INTEGRITY-TEST-001")
                .amount(new BigDecimal("999.99"))
                .currency("TWD")
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("Test with special chars: àáâãäåæçèéêë & symbols !@#$%^&*()")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        // When - Create order
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Then - Validate data integrity in logs
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                            "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' " +
                            "AND request_payload LIKE '%INTEGRITY-TEST-001%'");
                    return !logs.isEmpty();
                });

        List<Map<String, Object>> integrityLogs = jdbcTemplate.queryForList(
                "SELECT * FROM api_request_logs WHERE operation = 'CREATE_ORDER' " +
                "AND request_payload LIKE '%INTEGRITY-TEST-001%' ORDER BY timestamp DESC LIMIT 1");

        assertThat(integrityLogs).hasSize(1);
        Map<String, Object> integrityLog = integrityLogs.get(0);

        // Validate special characters are properly stored
        String requestPayload = (String) integrityLog.get("request_payload");
        assertThat(requestPayload).contains("àáâãäåæçèéêë");
        assertThat(requestPayload).contains("!@#$%^&*()");
        assertThat(requestPayload).contains("999.99");

        // Validate JSON structure is maintained
        assertThat(requestPayload).contains("customerId");
        assertThat(requestPayload).contains("cardHolderName");

        // Validate response payload integrity
        String responsePayload = (String) integrityLog.get("response_payload");
        assertThat(responsePayload).isNotNull();
        assertThat(responsePayload).contains("orderId");
        assertThat(responsePayload).contains("status");
    }
}
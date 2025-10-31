package com.example.orderservice.performance;

import com.example.orderservice.web.dto.CreateOrderRequest;
import com.example.orderservice.web.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class LoadTestIntegration {

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
        
        // Optimize for performance testing
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "5");
        registry.add("spring.rabbitmq.listener.simple.concurrency", () -> "10");
        registry.add("spring.rabbitmq.listener.simple.max-concurrency", () -> "20");
    }

    @BeforeEach
    void setUp() {
        // Clean up test data
        jdbcTemplate.execute("DELETE FROM api_request_logs WHERE operation = 'CREATE_ORDER' AND request_payload LIKE '%LOAD-TEST%'");
        jdbcTemplate.execute("DELETE FROM message_event_logs WHERE payload LIKE '%LOAD-TEST%'");
        jdbcTemplate.execute("DELETE FROM payment_requests WHERE order_id LIKE '%LOAD-TEST%'");
        jdbcTemplate.execute("DELETE FROM orders WHERE customer_id LIKE '%LOAD-TEST%'");
    }

    @Test
    void shouldHandleHighConcurrencyOrderProcessing() throws Exception {
        // Given - High concurrency test parameters
        int numberOfThreads = 20;
        int ordersPerThread = 5;
        int totalOrders = numberOfThreads * ordersPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        LocalDateTime testStartTime = LocalDateTime.now();

        // When - Execute concurrent order creation
        for (int threadId = 0; threadId < numberOfThreads; threadId++) {
            final int currentThreadId = threadId;
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int orderIndex = 0; orderIndex < ordersPerThread; orderIndex++) {
                    try {
                        long startTime = System.currentTimeMillis();
                        
                        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                                .customerId("LOAD-TEST-THREAD-" + currentThreadId + "-ORDER-" + orderIndex)
                                .amount(new BigDecimal("50.00"))
                                .currency("TWD")
                                .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                                        .cardNumber("4111111111111111")
                                        .expiryDate("12/25")
                                        .cvv("123")
                                        .cardHolderName("Load Test User " + currentThreadId + "-" + orderIndex)
                                        .build())
                                .billingAddress(CreateOrderRequest.BillingAddress.builder()
                                        .street("123 Test St")
                                        .city("Test City")
                                        .postalCode("12345")
                                        .country("TW")
                                        .build())
                                .merchantId("MERCHANT-001")
                                .description("Load test order")
                                .build();

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

                        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

                        long responseTime = System.currentTimeMillis() - startTime;
                        totalResponseTime.addAndGet(responseTime);

                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error in thread " + currentThreadId + ", order " + orderIndex + ": " + e.getMessage());
                    }
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(120, TimeUnit.SECONDS); // 2 minute timeout

        executor.shutdown();
        
        LocalDateTime testEndTime = LocalDateTime.now();
        Duration testDuration = Duration.between(testStartTime, testEndTime);

        // Then - Validate performance metrics
        int actualSuccessCount = successCount.get();
        int actualErrorCount = errorCount.get();
        double averageResponseTime = totalResponseTime.get() / (double) (actualSuccessCount + actualErrorCount);
        double throughput = actualSuccessCount / (testDuration.toMillis() / 1000.0);

        System.out.println("=== Load Test Results ===");
        System.out.println("Total Orders Attempted: " + totalOrders);
        System.out.println("Successful Orders: " + actualSuccessCount);
        System.out.println("Failed Orders: " + actualErrorCount);
        System.out.println("Success Rate: " + (actualSuccessCount * 100.0 / totalOrders) + "%");
        System.out.println("Average Response Time: " + averageResponseTime + "ms");
        System.out.println("Throughput: " + throughput + " orders/second");
        System.out.println("Test Duration: " + testDuration.toMillis() + "ms");

        // Performance assertions
        assertThat(actualSuccessCount).isGreaterThanOrEqualTo((int) (totalOrders * 0.95)); // 95% success rate
        assertThat(averageResponseTime).isLessThan(2000.0); // Average response time under 2 seconds
        assertThat(throughput).isGreaterThan(5.0); // At least 5 orders per second

        // Validate database consistency
        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                            "SELECT COUNT(*) as count FROM orders WHERE customer_id LIKE 'LOAD-TEST-THREAD-%'");
                    return ((Number) orders.get(0).get("count")).intValue() >= actualSuccessCount;
                });

        // Validate message processing
        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> paymentRequests = jdbcTemplate.queryForList(
                            "SELECT COUNT(*) as count FROM payment_requests WHERE order_id LIKE '%LOAD-TEST%'");
                    return ((Number) paymentRequests.get(0).get("count")).intValue() >= actualSuccessCount;
                });
    }

    @Test
    void shouldHandleMessageQueueBacklog() throws Exception {
        // Given - Create a backlog of messages
        int numberOfMessages = 50;
        AtomicInteger processedMessages = new AtomicInteger(0);
        LocalDateTime testStartTime = LocalDateTime.now();

        // When - Create multiple orders rapidly to create message backlog
        for (int i = 0; i < numberOfMessages; i++) {
            CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                    .customerId("BACKLOG-TEST-" + String.format("%03d", i))
                    .amount(new BigDecimal("25.00"))
                    .currency("TWD")
                    .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                            .cardNumber("4111111111111111")
                            .expiryDate("12/25")
                            .cvv("123")
                            .cardHolderName("Backlog Test " + i)
                            .build())
                    .billingAddress(CreateOrderRequest.BillingAddress.builder()
                            .street("123 Test St")
                            .city("Test City")
                            .postalCode("12345")
                            .country("TW")
                            .build())
                    .merchantId("MERCHANT-001")
                    .description("Backlog test order")
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

            ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                processedMessages.incrementAndGet();
            }
            
            // Small delay to create controlled backlog
            Thread.sleep(10);
        }

        // Then - Validate all messages are eventually processed
        await().atMost(120, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> paymentRequests = jdbcTemplate.queryForList(
                            "SELECT COUNT(*) as count FROM payment_requests WHERE order_id LIKE '%BACKLOG-TEST%'");
                    return ((Number) paymentRequests.get(0).get("count")).intValue() >= processedMessages.get();
                });

        LocalDateTime testEndTime = LocalDateTime.now();
        Duration processingTime = Duration.between(testStartTime, testEndTime);

        // Validate message processing performance
        List<Map<String, Object>> processedCount = jdbcTemplate.queryForList(
                "SELECT COUNT(*) as count FROM payment_requests WHERE order_id LIKE '%BACKLOG-TEST%'");
        int actualProcessedCount = ((Number) processedCount.get(0).get("count")).intValue();

        System.out.println("=== Message Queue Backlog Test Results ===");
        System.out.println("Messages Sent: " + numberOfMessages);
        System.out.println("Messages Processed: " + actualProcessedCount);
        System.out.println("Processing Time: " + processingTime.toMillis() + "ms");
        System.out.println("Processing Rate: " + (actualProcessedCount / (processingTime.toMillis() / 1000.0)) + " messages/second");

        assertThat(actualProcessedCount).isEqualTo(processedMessages.get());
        assertThat(processingTime.toSeconds()).isLessThan(60); // Should process within 1 minute
    }

    @Test
    void shouldMaintainStabilityUnderSustainedLoad() throws Exception {
        // Given - Sustained load test parameters
        int testDurationMinutes = 2; // Reduced for CI/CD
        int requestsPerSecond = 10;
        int totalRequests = testDurationMinutes * 60 * requestsPerSecond;
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        LocalDateTime testStartTime = LocalDateTime.now();
        LocalDateTime testEndTime = testStartTime.plusMinutes(testDurationMinutes);

        // When - Execute sustained load
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        while (LocalDateTime.now().isBefore(testEndTime)) {
            for (int i = 0; i < requestsPerSecond && LocalDateTime.now().isBefore(testEndTime); i++) {
                final int requestId = successCount.get() + errorCount.get();
                
                executor.submit(() -> {
                    try {
                        long startTime = System.currentTimeMillis();
                        
                        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                                .customerId("SUSTAINED-LOAD-" + requestId)
                                .amount(new BigDecimal("30.00"))
                                .currency("TWD")
                                .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                                        .cardNumber("4111111111111111")
                                        .expiryDate("12/25")
                                        .cvv("123")
                                        .cardHolderName("Sustained Load Test " + requestId)
                                        .build())
                                .billingAddress(CreateOrderRequest.BillingAddress.builder()
                                        .street("123 Test St")
                                        .city("Test City")
                                        .postalCode("12345")
                                        .country("TW")
                                        .build())
                                .merchantId("MERCHANT-001")
                                .description("Sustained load test order")
                                .build();

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

                        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

                        long responseTime = System.currentTimeMillis() - startTime;
                        totalResponseTime.addAndGet(responseTime);

                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                });
            }
            
            Thread.sleep(1000); // Wait 1 second before next batch
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        LocalDateTime actualEndTime = LocalDateTime.now();
        Duration actualTestDuration = Duration.between(testStartTime, actualEndTime);

        // Then - Validate system stability
        int actualSuccessCount = successCount.get();
        int actualErrorCount = errorCount.get();
        int totalProcessed = actualSuccessCount + actualErrorCount;
        double successRate = (actualSuccessCount * 100.0) / totalProcessed;
        double averageResponseTime = totalResponseTime.get() / (double) totalProcessed;
        double actualThroughput = actualSuccessCount / (actualTestDuration.toMillis() / 1000.0);

        System.out.println("=== Sustained Load Test Results ===");
        System.out.println("Test Duration: " + actualTestDuration.toMinutes() + " minutes");
        System.out.println("Total Requests: " + totalProcessed);
        System.out.println("Successful Requests: " + actualSuccessCount);
        System.out.println("Failed Requests: " + actualErrorCount);
        System.out.println("Success Rate: " + successRate + "%");
        System.out.println("Average Response Time: " + averageResponseTime + "ms");
        System.out.println("Actual Throughput: " + actualThroughput + " requests/second");

        // Stability assertions
        assertThat(successRate).isGreaterThanOrEqualTo(90.0); // 90% success rate under sustained load
        assertThat(averageResponseTime).isLessThan(3000.0); // Average response time under 3 seconds
        assertThat(actualThroughput).isGreaterThan(5.0); // Maintain at least 5 requests per second

        // Validate system resources haven't degraded significantly
        // Check database connections are still available
        List<Map<String, Object>> dbConnections = jdbcTemplate.queryForList(
                "SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active'");
        int activeConnections = ((Number) dbConnections.get(0).get("active_connections")).intValue();
        
        assertThat(activeConnections).isLessThan(50); // Should not exhaust connection pool
    }

    @Test
    void shouldRecoverFromResourceExhaustion() throws Exception {
        // Given - Create resource exhaustion scenario
        int overloadRequests = 100;
        AtomicInteger initialSuccessCount = new AtomicInteger(0);
        AtomicInteger initialErrorCount = new AtomicInteger(0);

        // When - Create overload
        ExecutorService overloadExecutor = Executors.newFixedThreadPool(50);
        
        for (int i = 0; i < overloadRequests; i++) {
            final int requestId = i;
            overloadExecutor.submit(() -> {
                try {
                    CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                            .customerId("OVERLOAD-TEST-" + requestId)
                            .amount(new BigDecimal("15.00"))
                            .currency("TWD")
                            .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                                    .cardNumber("4111111111111111")
                                    .expiryDate("12/25")
                                    .cvv("123")
                                    .cardHolderName("Overload Test " + requestId)
                                    .build())
                            .billingAddress(CreateOrderRequest.BillingAddress.builder()
                                    .street("123 Test St")
                                    .city("Test City")
                                    .postalCode("12345")
                                    .country("TW")
                                    .build())
                            .merchantId("MERCHANT-001")
                            .description("Overload test order")
                            .build();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

                    ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                            "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        initialSuccessCount.incrementAndGet();
                    } else {
                        initialErrorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    initialErrorCount.incrementAndGet();
                }
            });
        }

        overloadExecutor.shutdown();
        overloadExecutor.awaitTermination(60, TimeUnit.SECONDS);

        // Wait for system to stabilize
        Thread.sleep(10000);

        // Then - Test recovery with normal load
        AtomicInteger recoverySuccessCount = new AtomicInteger(0);
        int recoveryRequests = 20;

        for (int i = 0; i < recoveryRequests; i++) {
            CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                    .customerId("RECOVERY-TEST-" + i)
                    .amount(new BigDecimal("40.00"))
                    .currency("TWD")
                    .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                            .cardNumber("4111111111111111")
                            .expiryDate("12/25")
                            .cvv("123")
                            .cardHolderName("Recovery Test " + i)
                            .build())
                    .billingAddress(CreateOrderRequest.BillingAddress.builder()
                            .street("123 Test St")
                            .city("Test City")
                            .postalCode("12345")
                            .country("TW")
                            .build())
                    .merchantId("MERCHANT-001")
                    .description("Recovery test order")
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

            ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                recoverySuccessCount.incrementAndGet();
            }
            
            Thread.sleep(100); // Small delay between requests
        }

        // Validate recovery
        double recoveryRate = (recoverySuccessCount.get() * 100.0) / recoveryRequests;

        System.out.println("=== Resource Exhaustion Recovery Test Results ===");
        System.out.println("Overload Phase - Success: " + initialSuccessCount.get() + ", Errors: " + initialErrorCount.get());
        System.out.println("Recovery Phase - Success: " + recoverySuccessCount.get() + "/" + recoveryRequests);
        System.out.println("Recovery Rate: " + recoveryRate + "%");

        assertThat(recoveryRate).isGreaterThanOrEqualTo(80.0); // Should recover to at least 80% success rate
    }
}
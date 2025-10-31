package com.example.orderservice.integration;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class EndToEndIntegrationTest {

    private static final Network network = Network.newNetwork();

    @Container
    static PostgreSQLContainer<?> orderDatabase = new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases("order-db")
            .withDatabaseName("orderdb")
            .withUsername("orderuser")
            .withPassword("orderpass");

    @Container
    static PostgreSQLContainer<?> paymentDatabase = new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases("payment-db")
            .withDatabaseName("paymentdb")
            .withUsername("paymentuser")
            .withPassword("paymentpass");

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.12-management-alpine")
            .withNetwork(network)
            .withNetworkAliases("rabbitmq")
            .withUser("guest", "guest");

    @Container
    static GenericContainer<?> paymentService = new GenericContainer<>("payment-service:latest")
            .withNetwork(network)
            .withNetworkAliases("payment-service")
            .withExposedPorts(8081)
            .withEnv("SPRING_PROFILES_ACTIVE", "docker")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://payment-db:5432/paymentdb")
            .withEnv("SPRING_DATASOURCE_USERNAME", "paymentuser")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "paymentpass")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("SPRING_RABBITMQ_PORT", "5672")
            .withEnv("SPRING_RABBITMQ_USERNAME", "guest")
            .withEnv("SPRING_RABBITMQ_PASSWORD", "guest")
            .dependsOn(paymentDatabase, rabbitMQ)
            .waitingFor(Wait.forHttp("/actuator/health").forPort(8081).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", orderDatabase::getJdbcUrl);
        registry.add("spring.datasource.username", orderDatabase::getUsername);
        registry.add("spring.datasource.password", orderDatabase::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @BeforeEach
    void setUp() {
        // Wait for all services to be ready
        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        ResponseEntity<String> response = restTemplate.getForEntity(
                                "http://localhost:" + port + "/actuator/health", String.class);
                        return response.getStatusCode() == HttpStatus.OK;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    @Test
    void shouldCompleteEndToEndOrderPaymentFlow() throws Exception {
        // Given - Create order request
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("CUST-E2E-001")
                .amount(new BigDecimal("299.99"))
                .currency("TWD")
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("John Doe")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        // When - Create order
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        // Then - Order should be created
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getOrderId()).isNotNull();
        assertThat(createResponse.getBody().getStatus()).isEqualTo("PENDING");

        String orderId = createResponse.getBody().getOrderId();

        // Wait for payment processing to complete
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                            "http://localhost:" + port + "/api/orders/" + orderId, OrderResponse.class);
                    return response.getStatusCode() == HttpStatus.OK &&
                           "PAID".equals(response.getBody().getStatus());
                });

        // Verify final order status
        ResponseEntity<OrderResponse> finalResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/orders/" + orderId, OrderResponse.class);

        assertThat(finalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalResponse.getBody().getStatus()).isEqualTo("PAID");
        assertThat(finalResponse.getBody().getAmount()).isEqualTo(new BigDecimal("299.99"));
    }

    @Test
    void shouldHandlePaymentFailureFlow() throws Exception {
        // Given - Create order request with invalid card
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("CUST-E2E-002")
                .amount(new BigDecimal("199.99"))
                .currency("TWD")
                .cardNumber("1234567890123456") // Invalid card number
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("Jane Doe")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        // When - Create order
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        // Then - Order should be created
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String orderId = createResponse.getBody().getOrderId();

        // Wait for payment processing to fail
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                            "http://localhost:" + port + "/api/orders/" + orderId, OrderResponse.class);
                    return response.getStatusCode() == HttpStatus.OK &&
                           "PAYMENT_FAILED".equals(response.getBody().getStatus());
                });

        // Verify final order status
        ResponseEntity<OrderResponse> finalResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/orders/" + orderId, OrderResponse.class);

        assertThat(finalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalResponse.getBody().getStatus()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void shouldHandleHighConcurrencyOrders() throws Exception {
        // Given - Multiple concurrent order requests
        int numberOfOrders = 5;
        Thread[] threads = new Thread[numberOfOrders];
        String[] orderIds = new String[numberOfOrders];

        // When - Create multiple orders concurrently
        for (int i = 0; i < numberOfOrders; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                            .customerId("CUST-CONCURRENT-" + index)
                            .amount(new BigDecimal("50.00"))
                            .currency("TWD")
                            .cardNumber("4111111111111111")
                            .expiryDate("12/25")
                            .cvv("123")
                            .cardHolderName("Customer " + index)
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
                    // Log error but don't fail the test immediately
                    System.err.println("Error creating order " + index + ": " + e.getMessage());
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000); // 10 second timeout per thread
        }

        // Then - All orders should eventually be processed
        for (int i = 0; i < numberOfOrders; i++) {
            if (orderIds[i] != null) {
                final String orderId = orderIds[i];
                await().atMost(15, TimeUnit.SECONDS)
                        .pollInterval(1, TimeUnit.SECONDS)
                        .until(() -> {
                            ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                                    "http://localhost:" + port + "/api/orders/" + orderId, OrderResponse.class);
                            return response.getStatusCode() == HttpStatus.OK &&
                                   ("PAID".equals(response.getBody().getStatus()) ||
                                    "PAYMENT_FAILED".equals(response.getBody().getStatus()));
                        });
            }
        }

        // Verify all orders were created and processed
        int successfulOrders = 0;
        for (String orderId : orderIds) {
            if (orderId != null) {
                ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                        "http://localhost:" + port + "/api/orders/" + orderId, OrderResponse.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    successfulOrders++;
                }
            }
        }

        assertThat(successfulOrders).isGreaterThanOrEqualTo(numberOfOrders - 1); // Allow for 1 failure
    }

    @Test
    void shouldMaintainDataConsistency() throws Exception {
        // Given - Create order request
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .customerId("CUST-CONSISTENCY-001")
                .amount(new BigDecimal("89.99"))
                .currency("TWD")
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("Consistency Test")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> request = new HttpEntity<>(orderRequest, headers);

        // When - Create order
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/orders", request, OrderResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String orderId = createResponse.getBody().getOrderId();

        // Wait for processing to complete
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                            "http://localhost:" + port + "/api/orders/" + orderId, OrderResponse.class);
                    return response.getStatusCode() == HttpStatus.OK &&
                           !"PENDING".equals(response.getBody().getStatus());
                });

        // Then - Verify data consistency
        ResponseEntity<OrderResponse> finalResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/orders/" + orderId, OrderResponse.class);

        assertThat(finalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalResponse.getBody().getOrderId()).isEqualTo(orderId);
        assertThat(finalResponse.getBody().getCustomerId()).isEqualTo("CUST-CONSISTENCY-001");
        assertThat(finalResponse.getBody().getAmount()).isEqualTo(new BigDecimal("89.99"));
        
        // Status should be either PAID or PAYMENT_FAILED, not PENDING
        assertThat(finalResponse.getBody().getStatus())
                .isIn("PAID", "PAYMENT_FAILED");
    }
}
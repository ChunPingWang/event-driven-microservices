package com.example.orderservice.infrastructure;

import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import com.example.orderservice.infrastructure.repository.OrderRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 訂單倉儲集成測試 - 測試數據庫操作和事務處理
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderRepositoryImpl.class)
class OrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orderdb_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private OrderRepositoryImpl orderRepository;

    @Autowired
    private OrderRepositoryImpl.OrderJpaRepository jpaRepository;

    private Order testOrder;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        testOrder = createTestOrder();
    }

    @Test
    void shouldSaveAndRetrieveOrder() {
        // When
        orderRepository.save(testOrder);

        // Then
        Optional<Order> retrieved = orderRepository.findById(testOrder.getOrderId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getOrderId()).isEqualTo(testOrder.getOrderId());
        assertThat(retrieved.get().getCustomerId()).isEqualTo(testOrder.getCustomerId());
        assertThat(retrieved.get().getAmount()).isEqualTo(testOrder.getAmount());
        assertThat(retrieved.get().getStatus()).isEqualTo(testOrder.getStatus());
    }

    @Test
    void shouldFindOrderByTransactionId() {
        // Given
        orderRepository.save(testOrder);

        // When
        Optional<Order> found = orderRepository.findByTransactionId(testOrder.getTransactionId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(testOrder.getOrderId());
    }

    @Test
    void shouldFindOrdersByCustomerId() {
        // Given
        String customerId = "customer-123";
        Order order1 = createOrderForCustomer(customerId);
        Order order2 = createOrderForCustomer(customerId);
        Order order3 = createOrderForCustomer("other-customer");

        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);

        // When
        List<Order> customerOrders = orderRepository.findByCustomerId(customerId, 0, 10);

        // Then
        assertThat(customerOrders).hasSize(2);
        assertThat(customerOrders).allMatch(order -> 
            order.getCustomerId().getValue().equals(customerId));
    }

    @Test
    void shouldFindOrdersByStatus() {
        // Given
        Order pendingOrder1 = createOrderWithStatus(OrderStatus.PAYMENT_PENDING);
        Order pendingOrder2 = createOrderWithStatus(OrderStatus.PAYMENT_PENDING);
        Order confirmedOrder = createOrderWithStatus(OrderStatus.PAYMENT_CONFIRMED);

        orderRepository.save(pendingOrder1);
        orderRepository.save(pendingOrder2);
        orderRepository.save(confirmedOrder);

        // When
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10);

        // Then
        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders).allMatch(order -> 
            order.getStatus() == OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void shouldFindOrdersByDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        
        orderRepository.save(testOrder);

        // When
        List<Order> ordersInRange = orderRepository.findByDateRange(startDate, endDate, 0, 10);

        // Then
        assertThat(ordersInRange).hasSize(1);
        assertThat(ordersInRange.get(0).getOrderId()).isEqualTo(testOrder.getOrderId());
    }

    @Test
    void shouldCountOrdersWithFilters() {
        // Given
        String customerId = "customer-123";
        Order order1 = createOrderForCustomer(customerId);
        Order order2 = createOrderForCustomer(customerId);
        order2.confirmPayment("payment-123");

        orderRepository.save(order1);
        orderRepository.save(order2);

        // When
        int totalCount = orderRepository.countOrders(customerId, null, null, null);
        int pendingCount = orderRepository.countOrders(customerId, OrderStatus.PAYMENT_PENDING, null, null);
        int confirmedCount = orderRepository.countOrders(customerId, OrderStatus.PAYMENT_CONFIRMED, null, null);

        // Then
        assertThat(totalCount).isEqualTo(2);
        assertThat(pendingCount).isEqualTo(1);
        assertThat(confirmedCount).isEqualTo(1);
    }

    @Test
    void shouldHandlePaginationCorrectly() {
        // Given
        String customerId = "customer-123";
        for (int i = 0; i < 15; i++) {
            orderRepository.save(createOrderForCustomer(customerId));
        }

        // When
        List<Order> page1 = orderRepository.findByCustomerId(customerId, 0, 10);
        List<Order> page2 = orderRepository.findByCustomerId(customerId, 1, 10);

        // Then
        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(5);
    }

    @Test
    void shouldThrowExceptionForNullOrder() {
        // When & Then
        assertThatThrownBy(() -> orderRepository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullOrderId() {
        // When & Then
        assertThatThrownBy(() -> orderRepository.findById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order ID cannot be null");
    }

    @Test
    void shouldThrowExceptionForInvalidPagination() {
        // When & Then
        assertThatThrownBy(() -> orderRepository.findByCustomerId("customer-123", -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page number cannot be negative");

        assertThatThrownBy(() -> orderRepository.findByCustomerId("customer-123", 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be positive");

        assertThatThrownBy(() -> orderRepository.findByCustomerId("customer-123", 0, 1001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size cannot exceed 1000");
    }

    @Test
    void shouldHandleTransactionRollback() {
        // Given
        Order order = createTestOrder();

        // When - 模擬事務回滾場景
        try {
            orderRepository.save(order);
            // 模擬異常導致事務回滾
            throw new RuntimeException("Simulated transaction failure");
        } catch (RuntimeException e) {
            // 預期的異常
        }

        // Then - 驗證數據未被保存
        Optional<Order> retrieved = orderRepository.findById(order.getOrderId());
        assertThat(retrieved).isEmpty();
    }

    private Order createTestOrder() {
        Order order = new Order(
                new CustomerId("customer-123"),
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD"))
        );
        order.requestPayment(UUID.randomUUID().toString());
        return order;
    }

    private Order createOrderForCustomer(String customerId) {
        Order order = new Order(
                new CustomerId(customerId),
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD"))
        );
        order.requestPayment(UUID.randomUUID().toString());
        return order;
    }

    private Order createOrderWithStatus(OrderStatus status) {
        Order order = new Order(
                new CustomerId("customer-123"),
                new Money(new BigDecimal("100.00"), Currency.getInstance("USD"))
        );
        
        if (status == OrderStatus.PAYMENT_PENDING) {
            order.requestPayment(UUID.randomUUID().toString());
        } else if (status == OrderStatus.PAYMENT_CONFIRMED) {
            order.requestPayment(UUID.randomUUID().toString());
            order.confirmPayment("payment-123");
        } else if (status == OrderStatus.PAYMENT_FAILED) {
            order.requestPayment(UUID.randomUUID().toString());
            order.failPayment("Payment failed");
        }
        return order;
    }
}
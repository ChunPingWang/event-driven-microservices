package com.example.orderservice.application.handler;

import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.application.query.OrderListQuery;
import com.example.orderservice.application.query.OrderListQueryResult;
import com.example.orderservice.application.query.OrderQuery;
import com.example.orderservice.application.query.OrderQueryResult;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 訂單查詢處理器單元測試
 */
@ExtendWith(MockitoExtension.class)
class OrderQueryHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryHandler orderQueryHandler;

    private Order testOrder1;
    private Order testOrder2;

    @BeforeEach
    void setUp() {
        // 創建測試訂單
        CustomerId customerId1 = new CustomerId("CUST-001");
        Money amount1 = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        testOrder1 = new Order(customerId1, amount1);
        testOrder1.requestPayment("TXN-001");

        CustomerId customerId2 = new CustomerId("CUST-001");
        Money amount2 = new Money(new BigDecimal("200.00"), Currency.getInstance("USD"));
        testOrder2 = new Order(customerId2, amount2);
        testOrder2.requestPayment("TXN-002");
        testOrder2.confirmPayment("PAY-002");
    }

    @Test
    void shouldHandleOrderQuerySuccessfully() {
        // Given
        OrderQuery query = OrderQuery.builder()
            .orderId(testOrder1.getOrderId().getValue())
            .build();
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.of(testOrder1));

        // When
        OrderQueryResult result = orderQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(testOrder1.getOrderId().getValue());
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getStatus()).isEqualTo("PAYMENT_PENDING");
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(orderRepository).findById(any(OrderId.class));
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        OrderQuery query = OrderQuery.builder()
            .orderId("NON-EXISTENT-ORDER")
            .build();
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderQueryHandler.handle(query))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Order not found: NON-EXISTENT-ORDER");

        verify(orderRepository).findById(any(OrderId.class));
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsNull() {
        // Given
        OrderQuery query = OrderQuery.builder()
            .orderId(null)
            .build();

        // When & Then
        assertThatThrownBy(() -> orderQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order ID is required");
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsEmpty() {
        // Given
        OrderQuery query = OrderQuery.builder()
            .orderId("")
            .build();

        // When & Then
        assertThatThrownBy(() -> orderQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order ID is required");
    }

    @Test
    void shouldHandleOrderListQueryByCustomerId() {
        // Given
        OrderListQuery query = OrderListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(10)
            .build();
        
        List<Order> orders = Arrays.asList(testOrder1, testOrder2);
        when(orderRepository.findByCustomerId("CUST-001", 0, 10)).thenReturn(orders);
        when(orderRepository.countOrders("CUST-001", null, null, null)).thenReturn(2);

        // When
        OrderListQueryResult result = orderQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrders()).hasSize(2);
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.isHasNext()).isFalse();

        // 驗證第一個訂單
        OrderQueryResult firstOrder = result.getOrders().get(0);
        assertThat(firstOrder.getOrderId()).isEqualTo(testOrder1.getOrderId().getValue());
        assertThat(firstOrder.getCustomerId()).isEqualTo("CUST-001");
        assertThat(firstOrder.getStatus()).isEqualTo("PAYMENT_PENDING");

        // 驗證第二個訂單
        OrderQueryResult secondOrder = result.getOrders().get(1);
        assertThat(secondOrder.getOrderId()).isEqualTo(testOrder2.getOrderId().getValue());
        assertThat(secondOrder.getCustomerId()).isEqualTo("CUST-001");
        assertThat(secondOrder.getStatus()).isEqualTo("PAYMENT_CONFIRMED");

        verify(orderRepository).findByCustomerId("CUST-001", 0, 10);
        verify(orderRepository).countOrders("CUST-001", null, null, null);
    }

    @Test
    void shouldHandleOrderListQueryByStatus() {
        // Given
        OrderListQuery query = OrderListQuery.builder()
            .status("PAYMENT_PENDING")
            .page(0)
            .size(10)
            .build();
        
        List<Order> orders = Arrays.asList(testOrder1);
        when(orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10)).thenReturn(orders);
        when(orderRepository.countOrders(null, OrderStatus.PAYMENT_PENDING, null, null)).thenReturn(1);

        // When
        OrderListQueryResult result = orderQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrders()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.isHasNext()).isFalse();

        OrderQueryResult orderResult = result.getOrders().get(0);
        assertThat(orderResult.getStatus()).isEqualTo("PAYMENT_PENDING");

        verify(orderRepository).findByStatus(OrderStatus.PAYMENT_PENDING, 0, 10);
        verify(orderRepository).countOrders(null, OrderStatus.PAYMENT_PENDING, null, null);
    }

    @Test
    void shouldHandleOrderListQueryByDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        OrderListQuery query = OrderListQuery.builder()
            .startDate(startDate)
            .endDate(endDate)
            .page(0)
            .size(10)
            .build();
        
        List<Order> orders = Arrays.asList(testOrder1, testOrder2);
        when(orderRepository.findByDateRange(startDate, endDate, 0, 10)).thenReturn(orders);
        when(orderRepository.countOrders(null, null, startDate, endDate)).thenReturn(2);

        // When
        OrderListQueryResult result = orderQueryHandler.handle(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrders()).hasSize(2);
        assertThat(result.getTotalCount()).isEqualTo(2);

        verify(orderRepository).findByDateRange(startDate, endDate, 0, 10);
        verify(orderRepository).countOrders(null, null, startDate, endDate);
    }

    @Test
    void shouldCalculateHasNextCorrectly() {
        // Given
        OrderListQuery query = OrderListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(1)
            .build();
        
        List<Order> orders = Arrays.asList(testOrder1);
        when(orderRepository.findByCustomerId("CUST-001", 0, 1)).thenReturn(orders);
        when(orderRepository.countOrders("CUST-001", null, null, null)).thenReturn(2);

        // When
        OrderListQueryResult result = orderQueryHandler.handle(query);

        // Then
        assertThat(result.isHasNext()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenPageIsNegative() {
        // Given
        OrderListQuery query = OrderListQuery.builder()
            .customerId("CUST-001")
            .page(-1)
            .size(10)
            .build();

        // When & Then
        assertThatThrownBy(() -> orderQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page number cannot be negative");
    }

    @Test
    void shouldThrowExceptionWhenSizeIsZero() {
        // Given
        OrderListQuery query = OrderListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(0)
            .build();

        // When & Then
        assertThatThrownBy(() -> orderQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size must be positive");
    }

    @Test
    void shouldThrowExceptionWhenSizeIsNegative() {
        // Given
        OrderListQuery query = OrderListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(-1)
            .build();

        // When & Then
        assertThatThrownBy(() -> orderQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size must be positive");
    }

    @Test
    void shouldThrowExceptionWhenSizeExceedsLimit() {
        // Given
        OrderListQuery query = OrderListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(101)
            .build();

        // When & Then
        assertThatThrownBy(() -> orderQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page size cannot exceed 100");
    }

    @Test
    void shouldThrowExceptionWhenNoCriteriaProvided() {
        // Given
        OrderListQuery query = OrderListQuery.builder()
            .page(0)
            .size(10)
            .build();

        // When & Then
        assertThatThrownBy(() -> orderQueryHandler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one query criteria must be provided");
    }
}
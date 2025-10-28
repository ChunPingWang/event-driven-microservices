package com.example.orderservice.application.service;

import com.example.orderservice.application.command.CreateOrderCommand;
import com.example.orderservice.application.command.OrderCommandResult;
import com.example.orderservice.application.handler.OrderCommandHandler;
import com.example.orderservice.application.handler.OrderQueryHandler;
import com.example.orderservice.application.port.out.DomainEventPublisher;
import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.application.query.OrderListQuery;
import com.example.orderservice.application.query.OrderListQueryResult;
import com.example.orderservice.application.query.OrderQuery;
import com.example.orderservice.application.query.OrderQueryResult;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.event.PaymentConfirmedEvent;
import com.example.orderservice.domain.order.event.PaymentFailedEvent;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import com.example.orderservice.domain.shared.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

/**
 * 訂單應用服務單元測試
 */
@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderCommandHandler commandHandler;

    @Mock
    private OrderQueryHandler queryHandler;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    private CreateOrderCommand createOrderCommand;
    private OrderQuery orderQuery;
    private OrderListQuery orderListQuery;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 準備測試數據
        createOrderCommand = CreateOrderCommand.builder()
            .customerId("CUST-001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .creditCard(CreateOrderCommand.CreditCardInfo.builder()
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("John Doe")
                .build())
            .merchantId("MERCHANT-001")
            .description("Test order")
            .build();

        orderQuery = OrderQuery.builder()
            .orderId("ORDER-001")
            .build();

        orderListQuery = OrderListQuery.builder()
            .customerId("CUST-001")
            .page(0)
            .size(10)
            .build();

        // 創建測試訂單
        CustomerId customerId = new CustomerId("CUST-001");
        Money amount = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        testOrder = new Order(customerId, amount);
        testOrder.requestPayment("TXN-001");
        // 清除初始事件，模擬從數據庫加載的狀態
        testOrder.clearDomainEvents();
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        OrderCommandResult expectedResult = OrderCommandResult.success("ORDER-001", "TXN-001");
        when(commandHandler.handle(createOrderCommand)).thenReturn(expectedResult);

        // When
        OrderCommandResult result = orderApplicationService.createOrder(createOrderCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getTransactionId()).isEqualTo("TXN-001");
        
        verify(commandHandler).handle(createOrderCommand);
    }

    @Test
    void shouldGetOrderSuccessfully() {
        // Given
        OrderQueryResult expectedResult = OrderQueryResult.builder()
            .orderId("ORDER-001")
            .customerId("CUST-001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .status("PAYMENT_PENDING")
            .transactionId("TXN-001")
            .createdAt(LocalDateTime.now())
            .build();
        when(queryHandler.handle(orderQuery)).thenReturn(expectedResult);

        // When
        OrderQueryResult result = orderApplicationService.getOrder(orderQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORDER-001");
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
        
        verify(queryHandler).handle(orderQuery);
    }

    @Test
    void shouldGetOrdersSuccessfully() {
        // Given
        List<OrderQueryResult> orders = Arrays.asList(
            OrderQueryResult.builder()
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status("PAYMENT_PENDING")
                .build(),
            OrderQueryResult.builder()
                .orderId("ORDER-002")
                .customerId("CUST-001")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .status("PAYMENT_CONFIRMED")
                .build()
        );
        
        OrderListQueryResult expectedResult = OrderListQueryResult.builder()
            .orders(orders)
            .totalCount(2)
            .page(0)
            .size(10)
            .hasNext(false)
            .build();
        
        when(queryHandler.handle(orderListQuery)).thenReturn(expectedResult);

        // When
        OrderListQueryResult result = orderApplicationService.getOrders(orderListQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrders()).hasSize(2);
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.isHasNext()).isFalse();
        
        verify(queryHandler).handle(orderListQuery);
    }

    @Test
    void shouldHandlePaymentConfirmationSuccessfully() {
        // Given
        String orderId = testOrder.getOrderId().getValue();
        String paymentId = "PAY-001";
        String transactionId = "TXN-001";
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.of(testOrder));

        // When
        orderApplicationService.handlePaymentConfirmation(orderId, paymentId, transactionId);

        // Then
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
        
        verify(orderRepository).findById(any(OrderId.class));
        verify(orderRepository).save(testOrder);
        verify(domainEventPublisher).publishEvents(any(List.class));
        
        // 驗證事件已被清除
        assertThat(testOrder.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldHandlePaymentFailureSuccessfully() {
        // Given
        String orderId = testOrder.getOrderId().getValue();
        String transactionId = "TXN-001";
        String reason = "Insufficient funds";
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.of(testOrder));

        // When
        orderApplicationService.handlePaymentFailure(orderId, transactionId, reason);

        // Then
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        
        verify(orderRepository).findById(any(OrderId.class));
        verify(orderRepository).save(testOrder);
        verify(domainEventPublisher).publishEvents(any(List.class));
        
        // 驗證事件已被清除
        assertThat(testOrder.getDomainEvents()).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundForPaymentConfirmation() {
        // Given
        String orderId = "NON-EXISTENT-ORDER";
        String paymentId = "PAY-001";
        String transactionId = "TXN-001";
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            orderApplicationService.handlePaymentConfirmation(orderId, paymentId, transactionId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to handle payment confirmation");
        
        verify(orderRepository).findById(any(OrderId.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdMismatchForPaymentConfirmation() {
        // Given
        String orderId = testOrder.getOrderId().getValue();
        String paymentId = "PAY-001";
        String wrongTransactionId = "WRONG-TXN-001";
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> 
            orderApplicationService.handlePaymentConfirmation(orderId, paymentId, wrongTransactionId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to handle payment confirmation");
        
        verify(orderRepository).findById(any(OrderId.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundForPaymentFailure() {
        // Given
        String orderId = "NON-EXISTENT-ORDER";
        String transactionId = "TXN-001";
        String reason = "Insufficient funds";
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            orderApplicationService.handlePaymentFailure(orderId, transactionId, reason))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to handle payment failure");
        
        verify(orderRepository).findById(any(OrderId.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldThrowExceptionWhenTransactionIdMismatchForPaymentFailure() {
        // Given
        String orderId = testOrder.getOrderId().getValue();
        String wrongTransactionId = "WRONG-TXN-001";
        String reason = "Insufficient funds";
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> 
            orderApplicationService.handlePaymentFailure(orderId, wrongTransactionId, reason))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to handle payment failure");
        
        verify(orderRepository).findById(any(OrderId.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(domainEventPublisher, never()).publishEvents(any(List.class));
    }

    @Test
    void shouldClearDomainEventsAfterPublishing() {
        // Given
        String orderId = testOrder.getOrderId().getValue();
        String paymentId = "PAY-001";
        String transactionId = "TXN-001";
        
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.of(testOrder));

        // When
        orderApplicationService.handlePaymentConfirmation(orderId, paymentId, transactionId);

        // Then
        verify(domainEventPublisher).publishEvents(any(List.class));
        
        // 驗證事件已被清除
        assertThat(testOrder.getDomainEvents()).isEmpty();
    }
}
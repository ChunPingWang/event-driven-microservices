package com.example.orderservice.domain.order;

import com.example.orderservice.domain.order.event.PaymentConfirmedEvent;
import com.example.orderservice.domain.order.event.PaymentFailedEvent;
import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import com.example.orderservice.domain.shared.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 訂單聚合根單元測試
 */
class OrderTest {
    
    private CustomerId customerId;
    private Money amount;
    
    @BeforeEach
    void setUp() {
        customerId = CustomerId.of("CUSTOMER-123");
        amount = Money.usd(new BigDecimal("100.00"));
    }
    
    @Test
    void shouldCreateOrderWithValidParameters() {
        // When
        Order order = new Order(customerId, amount);
        
        // Then
        assertNotNull(order.getOrderId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(amount, order.getAmount());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
        assertNull(order.getTransactionId());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingOrderWithNullCustomerId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(null, amount)
        );
        assertEquals("Customer ID cannot be null", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingOrderWithNullAmount() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Order(customerId, null)
        );
        assertEquals("Amount cannot be null", exception.getMessage());
    }
    
    @Test
    void shouldRequestPaymentSuccessfully() {
        // Given
        Order order = new Order(customerId, amount);
        String transactionId = "TXN-123";
        
        // When
        order.requestPayment(transactionId);
        
        // Then
        assertEquals(OrderStatus.PAYMENT_PENDING, order.getStatus());
        assertEquals(transactionId, order.getTransactionId());
        
        // 驗證領域事件
        List<DomainEvent> events = order.getDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof PaymentRequestedEvent);
        
        PaymentRequestedEvent event = (PaymentRequestedEvent) events.get(0);
        assertEquals(order.getOrderId(), event.getOrderId());
        assertEquals(transactionId, event.getTransactionId());
        assertEquals(customerId.getValue(), event.getCustomerId());
    }
    
    @Test
    void shouldThrowExceptionWhenRequestingPaymentWithInvalidStatus() {
        // Given
        Order order = new Order(customerId, amount);
        order.requestPayment("TXN-123");
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> order.requestPayment("TXN-456")
        );
        assertTrue(exception.getMessage().contains("Cannot request payment for order in status"));
    }
    
    @Test
    void shouldThrowExceptionWhenRequestingPaymentWithNullTransactionId() {
        // Given
        Order order = new Order(customerId, amount);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> order.requestPayment(null)
        );
        assertEquals("Transaction ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenRequestingPaymentWithEmptyTransactionId() {
        // Given
        Order order = new Order(customerId, amount);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> order.requestPayment("   ")
        );
        assertEquals("Transaction ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldConfirmPaymentSuccessfully() {
        // Given
        Order order = new Order(customerId, amount);
        order.requestPayment("TXN-123");
        String paymentId = "PAY-456";
        
        // When
        order.confirmPayment(paymentId);
        
        // Then
        assertEquals(OrderStatus.PAYMENT_CONFIRMED, order.getStatus());
        
        // 驗證領域事件
        List<DomainEvent> events = order.getDomainEvents();
        assertEquals(2, events.size()); // PaymentRequestedEvent + PaymentConfirmedEvent
        assertTrue(events.get(1) instanceof PaymentConfirmedEvent);
        
        PaymentConfirmedEvent event = (PaymentConfirmedEvent) events.get(1);
        assertEquals(order.getOrderId(), event.getOrderId());
        assertEquals("TXN-123", event.getTransactionId());
        assertEquals(paymentId, event.getPaymentId());
    }
    
    @Test
    void shouldThrowExceptionWhenConfirmingPaymentWithInvalidStatus() {
        // Given
        Order order = new Order(customerId, amount);
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> order.confirmPayment("PAY-123")
        );
        assertTrue(exception.getMessage().contains("Cannot confirm payment for order in status"));
    }
    
    @Test
    void shouldFailPaymentSuccessfully() {
        // Given
        Order order = new Order(customerId, amount);
        order.requestPayment("TXN-123");
        String reason = "Insufficient funds";
        
        // When
        order.failPayment(reason);
        
        // Then
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        
        // 驗證領域事件
        List<DomainEvent> events = order.getDomainEvents();
        assertEquals(2, events.size());
        assertTrue(events.get(1) instanceof PaymentFailedEvent);
        
        PaymentFailedEvent event = (PaymentFailedEvent) events.get(1);
        assertEquals(order.getOrderId(), event.getOrderId());
        assertEquals("TXN-123", event.getTransactionId());
        assertEquals(reason, event.getReason());
    }
    
    @Test
    void shouldCancelOrderWhenInCreatedStatus() {
        // Given
        Order order = new Order(customerId, amount);
        
        // When
        order.cancel();
        
        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
    
    @Test
    void shouldCancelOrderWhenInPaymentFailedStatus() {
        // Given
        Order order = new Order(customerId, amount);
        order.requestPayment("TXN-123");
        order.failPayment("Payment failed");
        
        // When
        order.cancel();
        
        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
    
    @Test
    void shouldThrowExceptionWhenCancellingOrderWithInvalidStatus() {
        // Given
        Order order = new Order(customerId, amount);
        order.requestPayment("TXN-123");
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            order::cancel
        );
        assertTrue(exception.getMessage().contains("Cannot cancel order in status"));
    }
    
    @Test
    void shouldRetryPaymentWhenInFailedStatus() {
        // Given
        Order order = new Order(customerId, amount);
        order.requestPayment("TXN-123");
        order.failPayment("Payment failed");
        
        // When
        order.retryPayment("TXN-456");
        
        // Then
        assertEquals(OrderStatus.PAYMENT_PENDING, order.getStatus());
        assertEquals("TXN-456", order.getTransactionId());
        assertTrue(order.getDomainEvents().size() >= 3); // Original events + new PaymentRequestedEvent
    }
    
    @Test
    void shouldReturnTrueWhenCanRetryPayment() {
        // Given
        Order order = new Order(customerId, amount);
        order.requestPayment("TXN-123");
        order.failPayment("Payment failed");
        
        // When & Then
        assertTrue(order.canRetryPayment());
    }
    
    @Test
    void shouldReturnFalseWhenCannotRetryPayment() {
        // Given
        Order order = new Order(customerId, amount);
        
        // When & Then
        assertFalse(order.canRetryPayment());
    }
    
    @Test
    void shouldUpdateTimestampWhenStatusChanges() {
        // Given
        Order order = new Order(customerId, amount);
        LocalDateTime initialUpdatedAt = order.getUpdatedAt();
        
        // When
        try {
            Thread.sleep(1); // 確保時間戳不同
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        order.requestPayment("TXN-123");
        
        // Then
        assertTrue(order.getUpdatedAt().isAfter(initialUpdatedAt));
    }
    
    @Test
    void shouldHaveCorrectToStringRepresentation() {
        // Given
        Order order = new Order(customerId, amount);
        
        // When
        String toString = order.toString();
        
        // Then
        assertTrue(toString.contains("Order{"));
        assertTrue(toString.contains("orderId="));
        assertTrue(toString.contains("customerId="));
        assertTrue(toString.contains("amount="));
        assertTrue(toString.contains("status="));
    }
}
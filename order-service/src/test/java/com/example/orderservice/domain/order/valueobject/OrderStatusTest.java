package com.example.orderservice.domain.order.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 訂單狀態枚舉單元測試
 */
class OrderStatusTest {
    
    @Test
    void shouldAllowRequestPaymentWhenStatusIsCreated() {
        // Given
        OrderStatus status = OrderStatus.CREATED;
        
        // When & Then
        assertTrue(status.canRequestPayment());
    }
    
    @Test
    void shouldAllowRequestPaymentWhenStatusIsPaymentFailed() {
        // Given
        OrderStatus status = OrderStatus.PAYMENT_FAILED;
        
        // When & Then
        assertTrue(status.canRequestPayment());
    }
    
    @Test
    void shouldNotAllowRequestPaymentWhenStatusIsNotCreatedOrPaymentFailed() {
        // Given & When & Then
        assertFalse(OrderStatus.PAYMENT_PENDING.canRequestPayment());
        assertFalse(OrderStatus.PAYMENT_CONFIRMED.canRequestPayment());
        assertFalse(OrderStatus.CANCELLED.canRequestPayment());
    }
    
    @Test
    void shouldAllowConfirmPaymentWhenStatusIsPaymentPending() {
        // Given
        OrderStatus status = OrderStatus.PAYMENT_PENDING;
        
        // When & Then
        assertTrue(status.canConfirmPayment());
    }
    
    @Test
    void shouldNotAllowConfirmPaymentWhenStatusIsNotPaymentPending() {
        // Given & When & Then
        assertFalse(OrderStatus.CREATED.canConfirmPayment());
        assertFalse(OrderStatus.PAYMENT_CONFIRMED.canConfirmPayment());
        assertFalse(OrderStatus.PAYMENT_FAILED.canConfirmPayment());
        assertFalse(OrderStatus.CANCELLED.canConfirmPayment());
    }
    
    @Test
    void shouldAllowFailPaymentWhenStatusIsPaymentPending() {
        // Given
        OrderStatus status = OrderStatus.PAYMENT_PENDING;
        
        // When & Then
        assertTrue(status.canFailPayment());
    }
    
    @Test
    void shouldNotAllowFailPaymentWhenStatusIsNotPaymentPending() {
        // Given & When & Then
        assertFalse(OrderStatus.CREATED.canFailPayment());
        assertFalse(OrderStatus.PAYMENT_CONFIRMED.canFailPayment());
        assertFalse(OrderStatus.PAYMENT_FAILED.canFailPayment());
        assertFalse(OrderStatus.CANCELLED.canFailPayment());
    }
    
    @Test
    void shouldAllowCancelWhenStatusIsCreated() {
        // Given
        OrderStatus status = OrderStatus.CREATED;
        
        // When & Then
        assertTrue(status.canCancel());
    }
    
    @Test
    void shouldAllowCancelWhenStatusIsPaymentFailed() {
        // Given
        OrderStatus status = OrderStatus.PAYMENT_FAILED;
        
        // When & Then
        assertTrue(status.canCancel());
    }
    
    @Test
    void shouldNotAllowCancelWhenStatusIsPaymentPending() {
        // Given
        OrderStatus status = OrderStatus.PAYMENT_PENDING;
        
        // When & Then
        assertFalse(status.canCancel());
    }
    
    @Test
    void shouldNotAllowCancelWhenStatusIsPaymentConfirmed() {
        // Given
        OrderStatus status = OrderStatus.PAYMENT_CONFIRMED;
        
        // When & Then
        assertFalse(status.canCancel());
    }
    
    @Test
    void shouldNotAllowCancelWhenStatusIsAlreadyCancelled() {
        // Given
        OrderStatus status = OrderStatus.CANCELLED;
        
        // When & Then
        assertFalse(status.canCancel());
    }
    
    @Test
    void shouldReturnTrueWhenOrderIsCompleted() {
        // Given & When & Then
        assertTrue(OrderStatus.PAYMENT_CONFIRMED.isCompleted());
        assertTrue(OrderStatus.PAYMENT_FAILED.isCompleted());
        assertTrue(OrderStatus.CANCELLED.isCompleted());
    }
    
    @Test
    void shouldReturnFalseWhenOrderIsNotCompleted() {
        // Given & When & Then
        assertFalse(OrderStatus.CREATED.isCompleted());
        assertFalse(OrderStatus.PAYMENT_PENDING.isCompleted());
    }
    
    @Test
    void shouldHaveCorrectEnumValues() {
        // Given
        OrderStatus[] expectedValues = {
            OrderStatus.CREATED,
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.PAYMENT_CONFIRMED,
            OrderStatus.PAYMENT_FAILED,
            OrderStatus.CANCELLED
        };
        
        // When
        OrderStatus[] actualValues = OrderStatus.values();
        
        // Then
        assertArrayEquals(expectedValues, actualValues);
    }
    
    @Test
    void shouldConvertFromStringCorrectly() {
        // Given & When & Then
        assertEquals(OrderStatus.CREATED, OrderStatus.valueOf("CREATED"));
        assertEquals(OrderStatus.PAYMENT_PENDING, OrderStatus.valueOf("PAYMENT_PENDING"));
        assertEquals(OrderStatus.PAYMENT_CONFIRMED, OrderStatus.valueOf("PAYMENT_CONFIRMED"));
        assertEquals(OrderStatus.PAYMENT_FAILED, OrderStatus.valueOf("PAYMENT_FAILED"));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.valueOf("CANCELLED"));
    }
}
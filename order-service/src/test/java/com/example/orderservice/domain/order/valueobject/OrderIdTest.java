package com.example.orderservice.domain.order.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 訂單ID值對象單元測試
 */
class OrderIdTest {
    
    @Test
    void shouldCreateOrderIdWithValidValue() {
        // Given
        String value = "ORDER-123";
        
        // When
        OrderId orderId = new OrderId(value);
        
        // Then
        assertEquals(value, orderId.getValue());
    }
    
    @Test
    void shouldTrimWhitespaceWhenCreatingOrderId() {
        // Given
        String value = "  ORDER-123  ";
        
        // When
        OrderId orderId = new OrderId(value);
        
        // Then
        assertEquals("ORDER-123", orderId.getValue());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingOrderIdWithNullValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new OrderId(null)
        );
        assertEquals("Order ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingOrderIdWithEmptyValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new OrderId("")
        );
        assertEquals("Order ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingOrderIdWithWhitespaceOnlyValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new OrderId("   ")
        );
        assertEquals("Order ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldGenerateUniqueOrderIds() {
        // When
        OrderId orderId1 = OrderId.generate();
        OrderId orderId2 = OrderId.generate();
        
        // Then
        assertNotNull(orderId1);
        assertNotNull(orderId2);
        assertNotEquals(orderId1, orderId2);
        assertTrue(orderId1.getValue().startsWith("ORDER-"));
        assertTrue(orderId2.getValue().startsWith("ORDER-"));
    }
    
    @Test
    void shouldCreateOrderIdFromString() {
        // Given
        String value = "ORDER-456";
        
        // When
        OrderId orderId = OrderId.of(value);
        
        // Then
        assertEquals(value, orderId.getValue());
    }
    
    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        String value = "ORDER-123";
        OrderId orderId1 = new OrderId(value);
        OrderId orderId2 = new OrderId(value);
        
        // When & Then
        assertEquals(orderId1, orderId2);
        assertEquals(orderId1.hashCode(), orderId2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        OrderId orderId1 = new OrderId("ORDER-123");
        OrderId orderId2 = new OrderId("ORDER-456");
        
        // When & Then
        assertNotEquals(orderId1, orderId2);
    }
    
    @Test
    void shouldNotBeEqualToNull() {
        // Given
        OrderId orderId = new OrderId("ORDER-123");
        
        // When & Then
        assertNotEquals(orderId, null);
    }
    
    @Test
    void shouldNotBeEqualToDifferentType() {
        // Given
        OrderId orderId = new OrderId("ORDER-123");
        String string = "ORDER-123";
        
        // When & Then
        assertNotEquals(orderId, string);
    }
    
    @Test
    void shouldBeEqualToItself() {
        // Given
        OrderId orderId = new OrderId("ORDER-123");
        
        // When & Then
        assertEquals(orderId, orderId);
    }
    
    @Test
    void shouldHaveCorrectToStringRepresentation() {
        // Given
        String value = "ORDER-123";
        OrderId orderId = new OrderId(value);
        
        // When
        String toString = orderId.toString();
        
        // Then
        assertEquals(value, toString);
    }
}
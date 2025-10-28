package com.example.orderservice.domain.order.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客戶ID值對象單元測試
 */
class CustomerIdTest {
    
    @Test
    void shouldCreateCustomerIdWithValidValue() {
        // Given
        String value = "CUSTOMER-123";
        
        // When
        CustomerId customerId = new CustomerId(value);
        
        // Then
        assertEquals(value, customerId.getValue());
    }
    
    @Test
    void shouldTrimWhitespaceWhenCreatingCustomerId() {
        // Given
        String value = "  CUSTOMER-123  ";
        
        // When
        CustomerId customerId = new CustomerId(value);
        
        // Then
        assertEquals("CUSTOMER-123", customerId.getValue());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCustomerIdWithNullValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CustomerId(null)
        );
        assertEquals("Customer ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCustomerIdWithEmptyValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CustomerId("")
        );
        assertEquals("Customer ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCustomerIdWithWhitespaceOnlyValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CustomerId("   ")
        );
        assertEquals("Customer ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldCreateCustomerIdFromString() {
        // Given
        String value = "CUSTOMER-456";
        
        // When
        CustomerId customerId = CustomerId.of(value);
        
        // Then
        assertEquals(value, customerId.getValue());
    }
    
    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        String value = "CUSTOMER-123";
        CustomerId customerId1 = new CustomerId(value);
        CustomerId customerId2 = new CustomerId(value);
        
        // When & Then
        assertEquals(customerId1, customerId2);
        assertEquals(customerId1.hashCode(), customerId2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        CustomerId customerId1 = new CustomerId("CUSTOMER-123");
        CustomerId customerId2 = new CustomerId("CUSTOMER-456");
        
        // When & Then
        assertNotEquals(customerId1, customerId2);
    }
    
    @Test
    void shouldNotBeEqualToNull() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-123");
        
        // When & Then
        assertNotEquals(customerId, null);
    }
    
    @Test
    void shouldNotBeEqualToDifferentType() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-123");
        String string = "CUSTOMER-123";
        
        // When & Then
        assertNotEquals(customerId, string);
    }
    
    @Test
    void shouldBeEqualToItself() {
        // Given
        CustomerId customerId = new CustomerId("CUSTOMER-123");
        
        // When & Then
        assertEquals(customerId, customerId);
    }
    
    @Test
    void shouldHaveCorrectToStringRepresentation() {
        // Given
        String value = "CUSTOMER-123";
        CustomerId customerId = new CustomerId(value);
        
        // When
        String toString = customerId.toString();
        
        // Then
        assertEquals(value, toString);
    }
}
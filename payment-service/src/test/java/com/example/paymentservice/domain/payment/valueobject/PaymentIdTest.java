package com.example.paymentservice.domain.payment.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付ID值對象單元測試
 */
class PaymentIdTest {
    
    @Test
    void shouldCreatePaymentIdWithValidValue() {
        // Given
        String value = "PAY-123";
        
        // When
        PaymentId paymentId = new PaymentId(value);
        
        // Then
        assertEquals(value, paymentId.getValue());
    }
    
    @Test
    void shouldTrimWhitespaceWhenCreatingPaymentId() {
        // Given
        String value = "  PAY-123  ";
        
        // When
        PaymentId paymentId = new PaymentId(value);
        
        // Then
        assertEquals("PAY-123", paymentId.getValue());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingPaymentIdWithNullValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PaymentId(null)
        );
        assertEquals("Payment ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingPaymentIdWithEmptyValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PaymentId("")
        );
        assertEquals("Payment ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingPaymentIdWithWhitespaceOnlyValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PaymentId("   ")
        );
        assertEquals("Payment ID cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldGenerateUniquePaymentIds() {
        // When
        PaymentId paymentId1 = PaymentId.generate();
        PaymentId paymentId2 = PaymentId.generate();
        
        // Then
        assertNotNull(paymentId1);
        assertNotNull(paymentId2);
        assertNotEquals(paymentId1, paymentId2);
        assertTrue(paymentId1.getValue().startsWith("PAY-"));
        assertTrue(paymentId2.getValue().startsWith("PAY-"));
    }
    
    @Test
    void shouldCreatePaymentIdFromString() {
        // Given
        String value = "PAY-456";
        
        // When
        PaymentId paymentId = PaymentId.of(value);
        
        // Then
        assertEquals(value, paymentId.getValue());
    }
    
    @Test
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        String value = "PAY-123";
        PaymentId paymentId1 = new PaymentId(value);
        PaymentId paymentId2 = new PaymentId(value);
        
        // When & Then
        assertEquals(paymentId1, paymentId2);
        assertEquals(paymentId1.hashCode(), paymentId2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenValuesAreDifferent() {
        // Given
        PaymentId paymentId1 = new PaymentId("PAY-123");
        PaymentId paymentId2 = new PaymentId("PAY-456");
        
        // When & Then
        assertNotEquals(paymentId1, paymentId2);
    }
    
    @Test
    void shouldNotBeEqualToNull() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-123");
        
        // When & Then
        assertNotEquals(paymentId, null);
    }
    
    @Test
    void shouldNotBeEqualToDifferentType() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-123");
        String string = "PAY-123";
        
        // When & Then
        assertNotEquals(paymentId, string);
    }
    
    @Test
    void shouldBeEqualToItself() {
        // Given
        PaymentId paymentId = new PaymentId("PAY-123");
        
        // When & Then
        assertEquals(paymentId, paymentId);
    }
    
    @Test
    void shouldHaveCorrectToStringRepresentation() {
        // Given
        String value = "PAY-123";
        PaymentId paymentId = new PaymentId(value);
        
        // When
        String toString = paymentId.toString();
        
        // Then
        assertEquals(value, toString);
    }
}
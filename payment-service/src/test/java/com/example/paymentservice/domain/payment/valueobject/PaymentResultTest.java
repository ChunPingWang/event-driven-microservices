package com.example.paymentservice.domain.payment.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付結果值對象單元測試
 */
class PaymentResultTest {
    
    @Test
    void shouldCreateSuccessfulPaymentResult() {
        // Given
        String paymentId = "PAY-123";
        
        // When
        PaymentResult result = PaymentResult.success(paymentId);
        
        // Then
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(paymentId, result.getPaymentId());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingSuccessfulResultWithNullPaymentId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PaymentResult.success(null)
        );
        assertEquals("Payment ID cannot be null or empty for successful result", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingSuccessfulResultWithEmptyPaymentId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PaymentResult.success("")
        );
        assertEquals("Payment ID cannot be null or empty for successful result", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingSuccessfulResultWithWhitespaceOnlyPaymentId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PaymentResult.success("   ")
        );
        assertEquals("Payment ID cannot be null or empty for successful result", exception.getMessage());
    }
    
    @Test
    void shouldCreateFailedPaymentResult() {
        // Given
        String errorMessage = "Insufficient funds";
        
        // When
        PaymentResult result = PaymentResult.failed(errorMessage);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertNull(result.getPaymentId());
        assertEquals(errorMessage, result.getErrorMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingFailedResultWithNullErrorMessage() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PaymentResult.failed(null)
        );
        assertEquals("Error message cannot be null or empty for failed result", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingFailedResultWithEmptyErrorMessage() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PaymentResult.failed("")
        );
        assertEquals("Error message cannot be null or empty for failed result", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingFailedResultWithWhitespaceOnlyErrorMessage() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PaymentResult.failed("   ")
        );
        assertEquals("Error message cannot be null or empty for failed result", exception.getMessage());
    }
    
    @Test
    void shouldBeEqualWhenBothAreSuccessfulWithSamePaymentId() {
        // Given
        String paymentId = "PAY-123";
        PaymentResult result1 = PaymentResult.success(paymentId);
        PaymentResult result2 = PaymentResult.success(paymentId);
        
        // When & Then
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenSuccessfulWithDifferentPaymentIds() {
        // Given
        PaymentResult result1 = PaymentResult.success("PAY-123");
        PaymentResult result2 = PaymentResult.success("PAY-456");
        
        // When & Then
        assertNotEquals(result1, result2);
    }
    
    @Test
    void shouldBeEqualWhenBothAreFailedWithSameErrorMessage() {
        // Given
        String errorMessage = "Insufficient funds";
        PaymentResult result1 = PaymentResult.failed(errorMessage);
        PaymentResult result2 = PaymentResult.failed(errorMessage);
        
        // When & Then
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenFailedWithDifferentErrorMessages() {
        // Given
        PaymentResult result1 = PaymentResult.failed("Insufficient funds");
        PaymentResult result2 = PaymentResult.failed("Card expired");
        
        // When & Then
        assertNotEquals(result1, result2);
    }
    
    @Test
    void shouldNotBeEqualWhenOneIsSuccessfulAndOtherIsFailed() {
        // Given
        PaymentResult successResult = PaymentResult.success("PAY-123");
        PaymentResult failedResult = PaymentResult.failed("Error");
        
        // When & Then
        assertNotEquals(successResult, failedResult);
    }
    
    @Test
    void shouldNotBeEqualToNull() {
        // Given
        PaymentResult result = PaymentResult.success("PAY-123");
        
        // When & Then
        assertNotEquals(result, null);
    }
    
    @Test
    void shouldNotBeEqualToDifferentType() {
        // Given
        PaymentResult result = PaymentResult.success("PAY-123");
        String string = "PAY-123";
        
        // When & Then
        assertNotEquals(result, string);
    }
    
    @Test
    void shouldBeEqualToItself() {
        // Given
        PaymentResult result = PaymentResult.success("PAY-123");
        
        // When & Then
        assertEquals(result, result);
    }
    
    @Test
    void shouldHaveCorrectToStringRepresentationForSuccessfulResult() {
        // Given
        String paymentId = "PAY-123";
        PaymentResult result = PaymentResult.success(paymentId);
        
        // When
        String toString = result.toString();
        
        // Then
        assertTrue(toString.contains("PaymentResult{success=true"));
        assertTrue(toString.contains("paymentId='PAY-123'"));
    }
    
    @Test
    void shouldHaveCorrectToStringRepresentationForFailedResult() {
        // Given
        String errorMessage = "Insufficient funds";
        PaymentResult result = PaymentResult.failed(errorMessage);
        
        // When
        String toString = result.toString();
        
        // Then
        assertTrue(toString.contains("PaymentResult{success=false"));
        assertTrue(toString.contains("errorMessage='Insufficient funds'"));
    }
}
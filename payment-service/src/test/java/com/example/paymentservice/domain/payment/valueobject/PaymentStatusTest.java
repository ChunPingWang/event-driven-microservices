package com.example.paymentservice.domain.payment.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付狀態枚舉單元測試
 */
class PaymentStatusTest {
    
    @Test
    void shouldReturnTrueWhenStatusIsFinal() {
        // Given & When & Then
        assertTrue(PaymentStatus.SUCCESS.isFinalStatus());
        assertTrue(PaymentStatus.FAILED.isFinalStatus());
        assertTrue(PaymentStatus.REFUNDED.isFinalStatus());
    }
    
    @Test
    void shouldReturnFalseWhenStatusIsNotFinal() {
        // Given & When & Then
        assertFalse(PaymentStatus.PROCESSING.isFinalStatus());
    }
    
    @Test
    void shouldAllowRefundWhenStatusIsSuccess() {
        // Given
        PaymentStatus status = PaymentStatus.SUCCESS;
        
        // When & Then
        assertTrue(status.canRefund());
    }
    
    @Test
    void shouldNotAllowRefundWhenStatusIsNotSuccess() {
        // Given & When & Then
        assertFalse(PaymentStatus.PROCESSING.canRefund());
        assertFalse(PaymentStatus.FAILED.canRefund());
        assertFalse(PaymentStatus.REFUNDED.canRefund());
    }
    
    @Test
    void shouldReturnTrueWhenPaymentIsSuccessful() {
        // Given
        PaymentStatus status = PaymentStatus.SUCCESS;
        
        // When & Then
        assertTrue(status.isSuccessful());
    }
    
    @Test
    void shouldReturnFalseWhenPaymentIsNotSuccessful() {
        // Given & When & Then
        assertFalse(PaymentStatus.PROCESSING.isSuccessful());
        assertFalse(PaymentStatus.FAILED.isSuccessful());
        assertFalse(PaymentStatus.REFUNDED.isSuccessful());
    }
    
    @Test
    void shouldReturnTrueWhenPaymentIsFailed() {
        // Given
        PaymentStatus status = PaymentStatus.FAILED;
        
        // When & Then
        assertTrue(status.isFailed());
    }
    
    @Test
    void shouldReturnFalseWhenPaymentIsNotFailed() {
        // Given & When & Then
        assertFalse(PaymentStatus.PROCESSING.isFailed());
        assertFalse(PaymentStatus.SUCCESS.isFailed());
        assertFalse(PaymentStatus.REFUNDED.isFailed());
    }
    
    @Test
    void shouldHaveCorrectEnumValues() {
        // Given
        PaymentStatus[] expectedValues = {
            PaymentStatus.PROCESSING,
            PaymentStatus.SUCCESS,
            PaymentStatus.FAILED,
            PaymentStatus.REFUNDED
        };
        
        // When
        PaymentStatus[] actualValues = PaymentStatus.values();
        
        // Then
        assertArrayEquals(expectedValues, actualValues);
    }
    
    @Test
    void shouldConvertFromStringCorrectly() {
        // Given & When & Then
        assertEquals(PaymentStatus.PROCESSING, PaymentStatus.valueOf("PROCESSING"));
        assertEquals(PaymentStatus.SUCCESS, PaymentStatus.valueOf("SUCCESS"));
        assertEquals(PaymentStatus.FAILED, PaymentStatus.valueOf("FAILED"));
        assertEquals(PaymentStatus.REFUNDED, PaymentStatus.valueOf("REFUNDED"));
    }
}
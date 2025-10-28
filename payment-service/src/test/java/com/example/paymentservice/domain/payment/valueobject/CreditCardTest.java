package com.example.paymentservice.domain.payment.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 信用卡值對象單元測試
 */
class CreditCardTest {
    
    @Test
    void shouldCreateCreditCardWithValidParameters() {
        // Given
        String cardNumber = "4111111111111111";
        String expiryDate = "12/25";
        String cvv = "123";
        String cardHolderName = "John Doe";
        
        // When
        CreditCard creditCard = new CreditCard(cardNumber, expiryDate, cvv, cardHolderName);
        
        // Then
        assertEquals("**** **** **** 1111", creditCard.getCardNumber());
        assertEquals(expiryDate, creditCard.getExpiryDate());
        assertEquals(cvv, creditCard.getCvv());
        assertEquals("JOHN DOE", creditCard.getCardHolderName());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithNullCardNumber() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard(null, "12/25", "123", "John Doe")
        );
        assertEquals("Card number cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithEmptyCardNumber() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("", "12/25", "123", "John Doe")
        );
        assertEquals("Card number cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithInvalidCardNumberFormat() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("123", "12/25", "123", "John Doe")
        );
        assertEquals("Invalid card number format", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithInvalidLuhnCheck() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111112", "12/25", "123", "John Doe")
        );
        assertEquals("Invalid card number - failed Luhn check", exception.getMessage());
    }
    
    @Test
    void shouldAcceptValidVisaCardNumber() {
        // Given
        String visaCardNumber = "4111111111111111";
        
        // When
        CreditCard creditCard = new CreditCard(visaCardNumber, "12/25", "123", "John Doe");
        
        // Then
        assertEquals("VISA", creditCard.getBrand());
    }
    
    @Test
    void shouldAcceptValidMasterCardNumber() {
        // Given
        String masterCardNumber = "5555555555554444";
        
        // When
        CreditCard creditCard = new CreditCard(masterCardNumber, "12/25", "123", "John Doe");
        
        // Then
        assertEquals("MASTERCARD", creditCard.getBrand());
    }
    
    @Test
    void shouldAcceptValidAmexCardNumber() {
        // Given
        String amexCardNumber = "378282246310005";
        
        // When
        CreditCard creditCard = new CreditCard(amexCardNumber, "12/25", "1234", "John Doe");
        
        // Then
        assertEquals("AMEX", creditCard.getBrand());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithNullExpiryDate() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111111", null, "123", "John Doe")
        );
        assertEquals("Expiry date cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithInvalidExpiryDateFormat() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111111", "1225", "123", "John Doe")
        );
        assertEquals("Invalid expiry date format. Expected MM/YY", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithExpiredDate() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111111", "01/20", "123", "John Doe")
        );
        assertEquals("Card has expired", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithNullCvv() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111111", "12/25", null, "John Doe")
        );
        assertEquals("CVV cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithInvalidCvvFormat() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111111", "12/25", "12", "John Doe")
        );
        assertEquals("Invalid CVV format", exception.getMessage());
    }
    
    @Test
    void shouldAcceptFourDigitCvvForAmex() {
        // Given
        String amexCardNumber = "378282246310005";
        String fourDigitCvv = "1234";
        
        // When
        CreditCard creditCard = new CreditCard(amexCardNumber, "12/25", fourDigitCvv, "John Doe");
        
        // Then
        assertEquals(fourDigitCvv, creditCard.getCvv());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithNullCardHolderName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111111", "12/25", "123", null)
        );
        assertEquals("Card holder name cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithTooShortCardHolderName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111111", "12/25", "123", "A")
        );
        assertEquals("Card holder name too short", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingCreditCardWithTooLongCardHolderName() {
        // Given
        String longName = "A".repeat(51);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CreditCard("4111111111111111", "12/25", "123", longName)
        );
        assertEquals("Card holder name too long", exception.getMessage());
    }
    
    @Test
    void shouldTrimAndUppercaseCardHolderName() {
        // Given
        String cardHolderName = "  john doe  ";
        
        // When
        CreditCard creditCard = new CreditCard("4111111111111111", "12/25", "123", cardHolderName);
        
        // Then
        assertEquals("JOHN DOE", creditCard.getCardHolderName());
    }
    
    @Test
    void shouldMaskCardNumberCorrectly() {
        // Given
        String cardNumber = "4111111111111111";
        
        // When
        CreditCard creditCard = new CreditCard(cardNumber, "12/25", "123", "John Doe");
        
        // Then
        assertEquals("**** **** **** 1111", creditCard.getCardNumber());
    }
    
    @Test
    void shouldHandleCardNumberWithSpaces() {
        // Given
        String cardNumberWithSpaces = "4111 1111 1111 1111";
        
        // When
        CreditCard creditCard = new CreditCard(cardNumberWithSpaces, "12/25", "123", "John Doe");
        
        // Then
        assertEquals("**** **** **** 1111", creditCard.getCardNumber());
    }
    
    @Test
    void shouldReturnFalseWhenCardIsNotExpired() {
        // Given
        CreditCard creditCard = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        
        // When & Then
        // The card should not be expired since it expires in 2025
        assertFalse(creditCard.isExpired());
    }
    
    @Test
    void shouldReturnCorrectBrandForVisa() {
        // Given
        CreditCard creditCard = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        
        // When & Then
        assertEquals("VISA", creditCard.getBrand());
    }
    
    @Test
    void shouldReturnCorrectBrandForMasterCard() {
        // Given
        CreditCard creditCard = new CreditCard("5555555555554444", "12/25", "123", "John Doe");
        
        // When & Then
        assertEquals("MASTERCARD", creditCard.getBrand());
    }
    
    @Test
    void shouldReturnCorrectBrandForAmex() {
        // Given
        CreditCard creditCard = new CreditCard("378282246310005", "12/25", "1234", "John Doe");
        
        // When & Then
        assertEquals("AMEX", creditCard.getBrand());
    }
    
    @Test
    void shouldBeEqualWhenAllFieldsAreEqual() {
        // Given
        CreditCard creditCard1 = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        CreditCard creditCard2 = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        
        // When & Then
        assertEquals(creditCard1, creditCard2);
        assertEquals(creditCard1.hashCode(), creditCard2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualWhenCardNumbersDiffer() {
        // Given
        CreditCard creditCard1 = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        CreditCard creditCard2 = new CreditCard("5555555555554444", "12/25", "123", "John Doe");
        
        // When & Then
        assertNotEquals(creditCard1, creditCard2);
    }
    
    @Test
    void shouldNotBeEqualWhenExpiryDatesDiffer() {
        // Given
        CreditCard creditCard1 = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        CreditCard creditCard2 = new CreditCard("4111111111111111", "01/26", "123", "John Doe");
        
        // When & Then
        assertNotEquals(creditCard1, creditCard2);
    }
    
    @Test
    void shouldNotBeEqualWhenCardHolderNamesDiffer() {
        // Given
        CreditCard creditCard1 = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        CreditCard creditCard2 = new CreditCard("4111111111111111", "12/25", "123", "Jane Smith");
        
        // When & Then
        assertNotEquals(creditCard1, creditCard2);
    }
    
    @Test
    void shouldHaveCorrectToStringRepresentation() {
        // Given
        CreditCard creditCard = new CreditCard("4111111111111111", "12/25", "123", "John Doe");
        
        // When
        String toString = creditCard.toString();
        
        // Then
        assertTrue(toString.contains("CreditCard{"));
        assertTrue(toString.contains("cardNumber="));
        assertTrue(toString.contains("expiryDate="));
        assertTrue(toString.contains("cardHolderName="));
    }
}
package com.example.orderservice.web.exception;

/**
 * 無效訂單狀態異常
 */
public class InvalidOrderStateException extends RuntimeException {
    
    public InvalidOrderStateException(String message) {
        super(message);
    }
    
    public InvalidOrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.example.orderservice.web.exception;

/**
 * 訂單未找到異常
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(String orderId) {
        super("Order not found with ID: " + orderId);
    }
    
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
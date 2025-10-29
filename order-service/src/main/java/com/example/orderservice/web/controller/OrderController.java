package com.example.orderservice.web.controller;

import com.example.logging.annotation.LogApiRequest;
import com.example.orderservice.application.command.OrderCommandResult;
import com.example.orderservice.application.query.OrderListQueryResult;
import com.example.orderservice.application.query.OrderQueryResult;
import com.example.orderservice.application.service.OrderApplicationService;
import com.example.orderservice.web.dto.CreateOrderRequest;
import com.example.orderservice.web.dto.OrderListResponse;
import com.example.orderservice.web.dto.OrderResponse;
import com.example.orderservice.web.exception.OrderNotFoundException;
import com.example.orderservice.web.exception.PaymentProcessingException;
import com.example.orderservice.web.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;

/**
 * 訂單控制器 - 處理訂單相關的 HTTP 請求
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
@LogApiRequest
public class OrderController {
    
    private final OrderApplicationService orderApplicationService;
    private final OrderMapper orderMapper;
    
    /**
     * 創建訂單
     */
    @PostMapping
    @LogApiRequest(operation = "CREATE_ORDER", logRequest = true, logResponse = true)
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request for customer: {}", request.getCustomerId());
        
        try {
            // 轉換請求為命令
            var command = orderMapper.toCommand(request);
            
            // 執行創建訂單命令
            OrderCommandResult result = orderApplicationService.createOrder(command);
            
            if (!result.isSuccess()) {
                log.warn("Order creation failed: {}", result.getMessage());
                throw new PaymentProcessingException(result.getMessage());
            }
            
            // 查詢創建的訂單詳情
            var query = orderMapper.toQuery(result.getOrderId());
            OrderQueryResult orderResult = orderApplicationService.getOrder(query);
            
            // 轉換為響應
            OrderResponse response = orderMapper.toResponse(orderResult);
            
            log.info("Order created successfully: orderId={}, transactionId={}", 
                result.getOrderId(), result.getTransactionId());
            
            // 返回 201 Created 狀態碼和 Location 頭
            URI location = URI.create("/api/v1/orders/" + result.getOrderId());
            return ResponseEntity.created(location).body(response);
            
        } catch (PaymentProcessingException e) {
            log.error("Payment processing failed for customer: {}", request.getCustomerId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating order for customer: {}", request.getCustomerId(), e);
            throw new PaymentProcessingException("Failed to create order: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根據 ID 查詢訂單
     */
    @GetMapping("/{orderId}")
    @LogApiRequest(operation = "GET_ORDER", logRequest = true, logResponse = true)
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable 
            @Pattern(regexp = "^[a-zA-Z0-9-_]{1,50}$", message = "Invalid order ID format") 
            String orderId) {
        
        log.info("Received get order request: orderId={}", orderId);
        
        try {
            // 創建查詢
            var query = orderMapper.toQuery(orderId);
            
            // 執行查詢
            OrderQueryResult result = orderApplicationService.getOrder(query);
            
            if (result == null) {
                log.warn("Order not found: orderId={}", orderId);
                throw new OrderNotFoundException(orderId);
            }
            
            // 轉換為響應
            OrderResponse response = orderMapper.toResponse(result);
            
            log.info("Order retrieved successfully: orderId={}", orderId);
            return ResponseEntity.ok(response);
            
        } catch (OrderNotFoundException e) {
            log.warn("Order not found: orderId={}", orderId);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving order: orderId={}", orderId, e);
            throw new RuntimeException("Failed to retrieve order: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查詢訂單列表
     */
    @GetMapping
    @LogApiRequest(operation = "GET_ORDERS", logRequest = true, logResponse = true)
    public ResponseEntity<OrderListResponse> getOrders(
            @RequestParam(required = false) 
            @Pattern(regexp = "^[a-zA-Z0-9-_]{1,50}$", message = "Invalid customer ID format")
            String customerId,
            
            @RequestParam(required = false)
            @Pattern(regexp = "^(CREATED|PAYMENT_PENDING|PAYMENT_CONFIRMED|PAYMENT_FAILED|CANCELLED)$", 
                    message = "Invalid status. Must be one of: CREATED, PAYMENT_PENDING, PAYMENT_CONFIRMED, PAYMENT_FAILED, CANCELLED")
            String status,
            
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be non-negative")
            int page,
            
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be positive")
            int size) {
        
        log.info("Received get orders request: customerId={}, status={}, page={}, size={}", 
            customerId, status, page, size);
        
        try {
            // 創建列表查詢
            var query = orderMapper.toListQuery(customerId, status, page, size);
            
            // 執行查詢
            OrderListQueryResult result = orderApplicationService.getOrders(query);
            
            // 轉換為響應
            OrderListResponse response = orderMapper.toListResponse(result);
            
            log.info("Orders retrieved successfully: count={}, page={}, size={}", 
                result.getTotalCount(), page, size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Unexpected error retrieving orders: customerId={}, status={}, page={}, size={}", 
                customerId, status, page, size, e);
            throw new RuntimeException("Failed to retrieve orders: " + e.getMessage(), e);
        }
    }
    
    /**
     * 健康檢查端點
     */
    @GetMapping("/health")
    @LogApiRequest(operation = "HEALTH_CHECK", logRequest = false, logResponse = false)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is healthy");
    }
}
package com.example.orderservice.application.handler;

import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.application.query.OrderListQuery;
import com.example.orderservice.application.query.OrderListQueryResult;
import com.example.orderservice.application.query.OrderQuery;
import com.example.orderservice.application.query.OrderQueryResult;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 訂單查詢處理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHandler {
    
    private final OrderRepository orderRepository;
    
    /**
     * 處理訂單查詢
     * @param query 訂單查詢
     * @return 查詢結果
     */
    public OrderQueryResult handle(OrderQuery query) {
        log.info("Processing order query for order: {}", query.getOrderId());
        
        if (query.getOrderId() == null || query.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        
        OrderId orderId = new OrderId(query.getOrderId());
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + query.getOrderId()));
        
        return mapToQueryResult(order);
    }
    
    /**
     * 處理訂單列表查詢
     * @param query 訂單列表查詢
     * @return 查詢結果
     */
    public OrderListQueryResult handle(OrderListQuery query) {
        log.info("Processing order list query: customerId={}, status={}, page={}, size={}", 
            query.getCustomerId(), query.getStatus(), query.getPage(), query.getSize());
        
        validateListQuery(query);
        
        List<Order> orders = findOrders(query);
        int totalCount = countOrders(query);
        
        List<OrderQueryResult> orderResults = orders.stream()
            .map(this::mapToQueryResult)
            .collect(Collectors.toList());
        
        return OrderListQueryResult.builder()
            .orders(orderResults)
            .totalCount(totalCount)
            .page(query.getPage())
            .size(query.getSize())
            .hasNext(hasNextPage(query.getPage(), query.getSize(), totalCount))
            .build();
    }
    
    private void validateListQuery(OrderListQuery query) {
        if (query.getPage() < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (query.getSize() <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        if (query.getSize() > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
    }
    
    private List<Order> findOrders(OrderListQuery query) {
        // 根據查詢條件選擇合適的查詢方法
        if (query.getCustomerId() != null && !query.getCustomerId().trim().isEmpty()) {
            return orderRepository.findByCustomerId(query.getCustomerId(), query.getPage(), query.getSize());
        } else if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
            OrderStatus status = OrderStatus.valueOf(query.getStatus().toUpperCase());
            return orderRepository.findByStatus(status, query.getPage(), query.getSize());
        } else if (query.getStartDate() != null && query.getEndDate() != null) {
            return orderRepository.findByDateRange(query.getStartDate(), query.getEndDate(), query.getPage(), query.getSize());
        } else {
            throw new IllegalArgumentException("At least one query criteria must be provided");
        }
    }
    
    private int countOrders(OrderListQuery query) {
        OrderStatus status = null;
        if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
            status = OrderStatus.valueOf(query.getStatus().toUpperCase());
        }
        
        return orderRepository.countOrders(
            query.getCustomerId(), 
            status, 
            query.getStartDate(), 
            query.getEndDate()
        );
    }
    
    private boolean hasNextPage(int page, int size, int totalCount) {
        return (page + 1) * size < totalCount;
    }
    
    private OrderQueryResult mapToQueryResult(Order order) {
        return OrderQueryResult.builder()
            .orderId(order.getOrderId().getValue())
            .customerId(order.getCustomerId().getValue())
            .amount(order.getAmount().getAmount())
            .currency(order.getAmount().getCurrency())
            .status(order.getStatus().name())
            .transactionId(order.getTransactionId())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }
}
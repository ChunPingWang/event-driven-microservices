package com.example.orderservice.infrastructure.repository;

import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.OrderId;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 訂單倉儲實現 (基礎設施層適配器)
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    
    public OrderRepositoryImpl(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public void save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        jpaRepository.save(order);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        return jpaRepository.findById(orderId);
    }
    
    @Override
    public Optional<Order> findByTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        return jpaRepository.findByTransactionId(transactionId);
    }
    
    @Override
    public List<Order> findByCustomerId(String customerId, int page, int size) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        validatePagination(page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByCustomerIdValue(customerId, pageable);
    }
    
    @Override
    public List<Order> findByStatus(OrderStatus status, int page, int size) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        validatePagination(page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByStatus(status, pageable);
    }
    
    @Override
    public List<Order> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        validatePagination(page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }
    
    @Override
    public int countOrders(String customerId, OrderStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        return jpaRepository.countOrdersWithFilters(customerId, status, startDate, endDate);
    }
    
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        if (size > 1000) {
            throw new IllegalArgumentException("Page size cannot exceed 1000");
        }
    }
    
    /**
     * Spring Data JPA Repository 接口
     */
    public interface OrderJpaRepository extends JpaRepository<Order, OrderId> {
        
        Optional<Order> findByTransactionId(String transactionId);
        
        List<Order> findByCustomerIdValue(String customerId, Pageable pageable);
        
        List<Order> findByStatus(OrderStatus status, Pageable pageable);
        
        List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
        
        @Query("SELECT COUNT(o) FROM Order o WHERE " +
               "(:customerId IS NULL OR o.customerId.value = :customerId) AND " +
               "(:status IS NULL OR o.status = :status) AND " +
               "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
               "(:endDate IS NULL OR o.createdAt <= :endDate)")
        int countOrdersWithFilters(@Param("customerId") String customerId,
                                  @Param("status") OrderStatus status,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    }
}
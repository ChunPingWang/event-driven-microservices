package com.example.paymentservice.infrastructure.repository;

import com.example.paymentservice.application.port.out.OutboxEventRepository;
import com.example.paymentservice.application.port.out.PaymentRepository;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.domain.payment.valueobject.PaymentStatus;
import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.infrastructure.outbox.OutboxService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付倉儲實現 (基礎設施層適配器)
 */
@Repository
public class PaymentRepositoryImpl implements PaymentRepository, OutboxEventRepository {
    
    private final PaymentJpaRepository jpaRepository;
    private final OutboxService outboxService;
    
    public PaymentRepositoryImpl(PaymentJpaRepository jpaRepository, OutboxService outboxService) {
        this.jpaRepository = jpaRepository;
        this.outboxService = outboxService;
    }
    
    @Override
    public void save(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }
        jpaRepository.save(payment);
    }
    
    @Override
    @Transactional
    public void saveEvent(DomainEvent domainEvent, String aggregateId, String aggregateType) {
        if (domainEvent == null) {
            throw new IllegalArgumentException("Domain event cannot be null");
        }
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate type cannot be null or empty");
        }
        
        outboxService.saveEvent(domainEvent, aggregateId, aggregateType);
    }
    
    @Override
    @Transactional
    public void savePaymentWithEvents(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }
        
        // 保存支付記錄
        jpaRepository.save(payment);
        
        // 保存領域事件到發件箱
        List<DomainEvent> domainEvents = payment.getDomainEvents();
        for (DomainEvent event : domainEvents) {
            outboxService.saveEvent(event, payment.getPaymentId().getValue(), "Payment");
        }
        
        // 清除已處理的領域事件
        payment.clearDomainEvents();
    }
    
    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        return jpaRepository.findById(paymentId);
    }
    
    @Override
    public Optional<Payment> findByTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        return jpaRepository.findByTransactionId(transactionId);
    }
    
    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        return jpaRepository.findByOrderId(orderId);
    }
    
    @Override
    public List<Payment> findByCustomerId(String customerId, int page, int size) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        validatePagination(page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByCustomerId(customerId, pageable);
    }
    
    @Override
    public List<Payment> findByStatus(PaymentStatus status, int page, int size) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        validatePagination(page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findByStatus(status, pageable);
    }
    
    @Override
    public List<Payment> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
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
    public int countPayments(String customerId, PaymentStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        return jpaRepository.countPaymentsWithFilters(customerId, status, startDate, endDate);
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
    public interface PaymentJpaRepository extends JpaRepository<Payment, PaymentId> {
        
        Optional<Payment> findByTransactionId(String transactionId);
        
        Optional<Payment> findByOrderId(String orderId);
        
        List<Payment> findByCustomerId(String customerId, Pageable pageable);
        
        List<Payment> findByStatus(PaymentStatus status, Pageable pageable);
        
        List<Payment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
        
        @Query("SELECT COUNT(p) FROM Payment p WHERE " +
               "(:customerId IS NULL OR p.customerId = :customerId) AND " +
               "(:status IS NULL OR p.status = :status) AND " +
               "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
               "(:endDate IS NULL OR p.createdAt <= :endDate)")
        int countPaymentsWithFilters(@Param("customerId") String customerId,
                                    @Param("status") PaymentStatus status,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
    }
}
package com.example.paymentservice.application.handler;

import com.example.paymentservice.application.port.out.PaymentRepository;
import com.example.paymentservice.application.query.PaymentListQuery;
import com.example.paymentservice.application.query.PaymentListQueryResult;
import com.example.paymentservice.application.query.PaymentQuery;
import com.example.paymentservice.application.query.PaymentQueryResult;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.domain.payment.valueobject.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 支付查詢處理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentQueryHandler {
    
    private final PaymentRepository paymentRepository;
    
    /**
     * 處理支付查詢
     * @param query 支付查詢
     * @return 查詢結果
     */
    public PaymentQueryResult handle(PaymentQuery query) {
        log.info("Processing payment query: paymentId={}, transactionId={}, orderId={}", 
            query.getPaymentId(), query.getTransactionId(), query.getOrderId());
        
        Payment payment = findPayment(query);
        return mapToQueryResult(payment);
    }
    
    /**
     * 處理支付列表查詢
     * @param query 支付列表查詢
     * @return 查詢結果
     */
    public PaymentListQueryResult handle(PaymentListQuery query) {
        log.info("Processing payment list query: customerId={}, status={}, page={}, size={}", 
            query.getCustomerId(), query.getStatus(), query.getPage(), query.getSize());
        
        validateListQuery(query);
        
        List<Payment> payments = findPayments(query);
        int totalCount = countPayments(query);
        
        List<PaymentQueryResult> paymentResults = payments.stream()
            .map(this::mapToQueryResult)
            .collect(Collectors.toList());
        
        return PaymentListQueryResult.builder()
            .payments(paymentResults)
            .totalCount(totalCount)
            .page(query.getPage())
            .size(query.getSize())
            .hasNext(hasNextPage(query.getPage(), query.getSize(), totalCount))
            .build();
    }
    
    private void validateListQuery(PaymentListQuery query) {
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
    
    private List<Payment> findPayments(PaymentListQuery query) {
        // 根據查詢條件選擇合適的查詢方法
        if (query.getCustomerId() != null && !query.getCustomerId().trim().isEmpty()) {
            return paymentRepository.findByCustomerId(query.getCustomerId(), query.getPage(), query.getSize());
        } else if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
            PaymentStatus status = PaymentStatus.valueOf(query.getStatus().toUpperCase());
            return paymentRepository.findByStatus(status, query.getPage(), query.getSize());
        } else if (query.getStartDate() != null && query.getEndDate() != null) {
            return paymentRepository.findByDateRange(query.getStartDate(), query.getEndDate(), query.getPage(), query.getSize());
        } else {
            throw new IllegalArgumentException("At least one query criteria must be provided");
        }
    }
    
    private int countPayments(PaymentListQuery query) {
        PaymentStatus status = null;
        if (query.getStatus() != null && !query.getStatus().trim().isEmpty()) {
            status = PaymentStatus.valueOf(query.getStatus().toUpperCase());
        }
        
        return paymentRepository.countPayments(
            query.getCustomerId(), 
            status, 
            query.getStartDate(), 
            query.getEndDate()
        );
    }
    
    private boolean hasNextPage(int page, int size, int totalCount) {
        return (page + 1) * size < totalCount;
    }
    
    private Payment findPayment(PaymentQuery query) {
        Optional<Payment> paymentOpt = Optional.empty();
        
        // 優先使用支付ID查詢
        if (query.getPaymentId() != null && !query.getPaymentId().trim().isEmpty()) {
            PaymentId paymentId = new PaymentId(query.getPaymentId());
            paymentOpt = paymentRepository.findById(paymentId);
        }
        
        // 如果沒有找到，使用交易ID查詢
        if (paymentOpt.isEmpty() && query.getTransactionId() != null && !query.getTransactionId().trim().isEmpty()) {
            paymentOpt = paymentRepository.findByTransactionId(query.getTransactionId());
        }
        
        // 如果還沒有找到，使用訂單ID查詢
        if (paymentOpt.isEmpty() && query.getOrderId() != null && !query.getOrderId().trim().isEmpty()) {
            paymentOpt = paymentRepository.findByOrderId(query.getOrderId());
        }
        
        return paymentOpt.orElseThrow(() -> {
            String message = String.format("Payment not found: paymentId=%s, transactionId=%s, orderId=%s", 
                query.getPaymentId(), query.getTransactionId(), query.getOrderId());
            return new RuntimeException(message);
        });
    }
    
    private PaymentQueryResult mapToQueryResult(Payment payment) {
        return PaymentQueryResult.builder()
            .paymentId(payment.getPaymentId().getValue())
            .transactionId(payment.getTransactionId())
            .orderId(payment.getOrderId())
            .customerId(payment.getCustomerId())
            .amount(payment.getAmount().getAmount())
            .currency(payment.getAmount().getCurrency())
            .status(payment.getStatus().name())
            .gatewayResponse(payment.getGatewayResponse())
            .errorMessage(payment.getErrorMessage())
            .createdAt(payment.getCreatedAt())
            .processedAt(payment.getProcessedAt())
            .build();
    }
}
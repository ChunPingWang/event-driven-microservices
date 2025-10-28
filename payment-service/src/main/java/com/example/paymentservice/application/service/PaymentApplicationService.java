package com.example.paymentservice.application.service;

import com.example.paymentservice.application.command.ProcessPaymentCommand;
import com.example.paymentservice.application.command.PaymentCommandResult;
import com.example.paymentservice.application.handler.PaymentCommandHandler;
import com.example.paymentservice.application.handler.PaymentQueryHandler;
import com.example.paymentservice.application.port.in.PaymentUseCase;
import com.example.paymentservice.application.port.out.DomainEventPublisher;
import com.example.paymentservice.application.port.out.PaymentRepository;
import com.example.paymentservice.application.query.PaymentListQuery;
import com.example.paymentservice.application.query.PaymentListQueryResult;
import com.example.paymentservice.application.query.PaymentQuery;
import com.example.paymentservice.application.query.PaymentQueryResult;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付應用服務 - 處理支付流程
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApplicationService implements PaymentUseCase {
    
    private final PaymentCommandHandler commandHandler;
    private final PaymentQueryHandler queryHandler;
    private final PaymentRepository paymentRepository;
    private final DomainEventPublisher domainEventPublisher;
    
    @Override
    public PaymentCommandResult processPayment(ProcessPaymentCommand command) {
        log.info("Processing payment for transaction: {}, order: {}", 
            command.getTransactionId(), command.getOrderId());
        return commandHandler.handle(command);
    }
    
    @Override
    public PaymentQueryResult getPayment(PaymentQuery query) {
        log.info("Retrieving payment: paymentId={}, transactionId={}, orderId={}", 
            query.getPaymentId(), query.getTransactionId(), query.getOrderId());
        return queryHandler.handle(query);
    }
    
    @Override
    public PaymentListQueryResult getPayments(PaymentListQuery query) {
        log.info("Retrieving payments: customerId={}, status={}, page={}, size={}", 
            query.getCustomerId(), query.getStatus(), query.getPage(), query.getSize());
        return queryHandler.handle(query);
    }
    
    @Override
    @Transactional
    public void refundPayment(String paymentId, String refundReason) {
        log.info("Processing refund for payment: {}, reason: {}", paymentId, refundReason);
        
        try {
            if (paymentId == null || paymentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Payment ID is required");
            }
            if (refundReason == null || refundReason.trim().isEmpty()) {
                throw new IllegalArgumentException("Refund reason is required");
            }
            
            PaymentId paymentIdVO = new PaymentId(paymentId);
            Payment payment = paymentRepository.findById(paymentIdVO)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
            
            // 執行退款
            payment.refund(refundReason);
            
            // 保存支付記錄
            paymentRepository.save(payment);
            
            // 發布領域事件
            domainEventPublisher.publishEvents(payment.getDomainEvents());
            payment.clearDomainEvents();
            
            log.info("Refund processed successfully for payment: {}", paymentId);
            
        } catch (Exception e) {
            log.error("Failed to process refund for payment: {}", paymentId, e);
            throw new RuntimeException("Failed to process refund", e);
        }
    }
}
package com.example.orderservice.application.service;

import com.example.orderservice.application.command.CreateOrderCommand;
import com.example.orderservice.application.command.OrderCommandResult;
import com.example.orderservice.application.handler.OrderCommandHandler;
import com.example.orderservice.application.handler.OrderQueryHandler;
import com.example.orderservice.application.port.in.OrderUseCase;
import com.example.orderservice.application.port.out.DomainEventPublisher;
import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.application.query.OrderListQuery;
import com.example.orderservice.application.query.OrderListQueryResult;
import com.example.orderservice.application.query.OrderQuery;
import com.example.orderservice.application.query.OrderQueryResult;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 訂單應用服務 - 協調訂單業務流程
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderApplicationService implements OrderUseCase {
    
    private final OrderCommandHandler commandHandler;
    private final OrderQueryHandler queryHandler;
    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    
    @Override
    public OrderCommandResult createOrder(CreateOrderCommand command) {
        log.info("Creating order for customer: {}", command.getCustomerId());
        return commandHandler.handle(command);
    }
    
    @Override
    public OrderQueryResult getOrder(OrderQuery query) {
        log.info("Retrieving order: {}", query.getOrderId());
        return queryHandler.handle(query);
    }
    
    @Override
    public OrderListQueryResult getOrders(OrderListQuery query) {
        log.info("Retrieving orders: customerId={}, status={}, page={}, size={}", 
            query.getCustomerId(), query.getStatus(), query.getPage(), query.getSize());
        return queryHandler.handle(query);
    }
    
    @Override
    @Transactional
    public void handlePaymentConfirmation(String orderId, String paymentId, String transactionId) {
        log.info("Handling payment confirmation for order: {}, payment: {}, transaction: {}", 
            orderId, paymentId, transactionId);
        
        try {
            OrderId orderIdVO = new OrderId(orderId);
            Order order = orderRepository.findById(orderIdVO)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            // 驗證交易ID匹配
            if (!transactionId.equals(order.getTransactionId())) {
                throw new IllegalArgumentException(
                    String.format("Transaction ID mismatch. Expected: %s, Actual: %s", 
                        order.getTransactionId(), transactionId));
            }
            
            // 確認支付
            order.confirmPayment(paymentId);
            
            // 保存訂單
            orderRepository.save(order);
            
            // 發布領域事件
            domainEventPublisher.publishEvents(order.getDomainEvents());
            order.clearDomainEvents();
            
            log.info("Payment confirmed successfully for order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Failed to handle payment confirmation for order: {}", orderId, e);
            throw new RuntimeException("Failed to handle payment confirmation", e);
        }
    }
    
    @Override
    @Transactional
    public void handlePaymentFailure(String orderId, String transactionId, String reason) {
        log.info("Handling payment failure for order: {}, transaction: {}, reason: {}", 
            orderId, transactionId, reason);
        
        try {
            OrderId orderIdVO = new OrderId(orderId);
            Order order = orderRepository.findById(orderIdVO)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            // 驗證交易ID匹配
            if (!transactionId.equals(order.getTransactionId())) {
                throw new IllegalArgumentException(
                    String.format("Transaction ID mismatch. Expected: %s, Actual: %s", 
                        order.getTransactionId(), transactionId));
            }
            
            // 標記支付失敗
            order.failPayment(reason);
            
            // 保存訂單
            orderRepository.save(order);
            
            // 發布領域事件
            domainEventPublisher.publishEvents(order.getDomainEvents());
            order.clearDomainEvents();
            
            log.info("Payment failure handled successfully for order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Failed to handle payment failure for order: {}", orderId, e);
            throw new RuntimeException("Failed to handle payment failure", e);
        }
    }
}
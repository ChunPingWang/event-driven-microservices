package com.example.orderservice.application.handler;

import com.example.orderservice.application.command.CreateOrderCommand;
import com.example.orderservice.application.command.OrderCommandResult;
import com.example.orderservice.application.port.out.DomainEventPublisher;
import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.application.port.out.PaymentRequestPublisher;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.messaging.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

/**
 * 訂單命令處理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCommandHandler {
    
    private final OrderRepository orderRepository;
    private final PaymentRequestPublisher paymentRequestPublisher;
    private final DomainEventPublisher domainEventPublisher;
    
    /**
     * 處理創建訂單命令
     * @param command 創建訂單命令
     * @return 命令執行結果
     */
    @Transactional
    public OrderCommandResult handle(CreateOrderCommand command) {
        try {
            log.info("Processing create order command for customer: {}", command.getCustomerId());
            
            // 驗證命令
            validateCommand(command);
            
            // 創建訂單聚合
            Order order = createOrder(command);
            
            // 生成交易ID並請求支付
            String transactionId = generateTransactionId();
            order.requestPayment(transactionId);
            
            // 保存訂單
            orderRepository.save(order);
            
            // 發布領域事件
            domainEventPublisher.publishEvents(order.getDomainEvents());
            order.clearDomainEvents();
            
            // 創建並發送支付請求
            PaymentRequest paymentRequest = createPaymentRequest(order, command, transactionId);
            paymentRequestPublisher.publishPaymentRequest(paymentRequest);
            
            log.info("Order created successfully: {}, transaction: {}", 
                order.getOrderId().getValue(), transactionId);
            
            return OrderCommandResult.success(order.getOrderId().getValue(), transactionId);
            
        } catch (Exception e) {
            log.error("Failed to create order for customer: {}", command.getCustomerId(), e);
            return OrderCommandResult.failure("Failed to create order: " + e.getMessage());
        }
    }
    
    private void validateCommand(CreateOrderCommand command) {
        if (command.getCustomerId() == null || command.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (command.getAmount() == null || command.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (command.getCurrency() == null || command.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (command.getCreditCard() == null) {
            throw new IllegalArgumentException("Credit card information is required");
        }
        validateCreditCard(command.getCreditCard());
    }
    
    private void validateCreditCard(CreateOrderCommand.CreditCardInfo creditCard) {
        if (creditCard.getCardNumber() == null || creditCard.getCardNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Card number is required");
        }
        if (creditCard.getExpiryDate() == null || creditCard.getExpiryDate().trim().isEmpty()) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        if (creditCard.getCvv() == null || creditCard.getCvv().trim().isEmpty()) {
            throw new IllegalArgumentException("CVV is required");
        }
        if (creditCard.getCardHolderName() == null || creditCard.getCardHolderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Card holder name is required");
        }
    }
    
    private Order createOrder(CreateOrderCommand command) {
        CustomerId customerId = new CustomerId(command.getCustomerId());
        Money amount = new Money(command.getAmount(), Currency.getInstance(command.getCurrency()));
        return new Order(customerId, amount);
    }
    
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString();
    }
    
    private PaymentRequest createPaymentRequest(Order order, CreateOrderCommand command, String transactionId) {
        return PaymentRequest.builder()
            .transactionId(transactionId)
            .orderId(order.getOrderId().getValue())
            .customerId(order.getCustomerId().getValue())
            .amount(order.getAmount().getAmount())
            .currency(order.getAmount().getCurrency())
            .creditCard(PaymentRequest.CreditCardInfo.builder()
                .cardNumber(command.getCreditCard().getCardNumber())
                .expiryDate(command.getCreditCard().getExpiryDate())
                .cvv(command.getCreditCard().getCvv())
                .cardHolderName(command.getCreditCard().getCardHolderName())
                .build())
            .billingAddress(command.getBillingAddress() != null ? 
                PaymentRequest.BillingAddress.builder()
                    .street(command.getBillingAddress().getStreet())
                    .city(command.getBillingAddress().getCity())
                    .postalCode(command.getBillingAddress().getPostalCode())
                    .country(command.getBillingAddress().getCountry())
                    .build() : null)
            .merchantId(command.getMerchantId())
            .description(command.getDescription())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
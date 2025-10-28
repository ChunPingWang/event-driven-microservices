package com.example.paymentservice.application.handler;

import com.example.paymentservice.application.command.ProcessPaymentCommand;
import com.example.paymentservice.application.command.PaymentCommandResult;
import com.example.paymentservice.application.port.out.DomainEventPublisher;
import com.example.paymentservice.application.port.out.PaymentConfirmationPublisher;
import com.example.paymentservice.application.port.out.PaymentGateway;
import com.example.paymentservice.application.port.out.PaymentRepository;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.valueobject.CreditCard;
import com.example.paymentservice.domain.payment.valueobject.Money;
import com.example.paymentservice.domain.payment.valueobject.PaymentResult;
import com.example.paymentservice.messaging.PaymentConfirmation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Currency;

/**
 * 支付命令處理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandHandler {
    
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentConfirmationPublisher confirmationPublisher;
    private final DomainEventPublisher domainEventPublisher;
    
    /**
     * 處理支付命令
     * @param command 處理支付命令
     * @return 命令執行結果
     */
    @Transactional
    public PaymentCommandResult handle(ProcessPaymentCommand command) {
        try {
            log.info("Processing payment command for transaction: {}, order: {}", 
                command.getTransactionId(), command.getOrderId());
            
            // 驗證命令
            validateCommand(command);
            
            // 創建支付聚合
            Payment payment = createPayment(command);
            
            // 調用支付網關處理支付
            String gatewayResponse = paymentGateway.processPayment(
                command.getTransactionId(),
                payment.getAmount(),
                payment.getCreditCard(),
                command.getMerchantId()
            );
            
            // 處理支付結果
            PaymentResult result = payment.process(gatewayResponse);
            
            // 保存支付記錄
            paymentRepository.save(payment);
            
            // 發布領域事件
            domainEventPublisher.publishEvents(payment.getDomainEvents());
            payment.clearDomainEvents();
            
            // 發送支付確認消息
            PaymentConfirmation confirmation = createPaymentConfirmation(payment, result);
            confirmationPublisher.publishPaymentConfirmation(confirmation);
            
            if (result.isSuccess()) {
                log.info("Payment processed successfully: {}, transaction: {}", 
                    result.getPaymentId(), command.getTransactionId());
                return PaymentCommandResult.success(
                    result.getPaymentId(), 
                    command.getTransactionId(), 
                    command.getOrderId(), 
                    gatewayResponse
                );
            } else {
                log.warn("Payment processing failed: transaction: {}, reason: {}", 
                    command.getTransactionId(), result.getErrorMessage());
                return PaymentCommandResult.failure(
                    command.getTransactionId(), 
                    command.getOrderId(), 
                    result.getErrorMessage(), 
                    gatewayResponse
                );
            }
            
        } catch (Exception e) {
            log.error("Failed to process payment for transaction: {}", command.getTransactionId(), e);
            return PaymentCommandResult.failure(
                command.getTransactionId(), 
                command.getOrderId(), 
                "Payment processing failed: " + e.getMessage(), 
                null
            );
        }
    }
    
    private void validateCommand(ProcessPaymentCommand command) {
        if (command.getTransactionId() == null || command.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (command.getOrderId() == null || command.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
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
    
    private void validateCreditCard(ProcessPaymentCommand.CreditCardInfo creditCard) {
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
    
    private Payment createPayment(ProcessPaymentCommand command) {
        Money amount = new Money(command.getAmount(), Currency.getInstance(command.getCurrency()));
        CreditCard creditCard = new CreditCard(
            command.getCreditCard().getCardNumber(),
            command.getCreditCard().getExpiryDate(),
            command.getCreditCard().getCvv(),
            command.getCreditCard().getCardHolderName()
        );
        
        return new Payment(
            command.getTransactionId(),
            command.getOrderId(),
            command.getCustomerId(),
            amount,
            creditCard
        );
    }
    
    private PaymentConfirmation createPaymentConfirmation(Payment payment, PaymentResult result) {
        PaymentConfirmation.PaymentStatus status = result.isSuccess() 
            ? PaymentConfirmation.PaymentStatus.SUCCESS 
            : PaymentConfirmation.PaymentStatus.FAILED;
            
        return PaymentConfirmation.builder()
            .paymentId(payment.getPaymentId().getValue())
            .transactionId(payment.getTransactionId())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount().getAmount())
            .currency(payment.getAmount().getCurrency())
            .status(status)
            .gatewayResponse(payment.getGatewayResponse())
            .errorMessage(result.isSuccess() ? null : result.getErrorMessage())
            .processedAt(LocalDateTime.now())
            .build();
    }
}
package com.example.paymentservice.bdd;

import com.example.paymentservice.application.port.out.DomainEventPublisher;
import com.example.paymentservice.application.port.out.PaymentConfirmationPublisher;
import com.example.paymentservice.application.port.out.PaymentGateway;
import com.example.paymentservice.application.port.out.PaymentRepository;
import com.example.paymentservice.domain.payment.Payment;
import com.example.paymentservice.domain.payment.valueobject.CreditCard;
import com.example.paymentservice.domain.payment.valueobject.Money;
import com.example.paymentservice.domain.payment.valueobject.PaymentId;
import com.example.paymentservice.domain.shared.DomainEvent;
import com.example.paymentservice.messaging.PaymentConfirmation;
// Removed RabbitTemplate import since we don't need messaging for domain logic tests
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

@TestConfiguration
public class BddTestConfiguration {

    // Remove RabbitTemplate bean since we don't need messaging for domain logic tests

    @Bean
    @Primary
    public PaymentRepository paymentRepository() {
        return new PaymentRepository() {
            @Override
            public void save(Payment payment) {
                // Mock implementation - do nothing
            }

            @Override
            public Optional<Payment> findById(PaymentId paymentId) {
                return Optional.empty();
            }

            @Override
            public Optional<Payment> findByTransactionId(String transactionId) {
                return Optional.empty();
            }

            @Override
            public Optional<Payment> findByOrderId(String orderId) {
                return Optional.empty();
            }

            @Override
            public List<Payment> findByCustomerId(String customerId, int page, int size) {
                return List.of();
            }

            @Override
            public List<Payment> findByStatus(com.example.paymentservice.domain.payment.valueobject.PaymentStatus status, int page, int size) {
                return List.of();
            }

            @Override
            public List<Payment> findByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, int page, int size) {
                return List.of();
            }

            @Override
            public int countPayments(String customerId, com.example.paymentservice.domain.payment.valueobject.PaymentStatus status, 
                                   java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
                return 0;
            }
        };
    }

    @Bean
    @Primary
    public PaymentGateway paymentGateway() {
        return new PaymentGateway() {
            @Override
            public String processPayment(String transactionId, Money amount, CreditCard creditCard, String merchantId) {
                // Mock implementation - return success for valid cards
                if ("4111111111111111".equals(creditCard.getCardNumber())) {
                    return "SUCCESS: Payment processed successfully";
                } else {
                    return "FAILED: Invalid card number";
                }
            }

            @Override
            public boolean validateCreditCard(CreditCard creditCard) {
                // Mock implementation - return true for valid test card
                return "4111111111111111".equals(creditCard.getCardNumber());
            }
        };
    }

    @Bean
    @Primary
    public DomainEventPublisher domainEventPublisher() {
        return new DomainEventPublisher() {
            @Override
            public void publishEvents(List<DomainEvent> events) {
                // Mock implementation - do nothing
            }
        };
    }

    @Bean
    @Primary
    public PaymentConfirmationPublisher paymentConfirmationPublisher() {
        return new PaymentConfirmationPublisher() {
            @Override
            public void publishPaymentConfirmation(PaymentConfirmation confirmation) {
                // Mock implementation - do nothing
            }
        };
    }
}
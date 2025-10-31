package com.example.orderservice.domain.event;

import com.example.orderservice.domain.order.event.PaymentRequestedEvent;
import com.example.orderservice.messaging.PaymentRequest;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.infrastructure.repository.OrderRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 領域事件到消息轉換器 - 負責將領域事件轉換為消息格式
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventToMessageConverter {

    private final OrderRepositoryImpl orderRepository;

    /**
     * 將支付請求事件轉換為支付請求消息
     * @param event 支付請求事件
     * @return 支付請求消息
     */
    public PaymentRequest convertToPaymentRequest(PaymentRequestedEvent event) {
        log.debug("Converting PaymentRequestedEvent to PaymentRequest for order: {}", 
            event.getOrderId().getValue());

        // 從倉儲獲取完整的訂單信息
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new IllegalStateException(
                "Order not found for PaymentRequestedEvent: " + event.getOrderId().getValue()));

        // 構建支付請求消息
        return PaymentRequest.builder()
            .transactionId(event.getTransactionId())
            .orderId(event.getOrderId().getValue())
            .customerId(event.getCustomerId())
            .amount(order.getAmount().getAmount())
            .currency(order.getAmount().getCurrency())
            .creditCard(buildCreditCardInfo(order))
            .billingAddress(buildBillingAddress(order))
            .merchantId("MERCHANT_001") // 可以從配置或訂單中獲取
            .description("Payment for order " + order.getOrderId().getValue())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 構建信用卡信息
     * 注意：在實際應用中，信用卡信息應該從安全的地方獲取，而不是存儲在訂單中
     */
    private PaymentRequest.CreditCardInfo buildCreditCardInfo(Order order) {
        // 這裡應該從安全的信用卡信息存儲中獲取
        // 為了演示目的，使用模擬數據
        return PaymentRequest.CreditCardInfo.builder()
            .cardNumber("**** **** **** 1234") // 已遮罩的卡號
            .expiryDate("12/25")
            .cvv("***") // 已遮罩的CVV
            .cardHolderName("Customer " + order.getCustomerId().getValue())
            .build();
    }

    /**
     * 構建帳單地址信息
     */
    private PaymentRequest.BillingAddress buildBillingAddress(Order order) {
        // 這裡應該從客戶信息或訂單中獲取真實的帳單地址
        // 為了演示目的，使用模擬數據
        return PaymentRequest.BillingAddress.builder()
            .street("123 Main Street")
            .city("Taipei")
            .postalCode("10001")
            .country("TW")
            .build();
    }
}
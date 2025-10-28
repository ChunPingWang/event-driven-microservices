package com.example.orderservice.bdd;

import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.CustomerId;
import com.example.orderservice.domain.order.valueobject.Money;
import com.example.orderservice.domain.order.valueobject.OrderId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class TestDataHelper {

    private final Map<String, Order> testOrders = new HashMap<>();
    private final Map<String, Object> testData = new HashMap<>();

    public Order createTestOrder(String customerId, BigDecimal amount) {
        CustomerId customerIdVO = new CustomerId(customerId);
        Money money = Money.twd(amount);
        
        Order order = new Order(customerIdVO, money);
        testOrders.put(order.getOrderId().getValue(), order);
        return order;
    }

    public Order getTestOrder(String orderId) {
        return testOrders.get(orderId);
    }

    public void storeTestData(String key, Object value) {
        testData.put(key, value);
    }

    public Object getTestData(String key) {
        return testData.get(key);
    }

    public void clearTestData() {
        testOrders.clear();
        testData.clear();
    }

    public void simulatePaymentRequest(Order order) {
        // 模擬發送支付請求消息
        storeTestData("payment_request_sent_" + order.getOrderId().getValue(), true);
    }

    public void simulatePaymentConfirmation(Order order, boolean success) {
        if (success) {
            order.confirmPayment("PAYMENT-" + System.currentTimeMillis());
            storeTestData("payment_confirmed_" + order.getOrderId().getValue(), true);
        } else {
            order.failPayment("支付失敗");
            storeTestData("payment_failed_" + order.getOrderId().getValue(), true);
        }
    }

    public boolean isPaymentRequestSent(String orderId) {
        return Boolean.TRUE.equals(getTestData("payment_request_sent_" + orderId));
    }

    public boolean isPaymentConfirmed(String orderId) {
        return Boolean.TRUE.equals(getTestData("payment_confirmed_" + orderId));
    }

    public boolean isPaymentFailed(String orderId) {
        return Boolean.TRUE.equals(getTestData("payment_failed_" + orderId));
    }
}
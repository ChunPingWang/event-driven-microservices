package com.example.orderservice.infrastructure.retry;

import com.example.orderservice.application.port.out.OrderRepository;
import com.example.orderservice.application.port.out.PaymentRequestPublisher;
import com.example.orderservice.domain.order.Order;
import com.example.orderservice.domain.order.valueobject.OrderStatus;
import com.example.orderservice.messaging.PaymentRequest;
import com.example.logging.annotation.LogMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 支付重試服務 - 處理支付請求重試邏輯
 */
@Service
public class PaymentRetryService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentRetryService.class);
    
    private final OrderRepository orderRepository;
    private final PaymentRequestPublisher paymentRequestPublisher;
    private final RetryHistoryRepository retryHistoryRepository;
    
    @Value("${payment.retry.max-attempts:5}")
    private int maxRetryAttempts;
    
    @Value("${payment.retry.base-delay-minutes:1}")
    private int baseDelayMinutes;
    
    @Value("${payment.retry.timeout-minutes:30}")
    private int paymentTimeoutMinutes;
    
    @Value("${payment.retry.batch-size:50}")
    private int retryBatchSize;
    
    public PaymentRetryService(OrderRepository orderRepository, 
                              PaymentRequestPublisher paymentRequestPublisher,
                              RetryHistoryRepository retryHistoryRepository) {
        this.orderRepository = orderRepository;
        this.paymentRequestPublisher = paymentRequestPublisher;
        this.retryHistoryRepository = retryHistoryRepository;
    }
    
    /**
     * 定時處理支付重試 (每分鐘執行一次)
     */
    @Scheduled(fixedDelay = 60000) // 1 minute
    @Transactional
    @LogMessageEvent(eventType = "PAYMENT_RETRY_BATCH", logPayload = false)
    public void processPaymentRetries() {
        try {
            logger.debug("Starting payment retry processing");
            
            // 查找需要重試的訂單
            List<Order> ordersToRetry = findOrdersForRetry();
            
            if (ordersToRetry.isEmpty()) {
                logger.trace("No orders found for retry");
                return;
            }
            
            logger.info("Found {} orders for payment retry", ordersToRetry.size());
            
            int successCount = 0;
            int failureCount = 0;
            int skippedCount = 0;
            
            for (Order order : ordersToRetry) {
                try {
                    RetryResult result = processOrderRetry(order);
                    
                    switch (result.getStatus()) {
                        case SUCCESS:
                            successCount++;
                            break;
                        case FAILED:
                            failureCount++;
                            break;
                        case SKIPPED:
                            skippedCount++;
                            break;
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing retry for order: orderId={}", 
                        order.getOrderId().getValue(), e);
                    failureCount++;
                }
            }
            
            logger.info("Payment retry processing completed: success={}, failed={}, skipped={}", 
                successCount, failureCount, skippedCount);
            
        } catch (Exception e) {
            logger.error("Error during payment retry processing", e);
        }
    }
    
    /**
     * 處理單個訂單的重試
     */
    @LogMessageEvent(eventType = "PAYMENT_ORDER_RETRY", logPayload = true)
    private RetryResult processOrderRetry(Order order) {
        String orderId = order.getOrderId().getValue();
        
        try {
            // 檢查重試歷史
            RetryHistory retryHistory = retryHistoryRepository.findByOrderId(orderId)
                .orElse(new RetryHistory(orderId, order.getTransactionId()));
            
            // 檢查是否已達到最大重試次數
            if (retryHistory.getAttemptCount() >= maxRetryAttempts) {
                logger.warn("Order has reached maximum retry attempts: orderId={}, attempts={}", 
                    orderId, retryHistory.getAttemptCount());
                
                // 標記訂單為最終失敗
                markOrderAsFinallyFailed(order, "Maximum retry attempts exceeded");
                return RetryResult.failed("Maximum retry attempts exceeded");
            }
            
            // 檢查是否到了重試時間
            if (!isRetryTimeReached(retryHistory)) {
                logger.trace("Retry time not reached for order: orderId={}, nextRetryAt={}", 
                    orderId, retryHistory.getNextRetryAt());
                return RetryResult.skipped("Retry time not reached");
            }
            
            // 執行重試
            executeRetry(order, retryHistory);
            
            logger.info("Successfully executed payment retry: orderId={}, attempt={}", 
                orderId, retryHistory.getAttemptCount() + 1);
            
            return RetryResult.success();
            
        } catch (Exception e) {
            logger.error("Failed to process retry for order: orderId={}", orderId, e);
            return RetryResult.failed(e.getMessage());
        }
    }
    
    /**
     * 執行重試邏輯
     */
    private void executeRetry(Order order, RetryHistory retryHistory) {
        String orderId = order.getOrderId().getValue();
        
        // 生成新的交易ID
        String newTransactionId = UUID.randomUUID().toString();
        
        // 更新訂單狀態並設置新的交易ID
        order.retryPayment(newTransactionId);
        orderRepository.save(order);
        
        // 創建支付請求
        PaymentRequest paymentRequest = createPaymentRequest(order, newTransactionId);
        
        // 發送支付請求
        paymentRequestPublisher.publishPaymentRequest(paymentRequest);
        
        // 更新重試歷史
        retryHistory.recordAttempt(newTransactionId, calculateNextRetryTime(retryHistory.getAttemptCount() + 1));
        retryHistoryRepository.save(retryHistory);
        
        logger.info("Payment retry executed: orderId={}, newTransactionId={}, attempt={}", 
            orderId, newTransactionId, retryHistory.getAttemptCount());
    }
    
    /**
     * 創建支付請求
     */
    private PaymentRequest createPaymentRequest(Order order, String transactionId) {
        return PaymentRequest.builder()
            .transactionId(transactionId)
            .orderId(order.getOrderId().getValue())
            .customerId(order.getCustomerId().getValue())
            .amount(order.getAmount().getAmount())
            .currency(order.getAmount().getCurrency())
            .merchantId("MERCHANT_001") // 可以從配置中獲取
            .description("Order payment retry - " + order.getOrderId().getValue())
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * 查找需要重試的訂單
     */
    private List<Order> findOrdersForRetry() {
        // 查找支付失敗的訂單
        List<Order> failedOrders = orderRepository.findByStatus(OrderStatus.PAYMENT_FAILED, 0, retryBatchSize);
        
        // 查找支付超時的訂單
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        List<Order> timedOutOrders = orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING, 0, retryBatchSize)
            .stream()
            .filter(order -> order.getUpdatedAt().isBefore(timeoutThreshold))
            .toList();
        
        // 合併結果
        failedOrders.addAll(timedOutOrders);
        return failedOrders;
    }
    
    /**
     * 檢查是否到了重試時間
     */
    private boolean isRetryTimeReached(RetryHistory retryHistory) {
        if (retryHistory.getNextRetryAt() == null) {
            return true; // 第一次重試
        }
        
        return LocalDateTime.now().isAfter(retryHistory.getNextRetryAt());
    }
    
    /**
     * 計算下次重試時間（指數退避算法）
     */
    private LocalDateTime calculateNextRetryTime(int attemptNumber) {
        // 指數退避：2^attemptNumber * baseDelayMinutes，最大30分鐘
        long delayMinutes = Math.min(30, (long) Math.pow(2, attemptNumber) * baseDelayMinutes);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }
    
    /**
     * 標記訂單為最終失敗
     */
    private void markOrderAsFinallyFailed(Order order, String reason) {
        try {
            order.failPayment(reason);
            orderRepository.save(order);
            
            // 更新重試歷史
            RetryHistory retryHistory = retryHistoryRepository.findByOrderId(order.getOrderId().getValue())
                .orElse(new RetryHistory(order.getOrderId().getValue(), order.getTransactionId()));
            
            retryHistory.markAsFinallyFailed(reason);
            retryHistoryRepository.save(retryHistory);
            
            logger.warn("Order marked as finally failed: orderId={}, reason={}", 
                order.getOrderId().getValue(), reason);
            
        } catch (Exception e) {
            logger.error("Failed to mark order as finally failed: orderId={}", 
                order.getOrderId().getValue(), e);
        }
    }
    
    /**
     * 手動觸發訂單重試
     * @param orderId 訂單ID
     * @return 重試結果
     */
    @Transactional
    public RetryResult manualRetry(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        
        try {
            Order order = orderRepository.findById(new com.example.orderservice.domain.order.valueobject.OrderId(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
            
            if (!order.canRetryPayment()) {
                return RetryResult.failed("Order cannot be retried in current status: " + order.getStatus());
            }
            
            return processOrderRetry(order);
            
        } catch (Exception e) {
            logger.error("Manual retry failed for order: orderId={}", orderId, e);
            return RetryResult.failed(e.getMessage());
        }
    }
    
    /**
     * 獲取訂單重試統計信息
     */
    public RetryStatistics getRetryStatistics() {
        return retryHistoryRepository.getRetryStatistics();
    }
    
    /**
     * 重試結果
     */
    public static class RetryResult {
        private final RetryStatus status;
        private final String message;
        
        private RetryResult(RetryStatus status, String message) {
            this.status = status;
            this.message = message;
        }
        
        public static RetryResult success() {
            return new RetryResult(RetryStatus.SUCCESS, "Retry executed successfully");
        }
        
        public static RetryResult failed(String message) {
            return new RetryResult(RetryStatus.FAILED, message);
        }
        
        public static RetryResult skipped(String message) {
            return new RetryResult(RetryStatus.SKIPPED, message);
        }
        
        public RetryStatus getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public enum RetryStatus {
            SUCCESS, FAILED, SKIPPED
        }
    }
}
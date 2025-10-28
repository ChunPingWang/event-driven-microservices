package com.example.orderservice.application.port.in;

import com.example.orderservice.application.command.CreateOrderCommand;
import com.example.orderservice.application.command.OrderCommandResult;
import com.example.orderservice.application.query.OrderListQuery;
import com.example.orderservice.application.query.OrderListQueryResult;
import com.example.orderservice.application.query.OrderQuery;
import com.example.orderservice.application.query.OrderQueryResult;

/**
 * 訂單用例接口 (入站端口)
 */
public interface OrderUseCase {
    
    /**
     * 創建訂單
     * @param command 創建訂單命令
     * @return 命令執行結果
     */
    OrderCommandResult createOrder(CreateOrderCommand command);
    
    /**
     * 查詢訂單
     * @param query 訂單查詢
     * @return 查詢結果
     */
    OrderQueryResult getOrder(OrderQuery query);
    
    /**
     * 查詢訂單列表
     * @param query 訂單列表查詢
     * @return 查詢結果
     */
    OrderListQueryResult getOrders(OrderListQuery query);
    
    /**
     * 處理支付確認
     * @param orderId 訂單ID
     * @param paymentId 支付ID
     * @param transactionId 交易ID
     */
    void handlePaymentConfirmation(String orderId, String paymentId, String transactionId);
    
    /**
     * 處理支付失敗
     * @param orderId 訂單ID
     * @param transactionId 交易ID
     * @param reason 失敗原因
     */
    void handlePaymentFailure(String orderId, String transactionId, String reason);
}
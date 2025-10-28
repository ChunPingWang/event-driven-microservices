package com.example.paymentservice.application.port.in;

import com.example.paymentservice.application.command.ProcessPaymentCommand;
import com.example.paymentservice.application.command.PaymentCommandResult;
import com.example.paymentservice.application.query.PaymentListQuery;
import com.example.paymentservice.application.query.PaymentListQueryResult;
import com.example.paymentservice.application.query.PaymentQuery;
import com.example.paymentservice.application.query.PaymentQueryResult;

/**
 * 支付用例接口 (入站端口)
 */
public interface PaymentUseCase {
    
    /**
     * 處理支付
     * @param command 處理支付命令
     * @return 命令執行結果
     */
    PaymentCommandResult processPayment(ProcessPaymentCommand command);
    
    /**
     * 查詢支付
     * @param query 支付查詢
     * @return 查詢結果
     */
    PaymentQueryResult getPayment(PaymentQuery query);
    
    /**
     * 查詢支付列表
     * @param query 支付列表查詢
     * @return 查詢結果
     */
    PaymentListQueryResult getPayments(PaymentListQuery query);
    
    /**
     * 退款
     * @param paymentId 支付ID
     * @param refundReason 退款原因
     */
    void refundPayment(String paymentId, String refundReason);
}
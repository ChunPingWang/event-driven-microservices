package com.example.orderservice.web.mapper;

import com.example.orderservice.application.command.CreateOrderCommand;
import com.example.orderservice.application.query.OrderListQuery;
import com.example.orderservice.application.query.OrderListQueryResult;
import com.example.orderservice.application.query.OrderQuery;
import com.example.orderservice.application.query.OrderQueryResult;
import com.example.orderservice.web.dto.CreateOrderRequest;
import com.example.orderservice.web.dto.OrderListResponse;
import com.example.orderservice.web.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 訂單映射器 - 負責 DTO 與應用層對象之間的轉換
 */
@Component
public class OrderMapper {
    
    /**
     * 將創建訂單請求轉換為創建訂單命令
     */
    public CreateOrderCommand toCommand(CreateOrderRequest request) {
        return CreateOrderCommand.builder()
            .customerId(request.getCustomerId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .creditCard(toCreditCardInfo(request.getCreditCard()))
            .billingAddress(toBillingAddress(request.getBillingAddress()))
            .merchantId(request.getMerchantId())
            .description(request.getDescription())
            .build();
    }
    
    /**
     * 將訂單查詢結果轉換為訂單響應
     */
    public OrderResponse toResponse(OrderQueryResult queryResult) {
        return OrderResponse.builder()
            .orderId(queryResult.getOrderId())
            .customerId(queryResult.getCustomerId())
            .amount(queryResult.getAmount())
            .currency(queryResult.getCurrency())
            .status(queryResult.getStatus())
            .transactionId(queryResult.getTransactionId())
            .createdAt(queryResult.getCreatedAt())
            .updatedAt(queryResult.getUpdatedAt())
            .build();
    }
    
    /**
     * 將訂單列表查詢結果轉換為訂單列表響應
     */
    public OrderListResponse toListResponse(OrderListQueryResult queryResult) {
        return OrderListResponse.builder()
            .orders(queryResult.getOrders().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()))
            .totalCount(queryResult.getTotalCount())
            .page(queryResult.getPage())
            .size(queryResult.getSize())
            .hasNext(queryResult.isHasNext())
            .build();
    }
    
    /**
     * 創建訂單查詢對象
     */
    public OrderQuery toQuery(String orderId) {
        return OrderQuery.builder()
            .orderId(orderId)
            .build();
    }
    
    /**
     * 創建訂單列表查詢對象
     */
    public OrderListQuery toListQuery(String customerId, String status, int page, int size) {
        return OrderListQuery.builder()
            .customerId(customerId)
            .status(status)
            .page(page)
            .size(size)
            .build();
    }
    
    private CreateOrderCommand.CreditCardInfo toCreditCardInfo(CreateOrderRequest.CreditCardInfo creditCard) {
        return CreateOrderCommand.CreditCardInfo.builder()
            .cardNumber(creditCard.getCardNumber())
            .expiryDate(creditCard.getExpiryDate())
            .cvv(creditCard.getCvv())
            .cardHolderName(creditCard.getCardHolderName())
            .build();
    }
    
    private CreateOrderCommand.BillingAddress toBillingAddress(CreateOrderRequest.BillingAddress billingAddress) {
        return CreateOrderCommand.BillingAddress.builder()
            .street(billingAddress.getStreet())
            .city(billingAddress.getCity())
            .postalCode(billingAddress.getPostalCode())
            .country(billingAddress.getCountry())
            .build();
    }
}
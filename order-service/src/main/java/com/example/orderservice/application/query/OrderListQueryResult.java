package com.example.orderservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 訂單列表查詢結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListQueryResult {
    private List<OrderQueryResult> orders;
    private int totalCount;
    private int page;
    private int size;
    private boolean hasNext;
}
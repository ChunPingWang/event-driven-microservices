package com.example.paymentservice.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 支付列表查詢結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentListQueryResult {
    private List<PaymentQueryResult> payments;
    private int totalCount;
    private int page;
    private int size;
    private boolean hasNext;
}
package com.example.orderservice.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 訂單列表響應 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {
    
    @JsonProperty("orders")
    private List<OrderResponse> orders;
    
    @JsonProperty("totalCount")
    private int totalCount;
    
    @JsonProperty("page")
    private int page;
    
    @JsonProperty("size")
    private int size;
    
    @JsonProperty("hasNext")
    private boolean hasNext;
}
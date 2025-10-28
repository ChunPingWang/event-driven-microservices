package com.example.logging.service;

import com.example.logging.model.ApiRequestLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API 請求日誌服務接口
 * 定義 API 請求日誌的業務操作
 */
public interface RequestLogService {
    
    /**
     * 保存 API 請求日誌
     */
    void saveRequestLog(ApiRequestLog requestLog);
    
    /**
     * 根據操作名稱查找日誌記錄
     */
    List<ApiRequestLog> findByOperation(String operation);
    
    /**
     * 根據時間範圍查找日誌記錄
     */
    List<ApiRequestLog> findByTimeRange(LocalDateTime start, LocalDateTime end);
    
    /**
     * 根據狀態查找日誌記錄
     */
    List<ApiRequestLog> findByStatus(String status);
    
    /**
     * 查找執行時間超過指定閾值的慢請求
     */
    List<ApiRequestLog> findSlowRequests(Long thresholdMs);
    
    /**
     * 根據請求ID查找日誌記錄
     */
    ApiRequestLog findByRequestId(String requestId);
}
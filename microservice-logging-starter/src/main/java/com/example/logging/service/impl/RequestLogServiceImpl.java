package com.example.logging.service.impl;

import com.example.logging.model.ApiRequestLog;
import com.example.logging.repository.RequestLogRepository;
import com.example.logging.service.RequestLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API 請求日誌服務實現
 * 實現 API 請求日誌的業務邏輯
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RequestLogServiceImpl implements RequestLogService {
    
    private final RequestLogRepository requestLogRepository;
    
    @Override
    @Transactional
    public void saveRequestLog(ApiRequestLog requestLog) {
        try {
            requestLogRepository.save(requestLog);
            log.debug("Saved API request log with ID: {}", requestLog.getRequestId());
        } catch (Exception e) {
            log.error("Failed to save API request log with ID: {}", requestLog.getRequestId(), e);
            // 不重新拋出異常，避免影響主業務流程
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ApiRequestLog> findByOperation(String operation) {
        return requestLogRepository.findByOperation(operation);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ApiRequestLog> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        return requestLogRepository.findByTimeRange(start, end);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ApiRequestLog> findByStatus(String status) {
        return requestLogRepository.findByStatus(status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ApiRequestLog> findSlowRequests(Long thresholdMs) {
        return requestLogRepository.findSlowRequests(thresholdMs);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ApiRequestLog findByRequestId(String requestId) {
        return requestLogRepository.findById(requestId).orElse(null);
    }
}
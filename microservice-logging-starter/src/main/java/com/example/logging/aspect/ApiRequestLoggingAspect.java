package com.example.logging.aspect;

import com.example.logging.annotation.LogApiRequest;
import com.example.logging.model.ApiRequestLog;
import com.example.logging.service.RequestLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API 請求日誌記錄切面
 * 攔截標記了 @LogApiRequest 註解的方法，記錄請求和響應信息
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiRequestLoggingAspect {
    
    private final RequestLogService requestLogService;
    private final ObjectMapper objectMapper;
    
    @Around("@annotation(logApiRequest)")
    public Object logApiRequest(ProceedingJoinPoint joinPoint, LogApiRequest logApiRequest) throws Throwable {
        String requestId = UUID.randomUUID().toString();
        String operation = logApiRequest.operation().isEmpty() 
            ? joinPoint.getSignature().getName() 
            : logApiRequest.operation();
        
        ApiRequestLog requestLog = ApiRequestLog.builder()
            .requestId(requestId)
            .operation(operation)
            .className(joinPoint.getTarget().getClass().getSimpleName())
            .methodName(joinPoint.getSignature().getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // 記錄請求參數
        if (logApiRequest.logRequest()) {
            try {
                String requestPayload = serializeArgs(joinPoint.getArgs(), logApiRequest.maxPayloadLength());
                requestLog.setRequestPayload(requestPayload);
            } catch (Exception e) {
                log.warn("Failed to serialize request payload for requestId: {}", requestId, e);
                requestLog.setRequestPayload("Failed to serialize: " + e.getMessage());
            }
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            // 計算執行時間
            if (logApiRequest.logExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                requestLog.setExecutionTimeMs(executionTime);
            }
            
            requestLog.setStatus("SUCCESS");
            
            // 記錄響應結果
            if (logApiRequest.logResponse()) {
                try {
                    String responsePayload = serialize(result, logApiRequest.maxPayloadLength());
                    requestLog.setResponsePayload(responsePayload);
                } catch (Exception e) {
                    log.warn("Failed to serialize response payload for requestId: {}", requestId, e);
                    requestLog.setResponsePayload("Failed to serialize: " + e.getMessage());
                }
            }
            
            // 保存日誌記錄
            requestLogService.saveRequestLog(requestLog);
            
            log.info("API Request completed - RequestId: {}, Operation: {}, ExecutionTime: {}ms", 
                requestId, operation, requestLog.getExecutionTimeMs());
            
            return result;
            
        } catch (Exception e) {
            // 處理異常情況
            if (logApiRequest.logExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                requestLog.setExecutionTimeMs(executionTime);
            }
            
            requestLog.setStatus("ERROR");
            requestLog.setErrorMessage(truncateString(e.getMessage(), 1000));
            
            // 保存錯誤日誌
            requestLogService.saveRequestLog(requestLog);
            
            log.error("API Request failed - RequestId: {}, Operation: {}, Error: {}", 
                requestId, operation, e.getMessage(), e);
            
            throw e;
        }
    }
    
    /**
     * 序列化方法參數
     */
    private String serializeArgs(Object[] args, int maxLength) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            String serialized = objectMapper.writeValueAsString(args);
            return truncateString(serialized, maxLength);
        } catch (Exception e) {
            return "Failed to serialize arguments: " + e.getMessage();
        }
    }
    
    /**
     * 序列化對象
     */
    private String serialize(Object obj, int maxLength) {
        if (obj == null) {
            return "null";
        }
        
        try {
            String serialized = objectMapper.writeValueAsString(obj);
            return truncateString(serialized, maxLength);
        } catch (Exception e) {
            return "Failed to serialize object: " + e.getMessage();
        }
    }
    
    /**
     * 截斷字符串到指定長度
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        
        if (str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength) + "... [truncated]";
    }
}
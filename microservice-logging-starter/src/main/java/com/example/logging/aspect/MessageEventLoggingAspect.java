package com.example.logging.aspect;

import com.example.logging.annotation.LogMessageEvent;
import com.example.logging.model.MessageEventLog;
import com.example.logging.service.MessageLogService;
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
 * 消息事件日誌記錄切面
 * 攔截標記了 @LogMessageEvent 註解的方法，記錄消息處理信息
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageEventLoggingAspect {
    
    private final MessageLogService messageLogService;
    private final ObjectMapper objectMapper;
    
    @Around("@annotation(logMessageEvent)")
    public Object logMessageEvent(ProceedingJoinPoint joinPoint, LogMessageEvent logMessageEvent) throws Throwable {
        String messageId = UUID.randomUUID().toString();
        String eventType = logMessageEvent.eventType().isEmpty() 
            ? joinPoint.getSignature().getName() 
            : logMessageEvent.eventType();
        
        MessageEventLog eventLog = MessageEventLog.builder()
            .messageId(messageId)
            .eventType(eventType)
            .className(joinPoint.getTarget().getClass().getSimpleName())
            .methodName(joinPoint.getSignature().getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // 記錄消息載荷
        if (logMessageEvent.logPayload()) {
            try {
                String payload = serializeArgs(joinPoint.getArgs(), logMessageEvent.maxPayloadLength());
                eventLog.setPayload(payload);
            } catch (Exception e) {
                log.warn("Failed to serialize message payload for messageId: {}", messageId, e);
                eventLog.setPayload("Failed to serialize: " + e.getMessage());
            }
        }
        
        // 記錄消息標頭（如果需要）
        if (logMessageEvent.logHeaders()) {
            try {
                // 嘗試從參數中提取標頭信息
                String headers = extractHeaders(joinPoint.getArgs());
                eventLog.setHeaders(headers);
            } catch (Exception e) {
                log.warn("Failed to extract message headers for messageId: {}", messageId, e);
                eventLog.setHeaders("Failed to extract headers: " + e.getMessage());
            }
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            // 計算處理時間
            if (logMessageEvent.logProcessingTime()) {
                long processingTime = System.currentTimeMillis() - startTime;
                eventLog.setProcessingTimeMs(processingTime);
            }
            
            eventLog.setStatus("SUCCESS");
            
            // 保存日誌記錄
            messageLogService.saveMessageLog(eventLog);
            
            log.info("Message Event processed - MessageId: {}, EventType: {}, ProcessingTime: {}ms", 
                messageId, eventType, eventLog.getProcessingTimeMs());
            
            return result;
            
        } catch (Exception e) {
            // 處理異常情況
            if (logMessageEvent.logProcessingTime()) {
                long processingTime = System.currentTimeMillis() - startTime;
                eventLog.setProcessingTimeMs(processingTime);
            }
            
            eventLog.setStatus("ERROR");
            eventLog.setErrorMessage(truncateString(e.getMessage(), 1000));
            
            // 保存錯誤日誌
            messageLogService.saveMessageLog(eventLog);
            
            log.error("Message Event failed - MessageId: {}, EventType: {}, Error: {}", 
                messageId, eventType, e.getMessage(), e);
            
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
     * 從參數中提取標頭信息
     * 這是一個簡化的實現，實際使用時可能需要根據具體的消息格式進行調整
     */
    private String extractHeaders(Object[] args) {
        // 簡化實現：查找可能包含標頭的參數
        for (Object arg : args) {
            if (arg != null) {
                String className = arg.getClass().getSimpleName();
                if (className.toLowerCase().contains("header") || 
                    className.toLowerCase().contains("message")) {
                    try {
                        return objectMapper.writeValueAsString(arg);
                    } catch (Exception e) {
                        return "Failed to serialize headers: " + e.getMessage();
                    }
                }
            }
        }
        return "No headers found";
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
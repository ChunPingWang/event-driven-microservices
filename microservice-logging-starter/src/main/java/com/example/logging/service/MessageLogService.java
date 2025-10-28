package com.example.logging.service;

import com.example.logging.model.MessageEventLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息事件日誌服務接口
 * 定義消息事件日誌的業務操作
 */
public interface MessageLogService {
    
    /**
     * 保存消息事件日誌
     */
    void saveMessageLog(MessageEventLog messageLog);
    
    /**
     * 根據事件類型查找日誌記錄
     */
    List<MessageEventLog> findByEventType(String eventType);
    
    /**
     * 根據時間範圍查找日誌記錄
     */
    List<MessageEventLog> findByTimeRange(LocalDateTime start, LocalDateTime end);
    
    /**
     * 根據狀態查找日誌記錄
     */
    List<MessageEventLog> findByStatus(String status);
    
    /**
     * 查找處理時間超過指定閾值的慢消息
     */
    List<MessageEventLog> findSlowMessages(Long thresholdMs);
    
    /**
     * 根據消息ID查找日誌記錄
     */
    MessageEventLog findByMessageId(String messageId);
}
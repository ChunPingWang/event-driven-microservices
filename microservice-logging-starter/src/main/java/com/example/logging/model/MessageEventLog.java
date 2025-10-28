package com.example.logging.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息事件日誌實體
 * 記錄消息事件的詳細信息，包括載荷、標頭和處理狀態
 */
@Entity
@Table(name = "message_event_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEventLog {
    
    /**
     * 消息唯一標識符
     */
    @Id
    @Column(name = "message_id", length = 36)
    private String messageId;
    
    /**
     * 事件類型
     */
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;
    
    /**
     * 類名
     */
    @Column(name = "class_name", length = 200)
    private String className;
    
    /**
     * 方法名
     */
    @Column(name = "method_name", length = 100)
    private String methodName;
    
    /**
     * 消息載荷
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; 
   
    /**
     * 消息標頭
     */
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;
    
    /**
     * 處理狀態
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;
    
    /**
     * 錯誤信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 處理時間（毫秒）
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    /**
     * 時間戳
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    /**
     * 創建時間
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
package com.example.logging.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 請求日誌實體
 * 記錄 API 請求的詳細信息，包括請求參數、響應結果和執行時間
 */
@Entity
@Table(name = "api_request_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequestLog {
    
    /**
     * 請求唯一標識符
     */
    @Id
    @Column(name = "request_id", length = 36)
    private String requestId;
    
    /**
     * 操作名稱
     */
    @Column(name = "operation", length = 100, nullable = false)
    private String operation;
    
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
     * 請求載荷
     */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;
    
    /**
     * 響應載荷
     */
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;
    
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
     * 執行時間（毫秒）
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
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
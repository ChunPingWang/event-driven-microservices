package com.example.paymentservice.infrastructure.outbox;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 發件箱事件實體 - 實現 Outbox Pattern
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_processed", columnList = "processed, createdAt"),
    @Index(name = "idx_outbox_event_type", columnList = "eventType"),
    @Index(name = "idx_outbox_aggregate_id", columnList = "aggregateId")
})
public class OutboxEvent {
    
    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;
    
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "processed", nullable = false)
    private boolean processed = false;
    
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;
    
    @Column(name = "last_error")
    private String lastError;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // JPA 需要的默認構造函數
    protected OutboxEvent() {}
    
    /**
     * 創建新的發件箱事件
     * @param eventType 事件類型
     * @param aggregateId 聚合ID
     * @param aggregateType 聚合類型
     * @param payload 事件載荷
     */
    public OutboxEvent(String eventType, String aggregateId, String aggregateType, String payload) {
        this(eventType, aggregateId, aggregateType, payload, null);
    }
    
    /**
     * 創建新的發件箱事件（包含頭部信息）
     * @param eventType 事件類型
     * @param aggregateId 聚合ID
     * @param aggregateType 聚合類型
     * @param payload 事件載荷
     * @param headers 消息頭部
     */
    public OutboxEvent(String eventType, String aggregateId, String aggregateType, String payload, String headers) {
        validateConstructorParameters(eventType, aggregateId, aggregateType, payload);
        
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.payload = payload;
        this.headers = headers;
        this.createdAt = LocalDateTime.now();
        this.processed = false;
        this.retryCount = 0;
    }
    
    /**
     * 驗證構造函數參數
     */
    private void validateConstructorParameters(String eventType, String aggregateId, String aggregateType, String payload) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate type cannot be null or empty");
        }
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
    }
    
    /**
     * 標記事件為已處理
     */
    public void markAsProcessed() {
        if (this.processed) {
            throw new IllegalStateException("Event is already processed");
        }
        
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.lastError = null; // 清除錯誤信息
    }
    
    /**
     * 記錄處理失敗
     * @param errorMessage 錯誤信息
     */
    public void recordFailure(String errorMessage) {
        if (this.processed) {
            throw new IllegalStateException("Cannot record failure for processed event");
        }
        
        this.retryCount++;
        this.lastError = errorMessage;
    }
    
    /**
     * 檢查是否可以重試
     * @param maxRetries 最大重試次數
     * @return true 如果可以重試
     */
    public boolean canRetry(int maxRetries) {
        return !processed && retryCount < maxRetries;
    }
    
    /**
     * 檢查事件是否過期
     * @param maxAgeHours 最大存活時間（小時）
     * @return true 如果事件已過期
     */
    public boolean isExpired(int maxAgeHours) {
        LocalDateTime expiryTime = createdAt.plusHours(maxAgeHours);
        return LocalDateTime.now().isAfter(expiryTime);
    }
    
    // Getters
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public String getAggregateType() {
        return aggregateType;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public String getHeaders() {
        return headers;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public Long getVersion() {
        return version;
    }
    
    @Override
    public String toString() {
        return String.format("OutboxEvent{eventId='%s', eventType='%s', aggregateId='%s', processed=%s, retryCount=%d}", 
            eventId, eventType, aggregateId, processed, retryCount);
    }
}
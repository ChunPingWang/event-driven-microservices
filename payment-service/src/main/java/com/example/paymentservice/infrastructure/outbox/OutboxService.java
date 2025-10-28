package com.example.paymentservice.infrastructure.outbox;

import com.example.paymentservice.domain.shared.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 發件箱服務 - 處理領域事件到發件箱事件的轉換
 */
@Service
@Transactional
public class OutboxService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);
    
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 保存領域事件到發件箱
     * @param domainEvent 領域事件
     * @param aggregateId 聚合ID
     * @param aggregateType 聚合類型
     */
    public void saveEvent(DomainEvent domainEvent, String aggregateId, String aggregateType) {
        if (domainEvent == null) {
            throw new IllegalArgumentException("Domain event cannot be null");
        }
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate type cannot be null or empty");
        }
        
        try {
            String eventType = domainEvent.getClass().getSimpleName();
            String payload = serializeEvent(domainEvent);
            String headers = createHeaders(domainEvent);
            
            OutboxEvent outboxEvent = new OutboxEvent(eventType, aggregateId, aggregateType, payload, headers);
            outboxRepository.save(outboxEvent);
            
            logger.debug("Saved domain event to outbox: eventType={}, aggregateId={}", eventType, aggregateId);
            
        } catch (Exception e) {
            logger.error("Failed to save domain event to outbox: aggregateId={}, eventType={}", 
                aggregateId, domainEvent.getClass().getSimpleName(), e);
            throw new OutboxException("Failed to save event to outbox", e);
        }
    }
    
    /**
     * 獲取未處理的事件
     * @param batchSize 批次大小
     * @return 未處理的事件列表
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getUnprocessedEvents(int batchSize) {
        if (batchSize <= 0 || batchSize > 1000) {
            throw new IllegalArgumentException("Batch size must be between 1 and 1000");
        }
        
        return outboxRepository.findUnprocessedEvents(batchSize);
    }
    
    /**
     * 獲取可重試的事件
     * @param maxRetries 最大重試次數
     * @param batchSize 批次大小
     * @return 可重試的事件列表
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getRetryableEvents(int maxRetries, int batchSize) {
        if (maxRetries <= 0) {
            throw new IllegalArgumentException("Max retries must be positive");
        }
        if (batchSize <= 0 || batchSize > 1000) {
            throw new IllegalArgumentException("Batch size must be between 1 and 1000");
        }
        
        return outboxRepository.findRetryableEvents(maxRetries, batchSize);
    }
    
    /**
     * 標記事件為已處理
     * @param eventId 事件ID
     */
    public void markEventAsProcessed(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        
        OutboxEvent event = outboxRepository.findById(eventId)
            .orElseThrow(() -> new OutboxException("Outbox event not found: " + eventId));
        
        event.markAsProcessed();
        outboxRepository.save(event);
        
        logger.debug("Marked outbox event as processed: eventId={}", eventId);
    }
    
    /**
     * 記錄事件處理失敗
     * @param eventId 事件ID
     * @param errorMessage 錯誤信息
     */
    public void recordEventFailure(String eventId, String errorMessage) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        
        OutboxEvent event = outboxRepository.findById(eventId)
            .orElseThrow(() -> new OutboxException("Outbox event not found: " + eventId));
        
        event.recordFailure(errorMessage);
        outboxRepository.save(event);
        
        logger.warn("Recorded outbox event failure: eventId={}, retryCount={}, error={}", 
            eventId, event.getRetryCount(), errorMessage);
    }
    
    /**
     * 清理已處理的舊事件
     * @param olderThanHours 清理多少小時前的事件
     * @return 清理的事件數量
     */
    public int cleanupProcessedEvents(int olderThanHours) {
        if (olderThanHours <= 0) {
            throw new IllegalArgumentException("Hours must be positive");
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(olderThanHours);
        int deletedCount = outboxRepository.deleteProcessedEventsBefore(cutoffDate);
        
        logger.info("Cleaned up {} processed outbox events older than {} hours", deletedCount, olderThanHours);
        return deletedCount;
    }
    
    /**
     * 清理過期的失敗事件
     * @param olderThanHours 清理多少小時前的事件
     * @param maxRetries 最大重試次數
     * @return 清理的事件數量
     */
    public int cleanupExpiredFailedEvents(int olderThanHours, int maxRetries) {
        if (olderThanHours <= 0) {
            throw new IllegalArgumentException("Hours must be positive");
        }
        if (maxRetries <= 0) {
            throw new IllegalArgumentException("Max retries must be positive");
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(olderThanHours);
        int deletedCount = outboxRepository.deleteExpiredFailedEvents(cutoffDate, maxRetries);
        
        logger.info("Cleaned up {} expired failed outbox events older than {} hours", deletedCount, olderThanHours);
        return deletedCount;
    }
    
    /**
     * 獲取發件箱統計信息
     * @return 統計信息
     */
    @Transactional(readOnly = true)
    public OutboxStatistics getStatistics(int maxRetries) {
        long unprocessedCount = outboxRepository.countUnprocessedEvents();
        long failedCount = outboxRepository.countFailedEvents(maxRetries);
        long totalCount = outboxRepository.count();
        
        return new OutboxStatistics(totalCount, unprocessedCount, failedCount);
    }
    
    /**
     * 序列化領域事件
     */
    private String serializeEvent(DomainEvent domainEvent) throws JsonProcessingException {
        return objectMapper.writeValueAsString(domainEvent);
    }
    
    /**
     * 創建消息頭部
     */
    private String createHeaders(DomainEvent domainEvent) throws JsonProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put("eventType", domainEvent.getClass().getSimpleName());
        headers.put("eventId", domainEvent.getEventId());
        headers.put("occurredOn", domainEvent.getOccurredOn().toString());
        headers.put("version", "1.0");
        
        return objectMapper.writeValueAsString(headers);
    }
    
    /**
     * 發件箱統計信息
     */
    public static class OutboxStatistics {
        private final long totalEvents;
        private final long unprocessedEvents;
        private final long failedEvents;
        
        public OutboxStatistics(long totalEvents, long unprocessedEvents, long failedEvents) {
            this.totalEvents = totalEvents;
            this.unprocessedEvents = unprocessedEvents;
            this.failedEvents = failedEvents;
        }
        
        public long getTotalEvents() {
            return totalEvents;
        }
        
        public long getUnprocessedEvents() {
            return unprocessedEvents;
        }
        
        public long getFailedEvents() {
            return failedEvents;
        }
        
        public long getProcessedEvents() {
            return totalEvents - unprocessedEvents;
        }
        
        @Override
        public String toString() {
            return String.format("OutboxStatistics{total=%d, unprocessed=%d, failed=%d, processed=%d}", 
                totalEvents, unprocessedEvents, failedEvents, getProcessedEvents());
        }
    }
    
    /**
     * 發件箱異常
     */
    public static class OutboxException extends RuntimeException {
        public OutboxException(String message) {
            super(message);
        }
        
        public OutboxException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
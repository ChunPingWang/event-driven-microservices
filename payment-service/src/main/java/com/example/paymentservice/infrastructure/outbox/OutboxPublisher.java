package com.example.paymentservice.infrastructure.outbox;

import com.example.paymentservice.application.port.out.PaymentConfirmationPublisher;
import com.example.logging.annotation.LogMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 發件箱發布器 - 定時任務處理發件箱事件
 */
@Component
public class OutboxPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);
    
    private final OutboxService outboxService;
    private final PaymentConfirmationPublisher messagePublisher;
    
    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;
    
    @Value("${outbox.publisher.max-retries:5}")
    private int maxRetries;
    
    @Value("${outbox.publisher.cleanup.processed-events-hours:24}")
    private int cleanupProcessedEventsHours;
    
    @Value("${outbox.publisher.cleanup.failed-events-hours:72}")
    private int cleanupFailedEventsHours;
    
    public OutboxPublisher(OutboxService outboxService, PaymentConfirmationPublisher messagePublisher) {
        this.outboxService = outboxService;
        this.messagePublisher = messagePublisher;
    }
    
    /**
     * 定時發布未處理的事件 (每5秒執行一次)
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    @LogMessageEvent(eventType = "OUTBOX_PUBLISH_BATCH", logPayload = false)
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> unprocessedEvents = outboxService.getUnprocessedEvents(batchSize);
            
            if (unprocessedEvents.isEmpty()) {
                logger.trace("No unprocessed outbox events found");
                return;
            }
            
            logger.info("Processing {} unprocessed outbox events", unprocessedEvents.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (OutboxEvent event : unprocessedEvents) {
                try {
                    publishEvent(event);
                    outboxService.markEventAsProcessed(event.getEventId());
                    successCount++;
                    
                } catch (Exception e) {
                    String errorMessage = String.format("Failed to publish event: %s", e.getMessage());
                    outboxService.recordEventFailure(event.getEventId(), errorMessage);
                    failureCount++;
                    
                    logger.error("Failed to publish outbox event: eventId={}, eventType={}, retryCount={}", 
                        event.getEventId(), event.getEventType(), event.getRetryCount() + 1, e);
                }
            }
            
            logger.info("Outbox event processing completed: success={}, failure={}", successCount, failureCount);
            
        } catch (Exception e) {
            logger.error("Error during outbox event processing", e);
        }
    }
    
    /**
     * 定時重試失敗的事件 (每30秒執行一次)
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    @LogMessageEvent(eventType = "OUTBOX_RETRY_BATCH", logPayload = false)
    public void retryFailedEvents() {
        try {
            List<OutboxEvent> retryableEvents = outboxService.getRetryableEvents(maxRetries, batchSize);
            
            if (retryableEvents.isEmpty()) {
                logger.trace("No retryable outbox events found");
                return;
            }
            
            logger.info("Retrying {} failed outbox events", retryableEvents.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (OutboxEvent event : retryableEvents) {
                try {
                    // 使用指數退避策略
                    if (shouldRetryNow(event)) {
                        publishEvent(event);
                        outboxService.markEventAsProcessed(event.getEventId());
                        successCount++;
                        
                        logger.info("Successfully retried outbox event: eventId={}, retryCount={}", 
                            event.getEventId(), event.getRetryCount());
                    }
                    
                } catch (Exception e) {
                    String errorMessage = String.format("Retry failed: %s", e.getMessage());
                    outboxService.recordEventFailure(event.getEventId(), errorMessage);
                    failureCount++;
                    
                    logger.warn("Failed to retry outbox event: eventId={}, retryCount={}", 
                        event.getEventId(), event.getRetryCount() + 1, e);
                }
            }
            
            if (successCount > 0 || failureCount > 0) {
                logger.info("Outbox event retry completed: success={}, failure={}", successCount, failureCount);
            }
            
        } catch (Exception e) {
            logger.error("Error during outbox event retry processing", e);
        }
    }
    
    /**
     * 定時清理舊事件 (每小時執行一次)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    @LogMessageEvent(eventType = "OUTBOX_CLEANUP", logPayload = false)
    public void cleanupOldEvents() {
        try {
            // 清理已處理的舊事件
            int processedCleaned = outboxService.cleanupProcessedEvents(cleanupProcessedEventsHours);
            
            // 清理過期的失敗事件
            int failedCleaned = outboxService.cleanupExpiredFailedEvents(cleanupFailedEventsHours, maxRetries);
            
            if (processedCleaned > 0 || failedCleaned > 0) {
                logger.info("Outbox cleanup completed: processed={}, failed={}", processedCleaned, failedCleaned);
            }
            
        } catch (Exception e) {
            logger.error("Error during outbox cleanup", e);
        }
    }
    
    /**
     * 定時記錄統計信息 (每10分鐘執行一次)
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    @LogMessageEvent(eventType = "OUTBOX_STATISTICS", logPayload = false)
    public void logStatistics() {
        try {
            OutboxService.OutboxStatistics stats = outboxService.getStatistics(maxRetries);
            
            if (stats.getUnprocessedEvents() > 0 || stats.getFailedEvents() > 0) {
                logger.info("Outbox statistics: {}", stats);
                
                // 如果有太多未處理事件，記錄警告
                if (stats.getUnprocessedEvents() > 1000) {
                    logger.warn("High number of unprocessed outbox events: {}", stats.getUnprocessedEvents());
                }
                
                // 如果有太多失敗事件，記錄警告
                if (stats.getFailedEvents() > 100) {
                    logger.warn("High number of failed outbox events: {}", stats.getFailedEvents());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error while logging outbox statistics", e);
        }
    }
    
    /**
     * 發布單個事件
     * @param event 發件箱事件
     */
    @LogMessageEvent(eventType = "OUTBOX_EVENT_PUBLISH", logPayload = true)
    private void publishEvent(OutboxEvent event) {
        logger.debug("Publishing outbox event: eventId={}, eventType={}", event.getEventId(), event.getEventType());
        
        // 根據事件類型選擇適當的發布方式
        switch (event.getEventType()) {
            case "PaymentProcessedEvent":
            case "PaymentFailedEvent":
                messagePublisher.publishPaymentConfirmation(event.getPayload());
                break;
            default:
                logger.warn("Unknown event type for publishing: {}", event.getEventType());
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }
    
    /**
     * 判斷是否應該立即重試（使用指數退避策略）
     * @param event 發件箱事件
     * @return true 如果應該重試
     */
    private boolean shouldRetryNow(OutboxEvent event) {
        if (event.getRetryCount() == 0) {
            return true; // 第一次重試立即執行
        }
        
        // 計算退避時間：2^retryCount 分鐘，最大30分鐘
        long backoffMinutes = Math.min(30, (long) Math.pow(2, event.getRetryCount()));
        long backoffMillis = TimeUnit.MINUTES.toMillis(backoffMinutes);
        
        long timeSinceCreation = System.currentTimeMillis() - 
            event.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        return timeSinceCreation >= backoffMillis;
    }
}
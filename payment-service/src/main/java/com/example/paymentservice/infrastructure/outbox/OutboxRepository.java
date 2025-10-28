package com.example.paymentservice.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 發件箱事件倉儲接口
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {
    
    /**
     * 查找未處理的事件（按創建時間排序）
     * @param limit 限制數量
     * @return 未處理的事件列表
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnprocessedEvents(@Param("limit") int limit);
    
    /**
     * 查找可重試的失敗事件
     * @param maxRetries 最大重試次數
     * @param limit 限制數量
     * @return 可重試的事件列表
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findRetryableEvents(@Param("maxRetries") int maxRetries, @Param("limit") int limit);
    
    /**
     * 查找特定聚合的未處理事件
     * @param aggregateId 聚合ID
     * @return 未處理的事件列表
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.aggregateId = :aggregateId AND e.processed = false ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnprocessedEventsByAggregateId(@Param("aggregateId") String aggregateId);
    
    /**
     * 查找特定事件類型的未處理事件
     * @param eventType 事件類型
     * @param limit 限制數量
     * @return 未處理的事件列表
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.eventType = :eventType AND e.processed = false ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnprocessedEventsByType(@Param("eventType") String eventType, @Param("limit") int limit);
    
    /**
     * 統計未處理事件數量
     * @return 未處理事件數量
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = false")
    long countUnprocessedEvents();
    
    /**
     * 統計失敗事件數量
     * @param maxRetries 最大重試次數
     * @return 失敗事件數量
     */
    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.processed = false AND e.retryCount >= :maxRetries")
    long countFailedEvents(@Param("maxRetries") int maxRetries);
    
    /**
     * 刪除已處理的舊事件
     * @param beforeDate 刪除此日期之前的事件
     * @return 刪除的事件數量
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = true AND e.processedAt < :beforeDate")
    int deleteProcessedEventsBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * 刪除過期的失敗事件
     * @param beforeDate 刪除此日期之前的事件
     * @param maxRetries 最大重試次數
     * @return 刪除的事件數量
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = false AND e.retryCount >= :maxRetries AND e.createdAt < :beforeDate")
    int deleteExpiredFailedEvents(@Param("beforeDate") LocalDateTime beforeDate, @Param("maxRetries") int maxRetries);
    
    /**
     * 查找長時間未處理的事件（用於監控告警）
     * @param beforeDate 創建時間早於此日期的事件
     * @return 長時間未處理的事件列表
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.createdAt < :beforeDate ORDER BY e.createdAt ASC")
    List<OutboxEvent> findStaleEvents(@Param("beforeDate") LocalDateTime beforeDate);
}
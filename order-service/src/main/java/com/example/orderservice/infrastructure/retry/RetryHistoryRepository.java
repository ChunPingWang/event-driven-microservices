package com.example.orderservice.infrastructure.retry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 重試歷史倉儲接口
 */
@Repository
public interface RetryHistoryRepository extends JpaRepository<RetryHistory, Long> {
    
    /**
     * 根據訂單ID查找重試歷史
     * @param orderId 訂單ID
     * @return 重試歷史
     */
    Optional<RetryHistory> findByOrderId(String orderId);
    
    /**
     * 根據當前交易ID查找重試歷史
     * @param transactionId 交易ID
     * @return 重試歷史
     */
    Optional<RetryHistory> findByCurrentTransactionId(String transactionId);
    
    /**
     * 查找需要重試的記錄
     * @param currentTime 當前時間
     * @param maxAttempts 最大重試次數
     * @param limit 限制數量
     * @return 需要重試的記錄列表
     */
    @Query("SELECT r FROM RetryHistory r WHERE " +
           "r.status IN ('PENDING', 'RETRYING') AND " +
           "r.attemptCount < :maxAttempts AND " +
           "(r.nextRetryAt IS NULL OR r.nextRetryAt <= :currentTime) " +
           "ORDER BY r.firstAttemptAt ASC")
    List<RetryHistory> findRetryableRecords(@Param("currentTime") LocalDateTime currentTime,
                                          @Param("maxAttempts") int maxAttempts,
                                          @Param("limit") int limit);
    
    /**
     * 查找特定狀態的重試記錄
     * @param status 重試狀態
     * @param limit 限制數量
     * @return 重試記錄列表
     */
    @Query("SELECT r FROM RetryHistory r WHERE r.status = :status ORDER BY r.updatedAt DESC")
    List<RetryHistory> findByStatus(@Param("status") RetryHistory.RetryStatus status, @Param("limit") int limit);
    
    /**
     * 查找長時間未完成的重試記錄
     * @param beforeTime 時間閾值
     * @return 長時間未完成的記錄列表
     */
    @Query("SELECT r FROM RetryHistory r WHERE " +
           "r.status IN ('PENDING', 'RETRYING') AND " +
           "r.firstAttemptAt < :beforeTime " +
           "ORDER BY r.firstAttemptAt ASC")
    List<RetryHistory> findStaleRetries(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 統計各狀態的重試記錄數量
     * @return 重試統計信息
     */
    @Query("SELECT new com.example.orderservice.infrastructure.retry.RetryStatistics(" +
           "COUNT(CASE WHEN r.status = 'PENDING' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'RETRYING' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'SUCCESSFUL' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'FINALLY_FAILED' THEN 1 END), " +
           "AVG(r.attemptCount), " +
           "MAX(r.attemptCount)) " +
           "FROM RetryHistory r")
    RetryStatistics getRetryStatistics();
    
    /**
     * 統計特定時間範圍內的重試記錄
     * @param startTime 開始時間
     * @param endTime 結束時間
     * @return 重試統計信息
     */
    @Query("SELECT new com.example.orderservice.infrastructure.retry.RetryStatistics(" +
           "COUNT(CASE WHEN r.status = 'PENDING' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'RETRYING' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'SUCCESSFUL' THEN 1 END), " +
           "COUNT(CASE WHEN r.status = 'FINALLY_FAILED' THEN 1 END), " +
           "AVG(r.attemptCount), " +
           "MAX(r.attemptCount)) " +
           "FROM RetryHistory r WHERE r.createdAt BETWEEN :startTime AND :endTime")
    RetryStatistics getRetryStatistics(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 清理舊的成功重試記錄
     * @param beforeTime 時間閾值
     * @return 清理的記錄數量
     */
    @Modifying
    @Query("DELETE FROM RetryHistory r WHERE r.status = 'SUCCESSFUL' AND r.updatedAt < :beforeTime")
    int cleanupSuccessfulRetries(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 清理舊的最終失敗記錄
     * @param beforeTime 時間閾值
     * @return 清理的記錄數量
     */
    @Modifying
    @Query("DELETE FROM RetryHistory r WHERE r.status = 'FINALLY_FAILED' AND r.updatedAt < :beforeTime")
    int cleanupFinallyFailedRetries(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 查找特定訂單的所有重試嘗試
     * @param orderId 訂單ID
     * @return 重試嘗試列表
     */
    @Query("SELECT a FROM RetryAttempt a JOIN a.retryHistory r WHERE r.orderId = :orderId ORDER BY a.attemptNumber")
    List<RetryAttempt> findAttemptsByOrderId(@Param("orderId") String orderId);
    
    /**
     * 統計重試成功率
     * @param startTime 開始時間
     * @param endTime 結束時間
     * @return 成功率（0-1之間的小數）
     */
    @Query("SELECT CAST(COUNT(CASE WHEN r.status = 'SUCCESSFUL' THEN 1 END) AS DOUBLE) / COUNT(r) " +
           "FROM RetryHistory r WHERE r.createdAt BETWEEN :startTime AND :endTime")
    Double getRetrySuccessRate(@Param("startTime") LocalDateTime startTime, 
                              @Param("endTime") LocalDateTime endTime);
}
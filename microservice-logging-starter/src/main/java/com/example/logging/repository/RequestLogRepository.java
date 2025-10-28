package com.example.logging.repository;

import com.example.logging.model.ApiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API 請求日誌倉儲接口
 * 提供 API 請求日誌的數據訪問功能
 */
@Repository
public interface RequestLogRepository extends JpaRepository<ApiRequestLog, String> {
    
    /**
     * 根據操作名稱查找日誌記錄
     */
    List<ApiRequestLog> findByOperation(String operation);
    
    /**
     * 根據狀態查找日誌記錄
     */
    List<ApiRequestLog> findByStatus(String status);
    
    /**
     * 根據時間範圍查找日誌記錄
     */
    @Query("SELECT a FROM ApiRequestLog a WHERE a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<ApiRequestLog> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 根據類名和方法名查找日誌記錄
     */
    List<ApiRequestLog> findByClassNameAndMethodName(String className, String methodName);
    
    /**
     * 查找執行時間超過指定閾值的日誌記錄
     */
    @Query("SELECT a FROM ApiRequestLog a WHERE a.executionTimeMs > :threshold ORDER BY a.executionTimeMs DESC")
    List<ApiRequestLog> findSlowRequests(@Param("threshold") Long threshold);
}
package com.example.logging.repository;

import com.example.logging.model.MessageEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息事件日誌倉儲接口
 * 提供消息事件日誌的數據訪問功能
 */
@Repository
public interface MessageLogRepository extends JpaRepository<MessageEventLog, String> {
    
    /**
     * 根據事件類型查找日誌記錄
     */
    List<MessageEventLog> findByEventType(String eventType);
    
    /**
     * 根據狀態查找日誌記錄
     */
    List<MessageEventLog> findByStatus(String status);
    
    /**
     * 根據時間範圍查找日誌記錄
     */
    @Query("SELECT m FROM MessageEventLog m WHERE m.timestamp BETWEEN :start AND :end ORDER BY m.timestamp DESC")
    List<MessageEventLog> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 根據類名和方法名查找日誌記錄
     */
    List<MessageEventLog> findByClassNameAndMethodName(String className, String methodName);
    
    /**
     * 查找處理時間超過指定閾值的日誌記錄
     */
    @Query("SELECT m FROM MessageEventLog m WHERE m.processingTimeMs > :threshold ORDER BY m.processingTimeMs DESC")
    List<MessageEventLog> findSlowMessages(@Param("threshold") Long threshold);
}
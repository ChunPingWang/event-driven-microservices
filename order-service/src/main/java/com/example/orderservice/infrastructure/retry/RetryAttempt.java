package com.example.orderservice.infrastructure.retry;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 重試嘗試實體
 */
@Entity
@Table(name = "retry_attempts", indexes = {
    @Index(name = "idx_retry_attempt_transaction_id", columnList = "transactionId"),
    @Index(name = "idx_retry_attempt_created_at", columnList = "createdAt")
})
public class RetryAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retry_history_id", nullable = false)
    private RetryHistory retryHistory;
    
    @Column(name = "transaction_id", nullable = false, length = 36)
    private String transactionId;
    
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "result")
    private String result;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // JPA 需要的默認構造函數
    protected RetryAttempt() {}
    
    /**
     * 創建新的重試嘗試
     * @param retryHistory 重試歷史
     * @param transactionId 交易ID
     * @param attemptNumber 嘗試次數
     */
    public RetryAttempt(RetryHistory retryHistory, String transactionId, int attemptNumber) {
        if (retryHistory == null) {
            throw new IllegalArgumentException("Retry history cannot be null");
        }
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        if (attemptNumber <= 0) {
            throw new IllegalArgumentException("Attempt number must be positive");
        }
        
        this.retryHistory = retryHistory;
        this.transactionId = transactionId;
        this.attemptNumber = attemptNumber;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * 記錄成功結果
     * @param result 結果信息
     */
    public void recordSuccess(String result) {
        this.result = result;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;
    }
    
    /**
     * 記錄失敗結果
     * @param errorMessage 錯誤信息
     */
    public void recordFailure(String errorMessage) {
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        this.result = "FAILED";
    }
    
    /**
     * 檢查嘗試是否已完成
     * @return true 如果已完成
     */
    public boolean isCompleted() {
        return completedAt != null;
    }
    
    /**
     * 檢查嘗試是否成功
     * @return true 如果成功
     */
    public boolean isSuccessful() {
        return isCompleted() && errorMessage == null;
    }
    
    /**
     * 獲取嘗試持續時間（分鐘）
     * @return 持續時間，如果未完成則返回null
     */
    public Long getDurationMinutes() {
        if (!isCompleted()) {
            return null;
        }
        
        return java.time.Duration.between(createdAt, completedAt).toMinutes();
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public RetryHistory getRetryHistory() {
        return retryHistory;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public int getAttemptNumber() {
        return attemptNumber;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public String getResult() {
        return result;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    @Override
    public String toString() {
        return String.format("RetryAttempt{transactionId='%s', attemptNumber=%d, result='%s', completed=%s}", 
            transactionId, attemptNumber, result, isCompleted());
    }
}
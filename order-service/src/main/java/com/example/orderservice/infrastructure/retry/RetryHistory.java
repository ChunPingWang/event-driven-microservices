package com.example.orderservice.infrastructure.retry;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 重試歷史實體
 */
@Entity
@Table(name = "retry_history", indexes = {
    @Index(name = "idx_retry_order_id", columnList = "orderId"),
    @Index(name = "idx_retry_status", columnList = "status"),
    @Index(name = "idx_retry_next_retry_at", columnList = "nextRetryAt")
})
public class RetryHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;
    
    @Column(name = "original_transaction_id", nullable = false, length = 36)
    private String originalTransactionId;
    
    @Column(name = "current_transaction_id", length = 36)
    private String currentTransactionId;
    
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RetryStatus status = RetryStatus.PENDING;
    
    @Column(name = "first_attempt_at", nullable = false)
    private LocalDateTime firstAttemptAt;
    
    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @Column(name = "final_failure_reason")
    private String finalFailureReason;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "retryHistory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RetryAttempt> attempts = new ArrayList<>();
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // JPA 需要的默認構造函數
    protected RetryHistory() {}
    
    /**
     * 創建新的重試歷史
     * @param orderId 訂單ID
     * @param originalTransactionId 原始交易ID
     */
    public RetryHistory(String orderId, String originalTransactionId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (originalTransactionId == null || originalTransactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Original transaction ID cannot be null or empty");
        }
        
        this.orderId = orderId;
        this.originalTransactionId = originalTransactionId;
        this.currentTransactionId = originalTransactionId;
        this.attemptCount = 0;
        this.status = RetryStatus.PENDING;
        this.firstAttemptAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 記錄重試嘗試
     * @param newTransactionId 新的交易ID
     * @param nextRetryTime 下次重試時間
     */
    public void recordAttempt(String newTransactionId, LocalDateTime nextRetryTime) {
        if (newTransactionId == null || newTransactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("New transaction ID cannot be null or empty");
        }
        
        if (this.status == RetryStatus.FINALLY_FAILED) {
            throw new IllegalStateException("Cannot record attempt for finally failed retry");
        }
        
        this.attemptCount++;
        this.currentTransactionId = newTransactionId;
        this.lastAttemptAt = LocalDateTime.now();
        this.nextRetryAt = nextRetryTime;
        this.status = RetryStatus.RETRYING;
        this.updatedAt = LocalDateTime.now();
        
        // 添加重試嘗試記錄
        RetryAttempt attempt = new RetryAttempt(this, newTransactionId, this.attemptCount);
        this.attempts.add(attempt);
    }
    
    /**
     * 標記為最終失敗
     * @param reason 失敗原因
     */
    public void markAsFinallyFailed(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure reason cannot be null or empty");
        }
        
        this.status = RetryStatus.FINALLY_FAILED;
        this.finalFailureReason = reason;
        this.updatedAt = LocalDateTime.now();
        this.nextRetryAt = null; // 清除下次重試時間
    }
    
    /**
     * 標記為成功
     */
    public void markAsSuccessful() {
        this.status = RetryStatus.SUCCESSFUL;
        this.updatedAt = LocalDateTime.now();
        this.nextRetryAt = null; // 清除下次重試時間
    }
    
    /**
     * 檢查是否可以重試
     * @param maxAttempts 最大重試次數
     * @return true 如果可以重試
     */
    public boolean canRetry(int maxAttempts) {
        return this.status != RetryStatus.FINALLY_FAILED 
            && this.status != RetryStatus.SUCCESSFUL
            && this.attemptCount < maxAttempts;
    }
    
    /**
     * 獲取總重試時間（從第一次嘗試到現在）
     * @return 總重試時間（分鐘）
     */
    public long getTotalRetryTimeMinutes() {
        if (lastAttemptAt == null) {
            return 0;
        }
        
        return java.time.Duration.between(firstAttemptAt, lastAttemptAt).toMinutes();
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getOriginalTransactionId() {
        return originalTransactionId;
    }
    
    public String getCurrentTransactionId() {
        return currentTransactionId;
    }
    
    public int getAttemptCount() {
        return attemptCount;
    }
    
    public RetryStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getFirstAttemptAt() {
        return firstAttemptAt;
    }
    
    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }
    
    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }
    
    public String getFinalFailureReason() {
        return finalFailureReason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public List<RetryAttempt> getAttempts() {
        return new ArrayList<>(attempts);
    }
    
    public Long getVersion() {
        return version;
    }
    
    /**
     * 重試狀態枚舉
     */
    public enum RetryStatus {
        PENDING,        // 等待重試
        RETRYING,       // 重試中
        SUCCESSFUL,     // 重試成功
        FINALLY_FAILED  // 最終失敗
    }
    
    @Override
    public String toString() {
        return String.format("RetryHistory{orderId='%s', attemptCount=%d, status=%s, nextRetryAt=%s}", 
            orderId, attemptCount, status, nextRetryAt);
    }
}
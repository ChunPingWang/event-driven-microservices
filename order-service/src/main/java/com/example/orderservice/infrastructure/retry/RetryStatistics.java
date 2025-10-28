package com.example.orderservice.infrastructure.retry;

/**
 * 重試統計信息
 */
public class RetryStatistics {
    
    private final long pendingCount;
    private final long retryingCount;
    private final long successfulCount;
    private final long finallyFailedCount;
    private final double averageAttempts;
    private final int maxAttempts;
    
    public RetryStatistics(long pendingCount, long retryingCount, long successfulCount, 
                          long finallyFailedCount, double averageAttempts, int maxAttempts) {
        this.pendingCount = pendingCount;
        this.retryingCount = retryingCount;
        this.successfulCount = successfulCount;
        this.finallyFailedCount = finallyFailedCount;
        this.averageAttempts = averageAttempts;
        this.maxAttempts = maxAttempts;
    }
    
    /**
     * 獲取總記錄數
     */
    public long getTotalCount() {
        return pendingCount + retryingCount + successfulCount + finallyFailedCount;
    }
    
    /**
     * 獲取活躍重試數（等待中 + 重試中）
     */
    public long getActiveRetryCount() {
        return pendingCount + retryingCount;
    }
    
    /**
     * 獲取完成的重試數（成功 + 最終失敗）
     */
    public long getCompletedRetryCount() {
        return successfulCount + finallyFailedCount;
    }
    
    /**
     * 計算成功率
     */
    public double getSuccessRate() {
        long completedCount = getCompletedRetryCount();
        if (completedCount == 0) {
            return 0.0;
        }
        return (double) successfulCount / completedCount;
    }
    
    /**
     * 計算失敗率
     */
    public double getFailureRate() {
        long completedCount = getCompletedRetryCount();
        if (completedCount == 0) {
            return 0.0;
        }
        return (double) finallyFailedCount / completedCount;
    }
    
    // Getters
    public long getPendingCount() {
        return pendingCount;
    }
    
    public long getRetryingCount() {
        return retryingCount;
    }
    
    public long getSuccessfulCount() {
        return successfulCount;
    }
    
    public long getFinallyFailedCount() {
        return finallyFailedCount;
    }
    
    public double getAverageAttempts() {
        return averageAttempts;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    @Override
    public String toString() {
        return String.format(
            "RetryStatistics{total=%d, pending=%d, retrying=%d, successful=%d, failed=%d, " +
            "successRate=%.2f%%, avgAttempts=%.1f, maxAttempts=%d}",
            getTotalCount(), pendingCount, retryingCount, successfulCount, finallyFailedCount,
            getSuccessRate() * 100, averageAttempts, maxAttempts
        );
    }
}
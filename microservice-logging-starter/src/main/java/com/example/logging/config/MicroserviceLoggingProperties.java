package com.example.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 微服務日誌記錄配置屬性
 * 定義日誌記錄功能的可配置參數
 */
@ConfigurationProperties(prefix = "microservice.logging")
@Data
public class MicroserviceLoggingProperties {
    
    /**
     * 是否啟用日誌記錄功能
     */
    private boolean enabled = true;
    
    /**
     * 是否記錄請求載荷
     */
    private boolean logRequestPayload = true;
    
    /**
     * 是否記錄響應載荷
     */
    private boolean logResponsePayload = true;
    
    /**
     * 是否記錄消息載荷
     */
    private boolean logMessagePayload = true;
    
    /**
     * 是否記錄消息標頭
     */
    private boolean logMessageHeaders = false;
    
    /**
     * 最大載荷長度，超過此長度將被截斷
     */
    private int maxPayloadLength = 10000;
    
    /**
     * 排除模式列表，匹配的類或方法將不會被記錄
     */
    private List<String> excludePatterns = new ArrayList<>();
    
    /**
     * 是否記錄執行時間
     */
    private boolean logExecutionTime = true;
    
    /**
     * 是否記錄處理時間
     */
    private boolean logProcessingTime = true;
    
    /**
     * 慢請求閾值（毫秒），超過此時間的請求將被標記為慢請求
     */
    private long slowRequestThreshold = 5000;
    
    /**
     * 慢消息閾值（毫秒），超過此時間的消息處理將被標記為慢消息
     */
    private long slowMessageThreshold = 3000;
    
    /**
     * 是否啟用異步日誌記錄
     */
    private boolean asyncLogging = false;
    
    /**
     * 異步日誌記錄的線程池大小
     */
    private int asyncThreadPoolSize = 5;
}
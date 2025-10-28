package com.example.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 註解用於標記需要記錄 API 請求日誌的方法或類
 * 支持記錄請求參數、響應結果和執行時間
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogApiRequest {
    
    /**
     * 操作名稱，用於標識具體的業務操作
     * 如果為空，將使用方法名作為操作名稱
     */
    String operation() default "";
    
    /**
     * 是否記錄請求參數
     */
    boolean logRequest() default true;
    
    /**
     * 是否記錄響應結果
     */
    boolean logResponse() default true;
    
    /**
     * 是否記錄執行時間
     */
    boolean logExecutionTime() default true;
    
    /**
     * 最大載荷長度，超過此長度將被截斷
     */
    int maxPayloadLength() default 10000;
}
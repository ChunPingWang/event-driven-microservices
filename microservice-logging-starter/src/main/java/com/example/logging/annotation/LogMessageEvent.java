package com.example.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 註解用於標記需要記錄消息事件日誌的方法或類
 * 支持記錄消息載荷、標頭和處理狀態
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogMessageEvent {
    
    /**
     * 事件類型，用於標識具體的消息事件
     * 如果為空，將使用方法名作為事件類型
     */
    String eventType() default "";
    
    /**
     * 是否記錄消息載荷
     */
    boolean logPayload() default true;
    
    /**
     * 是否記錄消息標頭
     */
    boolean logHeaders() default false;
    
    /**
     * 最大載荷長度，超過此長度將被截斷
     */
    int maxPayloadLength() default 10000;
    
    /**
     * 是否記錄處理時間
     */
    boolean logProcessingTime() default true;
}
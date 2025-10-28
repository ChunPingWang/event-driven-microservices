package com.example.orderservice.domain.shared;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 領域事件抽象基類
 */
public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredOn;
    
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }
    
    /**
     * 獲取事件ID
     * @return 事件唯一標識符
     */
    public String getEventId() {
        return eventId;
    }
    
    /**
     * 獲取事件發生時間
     * @return 事件發生的時間戳
     */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
    
    /**
     * 獲取事件類型
     * @return 事件類型名稱
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
package com.example.orderservice.domain.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合根基類，支持領域事件的發布和管理
 */
public abstract class AggregateRoot {
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    /**
     * 添加領域事件
     * @param event 領域事件
     */
    protected void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    /**
     * 獲取所有領域事件（只讀）
     * @return 領域事件列表
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    /**
     * 清除所有領域事件
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
    
    /**
     * 檢查是否有待發布的領域事件
     * @return true 如果有待發布的事件
     */
    public boolean hasDomainEvents() {
        return !domainEvents.isEmpty();
    }
}
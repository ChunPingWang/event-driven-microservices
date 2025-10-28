package com.example.logging.service.impl;

import com.example.logging.model.MessageEventLog;
import com.example.logging.repository.MessageLogRepository;
import com.example.logging.service.MessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息事件日誌服務實現
 * 實現消息事件日誌的業務邏輯
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageLogServiceImpl implements MessageLogService {
    
    private final MessageLogRepository messageLogRepository;
    
    @Override
    @Transactional
    public void saveMessageLog(MessageEventLog messageLog) {
        try {
            messageLogRepository.save(messageLog);
            log.debug("Saved message event log with ID: {}", messageLog.getMessageId());
        } catch (Exception e) {
            log.error("Failed to save message event log with ID: {}", messageLog.getMessageId(), e);
            // 不重新拋出異常，避免影響主業務流程
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MessageEventLog> findByEventType(String eventType) {
        return messageLogRepository.findByEventType(eventType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MessageEventLog> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        return messageLogRepository.findByTimeRange(start, end);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MessageEventLog> findByStatus(String status) {
        return messageLogRepository.findByStatus(status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MessageEventLog> findSlowMessages(Long thresholdMs) {
        return messageLogRepository.findSlowMessages(thresholdMs);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MessageEventLog findByMessageId(String messageId) {
        return messageLogRepository.findById(messageId).orElse(null);
    }
}
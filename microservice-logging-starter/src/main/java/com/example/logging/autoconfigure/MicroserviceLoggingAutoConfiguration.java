package com.example.logging.autoconfigure;

import com.example.logging.aspect.ApiRequestLoggingAspect;
import com.example.logging.aspect.MessageEventLoggingAspect;
import com.example.logging.config.MicroserviceLoggingProperties;
import com.example.logging.repository.MessageLogRepository;
import com.example.logging.repository.RequestLogRepository;
import com.example.logging.service.MessageLogService;
import com.example.logging.service.RequestLogService;
import com.example.logging.service.impl.MessageLogServiceImpl;
import com.example.logging.service.impl.RequestLogServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 微服務日誌記錄自動配置類
 * 自動配置日誌記錄相關的 Bean 和組件
 */
@AutoConfiguration
@ConditionalOnClass({ApiRequestLoggingAspect.class, MessageEventLoggingAspect.class})
@ConditionalOnProperty(name = "microservice.logging.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MicroserviceLoggingProperties.class)
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = "com.example.logging.repository")
@EntityScan(basePackages = "com.example.logging.model")
@Slf4j
public class MicroserviceLoggingAutoConfiguration {
    
    /**
     * 配置 ObjectMapper Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    /**
     * 配置 RequestLogService Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestLogService requestLogService(RequestLogRepository repository) {
        log.info("Configuring RequestLogService");
        return new RequestLogServiceImpl(repository);
    }
    
    /**
     * 配置 MessageLogService Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageLogService messageLogService(MessageLogRepository repository) {
        log.info("Configuring MessageLogService");
        return new MessageLogServiceImpl(repository);
    }
    
    /**
     * 配置 API 請求日誌記錄切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "microservice.logging.api-request.enabled", havingValue = "true", matchIfMissing = true)
    public ApiRequestLoggingAspect apiRequestLoggingAspect(
            RequestLogService requestLogService, 
            ObjectMapper objectMapper) {
        log.info("Configuring ApiRequestLoggingAspect");
        return new ApiRequestLoggingAspect(requestLogService, objectMapper);
    }
    
    /**
     * 配置消息事件日誌記錄切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "microservice.logging.message-event.enabled", havingValue = "true", matchIfMissing = true)
    public MessageEventLoggingAspect messageEventLoggingAspect(
            MessageLogService messageLogService, 
            ObjectMapper objectMapper) {
        log.info("Configuring MessageEventLoggingAspect");
        return new MessageEventLoggingAspect(messageLogService, objectMapper);
    }
}
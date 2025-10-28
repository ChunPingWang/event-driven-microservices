package com.example.paymentservice.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 數據庫配置類
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.example.paymentservice.infrastructure.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class DatabaseConfig {
    
    /**
     * 生產環境數據源配置 (PostgreSQL)
     */
    @Bean
    @Profile({"sit", "uat", "prod"})
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }
    
    @Bean
    @Profile({"sit", "uat", "prod"})
    public DataSource dataSource(HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);
    }
    
    /**
     * JPA EntityManagerFactory 配置
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.paymentservice.domain", "com.example.paymentservice.infrastructure.outbox");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(hibernateProperties());
        
        return em;
    }
    
    /**
     * JPA 事務管理器
     */
    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
    
    /**
     * Hibernate 屬性配置
     */
    private Properties hibernateProperties() {
        Properties properties = new Properties();
        
        // 基本配置
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");
        
        // 連接池配置
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        properties.setProperty("hibernate.connection.autocommit", "false");
        
        // 查詢優化
        properties.setProperty("hibernate.jdbc.batch_size", "25");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        
        // 二級緩存配置 (可選)
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        properties.setProperty("hibernate.cache.use_query_cache", "false");
        
        // 統計信息
        properties.setProperty("hibernate.generate_statistics", "false");
        
        return properties;
    }
    
    /**
     * 開發環境特定配置
     */
    @Configuration
    @Profile("dev")
    static class DevelopmentConfig {
        
        @Bean
        public Properties devHibernateProperties() {
            Properties properties = new Properties();
            properties.setProperty("hibernate.show_sql", "true");
            properties.setProperty("hibernate.format_sql", "true");
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            return properties;
        }
    }
    
    /**
     * 測試環境特定配置
     */
    @Configuration
    @Profile("test")
    static class TestConfig {
        
        @Bean
        public Properties testHibernateProperties() {
            Properties properties = new Properties();
            properties.setProperty("hibernate.show_sql", "true");
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            properties.setProperty("hibernate.connection.pool_size", "5");
            return properties;
        }
    }
    
    /**
     * 生產環境特定配置
     */
    @Configuration
    @Profile({"sit", "uat", "prod"})
    static class ProductionConfig {
        
        @Bean
        public Properties prodHibernateProperties() {
            Properties properties = new Properties();
            properties.setProperty("hibernate.show_sql", "false");
            properties.setProperty("hibernate.hbm2ddl.auto", "validate");
            properties.setProperty("hibernate.connection.pool_size", "20");
            return properties;
        }
    }
}
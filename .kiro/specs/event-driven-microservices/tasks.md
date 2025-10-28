# 實現計劃

## 概述

本實現計劃將訂單處理和支付系統的設計轉換為具體的編碼任務。採用 DDD 領域建模、六角形架構、Spring AOP 日誌記錄，並創建共用的日誌 Starter。

## 任務清單

- [x] 1. 創建共用日誌 Starter 項目
  - 建立獨立的 Spring Boot Starter 項目，提供 AOP 日誌功能
  - 實現註解驅動的 API 請求和消息事件日誌記錄
  - _需求: 需求7, 需求8_

- [x] 1.1 設置日誌 Starter 項目結構
  - 創建 Gradle 多模塊項目結構
  - 配置 Spring Boot Starter 依賴和自動配置
  - _需求: 需求7_

- [x] 1.2 實現日誌記錄註解
  - 創建 @LogApiRequest 和 @LogMessageEvent 註解
  - 定義註解參數和配置選項
  - _需求: 需求7_

- [x] 1.3 實現 AOP 切面邏輯
  - 開發 ApiRequestLoggingAspect 處理 API 請求日誌
  - 開發 MessageEventLoggingAspect 處理消息事件日誌
  - 實現請求/響應序列化和異常處理
  - _需求: 需求7_

- [x] 1.4 創建日誌數據模型和服務
  - 定義 ApiRequestLog 和 MessageEventLog 實體
  - 實現 RequestLogService 和 MessageLogService
  - 創建對應的 Repository 接口
  - _需求: 需求6, 需求7_

- [x] 1.5 配置 Spring Boot 自動配置
  - 實現 MicroserviceLoggingAutoConfiguration
  - 創建 MicroserviceLoggingProperties 配置類
  - 配置 META-INF/spring.factories
  - _需求: 需求7_

- [x] 1.6 編寫日誌 Starter 單元測試
  - 測試 AOP 切面邏輯
  - 測試自動配置和條件裝配
  - 測試日誌記錄功能
  - _需求: 需求7_

- [x] 2. 建立項目基礎架構
  - 創建訂單服務和支付服務的基礎項目結構
  - 配置 Gradle 多模塊構建和依賴管理
  - _需求: 需求5_

- [x] 2.1 創建 Gradle 多模塊項目
  - 設置根項目和子模塊 (order-service, payment-service)
  - 配置共用依賴和版本管理
  - 引入日誌 Starter 依賴
  - _需求: 需求5_

- [x] 2.2 配置 Spring Boot 基礎設置
  - 創建 Application 主類和基礎配置
  - 配置多環境 Profile (dev, test, sit, prod)
  - 設置 H2 和 PostgreSQL 數據源配置
  - _需求: 需求5_

- [x] 2.3 集成 RabbitMQ 配置
  - 配置 RabbitMQ 連接和隊列定義
  - 設置死信隊列和重試機制
  - 配置消息序列化和反序列化
  - _需求: 需求2, 需求3_

- [x] 3. 實現 DDD 領域模型
  - 創建訂單和支付領域的聚合、實體、值對象
  - 實現領域服務和業務邏輯
  - _需求: 需求1, 需求4_

- [x] 3.1 實現訂單領域模型
  - 創建 Order 聚合根和相關值對象 (OrderId, CustomerId, Money)
  - 實現訂單狀態管理和業務邏輯方法
  - 定義訂單相關的領域事件
  - _需求: 需求1_

- [x] 3.2 實現支付領域模型
  - 創建 Payment 聚合根和 CreditCard 值對象
  - 實現支付處理邏輯和信用卡驗證
  - 定義支付相關的領域事件
  - _需求: 需求3, 需求4_

- [x] 3.3 創建聚合根基類和領域事件
  - 實現 AggregateRoot 基類支持領域事件
  - 定義 DomainEvent 抽象類和具體事件類
  - 實現領域事件發布機制
  - _需求: 需求1, 需求3_

- [x] 3.4 實現領域服務
  - 創建 PaymentDomainService 處理支付業務邏輯
  - 實現 CreditCardValidator 信用卡驗證服務
  - 定義領域服務接口和實現
  - _需求: 需求3, 需求4_

- [x] 3.5 編寫領域模型單元測試
  - 測試聚合根業務邏輯和狀態變更
  - 測試值對象驗證和不變性
  - 測試領域事件生成和發布
  - _需求: 需求7_

- [x] 4. BDD 測試實現 (測試先行)
  - 基於 Gherkin 場景實現 Cucumber 測試
  - 驗證端到端業務流程
  - 定義業務場景和驗收標準
  - _需求: 需求7_

- [x] 4.1 創建 Gherkin 業務場景
  - 編寫訂單創建和支付流程的 Feature 文件
  - 定義成功場景、失敗場景和邊界條件
  - 描述用戶故事和驗收標準
  - _需求: 需求1, 需求2, 需求3_

- [x] 4.2 實現 Cucumber 測試步驟定義
  - 創建 OrderStepDefinitions 實現訂單相關場景
  - 創建 PaymentStepDefinitions 實現支付相關場景
  - 實現測試數據準備和清理
  - _需求: 需求7_

- [x] 4.3 配置 Cucumber 測試環境
  - 設置 Testcontainers 用於集成測試
  - 配置測試數據庫和消息隊列
  - 實現測試工具類和輔助方法
  - _需求: 需求7_

- [x] 4.4 執行 BDD 測試場景
  - 運行成功訂單創建和支付流程測試
  - 運行錯誤處理和重試機制測試
  - 運行 Outbox Pattern 一致性測試
  - _需求: 需求7_

- [ ] 5. 實現六角形架構的應用層
  - 創建應用服務、命令處理器和查詢處理器
  - 實現端口接口和適配器模式
  - _需求: 需求5_

- [ ] 5.1 實現訂單應用服務
  - 創建 OrderApplicationService 協調訂單業務流程
  - 實現 CreateOrderCommand 和 OrderCommandHandler
  - 定義訂單相關的端口接口
  - _需求: 需求1, 需求5_

- [ ] 5.2 實現支付應用服務
  - 創建 PaymentApplicationService 處理支付流程
  - 實現 ProcessPaymentCommand 和 PaymentCommandHandler
  - 定義支付相關的端口接口
  - _需求: 需求3, 需求5_

- [ ] 5.3 創建查詢處理器
  - 實現 OrderQueryHandler 處理訂單查詢
  - 實現 PaymentQueryHandler 處理支付查詢
  - 定義查詢 DTO 和響應模型
  - _需求: 需求1, 需求3_

- [ ]* 5.4 編寫應用服務單元測試
  - 測試命令處理邏輯和業務流程協調
  - 測試查詢處理和數據轉換
  - 使用 Mock 隔離外部依賴
  - _需求: 需求7_

- [ ] 6. 實現基礎設施層適配器
  - 創建數據庫適配器、消息適配器和外部服務適配器
  - 實現 Repository 模式和 Outbox Pattern
  - _需求: 需求6, 需求8_

- [ ] 6.1 實現數據庫適配器
  - 創建 JPA 實體映射和 Repository 實現
  - 實現 OrderRepositoryImpl 和 PaymentRepositoryImpl
  - 配置數據庫連接池和事務管理
  - _需求: 需求6_

- [ ] 6.2 實現 Outbox Pattern
  - 創建 OutboxEvent 實體和 OutboxRepository
  - 實現 OutboxPublisher 定時任務
  - 確保數據庫操作和消息發送的強一致性
  - _需求: 需求8_

- [ ] 6.3 實現 RabbitMQ 消息適配器
  - 創建 MessagePublisherImpl 發送消息
  - 實現 PaymentRequestListener 監聽支付請求
  - 實現 PaymentConfirmationListener 監聽支付確認
  - _需求: 需求2, 需求3_

- [ ] 6.4 實現重試機制
  - 創建 PaymentRetryService 處理支付重試
  - 實現指數退避算法和最大重試限制
  - 記錄重試歷史和狀態追蹤
  - _需求: 需求2_

- [ ]* 6.5 編寫基礎設施層集成測試
  - 測試數據庫操作和事務處理
  - 測試消息發送和接收功能
  - 使用 Testcontainers 進行集成測試
  - _需求: 需求7_

- [ ] 7. 實現 Web 層控制器
  - 創建 REST API 控制器和請求/響應模型
  - 集成日誌記錄註解和異常處理
  - _需求: 需求1_

- [ ] 7.1 實現訂單控制器
  - 創建 OrderController 處理 HTTP 請求
  - 實現創建訂單和查詢訂單 API
  - 添加 @LogApiRequest 註解記錄 API 調用
  - _需求: 需求1_

- [ ] 7.2 創建請求/響應模型
  - 定義 CreateOrderRequest 和 OrderResponse DTO
  - 實現請求驗證和數據轉換
  - 配置 JSON 序列化和反序列化
  - _需求: 需求1, 需求4_

- [ ] 7.3 實現全局異常處理
  - 創建 GlobalExceptionHandler 統一異常處理
  - 定義錯誤響應格式和狀態碼
  - 記錄異常日誌和錯誤追蹤
  - _需求: 需求1_

- [ ]* 7.4 編寫控制器單元測試
  - 測試 API 端點和請求處理
  - 測試異常處理和錯誤響應
  - 使用 MockMvc 進行 Web 層測試
  - _需求: 需求7_

- [ ] 8. 實現領域事件處理
  - 創建領域事件發布器和事件處理器
  - 集成消息發送和日誌記錄
  - _需求: 需求1, 需求3_

- [ ] 8.1 實現領域事件發布器
  - 創建 DomainEventPublisher 處理領域事件
  - 實現事件到消息的轉換邏輯
  - 添加 @LogMessageEvent 註解記錄事件處理
  - _需求: 需求1, 需求3_

- [ ] 8.2 實現事件處理器
  - 創建 PaymentRequestedEventHandler
  - 創建 PaymentProcessedEventHandler
  - 實現事件處理邏輯和錯誤處理
  - _需求: 需求1, 需求3_

- [ ]* 8.3 編寫事件處理單元測試
  - 測試事件發布和處理邏輯
  - 測試事件到消息的轉換
  - 驗證日誌記錄功能
  - _需求: 需求7_

- [ ] 9. 配置和部署準備
  - 創建 Docker 配置和部署腳本
  - 配置監控和日誌收集
  - _需求: 需求5_

- [ ] 9.1 創建 Docker 配置
  - 編寫 Dockerfile 用於服務容器化
  - 創建 docker-compose.yml 用於本地開發
  - 配置多環境的容器編排
  - _需求: 需求5_

- [ ] 9.2 配置應用監控
  - 集成 Spring Boot Actuator
  - 配置健康檢查和指標收集
  - 設置日誌格式和輸出配置
  - _需求: 需求5_

- [ ] 9.3 創建數據庫遷移腳本
  - 編寫 Flyway 或 Liquibase 遷移腳本
  - 創建表結構和索引定義
  - 配置多環境的數據庫初始化
  - _需求: 需求6_

- [ ] 10. 系統集成和驗證
  - 整合所有組件並進行端到端測試
  - 驗證日誌記錄和監控功能
  - _需求: 需求1, 需求2, 需求3, 需求7_

- [ ] 10.1 集成測試執行
  - 啟動完整的微服務環境
  - 執行端到端業務流程測試
  - 驗證消息傳遞和數據一致性
  - _需求: 需求1, 需求2, 需求3_

- [ ] 10.2 日誌功能驗證
  - 驗證 API 請求日誌記錄功能
  - 驗證消息事件日誌記錄功能
  - 檢查日誌數據的完整性和準確性
  - _需求: 需求7_

- [ ] 10.3 性能和負載測試
  - 執行高並發訂單處理測試
  - 測試消息隊列積壓處理能力
  - 驗證系統在負載下的穩定性
  - _需求: 需求2, 需求3_
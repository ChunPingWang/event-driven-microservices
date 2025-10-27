# Event-Driven Microservices System

## 專案概述

這是一個基於事件驅動架構的微服務系統，實現訂單處理和信用卡支付功能。系統採用 DDD 領域建模、六角形架構，並使用 RabbitMQ 進行服務間通信。

## 技術架構

- **架構模式**: 六角形架構 (Hexagonal Architecture)
- **設計方法**: DDD 領域驅動設計 (Domain-Driven Design)
- **消息中間件**: RabbitMQ
- **數據一致性**: Outbox Pattern
- **日誌記錄**: Spring AOP 橫切關注點
- **開發框架**: Spring Boot 3.x, JDK 17
- **構建工具**: Gradle
- **測試框架**: JUnit 5, Mockito, Cucumber (BDD)

## 系統組件

### 微服務
- **訂單服務 (Order Service)**: 處理訂單創建和管理
- **支付服務 (Payment Service)**: 處理信用卡支付交易

### 共用組件
- **日誌 Starter**: 提供統一的 AOP 日誌記錄功能

### 基礎設施
- **RabbitMQ**: 服務間消息傳遞
- **PostgreSQL**: 生產環境數據庫
- **H2**: 開發和測試環境數據庫

## 核心特性

- ✅ **可靠消息傳遞**: 實現重試機制和確認機制
- ✅ **強數據一致性**: 通過 Outbox Pattern 確保數據庫與消息的一致性
- ✅ **完整審計追蹤**: 僅插入操作，維護完整的交易歷史
- ✅ **統一日誌記錄**: 使用 AOP 記錄所有 API 請求和消息事件
- ✅ **高度可測試**: 六角形架構支持完全的依賴隔離
- ✅ **BDD 測試**: 基於 Gherkin 場景的業務驗證

## 開發指南

### 環境要求
- JDK 17+
- Gradle 8.0+
- Docker & Docker Compose
- RabbitMQ (通過 Docker)
- PostgreSQL (生產環境)

### 快速開始

1. **克隆專案**
   ```bash
   git clone <repository-url>
   cd event-driven-for-microservices
   ```

2. **啟動基礎設施**
   ```bash
   docker-compose up -d rabbitmq postgres
   ```

3. **構建專案**
   ```bash
   ./gradlew build
   ```

4. **運行服務**
   ```bash
   # 啟動訂單服務
   ./gradlew :order-service:bootRun
   
   # 啟動支付服務
   ./gradlew :payment-service:bootRun
   ```

### 開發流程

本專案採用 **Spec 驅動開發 (SDD)** 方法：

1. **需求階段** - 定義業務需求和驗收標準
2. **設計階段** - 創建技術設計和架構方案  
3. **任務階段** - 將設計轉換為具體實現任務
4. **實現階段** - 按任務清單進行編碼開發

### 專案結構

```
event-driven-for-microservices/
├── .kiro/specs/event-driven-microservices/
│   ├── requirements.md          # 業務需求文檔
│   ├── design.md               # 技術設計文檔
│   ├── tasks.md                # 實現任務清單
│   └── gherkin-scenarios.feature # BDD 測試場景
├── microservice-logging-starter/  # 共用日誌 Starter
├── order-service/                 # 訂單微服務
├── payment-service/               # 支付微服務
├── docker-compose.yml            # 本地開發環境
└── README.md                     # 專案說明
```

## 實現進度

- [x] 需求分析和設計完成
- [ ] 共用日誌 Starter 開發
- [ ] 訂單服務實現
- [ ] 支付服務實現
- [ ] 集成測試和 BDD 驗證
- [ ] 部署和監控配置

## 貢獻指南

1. 查看 `tasks.md` 了解當前開發任務
2. 選擇未完成的任務進行開發
3. 遵循 DDD 和六角形架構原則
4. 編寫單元測試和集成測試
5. 提交前運行完整測試套件

## 文檔

- [需求文檔](.kiro/specs/event-driven-microservices/requirements.md)
- [設計文檔](.kiro/specs/event-driven-microservices/design.md)
- [任務清單](.kiro/specs/event-driven-microservices/tasks.md)
- [BDD 場景](.kiro/specs/event-driven-microservices/gherkin-scenarios.feature)

## 授權

本專案採用 MIT 授權條款。
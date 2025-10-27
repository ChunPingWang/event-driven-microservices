# 需求文檔

## 介紹

本文檔概述了實現訂單處理和信用卡支付系統的需求，該系統採用六角形架構，使用 RabbitMQ 進行服務間通信，並實現可靠的支付處理和確認機制。系統將維護完整的交易歷史記錄，支持重試機制，並遵循 SOLID 原則。

## 術語表

- **Order_Service（訂單服務）**: 處理訂單創建和管理的微服務，提供外部 API 接口
- **Payment_Service（信用卡支付服務）**: 處理信用卡支付交易的微服務
- **RabbitMQ_Broker（RabbitMQ代理）**: 負責服務間消息傳遞的消息中間件
- **Payment_Request（支付請求）**: 包含支付所需信息的消息載體
- **Payment_Confirmation（支付確認）**: 支付服務回傳給訂單服務的處理結果
- **Retry_Mechanism（重試機制）**: 當未收到支付確認時的自動重試邏輯
- **Outbox_Pattern（發件箱模式）**: 確保數據庫操作與消息發送的強一致性模式
- **Outbox_Table（發件箱表）**: 存儲待發送消息的數據庫表
- **Message_Publisher（消息發布器）**: 負責從發件箱表讀取並發送消息的組件
- **Hexagonal_Architecture（六角形架構）**: 將業務邏輯與外部依賴隔離的架構模式
- **Order_Database（訂單數據庫）**: 訂單服務專用的數據存儲
- **Payment_Database（支付數據庫）**: 支付服務專用的數據存儲

## 需求

### 需求 1

**用戶故事：** 作為電商平台，我希望通過 API 創建訂單並觸發支付流程，以便完成客戶的購買交易。

#### 驗收標準

1. Order_Service 應提供 REST API 接口接收訂單創建請求
2. 當收到訂單請求時，Order_Service 應驗證訂單數據的完整性和有效性
3. Order_Service 應在 Order_Database 中記錄訂單信息
4. Order_Service 應通過 RabbitMQ_Broker 將 Payment_Request 發送給 Payment_Service
5. Order_Service 應為每個支付請求生成唯一的交易標識符

### 需求 2

**用戶故事：** 作為訂單服務，我希望可靠地將支付請求傳遞給支付服務，以便確保支付處理不會丟失。

#### 驗收標準

1. 當發送 Payment_Request 時，Order_Service 應在 Order_Database 中記錄支付請求狀態
2. Order_Service 應等待來自 Payment_Service 的 Payment_Confirmation
3. 如果在指定時間內未收到 Payment_Confirmation，Order_Service 應啟動 Retry_Mechanism
4. Order_Service 應在 Order_Database 中記錄每次重試嘗試和狀態變更
5. Order_Service 應實現指數退避策略進行重試，最多重試5次

### 需求 3

**用戶故事：** 作為支付服務，我希望接收並處理信用卡支付請求，以便完成客戶的付款交易。

#### 驗收標準

1. Payment_Service 應監聽 RabbitMQ_Broker 中的支付請求隊列
2. 當收到 Payment_Request 時，Payment_Service 應在 Payment_Database 中記錄支付請求信息
3. Payment_Service 應驗證信用卡信息並處理支付交易
4. Payment_Service 應使用 Outbox_Pattern 確保支付結果記錄與消息發送的強一致性
5. Payment_Service 應在同一數據庫事務中更新支付狀態和 Outbox_Table

### 需求 4

**用戶故事：** 作為開發人員，我希望支付請求包含完整的支付信息，以便支付服務能夠處理信用卡交易。

#### 驗收標準

1. Payment_Request 應包含訂單ID、客戶ID、交易金額和貨幣類型
2. Payment_Request 應包含信用卡號碼、到期日期、CVV和持卡人姓名
3. Payment_Request 應包含帳單地址信息（街道、城市、郵遞區號、國家）
4. Payment_Request 應包含商戶ID和交易描述信息
5. Payment_Request 應包含時間戳和請求來源標識

### 需求 5

**用戶故事：** 作為開發人員，我希望系統採用六角形架構，以便實現高度可測試和可維護的代碼。

#### 驗收標準

1. Order_Service 和 Payment_Service 應實現六角形架構模式
2. 業務邏輯應與外部依賴（數據庫、RabbitMQ）完全隔離
3. 所有外部依賴應通過接口進行抽象
4. 系統應使用 Spring 依賴注入容器管理組件依賴關係
5. 架構應遵循 SOLID 原則的所有五個原則

### 需求 6

**用戶故事：** 作為開發人員，我希望維護完整的交易歷史記錄，以便進行審計和故障排查。

#### 驗收標準

1. Order_Database 和 Payment_Database 應只執行插入操作，不進行更新操作
2. 所有訂單和支付活動應在相應數據庫中留下不可變記錄
3. 每條記錄應包含時間戳、交易ID、狀態和相關元數據
4. 系統應支持基於歷史記錄的交易追蹤和分析
5. 數據庫記錄應包含足夠信息以重建完整的支付流程

### 需求 7

**用戶故事：** 作為開發人員，我希望編寫全面的單元測試，以便確保支付系統的質量和可靠性。

#### 驗收標準

1. 所有業務邏輯組件應有對應的 JUnit 單元測試
2. 外部依賴應使用 Mockito 進行模擬測試
3. 測試應能夠獨立運行，不依賴外部系統（數據庫、RabbitMQ）
4. 測試覆蓋率應達到業務邏輯代碼的 90% 以上
5. 測試應驗證正常支付流程和異常情況的處理

### 需求 8

**用戶故事：** 作為支付服務，我希望實現 Outbox Pattern，以便確保支付處理結果與消息發送的強一致性。

#### 驗收標準

1. Payment_Service 應在 Payment_Database 中創建 Outbox_Table 存儲待發送消息
2. 當處理支付時，Payment_Service 應在同一事務中更新支付狀態和插入 Outbox_Table 記錄
3. Message_Publisher 應定期掃描 Outbox_Table 並發送未處理的消息
4. 當消息成功發送後，Message_Publisher 應標記 Outbox_Table 中的記錄為已處理
5. 如果消息發送失敗，Message_Publisher 應實現重試機制直到成功發送
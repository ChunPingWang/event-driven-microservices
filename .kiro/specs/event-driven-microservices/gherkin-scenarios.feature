# Gherkin 業務場景

## 訂單和支付處理 BDD 場景

### 場景 1: 成功的訂單創建和支付流程

```gherkin
Feature: 訂單創建和支付處理
  作為電商平台
  我希望能夠創建訂單並處理支付
  以便完成客戶的購買交易

  Background:
    Given 訂單服務和支付服務都已啟動
    And RabbitMQ 消息中間件正常運行
    And 數據庫連接正常

  Scenario: 成功創建訂單並完成支付
    Given 客戶 "CUST001" 想要購買商品
    When 我發送創建訂單請求包含以下信息:
      | 客戶ID    | 金額   | 貨幣 | 信用卡號           | 到期日期 | CVV | 持卡人姓名 |
      | CUST001  | 100.00 | USD | 4111111111111111  | 12/25   | 123 | John Doe  |
    Then 訂單應該被成功創建
    And 訂單狀態應該是 "PAYMENT_PENDING"
    And 支付請求應該被發送到 RabbitMQ
    And 支付服務應該收到支付請求
    And 支付應該被成功處理
    And 支付確認應該被發送回訂單服務
    And 訂單狀態應該更新為 "PAYMENT_CONFIRMED"

  Scenario: 信用卡驗證失敗
    Given 客戶 "CUST002" 想要購買商品
    When 我發送創建訂單請求包含無效的信用卡信息:
      | 客戶ID    | 金額   | 貨幣 | 信用卡號      | 到期日期 | CVV | 持卡人姓名 |
      | CUST002  | 50.00  | USD | 1234567890   | 01/20   | 999 | Jane Doe  |
    Then 訂單應該被創建
    And 支付請求應該被發送到支付服務
    And 支付服務應該拒絕支付請求
    And 支付失敗確認應該被發送回訂單服務
    And 訂單狀態應該更新為 "PAYMENT_FAILED"

  Scenario: 支付服務暫時不可用時的重試機制
    Given 客戶 "CUST003" 想要購買商品
    And 支付服務暫時不可用
    When 我發送創建訂單請求包含以下信息:
      | 客戶ID    | 金額   | 貨幣 | 信用卡號           | 到期日期 | CVV | 持卡人姓名   |
      | CUST003  | 75.00  | USD | 4111111111111111  | 06/26   | 456 | Alice Smith |
    Then 訂單應該被創建
    And 支付請求應該被發送但失敗
    And 系統應該記錄重試嘗試
    When 支付服務恢復正常
    And 重試機制被觸發
    Then 支付請求應該被重新發送
    And 支付應該被成功處理
    And 訂單狀態應該更新為 "PAYMENT_CONFIRMED"
```

### 場景 2: Outbox Pattern 一致性保證

```gherkin
Feature: Outbox Pattern 數據一致性
  作為支付服務
  我希望確保支付處理和消息發送的強一致性
  以便避免數據不一致的問題

  Scenario: 支付成功但消息發送失敗時的一致性保證
    Given 支付服務正常運行
    And RabbitMQ 暫時不可用
    When 支付服務收到有效的支付請求
    Then 支付應該被成功處理
    And 支付記錄應該被保存到數據庫
    And 支付確認消息應該被保存到發件箱表
    And 消息狀態應該標記為 "未處理"
    When RabbitMQ 恢復正常
    And 發件箱發布器運行
    Then 未處理的消息應該被發送到 RabbitMQ
    And 消息狀態應該更新為 "已處理"
    And 訂單服務應該收到支付確認

  Scenario: 發件箱發布器的重試機制
    Given 發件箱表中有未處理的消息
    And RabbitMQ 連接不穩定
    When 發件箱發布器嘗試發送消息
    And 消息發送失敗
    Then 消息應該保持 "未處理" 狀態
    And 系統應該在下次調度時重試
    When RabbitMQ 連接恢復
    And 發件箱發布器再次運行
    Then 消息應該被成功發送
    And 消息狀態應該更新為 "已處理"
```

### 場景 3: 錯誤處理和恢復

```gherkin
Feature: 錯誤處理和系統恢復
  作為系統管理員
  我希望系統能夠優雅地處理各種錯誤情況
  以便確保系統的穩定性和可靠性

  Scenario: 數據庫連接失敗的處理
    Given 訂單服務正常運行
    When 數據庫連接突然中斷
    And 客戶嘗試創建新訂單
    Then 系統應該返回適當的錯誤響應
    And 錯誤信息應該被記錄
    And 客戶應該收到友好的錯誤消息
    When 數據庫連接恢復
    Then 系統應該自動恢復正常操作

  Scenario: 消息處理失敗的死信隊列處理
    Given 支付服務正常運行
    When 支付服務收到格式錯誤的消息
    And 消息處理失敗超過最大重試次數
    Then 消息應該被路由到死信隊列
    And 錯誤應該被記錄和告警
    And 系統管理員應該收到通知

  Scenario: 系統負載過高時的限流處理
    Given 系統正常運行
    When 短時間內收到大量訂單請求
    And 系統負載超過閾值
    Then 系統應該啟動限流機制
    And 部分請求應該被延遲處理
    And 客戶應該收到適當的響應
    And 系統性能指標應該被監控
```

### 場景 4: 業務規則驗證

```gherkin
Feature: 業務規則驗證
  作為業務系統
  我希望確保所有業務規則得到正確執行
  以便維護數據完整性和業務邏輯正確性

  Scenario: 訂單金額驗證
    Given 客戶想要創建訂單
    When 我發送創建訂單請求包含負數金額:
      | 客戶ID    | 金額   | 貨幣 |
      | CUST004  | -10.00 | USD |
    Then 訂單創建應該被拒絕
    And 系統應該返回 "金額必須大於零" 的錯誤消息

  Scenario: 信用卡到期日期驗證
    Given 客戶想要使用過期的信用卡付款
    When 我發送支付請求包含過期的信用卡:
      | 信用卡號           | 到期日期 | CVV |
      | 4111111111111111  | 01/20   | 123 |
    Then 支付應該被拒絕
    And 系統應該返回 "信用卡已過期" 的錯誤消息

  Scenario: 重複訂單檢測
    Given 客戶 "CUST005" 已經創建了一個訂單 "ORDER001"
    When 客戶嘗試使用相同的訂單ID創建新訂單
    Then 系統應該檢測到重複訂單
    And 訂單創建應該被拒絕
    And 系統應該返回 "訂單ID已存在" 的錯誤消息
```

### 場景 5: 性能和擴展性

```gherkin
Feature: 系統性能和擴展性
  作為系統架構師
  我希望系統能夠處理高並發和大量數據
  以便滿足業務增長需求

  Scenario: 高並發訂單處理
    Given 系統正常運行
    When 同時收到 100 個訂單創建請求
    Then 所有訂單應該在 5 秒內被處理
    And 沒有訂單應該丟失
    And 系統響應時間應該保持在可接受範圍內

  Scenario: 大量歷史數據查詢性能
    Given 系統中有 100萬 條歷史訂單記錄
    When 客戶查詢特定時間範圍的訂單
    Then 查詢應該在 2 秒內返回結果
    And 結果應該準確完整

  Scenario: 消息隊列積壓處理
    Given RabbitMQ 中積壓了 1000 條待處理消息
    When 支付服務重新啟動
    Then 所有積壓消息應該在 10 分鐘內被處理完成
    And 消息處理順序應該保持正確
    And 沒有消息應該丟失或重複處理
```
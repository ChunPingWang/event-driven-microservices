# Microservice Logging Starter

這是一個 Spring Boot Starter，提供基於 AOP 的微服務日誌記錄功能。

## 功能特性

- **API 請求日誌記錄**: 使用 `@LogApiRequest` 註解記錄 REST API 請求和響應
- **消息事件日誌記錄**: 使用 `@LogMessageEvent` 註解記錄消息處理事件
- **自動配置**: 零配置自動啟用日誌記錄功能
- **可配置**: 支持豐富的配置選項
- **數據持久化**: 自動將日誌記錄保存到數據庫

## 使用方法

### 1. 添加依賴

在你的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':microservice-logging-starter')
}
```

### 2. 使用註解

#### API 請求日誌記錄

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @PostMapping
    @LogApiRequest(operation = "CREATE_ORDER", logRequest = true, logResponse = true)
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // 業務邏輯
    }
}
```

#### 消息事件日誌記錄

```java
@Component
public class PaymentListener {
    
    @RabbitListener(queues = "payment.request.queue")
    @LogMessageEvent(eventType = "PAYMENT_REQUEST_RECEIVED", logPayload = true)
    public void handlePaymentRequest(PaymentRequest request) {
        // 消息處理邏輯
    }
}
```

### 3. 配置選項

在 `application.yml` 中配置：

```yaml
microservice:
  logging:
    enabled: true
    log-request-payload: true
    log-response-payload: true
    max-payload-length: 10000
    slow-request-threshold: 5000
```

## 數據庫表結構

Starter 會自動創建以下表：

- `api_request_logs`: 存儲 API 請求日誌
- `message_event_logs`: 存儲消息事件日誌

## 註解參數

### @LogApiRequest

- `operation`: 操作名稱
- `logRequest`: 是否記錄請求參數
- `logResponse`: 是否記錄響應結果
- `logExecutionTime`: 是否記錄執行時間
- `maxPayloadLength`: 最大載荷長度

### @LogMessageEvent

- `eventType`: 事件類型
- `logPayload`: 是否記錄消息載荷
- `logHeaders`: 是否記錄消息標頭
- `logProcessingTime`: 是否記錄處理時間
- `maxPayloadLength`: 最大載荷長度
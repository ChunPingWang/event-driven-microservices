#!/bin/bash

# 系統集成測試腳本
# 此腳本將啟動完整的微服務環境並執行端到端測試

set -e

echo "🚀 開始系統集成測試..."

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 函數：打印帶顏色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 函數：檢查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        print_message $RED "❌ 錯誤: $1 未安裝"
        exit 1
    fi
}

# 檢查必要的工具
print_message $BLUE "🔍 檢查必要工具..."
check_command docker
check_command docker-compose
check_command gradle

# 清理之前的容器和鏡像
print_message $YELLOW "🧹 清理之前的容器..."
docker-compose down -v --remove-orphans 2>/dev/null || true
docker system prune -f

# 構建應用程序
print_message $BLUE "🔨 構建應用程序..."
./gradlew clean build -x test

# 構建 Docker 鏡像
print_message $BLUE "🐳 構建 Docker 鏡像..."
docker build -t order-service:latest -f order-service/Dockerfile .
docker build -t payment-service:latest -f payment-service/Dockerfile .

# 啟動基礎設施服務
print_message $BLUE "🚀 啟動基礎設施服務..."
docker-compose up -d order-db payment-db rabbitmq

# 等待基礎設施服務就緒
print_message $YELLOW "⏳ 等待基礎設施服務就緒..."
sleep 30

# 檢查數據庫連接
print_message $BLUE "🔍 檢查數據庫連接..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if docker exec $(docker-compose ps -q order-db) pg_isready -U orderuser -d orderdb > /dev/null 2>&1; then
        print_message $GREEN "✅ 訂單數據庫已就緒"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        print_message $RED "❌ 訂單數據庫連接超時"
        exit 1
    fi
    
    print_message $YELLOW "⏳ 等待訂單數據庫... (嘗試 $attempt/$max_attempts)"
    sleep 2
    ((attempt++))
done

attempt=1
while [ $attempt -le $max_attempts ]; do
    if docker exec $(docker-compose ps -q payment-db) pg_isready -U paymentuser -d paymentdb > /dev/null 2>&1; then
        print_message $GREEN "✅ 支付數據庫已就緒"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        print_message $RED "❌ 支付數據庫連接超時"
        exit 1
    fi
    
    print_message $YELLOW "⏳ 等待支付數據庫... (嘗試 $attempt/$max_attempts)"
    sleep 2
    ((attempt++))
done

# 檢查 RabbitMQ 連接
attempt=1
while [ $attempt -le $max_attempts ]; do
    if docker exec $(docker-compose ps -q rabbitmq) rabbitmq-diagnostics ping > /dev/null 2>&1; then
        print_message $GREEN "✅ RabbitMQ 已就緒"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        print_message $RED "❌ RabbitMQ 連接超時"
        exit 1
    fi
    
    print_message $YELLOW "⏳ 等待 RabbitMQ... (嘗試 $attempt/$max_attempts)"
    sleep 2
    ((attempt++))
done

# 啟動微服務
print_message $BLUE "🚀 啟動微服務..."
docker-compose up -d order-service payment-service

# 等待微服務就緒
print_message $YELLOW "⏳ 等待微服務就緒..."
sleep 45

# 檢查服務健康狀態
print_message $BLUE "🔍 檢查服務健康狀態..."
attempt=1
while [ $attempt -le $max_attempts ]; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_message $GREEN "✅ 訂單服務已就緒"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        print_message $RED "❌ 訂單服務健康檢查失敗"
        docker-compose logs order-service
        exit 1
    fi
    
    print_message $YELLOW "⏳ 等待訂單服務健康檢查... (嘗試 $attempt/$max_attempts)"
    sleep 3
    ((attempt++))
done

attempt=1
while [ $attempt -le $max_attempts ]; do
    if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        print_message $GREEN "✅ 支付服務已就緒"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        print_message $RED "❌ 支付服務健康檢查失敗"
        docker-compose logs payment-service
        exit 1
    fi
    
    print_message $YELLOW "⏳ 等待支付服務健康檢查... (嘗試 $attempt/$max_attempts)"
    sleep 3
    ((attempt++))
done

# 執行端到端測試
print_message $BLUE "🧪 執行端到端測試..."
test_results=0

# 執行訂單服務的端到端測試
print_message $YELLOW "📋 執行訂單服務端到端測試..."
if ./gradlew :order-service:test --tests "*EndToEndIntegrationTest" --info; then
    print_message $GREEN "✅ 訂單服務端到端測試通過"
else
    print_message $RED "❌ 訂單服務端到端測試失敗"
    test_results=1
fi

# 執行 BDD 測試
print_message $YELLOW "📋 執行 BDD 測試..."
if ./gradlew :order-service:test --tests "*CucumberTestRunner" --info; then
    print_message $GREEN "✅ 訂單服務 BDD 測試通過"
else
    print_message $RED "❌ 訂單服務 BDD 測試失敗"
    test_results=1
fi

if ./gradlew :payment-service:test --tests "*CucumberTestRunner" --info; then
    print_message $GREEN "✅ 支付服務 BDD 測試通過"
else
    print_message $RED "❌ 支付服務 BDD 測試失敗"
    test_results=1
fi

# 執行基本的 API 測試
print_message $YELLOW "📋 執行基本 API 測試..."

# 測試創建訂單
print_message $BLUE "🔍 測試創建訂單 API..."
order_response=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "INTEGRATION-TEST-001",
    "amount": 100.00,
    "currency": "TWD",
    "cardNumber": "4111111111111111",
    "expiryDate": "12/25",
    "cvv": "123",
    "cardHolderName": "Integration Test"
  }')

if echo "$order_response" | grep -q "orderId"; then
    order_id=$(echo "$order_response" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
    print_message $GREEN "✅ 訂單創建成功，訂單ID: $order_id"
    
    # 等待支付處理
    print_message $YELLOW "⏳ 等待支付處理..."
    sleep 10
    
    # 檢查訂單狀態
    order_status_response=$(curl -s http://localhost:8080/api/orders/$order_id)
    if echo "$order_status_response" | grep -q '"status":"PAID"'; then
        print_message $GREEN "✅ 支付處理成功"
    elif echo "$order_status_response" | grep -q '"status":"PAYMENT_FAILED"'; then
        print_message $YELLOW "⚠️ 支付失敗（這可能是預期的）"
    else
        print_message $RED "❌ 支付狀態異常"
        echo "訂單狀態響應: $order_status_response"
        test_results=1
    fi
else
    print_message $RED "❌ 訂單創建失敗"
    echo "響應: $order_response"
    test_results=1
fi

# 測試消息隊列狀態
print_message $BLUE "🔍 檢查消息隊列狀態..."
rabbitmq_status=$(curl -s -u guest:guest http://localhost:15672/api/queues)
if echo "$rabbitmq_status" | grep -q "payment.request.queue"; then
    print_message $GREEN "✅ 消息隊列配置正確"
else
    print_message $RED "❌ 消息隊列配置異常"
    test_results=1
fi

# 收集日誌
print_message $BLUE "📋 收集服務日誌..."
mkdir -p test-results/logs
docker-compose logs order-service > test-results/logs/order-service.log
docker-compose logs payment-service > test-results/logs/payment-service.log
docker-compose logs rabbitmq > test-results/logs/rabbitmq.log
docker-compose logs order-db > test-results/logs/order-db.log
docker-compose logs payment-db > test-results/logs/payment-db.log

# 生成測試報告
print_message $BLUE "📊 生成測試報告..."
cat > test-results/integration-test-report.md << EOF
# 系統集成測試報告

## 測試執行時間
- 開始時間: $(date)
- 測試環境: Docker Compose

## 服務狀態
- 訂單服務: ✅ 運行中 (http://localhost:8080)
- 支付服務: ✅ 運行中 (http://localhost:8081)
- RabbitMQ: ✅ 運行中 (http://localhost:15672)
- 訂單數據庫: ✅ 運行中 (localhost:5432)
- 支付數據庫: ✅ 運行中 (localhost:5433)

## 測試結果
- 端到端測試: $([ $test_results -eq 0 ] && echo "✅ 通過" || echo "❌ 失敗")
- API 測試: $([ $test_results -eq 0 ] && echo "✅ 通過" || echo "❌ 失敗")
- 消息隊列測試: $([ $test_results -eq 0 ] && echo "✅ 通過" || echo "❌ 失敗")

## 日誌文件
- 訂單服務日誌: logs/order-service.log
- 支付服務日誌: logs/payment-service.log
- RabbitMQ 日誌: logs/rabbitmq.log
- 數據庫日誌: logs/order-db.log, logs/payment-db.log

## 監控地址
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- RabbitMQ 管理界面: http://localhost:15672 (guest/guest)
EOF

# 最終結果
if [ $test_results -eq 0 ]; then
    print_message $GREEN "🎉 所有集成測試通過！"
    print_message $BLUE "📊 測試報告已生成: test-results/integration-test-report.md"
    print_message $BLUE "🔍 可以通過以下地址訪問監控界面:"
    print_message $BLUE "   - Grafana: http://localhost:3000 (admin/admin)"
    print_message $BLUE "   - Prometheus: http://localhost:9090"
    print_message $BLUE "   - RabbitMQ: http://localhost:15672 (guest/guest)"
else
    print_message $RED "❌ 部分集成測試失敗"
    print_message $BLUE "📋 請檢查日誌文件: test-results/logs/"
    exit 1
fi

print_message $BLUE "💡 提示: 使用 'docker-compose down -v' 清理測試環境"
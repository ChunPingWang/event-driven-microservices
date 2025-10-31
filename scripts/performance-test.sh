#!/bin/bash

# 性能和負載測試腳本
# 此腳本執行高並發訂單處理測試、消息隊列積壓處理能力測試和系統穩定性測試

set -e

echo "🚀 開始性能和負載測試..."

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

# 檢查服務是否運行
check_service() {
    local service_name=$1
    local port=$2
    
    if curl -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
        print_message $GREEN "✅ $service_name 服務運行正常"
        return 0
    else
        print_message $RED "❌ $service_name 服務不可用"
        return 1
    fi
}

# 執行 SQL 查詢
execute_sql() {
    local query=$1
    docker exec $(docker-compose ps -q order-db) psql -U orderuser -d orderdb -t -c "$query" 2>/dev/null | xargs
}

# 清理測試數據
cleanup_test_data() {
    print_message $BLUE "🧹 清理測試數據..."
    execute_sql "DELETE FROM api_request_logs WHERE request_payload LIKE '%PERF-TEST%';" > /dev/null
    execute_sql "DELETE FROM message_event_logs WHERE payload LIKE '%PERF-TEST%';" > /dev/null
    execute_sql "DELETE FROM payment_requests WHERE order_id LIKE '%PERF-TEST%';" > /dev/null
    execute_sql "DELETE FROM orders WHERE customer_id LIKE '%PERF-TEST%';" > /dev/null
}

# 檢查服務狀態
print_message $BLUE "🔍 檢查服務狀態..."
check_service "訂單服務" 8080 || exit 1
check_service "支付服務" 8081 || exit 1

# 清理舊的測試數據
cleanup_test_data

# 創建結果目錄
mkdir -p test-results/performance

# 測試1: 高並發訂單處理測試
print_message $BLUE "🧪 執行高並發訂單處理測試..."

concurrent_users=20
orders_per_user=5
total_orders=$((concurrent_users * orders_per_user))

print_message $YELLOW "📊 測試參數: $concurrent_users 並發用戶，每用戶 $orders_per_user 個訂單"

# 記錄開始時間
start_time=$(date +%s)

# 創建並發請求
pids=()
success_count=0
error_count=0

for ((user=1; user<=concurrent_users; user++)); do
    {
        user_success=0
        user_error=0
        
        for ((order=1; order<=orders_per_user; order++)); do
            order_start_time=$(date +%s%N)
            
            response=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/orders \
                -H "Content-Type: application/json" \
                -d "{
                    \"customerId\": \"PERF-TEST-USER-$user-ORDER-$order\",
                    \"amount\": 50.00,
                    \"currency\": \"TWD\",
                    \"cardNumber\": \"4111111111111111\",
                    \"expiryDate\": \"12/25\",
                    \"cvv\": \"123\",
                    \"cardHolderName\": \"Performance Test User $user Order $order\"
                }")
            
            order_end_time=$(date +%s%N)
            response_time=$(( (order_end_time - order_start_time) / 1000000 )) # 轉換為毫秒
            
            http_code="${response: -3}"
            response_body="${response%???}"
            
            if [ "$http_code" = "201" ]; then
                user_success=$((user_success + 1))
                echo "$response_time" >> test-results/performance/response_times.txt
            else
                user_error=$((user_error + 1))
                echo "User $user Order $order failed with HTTP $http_code" >> test-results/performance/errors.txt
            fi
        done
        
        echo "$user_success" > test-results/performance/user_${user}_success.txt
        echo "$user_error" > test-results/performance/user_${user}_error.txt
    } &
    pids+=($!)
done

# 等待所有並發請求完成
for pid in "${pids[@]}"; do
    wait $pid
done

# 記錄結束時間
end_time=$(date +%s)
test_duration=$((end_time - start_time))

# 統計結果
total_success=0
total_error=0

for ((user=1; user<=concurrent_users; user++)); do
    if [ -f "test-results/performance/user_${user}_success.txt" ]; then
        user_success=$(cat test-results/performance/user_${user}_success.txt)
        total_success=$((total_success + user_success))
    fi
    
    if [ -f "test-results/performance/user_${user}_error.txt" ]; then
        user_error=$(cat test-results/performance/user_${user}_error.txt)
        total_error=$((total_error + user_error))
    fi
done

# 計算性能指標
success_rate=$(echo "scale=2; $total_success * 100 / $total_orders" | bc)
throughput=$(echo "scale=2; $total_success / $test_duration" | bc)

if [ -f "test-results/performance/response_times.txt" ]; then
    avg_response_time=$(awk '{sum+=$1; count++} END {print sum/count}' test-results/performance/response_times.txt)
    max_response_time=$(sort -n test-results/performance/response_times.txt | tail -1)
    min_response_time=$(sort -n test-results/performance/response_times.txt | head -1)
else
    avg_response_time=0
    max_response_time=0
    min_response_time=0
fi

print_message $BLUE "📊 高並發測試結果:"
print_message $BLUE "   - 總訂單數: $total_orders"
print_message $BLUE "   - 成功訂單: $total_success"
print_message $BLUE "   - 失敗訂單: $total_error"
print_message $BLUE "   - 成功率: ${success_rate}%"
print_message $BLUE "   - 測試時長: ${test_duration}秒"
print_message $BLUE "   - 吞吐量: ${throughput} 訂單/秒"
print_message $BLUE "   - 平均響應時間: ${avg_response_time}ms"
print_message $BLUE "   - 最大響應時間: ${max_response_time}ms"
print_message $BLUE "   - 最小響應時間: ${min_response_time}ms"

# 驗證性能標準
if (( $(echo "$success_rate >= 95" | bc -l) )); then
    print_message $GREEN "✅ 成功率達標 (≥95%)"
else
    print_message $RED "❌ 成功率未達標 (<95%)"
fi

if (( $(echo "$avg_response_time <= 2000" | bc -l) )); then
    print_message $GREEN "✅ 平均響應時間達標 (≤2000ms)"
else
    print_message $RED "❌ 平均響應時間未達標 (>2000ms)"
fi

if (( $(echo "$throughput >= 5" | bc -l) )); then
    print_message $GREEN "✅ 吞吐量達標 (≥5 訂單/秒)"
else
    print_message $RED "❌ 吞吐量未達標 (<5 訂單/秒)"
fi

# 等待數據庫處理完成
print_message $YELLOW "⏳ 等待數據庫處理完成..."
sleep 10

# 驗證數據一致性
db_order_count=$(execute_sql "SELECT COUNT(*) FROM orders WHERE customer_id LIKE 'PERF-TEST-USER-%';")
db_payment_count=$(execute_sql "SELECT COUNT(*) FROM payment_requests WHERE order_id LIKE '%PERF-TEST%';")

print_message $BLUE "📊 數據一致性檢查:"
print_message $BLUE "   - 數據庫訂單記錄: $db_order_count"
print_message $BLUE "   - 數據庫支付請求記錄: $db_payment_count"

if [ "$db_order_count" -eq "$total_success" ]; then
    print_message $GREEN "✅ 訂單數據一致性驗證通過"
else
    print_message $RED "❌ 訂單數據一致性驗證失敗"
fi

# 測試2: 消息隊列積壓處理能力測試
print_message $BLUE "🧪 執行消息隊列積壓處理能力測試..."

backlog_messages=30
backlog_start_time=$(date +%s)

print_message $YELLOW "📊 創建 $backlog_messages 個消息積壓..."

# 快速創建消息積壓
for ((i=1; i<=backlog_messages; i++)); do
    curl -s -X POST http://localhost:8080/api/orders \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"BACKLOG-TEST-$(printf '%03d' $i)\",
            \"amount\": 25.00,
            \"currency\": \"TWD\",
            \"cardNumber\": \"4111111111111111\",
            \"expiryDate\": \"12/25\",
            \"cvv\": \"123\",
            \"cardHolderName\": \"Backlog Test $i\"
        }" > /dev/null &
    
    # 控制創建速度以產生積壓
    if (( i % 10 == 0 )); then
        sleep 0.1
    fi
done

wait # 等待所有請求發送完成

# 監控消息處理進度
print_message $YELLOW "⏳ 監控消息處理進度..."
processed_count=0
max_wait_time=120 # 最大等待2分鐘
wait_time=0

while [ $processed_count -lt $backlog_messages ] && [ $wait_time -lt $max_wait_time ]; do
    sleep 5
    wait_time=$((wait_time + 5))
    
    processed_count=$(execute_sql "SELECT COUNT(*) FROM payment_requests WHERE order_id LIKE '%BACKLOG-TEST%';")
    print_message $BLUE "   處理進度: $processed_count/$backlog_messages"
done

backlog_end_time=$(date +%s)
backlog_duration=$((backlog_end_time - backlog_start_time))
processing_rate=$(echo "scale=2; $processed_count / $backlog_duration" | bc)

print_message $BLUE "📊 消息隊列積壓測試結果:"
print_message $BLUE "   - 發送消息數: $backlog_messages"
print_message $BLUE "   - 處理消息數: $processed_count"
print_message $BLUE "   - 處理時長: ${backlog_duration}秒"
print_message $BLUE "   - 處理速率: ${processing_rate} 消息/秒"

if [ "$processed_count" -eq "$backlog_messages" ]; then
    print_message $GREEN "✅ 消息隊列積壓處理完成"
else
    print_message $YELLOW "⚠️ 消息隊列積壓處理未完全完成"
fi

# 測試3: 系統穩定性測試
print_message $BLUE "🧪 執行系統穩定性測試..."

stability_duration=60 # 1分鐘穩定性測試
requests_per_second=5
stability_start_time=$(date +%s)
stability_end_time=$((stability_start_time + stability_duration))

stability_success=0
stability_error=0

print_message $YELLOW "📊 執行 ${stability_duration}秒 穩定性測試，每秒 $requests_per_second 個請求..."

request_id=0
while [ $(date +%s) -lt $stability_end_time ]; do
    for ((j=1; j<=requests_per_second; j++)); do
        request_id=$((request_id + 1))
        
        {
            response=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/orders \
                -H "Content-Type: application/json" \
                -d "{
                    \"customerId\": \"STABILITY-TEST-$request_id\",
                    \"amount\": 30.00,
                    \"currency\": \"TWD\",
                    \"cardNumber\": \"4111111111111111\",
                    \"expiryDate\": \"12/25\",
                    \"cvv\": \"123\",
                    \"cardHolderName\": \"Stability Test $request_id\"
                }")
            
            http_code="${response: -3}"
            
            if [ "$http_code" = "201" ]; then
                echo "SUCCESS" >> test-results/performance/stability_results.txt
            else
                echo "ERROR" >> test-results/performance/stability_results.txt
            fi
        } &
    done
    
    sleep 1
done

wait # 等待所有請求完成

# 統計穩定性測試結果
if [ -f "test-results/performance/stability_results.txt" ]; then
    stability_success=$(grep -c "SUCCESS" test-results/performance/stability_results.txt)
    stability_error=$(grep -c "ERROR" test-results/performance/stability_results.txt)
fi

stability_total=$((stability_success + stability_error))
stability_success_rate=$(echo "scale=2; $stability_success * 100 / $stability_total" | bc)
actual_throughput=$(echo "scale=2; $stability_success / $stability_duration" | bc)

print_message $BLUE "📊 系統穩定性測試結果:"
print_message $BLUE "   - 總請求數: $stability_total"
print_message $BLUE "   - 成功請求: $stability_success"
print_message $BLUE "   - 失敗請求: $stability_error"
print_message $BLUE "   - 成功率: ${stability_success_rate}%"
print_message $BLUE "   - 實際吞吐量: ${actual_throughput} 請求/秒"

if (( $(echo "$stability_success_rate >= 90" | bc -l) )); then
    print_message $GREEN "✅ 系統穩定性達標 (≥90%)"
else
    print_message $RED "❌ 系統穩定性未達標 (<90%)"
fi

# 檢查系統資源使用情況
print_message $BLUE "🔍 檢查系統資源使用情況..."

# 檢查數據庫連接數
db_connections=$(execute_sql "SELECT count(*) FROM pg_stat_activity WHERE state = 'active';")
print_message $BLUE "   - 活躍數據庫連接數: $db_connections"

if [ "$db_connections" -lt "50" ]; then
    print_message $GREEN "✅ 數據庫連接數正常"
else
    print_message $YELLOW "⚠️ 數據庫連接數較高"
fi

# 檢查 RabbitMQ 隊列狀態
rabbitmq_queues=$(curl -s -u guest:guest http://localhost:15672/api/queues | grep -o '"messages":[0-9]*' | head -1 | cut -d':' -f2)
print_message $BLUE "   - RabbitMQ 隊列消息數: $rabbitmq_queues"

# 生成性能測試報告
print_message $BLUE "📊 生成性能測試報告..."

cat > test-results/performance/performance-test-report.md << EOF
# 性能和負載測試報告

## 測試執行時間
- 執行時間: $(date)

## 測試結果摘要

### 1. 高並發訂單處理測試
- **並發用戶數**: $concurrent_users
- **每用戶訂單數**: $orders_per_user
- **總訂單數**: $total_orders
- **成功訂單**: $total_success
- **失敗訂單**: $total_error
- **成功率**: ${success_rate}%
- **測試時長**: ${test_duration}秒
- **吞吐量**: ${throughput} 訂單/秒
- **平均響應時間**: ${avg_response_time}ms
- **最大響應時間**: ${max_response_time}ms
- **最小響應時間**: ${min_response_time}ms

### 2. 消息隊列積壓處理能力測試
- **發送消息數**: $backlog_messages
- **處理消息數**: $processed_count
- **處理時長**: ${backlog_duration}秒
- **處理速率**: ${processing_rate} 消息/秒
- **處理完成率**: $(echo "scale=2; $processed_count * 100 / $backlog_messages" | bc)%

### 3. 系統穩定性測試
- **測試時長**: ${stability_duration}秒
- **總請求數**: $stability_total
- **成功請求**: $stability_success
- **失敗請求**: $stability_error
- **成功率**: ${stability_success_rate}%
- **實際吞吐量**: ${actual_throughput} 請求/秒

## 系統資源使用情況
- **活躍數據庫連接數**: $db_connections
- **RabbitMQ 隊列消息數**: $rabbitmq_queues

## 性能標準驗證
- 成功率標準 (≥95%): $(if (( $(echo "$success_rate >= 95" | bc -l) )); then echo "✅ 通過"; else echo "❌ 未通過"; fi)
- 響應時間標準 (≤2000ms): $(if (( $(echo "$avg_response_time <= 2000" | bc -l) )); then echo "✅ 通過"; else echo "❌ 未通過"; fi)
- 吞吐量標準 (≥5 訂單/秒): $(if (( $(echo "$throughput >= 5" | bc -l) )); then echo "✅ 通過"; else echo "❌ 未通過"; fi)
- 穩定性標準 (≥90%): $(if (( $(echo "$stability_success_rate >= 90" | bc -l) )); then echo "✅ 通過"; else echo "❌ 未通過"; fi)

## 建議
- 監控系統在生產環境下的性能表現
- 根據實際負載調整數據庫連接池大小
- 考慮實施消息隊列監控和告警
- 定期執行性能測試以確保系統穩定性
EOF

# 清理測試文件
rm -f test-results/performance/user_*_success.txt
rm -f test-results/performance/user_*_error.txt
rm -f test-results/performance/response_times.txt
rm -f test-results/performance/errors.txt
rm -f test-results/performance/stability_results.txt

print_message $GREEN "🎉 性能和負載測試完成！"
print_message $BLUE "📊 測試報告已生成: test-results/performance/performance-test-report.md"

# 最終清理測試數據
cleanup_test_data

print_message $GREEN "✅ 所有性能測試完成！"
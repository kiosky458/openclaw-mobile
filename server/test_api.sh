#!/bin/bash
# 測試 OpenClaw Mobile API

API_URL="http://localhost:5001"
DEVICE_ID="test_device_$(date +%s)"

echo "🧪 測試 OpenClaw Mobile API"
echo "📍 API URL: $API_URL"
echo "📱 Device ID: $DEVICE_ID"
echo ""

# 1. 測試註冊
echo "1️⃣ 測試裝置註冊..."
curl -X POST "$API_URL/api/mobile/register" \
  -H "Content-Type: application/json" \
  -d "{\"device_id\": \"$DEVICE_ID\"}" \
  | jq .
echo ""

# 2. 測試發送訊息
echo "2️⃣ 測試發送訊息到 Spark..."
curl -X POST "$API_URL/api/mobile/send" \
  -H "Content-Type: application/json" \
  -d "{\"device_id\": \"$DEVICE_ID\", \"agent_id\": \"spark\", \"message\": \"你好\"}" \
  | jq .
echo ""

# 3. 等待 2 秒後輪詢
echo "⏳ 等待 2 秒..."
sleep 2

echo "3️⃣ 測試輪詢新訊息..."
curl -X POST "$API_URL/api/mobile/poll" \
  -H "Content-Type: application/json" \
  -d "{\"device_id\": \"$DEVICE_ID\", \"last_message_id\": 0}" \
  | jq .
echo ""

# 4. 測試 Dashboard API
echo "4️⃣ 測試列出 Sessions..."
curl -X GET "$API_URL/api/dashboard/sessions" | jq .
echo ""

echo "✅ 測試完成！"

#!/bin/bash
# 開發模式啟動腳本（使用虛擬環境）

cd "$(dirname "$0")"

echo "🚀 啟動 OpenClaw Mobile Server（開發模式）..."
echo "📍 位置：$(pwd)"
echo ""

# 啟動虛擬環境
source venv/bin/activate

# 檢查 OpenClaw CLI
OPENCLAW_BIN="/home/kiosky/.nvm/versions/node/v22.22.0/bin/openclaw"
if [ ! -f "$OPENCLAW_BIN" ]; then
    echo "❌ OpenClaw CLI 不存在：$OPENCLAW_BIN"
    exit 1
fi

echo "✅ OpenClaw CLI：$OPENCLAW_BIN"
echo ""

# 啟動 Server
echo "🎉 Server 啟動在 http://0.0.0.0:5001"
echo "📋 按 Ctrl+C 停止"
echo ""

python3 openclaw_mobile_server.py

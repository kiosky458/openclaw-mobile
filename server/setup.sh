#!/bin/bash
# OpenClaw Mobile Server 安裝腳本

set -e

echo "🚀 開始安裝 OpenClaw Mobile Server..."

# 1. 建立日誌目錄
echo "📁 建立日誌目錄..."
sudo mkdir -p /var/log/openclaw-mobile
sudo chown kiosky:kiosky /var/log/openclaw-mobile

# 2. 安裝 Python 依賴
echo "📦 安裝 Python 依賴..."
pip3 install -r requirements.txt --user

# 3. 複製 systemd 服務文件
echo "⚙️ 設置 systemd 服務..."
sudo cp openclaw-mobile.service /etc/systemd/system/
sudo systemctl daemon-reload

# 4. 設置免密碼 sudo（用於 systemctl）
echo "🔐 設置免密碼 sudo..."
SUDOERS_FILE="/etc/sudoers.d/openclaw-mobile"
sudo tee "$SUDOERS_FILE" > /dev/null <<EOF
kiosky ALL=(ALL) NOPASSWD: /bin/systemctl start openclaw-mobile
kiosky ALL=(ALL) NOPASSWD: /bin/systemctl stop openclaw-mobile
kiosky ALL=(ALL) NOPASSWD: /bin/systemctl restart openclaw-mobile
kiosky ALL=(ALL) NOPASSWD: /bin/systemctl status openclaw-mobile
kiosky ALL=(ALL) NOPASSWD: /bin/systemctl daemon-reload
kiosky ALL=(ALL) NOPASSWD: /bin/journalctl -u openclaw-mobile*
EOF
sudo chmod 0440 "$SUDOERS_FILE"

# 5. 啟動服務
echo "🎉 啟動服務..."
sudo systemctl enable openclaw-mobile
sudo systemctl start openclaw-mobile

# 6. 檢查狀態
echo ""
echo "✅ 安裝完成！"
echo ""
echo "📊 服務狀態："
sudo systemctl status openclaw-mobile --no-pager
echo ""
echo "📍 API 端點：http://localhost:5001"
echo "📋 查看日誌：sudo journalctl -u openclaw-mobile -f"
echo ""
echo "🔧 常用指令："
echo "  啟動：sudo systemctl start openclaw-mobile"
echo "  停止：sudo systemctl stop openclaw-mobile"
echo "  重啟：sudo systemctl restart openclaw-mobile"
echo "  狀態：sudo systemctl status openclaw-mobile"

#!/bin/bash
# OpenClaw Mobile - Git 自動設置腳本

set -e

GITHUB_USERNAME="kiosky458"
REPO_NAME="openclaw-mobile"

echo "========================================="
echo "OpenClaw Mobile - Git 設置"
echo "========================================="
echo ""

# 檢查是否已經是 git 倉庫
if [ -d ".git" ]; then
    echo "⚠️  已是 Git 倉庫"
    echo "目前遠端倉庫："
    git remote -v
    exit 0
fi

# 初始化 git
echo "🔧 初始化 Git 倉庫..."
git init
git branch -M main

# 加入 .gitignore
echo "📝 建立 .gitignore..."
cat > .gitignore << 'EOF'
# Gradle
.gradle/
build/
*/build/
gradle-app.setting

# IDE
.idea/
*.iml
.DS_Store
local.properties

# Android
*.apk
*.ap_
*.aab
captures/
.externalNativeBuild/
.cxx/

# Keystore
*.jks
*.keystore

# Logs
*.log

# Temp
.swp
*~
EOF

# 首次提交
echo "📦 首次提交..."
git add .
git commit -m "🎉 Initial commit: OpenClaw Mobile App

Features:
- 3 Agent Chat interfaces (Spark, Data, NumberOne)
- Dashboard for system monitoring
- WebSocket connection to artiforge.studio
- Device pairing mechanism
- File upload/download support"

echo ""
echo "========================================="
echo "✅ Git 設置完成！"
echo "========================================="
echo ""
echo "下一步："
echo "1. 前往 https://github.com/new 建立新倉庫"
echo "2. 倉庫名稱：$REPO_NAME"
echo "3. 不要勾選任何初始化選項"
echo "4. 建立後執行："
echo ""
echo "   git remote add origin https://github.com/$GITHUB_USERNAME/$REPO_NAME.git"
echo "   git push -u origin main"
echo ""
echo "5. GitHub Actions 會自動編譯 APK"
echo "6. 到 Actions 頁面下載編譯好的 APK"
echo ""

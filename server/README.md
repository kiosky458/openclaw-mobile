# OpenClaw Mobile Server

**OpenClaw Mobile HTTP API 後端**  
整合 OpenClaw CLI 工具，提供 Dashboard 功能

---

## 🚀 快速開始

### 安裝

```bash
cd /home/kiosky/.openclaw-data/workspace/openclaw-mobile/server
chmod +x setup.sh
./setup.sh
```

### 測試

```bash
chmod +x test_api.sh
./test_api.sh
```

---

## 📋 API 端點

### 基礎 Chat API

- `POST /api/mobile/register` - 裝置註冊
- `POST /api/mobile/send` - 發送訊息到 Agent
- `POST /api/mobile/poll` - 輪詢新訊息

### Dashboard API（OpenClaw 核心功能）

- `GET /api/dashboard/sessions` - 列出所有 Sessions
- `GET /api/dashboard/session_status/<session_key>` - 取得 Session 狀態
- `POST /api/dashboard/sessions/send` - 發送訊息到 Session
- `POST /api/dashboard/subagents` - 控制 Sub-Agents

詳細 API 規格請參考：`../API_SPEC.md`

---

## 🔧 服務管理

```bash
# 啟動服務
sudo systemctl start openclaw-mobile

# 停止服務
sudo systemctl stop openclaw-mobile

# 重啟服務
sudo systemctl restart openclaw-mobile

# 查看狀態
sudo systemctl status openclaw-mobile

# 查看日誌
sudo journalctl -u openclaw-mobile -f
```

---

## 📊 系統需求

- **Python**: 3.10+
- **OpenClaw CLI**: `/home/kiosky/.nvm/versions/node/v22.22.0/bin/openclaw`
- **依賴套件**: Flask, Flask-CORS, psutil

---

## 🐛 除錯

### 檢查 OpenClaw CLI

```bash
/home/kiosky/.nvm/versions/node/v22.22.0/bin/openclaw --version
```

### 手動啟動（開發模式）

```bash
python3 openclaw_mobile_server.py
```

### 查看錯誤日誌

```bash
sudo tail -f /var/log/openclaw-mobile/error.log
```

---

## 📝 注意事項

1. **Port**: 預設 5001（可在 `openclaw_mobile_server.py` 修改）
2. **日誌**: `/var/log/openclaw-mobile/`
3. **OpenClaw CLI**: 必須存在且可執行
4. **權限**: systemd 服務以 `kiosky` 用戶運行

---

**文檔完成** 🚀

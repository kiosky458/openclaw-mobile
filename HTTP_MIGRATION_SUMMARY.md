# OpenClaw Mobile - HTTP 輪詢架構遷移完成

**完成時間**: 2026-03-09 00:30  
**架構**: WebSocket → HTTP 輪詢  
**狀態**: ✅ **Step 1, 2, 4 完成**（剩 Step 3: Dashboard UI）

---

## ✅ 已完成工作

### Step 1: HTTPPollManager.kt（輪詢管理器）✅

**檔案**: `app/src/main/java/com/openclaw/mobile/http/HTTPPollManager.kt`

**功能**:
- ✅ HTTP GET/POST 取代 WebSocket
- ✅ 自動輪詢（3 秒間隔）
- ✅ 裝置註冊
- ✅ 訊息發送
- ✅ 訊息輪詢（增量更新）
- ✅ 自動重連
- ✅ 錯誤處理

**優勢**:
- 斷線時訊息不會丟失（Server 端緩存）
- Server 端更新不影響 Client
- 更簡單的錯誤處理
- 沒有 Socket.IO 版本相容問題

---

### Step 2: API 規格定義 ✅

**檔案**: `API_SPEC.md`

**定義的 API 端點**:

#### 基礎 Chat API
1. `POST /api/mobile/register` - 裝置註冊
2. `POST /api/mobile/send` - 發送訊息到 Agent
3. `POST /api/mobile/poll` - 輪詢新訊息

#### Dashboard API（OpenClaw 核心功能）
4. `GET /api/dashboard/sessions` - 列出所有 Sessions
5. `GET /api/dashboard/session_status/<session_key>` - 取得 Session 狀態
6. `POST /api/dashboard/sessions/send` - 發送訊息到 Session
7. `POST /api/dashboard/subagents` - 控制 Sub-Agents

**數據結構**:
- Session 物件
- SessionStatus 物件
- TokenUsage、Cost、Time、SystemInfo

---

### Step 4: Server 端 HTTP API ✅

**檔案**: `server/openclaw_mobile_server.py`

**功能**:
- ✅ Flask HTTP API 伺服器
- ✅ 整合 OpenClaw CLI（`openclaw chat`, `sessions_list`, `session_status` 等）
- ✅ 背景執行緒處理 Agent 訊息
- ✅ 系統資訊監控（CPU、記憶體、GPU、溫度）
- ✅ 訊息緩存（24 小時）
- ✅ 錯誤處理與日誌

**配套檔案**:
- ✅ `requirements.txt` - Python 依賴
- ✅ `openclaw-mobile.service` - systemd 服務文件
- ✅ `setup.sh` - 自動安裝腳本
- ✅ `test_api.sh` - API 測試腳本
- ✅ `README.md` - Server 端文檔

---

### Android 端整合 ✅

**檔案**: `app/src/main/java/com/openclaw/mobile/MainActivity.kt`

**修改**:
- ✅ 移除 `WebSocketManager`
- ✅ 改用 `HTTPPollManager`
- ✅ 取得裝置 ID（Android ID）
- ✅ 連接狀態顯示「已連接（HTTP）」

---

## 📋 下一步：Step 3（Dashboard UI）

**剩餘工作**:
1. **建立 Dashboard Fragment**（顯示所有 Sessions）
2. **Session 卡片 UI**（名稱、狀態、使用量、模型）
3. **TabLayout**（Spark, Data, NumberOne, Dash）
4. **Session 詳細頁面**（點擊卡片查看詳細資訊）
5. **Sub-Agents 管理 UI**（列出、殺死）

**建議優先級**:
- 先完成基礎的 Dashboard Fragment（列出 Sessions）
- 再加入 Session 卡片 UI
- 最後加入詳細頁面和 Sub-Agents 管理

---

## 🚀 快速部署

### Server 端（首次安裝）

```bash
cd /home/kiosky/.openclaw-data/workspace/openclaw-mobile/server
./setup.sh
```

### Server 端（測試）

```bash
./test_api.sh
```

### Android 端（編譯）

```bash
cd /home/kiosky/.openclaw-data/workspace/openclaw-mobile
git add .
git commit -m "feat: 遷移至 HTTP 輪詢架構（Step 1, 2, 4 完成）"
git push

# GitHub Actions 自動編譯 APK
# 下載：https://github.com/{user}/openclaw-mobile/actions
```

---

## 📊 架構比較

### 之前（WebSocket）
```
Android App 
    ↓ WebSocket (持久連接)
Server (Socket.IO)
```

**問題**:
- ❌ Socket.IO 版本相容問題
- ❌ 斷線時訊息丟失
- ❌ Server 端修改影響 Client

### 現在（HTTP 輪詢）
```
Android App 
    ↓ HTTP GET/POST (每 3 秒輪詢)
Server (Flask)
    ↓ subprocess
OpenClaw CLI
```

**優勢**:
- ✅ 無版本相容問題
- ✅ Server 端緩存訊息（斷線不丟失）
- ✅ Server 端獨立更新
- ✅ 更簡單的錯誤處理

---

## 🔧 技術細節

### 輪詢機制
- **間隔**: 3 秒（可配置）
- **超時**: 10 秒
- **增量更新**: 只取新訊息（`last_message_id`）
- **自動重連**: 失敗後繼續輪詢

### 訊息緩存
- **Server 端**: 緩存所有訊息（24 小時自動清理）
- **Client 端**: 記錄 `last_message_id`（避免重複）

### OpenClaw 整合
- **CLI 路徑**: `/home/kiosky/.nvm/versions/node/v22.22.0/bin/openclaw`
- **指令**:
  - `openclaw chat --label spark "訊息"`
  - `openclaw sessions list --json`（假設支援）
  - `openclaw tool session_status --sessionKey <key> --json`
  - `openclaw tool sessions_send --sessionKey <key> --message "訊息"`
  - `openclaw tool subagents --action list --json`

---

## 📝 注意事項

1. **OpenClaw CLI JSON 輸出**: 需確認 OpenClaw CLI 支援 `--json` 參數
   - 如不支援，需解析純文字輸出或使用其他方式
   
2. **Server 端 Port**: 預設 5001（避免與 android-stream-relay 的 5000 衝突）

3. **裝置 ID**: 使用 Android ID（`Settings.Secure.ANDROID_ID`）

4. **系統資訊**: 需要 `nvidia-smi`（GPU）和 `/sys/class/thermal/`（CPU 溫度）

---

## 🎉 總結

**已完成**: Step 1, 2, 4 (HTTPPollManager + API 規格 + Server 端)  
**待完成**: Step 3 (Dashboard UI)  
**架構**: ✅ **穩定、簡單、可維護**  
**下一步**: 建立 Dashboard Fragment + Session 卡片 UI

---

**文檔完成** 🚀  
**K Chung 可以檢查並繼續 Step 3** 👨‍💻

# OpenClaw Mobile App - 專案規格

**建立時間**：2026-03-08 13:41  
**目標平台**：Android

---

## 📋 **需求概述**

### 核心功能
1. **三個 Agent Chat 介面**（Spark, Data, NumberOne）
2. **資源監控 Dashboard**（Dash）
3. **WebSocket 連接**（artiforge.studio）
4. **配對認證機制**（Telegram-style pairing）
5. **即時串流顯示**（類似 OpenAI/Claude 網頁）
6. **檔案上傳下載**（類似 LINE）

---

## 🎨 **UI 設計**

### Tab 佈局（手機畫面最上方）
```
┌─────────────────────────────────────┐
│ Spark │ Data │ NumberOne │ Dash    │ ← Tab Bar
├─────────────────────────────────────┤
│                                     │
│      Agent Chat / Dashboard         │
│                                     │
│                                     │
│                                     │
├─────────────────────────────────────┤
│ [📎] [📷] [輸入訊息...] [發送]      │ ← Input Bar (Chat Tabs only)
└─────────────────────────────────────┘
```

### Chat 介面（Spark / Data / NumberOne）
- **即時串流顯示**：訊息逐字顯示（類似 ChatGPT）
- **訊息泡泡**：
  - 用戶訊息：靠右，藍色
  - Agent 回覆：靠左，灰色
- **檔案附件**：
  - 圖片預覽
  - 檔案圖示 + 名稱 + 大小
  - 點擊下載
- **輸入區**：
  - 📎 附件按鈕（選擇檔案/圖片）
  - 📷 拍照按鈕
  - 文字輸入框
  - 發送按鈕

### Dashboard 介面（Dash）
- **即時資源監控**：
  - CPU 使用率（圓形進度條 + 百分比）
  - GPU 使用率
  - 記憶體使用量（已用/總量）
  - 溫度（CPU/GPU）
  - 內存使用量（磁碟）
- **自動更新**：每 2 秒刷新

---

## 🔐 **配對認證機制**

### Telegram-style Pairing
```
流程：
1. App 啟動 → 檢查是否已配對
2. 未配對 → 顯示配對碼（6-8 位數字）
3. 用戶在主機端輸入配對碼
4. 主機端驗證 + 授權裝置
5. 下發 Token（JWT）
6. App 儲存 Token → 完成配對
```

### 裝置識別
- **裝置 ID**：Android ID
- **硬體資訊**：
  - 型號（Build.MODEL）
  - 製造商（Build.MANUFACTURER）
  - Android 版本（Build.VERSION.RELEASE）
  - CPU 架構（Build.CPU_ABI）
- **首次配對時間**
- **最後活躍時間**

### Token 管理
- **JWT Token**：包含裝置 ID、權限、過期時間
- **儲存**：Android Keystore（加密）
- **更新**：每 30 天自動更新
- **撤銷**：主機端可撤銷裝置授權

---

## 🌐 **WebSocket 通訊**

### 連接端點
```
wss://artiforge.studio/ws/mobile
```

### 訊息格式（JSON）
```json
{
  "type": "message|file|stream|system",
  "agentId": "spark|data|numberone",
  "content": "...",
  "timestamp": 1234567890,
  "messageId": "uuid"
}
```

### 串流格式
```json
{
  "type": "stream",
  "agentId": "spark",
  "delta": "逐字內容",
  "done": false
}
```

### 系統監控
```json
{
  "type": "system",
  "cpu": 45.2,
  "gpu": 30.1,
  "memory": {"used": 8192, "total": 16384},
  "temp": {"cpu": 65, "gpu": 58},
  "disk": {"used": 512000, "total": 1024000}
}
```

---

## 📁 **檔案上傳下載**

### 上傳流程
```
1. 用戶選擇檔案/拍照
2. App 壓縮圖片（如適用）
3. WebSocket 發送 base64 或分塊上傳
4. 主機端儲存到 workspace
5. 回傳檔案路徑
```

### 下載流程
```
1. Agent 生成檔案
2. 主機端發送檔案元資料（名稱、大小、路徑）
3. App 顯示下載按鈕
4. 用戶點擊 → WebSocket 下載檔案
5. 儲存到 Downloads 資料夾
```

---

## 🔧 **技術棧**

### Android
- **語言**：Kotlin
- **最低版本**：Android 8.0 (API 26)
- **目標版本**：Android 14 (API 34)

### UI 框架
- **Material Design 3**
- **TabLayout**（Tab 切換）
- **RecyclerView**（訊息列表）
- **ViewPager2**（Tab 內容切換）

### 網路
- **OkHttp**：WebSocket 連接
- **Retrofit**（如需 REST API）
- **Gson**：JSON 解析

### 儲存
- **SharedPreferences**：配對狀態
- **Keystore**：Token 加密儲存
- **File API**：檔案上傳下載

### 權限
- **INTERNET**：網路連接
- **CAMERA**：拍照
- **READ_EXTERNAL_STORAGE**：讀取檔案
- **WRITE_EXTERNAL_STORAGE**：儲存下載

---

## 📊 **專案結構**

```
openclaw-mobile/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/openclaw/mobile/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── chat/
│   │   │   │   │   │   ├── ChatFragment.kt
│   │   │   │   │   │   ├── ChatAdapter.kt
│   │   │   │   │   │   └── MessageViewHolder.kt
│   │   │   │   │   ├── dashboard/
│   │   │   │   │   │   └── DashboardFragment.kt
│   │   │   │   │   └── pairing/
│   │   │   │   │       └── PairingActivity.kt
│   │   │   │   ├── websocket/
│   │   │   │   │   ├── WebSocketManager.kt
│   │   │   │   │   └── MessageHandler.kt
│   │   │   │   ├── auth/
│   │   │   │   │   ├── TokenManager.kt
│   │   │   │   │   └── DeviceInfo.kt
│   │   │   │   └── utils/
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

---

## 🚀 **開發階段**

### Phase 1: 專案骨架（1-2 小時）
- ✅ 建立 Android 專案
- ✅ 設計 UI 佈局
- ✅ Tab 切換功能

### Phase 2: 配對機制（2-3 小時）
- ⏳ 配對碼生成與顯示
- ⏳ WebSocket 配對流程
- ⏳ Token 儲存與驗證

### Phase 3: Chat 介面（3-4 小時）
- ⏳ 訊息列表
- ⏳ 即時串流顯示
- ⏳ 輸入框 + 發送

### Phase 4: 檔案功能（2-3 小時）
- ⏳ 圖片上傳
- ⏳ 檔案下載
- ⏳ 拍照功能

### Phase 5: 資源監控（1-2 小時）
- ⏳ Dashboard UI
- ⏳ WebSocket 接收監控數據
- ⏳ 即時更新

### Phase 6: 測試與優化（2-3 小時）
- ⏳ 整合測試
- ⏳ UI/UX 優化
- ⏳ 效能優化

**總預估時間**：12-18 小時

---

## 🔐 **安全考量**

### 網路安全
- ✅ HTTPS/WSS only
- ✅ Certificate Pinning（防中間人攻擊）
- ✅ Token 過期機制

### 裝置安全
- ✅ Android Keystore 加密儲存
- ✅ 防止 Root 裝置（可選）
- ✅ 防止螢幕截圖（敏感頁面）

### 授權管理
- ✅ 主機端可隨時撤銷裝置
- ✅ 多裝置管理
- ✅ 異常登入檢測

---

**專案規格完成！準備開始建立 Android 專案** 🚀

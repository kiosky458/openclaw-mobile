# OpenClaw Mobile API 規格

**版本**: v1.0.0  
**協議**: HTTP/HTTPS  
**格式**: JSON  
**認證**: Device ID (未來可加 Token)

---

## 📋 API 端點

### 1. 裝置註冊
**端點**: `POST /api/mobile/register`  
**用途**: 註冊裝置，建立 Session

**請求**:
```json
{
  "device_id": "android_1234567890"
}
```

**回應**:
```json
{
  "status": "ok",
  "device_id": "android_1234567890",
  "server_time": "2026-03-09 00:00:00"
}
```

---

### 2. 發送訊息到 Agent
**端點**: `POST /api/mobile/send`  
**用途**: 發送訊息到指定 Agent（Spark, Data, NumberOne）

**請求**:
```json
{
  "device_id": "android_1234567890",
  "agent_id": "spark",
  "message": "你好，Spark"
}
```

**回應**:
```json
{
  "status": "ok",
  "message_id": "1709856000.123"
}
```

---

### 3. 輪詢新訊息
**端點**: `POST /api/mobile/poll`  
**用途**: 取得新訊息（輪詢間隔：3 秒）

**請求**:
```json
{
  "device_id": "android_1234567890",
  "last_message_id": 1709856000.0
}
```

**回應**:
```json
{
  "messages": [
    {
      "id": 1709856001.456,
      "content": "你好！我是 Spark",
      "from": "agent",
      "timestamp": "2026-03-09 00:00:01"
    },
    {
      "id": 1709856002.789,
      "content": "有什麼需要幫忙的嗎？",
      "from": "agent",
      "timestamp": "2026-03-09 00:00:02"
    }
  ]
}
```

---

## 🎯 Dashboard API（OpenClaw 核心功能複製）

### 4. 列出所有 Sessions
**端點**: `GET /api/dashboard/sessions`  
**用途**: 取得所有 OpenClaw Sessions（對應 `openclaw chat --list` 或 `sessions_list` 工具）

**請求**:
```
GET /api/dashboard/sessions
```

**回應**:
```json
{
  "sessions": [
    {
      "key": "agent:main:telegram:123456",
      "label": "spark",
      "kind": "agent",
      "active": true,
      "last_activity": "2026-03-09 00:00:00",
      "last_message": "處理完成",
      "message_count": 142
    },
    {
      "key": "agent:main:telegram:789012",
      "label": "data",
      "kind": "agent",
      "active": false,
      "last_activity": "2026-03-08 23:30:00",
      "last_message": "已收集數據",
      "message_count": 89
    }
  ]
}
```

---

### 5. 取得 Session 狀態（usage + time + cost）
**端點**: `GET /api/dashboard/session_status/{sessionKey}`  
**用途**: 取得單一 Session 的詳細狀態（對應 `session_status` 工具 📊）

**請求**:
```
GET /api/dashboard/session_status/agent:main:telegram:123456
```

**回應**:
```json
{
  "session_key": "agent:main:telegram:123456",
  "label": "spark",
  "model": "anthropic/claude-sonnet-4-5",
  "status": "active",
  "usage": {
    "total_tokens": 125000,
    "input_tokens": 80000,
    "output_tokens": 45000,
    "cache_read": 60000,
    "cache_write": 20000
  },
  "cost": {
    "total": 1.25,
    "input": 0.80,
    "output": 0.45,
    "currency": "USD"
  },
  "time": {
    "started": "2026-03-08 12:00:00",
    "last_activity": "2026-03-09 00:00:00",
    "elapsed_seconds": 43200
  },
  "system": {
    "cpu_percent": 45.2,
    "memory_mb": 8192,
    "gpu_percent": 30.1,
    "temp_cpu": 65,
    "temp_gpu": 58
  }
}
```

---

### 6. 發送訊息到指定 Session
**端點**: `POST /api/dashboard/sessions/send`  
**用途**: 發送訊息到指定 Session（對應 `sessions_send` 工具）

**請求**:
```json
{
  "session_key": "agent:main:telegram:123456",
  "message": "請幫我分析這個數據"
}
```

**回應**:
```json
{
  "status": "ok",
  "session_key": "agent:main:telegram:123456",
  "message_id": "1709856000.123"
}
```

---

### 7. 控制 Sub-Agents
**端點**: `POST /api/dashboard/subagents`  
**用途**: 列出、殺死、調度 Sub-Agents（對應 `subagents` 工具）

**請求（列出）**:
```json
{
  "action": "list"
}
```

**回應（列出）**:
```json
{
  "subagents": [
    {
      "session_key": "agent:main:subagent:abc123",
      "parent": "agent:main:telegram:123456",
      "status": "running",
      "task": "分析數據",
      "started": "2026-03-09 00:00:00",
      "elapsed_seconds": 120
    }
  ]
}
```

**請求（殺死）**:
```json
{
  "action": "kill",
  "session_key": "agent:main:subagent:abc123"
}
```

**回應（殺死）**:
```json
{
  "status": "ok",
  "session_key": "agent:main:subagent:abc123",
  "action": "killed"
}
```

---

## 📊 資料結構

### Session 物件
```kotlin
data class Session(
    val key: String,              // Session 唯一識別碼
    val label: String,            // Agent 名稱（spark, data, numberone）
    val kind: String,             // Session 類型（agent, user, subagent）
    val active: Boolean,          // 是否活躍
    val lastActivity: String,     // 最後活動時間
    val lastMessage: String,      // 最後一條訊息
    val messageCount: Int         // 訊息數量
)
```

### SessionStatus 物件
```kotlin
data class SessionStatus(
    val sessionKey: String,
    val label: String,
    val model: String,
    val status: String,
    val usage: TokenUsage,
    val cost: Cost,
    val time: Time,
    val system: SystemInfo
)

data class TokenUsage(
    val totalTokens: Int,
    val inputTokens: Int,
    val outputTokens: Int,
    val cacheRead: Int,
    val cacheWrite: Int
)

data class Cost(
    val total: Double,
    val input: Double,
    val output: Double,
    val currency: String
)

data class Time(
    val started: String,
    val lastActivity: String,
    val elapsedSeconds: Long
)

data class SystemInfo(
    val cpuPercent: Double,
    val memoryMb: Int,
    val gpuPercent: Double,
    val tempCpu: Int,
    val tempGpu: Int
)
```

---

## 🔧 錯誤處理

所有 API 在失敗時返回：
```json
{
  "error": "錯誤訊息",
  "code": "ERROR_CODE",
  "details": "詳細說明（可選）"
}
```

HTTP 狀態碼：
- `200 OK` - 成功
- `400 Bad Request` - 請求格式錯誤
- `401 Unauthorized` - 未授權
- `404 Not Found` - 找不到資源
- `500 Internal Server Error` - 伺服器錯誤

---

## 📝 實作注意事項

1. **輪詢間隔**: 3 秒（可配置）
2. **超時**: 10 秒
3. **重試**: 自動重連（指數退避）
4. **緩存**: Server 端緩存未讀訊息（24 小時）
5. **清理**: 定期清理過期 Sessions（7 天）

---

**文檔完成** 🚀

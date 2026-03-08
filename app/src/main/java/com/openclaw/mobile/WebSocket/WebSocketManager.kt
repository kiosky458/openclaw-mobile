package com.openclaw.mobile.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.websocket.WebSocket
import okhttp3.websocket.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WebSocketManager - WebSocket 通訊核心
 * 
 * 連接到 wss://artiforge.studio/ws/mobile
 * 處理配對流程、訊息發送、系統監控
 */
class WebSocketManager(
    private val listener: WebSocketListener
) : WebSocketListener() {
    
    companion object {
        private const val TAG = "WebSocketManager"
        private const val ENDPOINT = "wss://artiforge.studio/ws/mobile"
        
        // 連線配置
        private const val CONNECT_TIMEOUT = 30L // 秒
        private const val READ_TIMEOUT = 30L // 秒
        private const val WRITE_TIMEOUT = 30L // 秒
        private const val PING_INTERVAL = 60L // 秒
        
        // 重連配置
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY = 5000L // 毫秒
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
        .build()
    
    private val gson = GsonBuilder()
        .setLenient()
        .create()
    
    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private var isConnected = false
    
    /**
     * 啟動連線
     */
    fun connect() {
        if (isConnected) {
            Log.d(TAG, "Already connected")
            return
        }
        
        Log.d(TAG, "Attempting to connect to $ENDPOINT")
        
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("User-Agent", "OpenClawMobile/1.0.0")
            .build()
        
        try {
            webSocket = client.newWebSocket(request, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebSocket: ${e.message}")
            listener.onConnectionError(e)
        }
    }
    
    /**
     * 斷開連線
     */
    fun disconnect() {
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        isConnected = false
        reconnectAttempts = 0
        Log.d(TAG, "Disconnected")
    }
    
    /**
     * 發送配對請求
     */
    fun sendPairingRequest(pairingCode: String) {
        val message = JsonObject().apply {
            add("type", JsonPrimitive("pairing_request"))
            add("pairingCode", JsonPrimitive(pairingCode))
            add("timestamp", JsonPrimitive(System.currentTimeMillis()))
        }
        
        sendMessage(message.toString())
    }
    
    /**
     * 發送驗證 Token
     */
    fun sendAuthToken(token: String, pairingCode: String) {
        val message = JsonObject().apply {
            add("type", JsonPrimitive("auth_token"))
            add("token", JsonPrimitive(token))
            add("pairingCode", JsonPrimitive(pairingCode))
            add("timestamp", JsonPrimitive(System.currentTimeMillis()))
        }
        
        sendMessage(message.toString())
    }
    
    /**
     * 發送聊天訊息
     */
    fun sendMessageToAgent(agentId: String, message: String, messageId: String? = null) {
        val msgId = messageId ?: java.util.UUID.randomUUID().toString()
        
        val content = JsonObject().apply {
            add("type", JsonPrimitive("message"))
            add("agentId", JsonPrimitive(agentId))
            add("content", JsonPrimitive(message))
            add("timestamp", JsonPrimitive(System.currentTimeMillis()))
            add("messageId", JsonPrimitive(msgId))
        }
        
        sendMessage(content.toString())
    }
    
    /**
     * 開始串流（用於檔案、圖片）
     */
    fun startStream(agentId: String, streamType: String) {
        val content = JsonObject().apply {
            add("type", JsonPrimitive("stream_start"))
            add("agentId", JsonPrimitive(agentId))
            add("streamType", JsonPrimitive(streamType))
            add("timestamp", JsonPrimitive(System.currentTimeMillis()))
        }
        
        sendMessage(content.toString())
    }
    
    /**
     * 傳送檔案數據塊
     */
    fun sendStreamChunk(agentId: String, chunk: ByteArray, isLast: Boolean) {
        val content = JsonObject().apply {
            add("type", JsonPrimitive("stream_chunk"))
            add("agentId", JsonPrimitive(agentId))
            add("data", JsonPrimitive(Base64.getEncoder().encodeToString(chunk)))
            add("isLast", JsonPrimitive(isLast))
            add("timestamp", JsonPrimitive(System.currentTimeMillis()))
        }
        
        sendMessage(content.toString())
    }
    
    /**
     * 獲取系統監控數據
     */
    fun requestSystemStats(): JsonObject {
        val message = JsonObject().apply {
            add("type", JsonPrimitive("request_stats"))
            add("timestamp", JsonPrimitive(System.currentTimeMillis()))
        }
        
        sendMessage(message.toString())
        return message
    }
    
    /**
     * 發送_ping 保活訊息
     */
    fun ping() {
        val message = JsonObject().apply {
            add("type", JsonPrimitive("ping"))
            add("timestamp", JsonPrimitive(System.currentTimeMillis()))
        }
        
        sendMessage(message.toString())
    }
    
    /**
     * 內部方法：發送 JSON 訊息
     */
    private fun sendMessage(json: String) {
        if (!isConnected || webSocket == null) {
            Log.w(TAG, "Cannot send message: not connected")
            return
        }
        
        try {
            webSocket?.send(json)
            Log.d(TAG, "Sent: $json")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
        }
    }
    
    // ========== WebSocket 回調（由 OkHttp 呼叫）==========
    
    /**
     * WebSocket 已連線
     */
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "WebSocket connected")
        isConnected = true
        reconnectAttempts = 0
        listener.onConnected()
    }
    
    /**
     * 收到訊息
     */
    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "Received: $text")
        
        try {
            val json = JSONObject(text)
            val type = json.getString("type")
            
            when (type) {
                "pairing_success" -> {
                    val pairingCode = json.optString("pairingCode")
                    val token = json.optString("token")
                    listener.onPairingSuccess(pairingCode, token)
                }
                "auth_pending" -> {
                    val pairingCode = json.optString("pairingCode")
                    listener.onAuthPending(pairingCode)
                }
                "auth_success" -> {
                    listener.onAuthSuccess()
                }
                "auth_failed" -> {
                    val reason = json.optString("reason", "Unknown error")
                    listener.onAuthFailed(reason)
                }
                "message", "stream" -> {
                    // Chat message or stream data
                    listener.onMessageReceived(text)
                }
                "stats" -> {
                    // System stats
                    val statsJson = JSONObject(text)
                    listener.onStatsReceived(statsJson)
                }
                "error" -> {
                    val error = json.optString("message", "Unknown error")
                    listener.onError(error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Message parsing error: ${e.message}")
            listener.onConnectionError(e)
        }
    }
    
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        // 處理二進制數據
        Log.d(TAG, "Received binary data: ${bytes.size()} bytes")
    }
    
    /**
     * 連線關閉
     */
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WebSocket closing: $code - $reason")
        webSocket.close(1000, "Normal closure")
        isConnected = false
    }
    
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WebSocket closed: $code - $reason")
        isConnected = false
        
        // 自動重連機制
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            Log.d(TAG, "Will reconnect in ${RECONNECT_DELAY}ms (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
            
            Thread {
                try {
                    Thread.sleep(RECONNECT_DELAY)
                    if (webSocket == null) {
                        connect()
                    }
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Reconnect interrupted", e)
                }
            }.start()
        } else {
            Log.e(TAG, "Max reconnect attempts reached")
            listener.onConnectionError(Exception("Max reconnect attempts reached"))
        }
    }
    
    override fun onFail(webSocket: WebSocket, error: Throwable, response: Response?) {
        Log.e(TAG, "WebSocket failed: ${error.message}", error)
        isConnected = false
        listener.onConnectionError(error)
    }
    
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
    }
}

/**
 * Base64 編碼輔助函數
 */
private fun Base64.getEncoder(): java.util.Base64.Encoder {
    return java.util.Base64.getEncoder()
}

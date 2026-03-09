package com.openclaw.mobile.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException

/**
 * SocketIOManager - Socket.IO WebSocket 管理器
 * 
 * 使用 Socket.IO 實現即時雙向通訊
 * 優勢：
 * - 即時推送（無需輪詢）
 * - 自動重連
 * - 事件驅動（簡單易用）
 */
class SocketIOManager(
    private val serverUrl: String,
    private val deviceId: String,
    private val listener: MessageListener
) {
    
    companion object {
        private const val TAG = "SocketIOManager"
        private const val RECONNECT_DELAY = 3000L  // 3 秒後重連
    }
    
    interface MessageListener {
        fun onConnected()
        fun onDisconnected()
        fun onMessageReceived(message: ChatMessage)
        fun onError(error: String)
        fun onAgentProcessing()
        fun onAgentDone(messageCount: Int)
    }
    
    data class ChatMessage(
        val id: Double,
        val content: String,
        val from: String,
        val timestamp: String,
        val streaming: Boolean = false
    )
    
    private var socket: Socket? = null
    private var isConnecting = false
    private var isRegistered = false
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // ==================== 公開方法 ====================
    
    /**
     * 連接到 Server
     */
    fun connect() {
        if (socket != null && socket!!.connected()) {
            Log.d(TAG, "已經連接，無需重複連接")
            return
        }
        
        if (isConnecting) {
            Log.d(TAG, "正在連接中...")
            return
        }
        
        isConnecting = true
        isRegistered = false
        
        try {
            val opts = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Integer.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 10000
            }
            
            socket = IO.socket(serverUrl, opts)
            
            // 註冊事件監聽器
            setupEventListeners()
            
            // 開始連接
            socket?.connect()
            Log.i(TAG, "開始連接：$serverUrl")
            
        } catch (e: URISyntaxException) {
            isConnecting = false
            Log.e(TAG, "Server URL 格式錯誤", e)
            notifyError("連接失敗：URL 格式錯誤")
        } catch (e: Exception) {
            isConnecting = false
            Log.e(TAG, "連接失敗", e)
            notifyError("連接失敗：${e.message}")
        }
    }
    
    /**
     * 斷開連接
     */
    fun disconnect() {
        Log.i(TAG, "主動斷線")
        isRegistered = false
        socket?.disconnect()
        socket?.off()
        socket = null
    }
    
    /**
     * 發送訊息到 Agent
     */
    fun sendMessage(agentId: String, message: String) {
        if (!isConnected()) {
            Log.w(TAG, "未連接，無法發送訊息")
            notifyError("未連接到 Server")
            return
        }
        
        if (!isRegistered) {
            Log.w(TAG, "未註冊，無法發送訊息")
            notifyError("裝置未註冊")
            return
        }
        
        try {
            val data = JSONObject().apply {
                put("device_id", deviceId)
                put("agent_id", agentId)
                put("message", message)
            }
            
            socket?.emit("send_message", data)
            Log.i(TAG, "發送訊息：$message → $agentId")
            
        } catch (e: JSONException) {
            Log.e(TAG, "發送訊息失敗", e)
            notifyError("發送失敗：${e.message}")
        }
    }
    
    /**
     * 檢查連接狀態
     */
    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 設置所有事件監聽器
     */
    private fun setupEventListeners() {
        socket?.apply {
            // 基本連接事件
            on(Socket.EVENT_CONNECT, onConnect)
            on(Socket.EVENT_DISCONNECT, onDisconnect)
            on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            on("reconnect", onReconnect)
            on("reconnecting", onReconnecting)
            
            // 自定義事件
            on("connected", onServerConnected)
            on("registered", onRegistered)
            on("new_message", onNewMessage)
            on("messages", onMessages)
            on("message_received", onMessageReceived)
            on("agent_done", onAgentDone)
            on("error", onError)
        }
    }
    
    // ==================== Socket.IO 事件處理 ====================
    
    private val onConnect = Emitter.Listener {
        Log.i(TAG, "✅ Socket 連接成功")
        isConnecting = false
        
        // 自動註冊裝置
        registerDevice()
    }
    
    private val onDisconnect = Emitter.Listener { args ->
        val reason = if (args.isNotEmpty()) args[0].toString() else "unknown"
        Log.w(TAG, "🔌 Socket 斷線：$reason")
        isRegistered = false
        notifyDisconnected()
    }
    
    private val onConnectError = Emitter.Listener { args ->
        val error = if (args.isNotEmpty()) args[0].toString() else "unknown"
        Log.e(TAG, "❌ 連接錯誤：$error")
        isConnecting = false
        notifyError("連接失敗：$error")
    }
    
    private val onReconnect = Emitter.Listener { args ->
        val attempt = if (args.isNotEmpty()) args[0].toString() else "?"
        Log.i(TAG, "🔄 重新連接成功（第 $attempt 次）")
    }
    
    private val onReconnecting = Emitter.Listener { args ->
        val attempt = if (args.isNotEmpty()) args[0].toString() else "?"
        Log.d(TAG, "🔄 正在重新連接... (第 $attempt 次)")
    }
    
    private val onServerConnected = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            Log.d(TAG, "Server 確認：${data.getString("message")}")
        } catch (e: Exception) {
            Log.e(TAG, "解析 connected 事件失敗", e)
        }
    }
    
    private val onRegistered = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val status = data.getString("status")
            
            if (status == "ok") {
                isRegistered = true
                Log.i(TAG, "✅ 裝置註冊成功：$deviceId")
                notifyConnected()
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析 registered 事件失敗", e)
        }
    }
    
    private val onNewMessage = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val msgJson = data.getJSONObject("message")
            
            val message = ChatMessage(
                id = msgJson.getDouble("id"),
                content = msgJson.getString("content"),
                from = msgJson.getString("from"),
                timestamp = msgJson.getString("timestamp"),
                streaming = msgJson.optBoolean("streaming", false)
            )
            
            Log.d(TAG, "📬 收到新訊息：${message.content.take(50)}...")
            notifyMessage(message)
            
        } catch (e: Exception) {
            Log.e(TAG, "解析 new_message 事件失敗", e)
        }
    }
    
    private val onMessages = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val messagesArray = data.getJSONArray("messages")
            
            Log.d(TAG, "📬 收到 ${messagesArray.length()} 條歷史訊息")
            
            for (i in 0 until messagesArray.length()) {
                val msgJson = messagesArray.getJSONObject(i)
                val message = ChatMessage(
                    id = msgJson.getDouble("id"),
                    content = msgJson.getString("content"),
                    from = msgJson.getString("from"),
                    timestamp = msgJson.getString("timestamp"),
                    streaming = msgJson.optBoolean("streaming", false)
                )
                
                notifyMessage(message)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "解析 messages 事件失敗", e)
        }
    }
    
    private val onMessageReceived = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            Log.d(TAG, "✅ Server 確認收到訊息：${data.getString("message")}")
            notifyAgentProcessing()
        } catch (e: Exception) {
            Log.e(TAG, "解析 message_received 事件失敗", e)
        }
    }
    
    private val onAgentDone = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val count = data.getInt("message_count")
            Log.i(TAG, "✅ Agent 處理完成：$count 條訊息")
            notifyAgentDone(count)
        } catch (e: Exception) {
            Log.e(TAG, "解析 agent_done 事件失敗", e)
        }
    }
    
    private val onError = Emitter.Listener { args ->
        try {
            val data = args[0] as JSONObject
            val errorMsg = data.getString("message")
            Log.e(TAG, "❌ Server 錯誤：$errorMsg")
            notifyError(errorMsg)
        } catch (e: Exception) {
            Log.e(TAG, "解析 error 事件失敗", e)
        }
    }
    
    // ==================== 註冊裝置 ====================
    
    private fun registerDevice() {
        try {
            val data = JSONObject().apply {
                put("device_id", deviceId)
            }
            
            socket?.emit("register", data)
            Log.i(TAG, "發送註冊請求：$deviceId")
            
        } catch (e: JSONException) {
            Log.e(TAG, "註冊裝置失敗", e)
            notifyError("註冊失敗：${e.message}")
        }
    }
    
    // ==================== 通知回調 ====================
    
    private fun notifyConnected() {
        mainHandler.post { listener.onConnected() }
    }
    
    private fun notifyDisconnected() {
        mainHandler.post { listener.onDisconnected() }
    }
    
    private fun notifyMessage(message: ChatMessage) {
        mainHandler.post { listener.onMessageReceived(message) }
    }
    
    private fun notifyError(error: String) {
        mainHandler.post { listener.onError(error) }
    }
    
    private fun notifyAgentProcessing() {
        mainHandler.post { listener.onAgentProcessing() }
    }
    
    private fun notifyAgentDone(count: Int) {
        mainHandler.post { listener.onAgentDone(count) }
    }
}

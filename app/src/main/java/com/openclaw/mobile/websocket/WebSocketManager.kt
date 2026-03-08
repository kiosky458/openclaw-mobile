package com.openclaw.mobile.websocket

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class WebSocketManager private constructor() {
    
    private val TAG = "WebSocketManager"
    private val WS_URL = "wss://artiforge.studio/ws/mobile"
    
    private var webSocket: WebSocket? = null
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    private var messageListener: MessageListener? = null
    private var connectionListener: ConnectionListener? = null
    
    companion object {
        @Volatile
        private var instance: WebSocketManager? = null
        
        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }
    }
    
    /**
     * 連接 WebSocket
     */
    fun connect(token: String) {
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                connectionListener?.onConnected()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
                messageListener?.onTextMessage(text)
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Binary message received: ${bytes.size()} bytes")
                messageListener?.onBinaryMessage(bytes.toByteArray())
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                webSocket.close(1000, null)
                connectionListener?.onDisconnected()
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                connectionListener?.onDisconnected()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                connectionListener?.onError(t)
            }
        })
    }
    
    /**
     * 發送訊息
     */
    fun sendMessage(agentId: String, message: String) {
        val json = """
            {
                "type": "message",
                "agentId": "$agentId",
                "content": "$message",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
        
        webSocket?.send(json)
    }
    
    /**
     * 發送檔案
     */
    fun sendFile(agentId: String, fileName: String, fileData: ByteArray) {
        // TODO: 實作檔案上傳邏輯
        val base64 = android.util.Base64.encodeToString(fileData, android.util.Base64.DEFAULT)
        val json = """
            {
                "type": "file",
                "agentId": "$agentId",
                "fileName": "$fileName",
                "data": "$base64",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()
        
        webSocket?.send(json)
    }
    
    /**
     * 斷開連接
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
    
    /**
     * 設置訊息監聽器
     */
    fun setMessageListener(listener: MessageListener) {
        this.messageListener = listener
    }
    
    /**
     * 設置連接監聽器
     */
    fun setConnectionListener(listener: ConnectionListener) {
        this.connectionListener = listener
    }
    
    /**
     * 訊息監聽介面
     */
    interface MessageListener {
        fun onTextMessage(message: String)
        fun onBinaryMessage(data: ByteArray)
    }
    
    /**
     * 連接監聽介面
     */
    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: Throwable)
    }
}

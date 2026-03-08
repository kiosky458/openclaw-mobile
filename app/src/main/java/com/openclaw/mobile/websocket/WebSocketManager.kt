package com.openclaw.mobile.websocket

import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocketManager - WebSocket 連接管理器
 * 
 * Phase 1 簡化版：
 * - 基本連接功能
 * - 訊息收發
 * - 自動重連
 */
class WebSocketManager(
    private val wsUrl: String,
    private val listener: MessageListener
) {
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
    private var isManualDisconnect = false
    
    interface MessageListener {
        fun onMessage(message: String)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
    
    /**
     * 連接 WebSocket
     */
    fun connect() {
        if (webSocket != null) {
            return  // 已連接
        }
        
        isManualDisconnect = false
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onConnected()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                listener.onMessage(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@WebSocketManager.webSocket = null
                listener.onDisconnected()
                
                // 自動重連（如果不是手動斷線）
                if (!isManualDisconnect) {
                    reconnectAfterDelay()
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@WebSocketManager.webSocket = null
                listener.onError(t.message ?: "連接失敗")
                
                // 自動重連
                if (!isManualDisconnect) {
                    reconnectAfterDelay()
                }
            }
        })
    }
    
    /**
     * 發送訊息
     */
    fun sendMessage(message: String) {
        webSocket?.send(message) ?: run {
            listener.onError("未連接")
        }
    }
    
    /**
     * 斷開連接
     */
    fun disconnect() {
        isManualDisconnect = true
        webSocket?.close(1000, "手動斷開")
        webSocket = null
    }
    
    /**
     * 重新連接
     */
    fun reconnect() {
        disconnect()
        connect()
    }
    
    /**
     * 延遲重連（5 秒後）
     */
    private fun reconnectAfterDelay() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isManualDisconnect && webSocket == null) {
                listener.onError("5秒後重新連接...")
                connect()
            }
        }, 5000)
    }
}

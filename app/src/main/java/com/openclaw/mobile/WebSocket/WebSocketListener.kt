package com.openclaw.mobile.websocket

import org.json.JSONObject

/**
 * WebSocketListener - WebSocket 事件回調介面
 * 
 * 所有 WebSocket 事件的處理邏輯都在這裡
 */
interface WebSocketListener {
    
    /**
     * 連線成功
     */
    fun onConnected()
    
    /**
     * 配對成功（取得 Token）
     */
    fun onPairingSuccess(pairingCode: String, token: String)
    
    /**
     * 等待主機驗證
     */
    fun onAuthPending(pairingCode: String)
    
    /**
     * 驗證成功
     */
    fun onAuthSuccess()
    
    /**
     * 驗證失敗
     */
    fun onAuthFailed(reason: String)
    
    /**
     * 收到訊息（聊天或串流）
     */
    fun onMessageReceived(jsonString: String)
    
    /**
     * 收到系統監控數據
     */
    fun onStatsReceived(json: JSONObject)
    
    /**
     * 錯誤
     */
    fun onError(message: String)
    
    /**
     * 連線錯誤
     */
    fun onConnectionError(error: Throwable)
}

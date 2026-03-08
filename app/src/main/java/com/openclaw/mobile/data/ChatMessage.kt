package com.openclaw.mobile.data

import java.util.UUID

/**
 * 訊息狀態
 */
enum class MessageStatus {
    SENDING,      // 發送中
    SENT,         // 已發送
    STREAMING,    // 串流中（Agent 回覆）
    COMPLETE,     // 完成
    ERROR         // 錯誤
}

/**
 * 訊息類型
 */
enum class MessageType {
    TEXT,         // 純文字
    FILE,         // 檔案
    IMAGE,        // 圖片
    SYSTEM        // 系統訊息
}

/**
 * 聊天訊息資料類別
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    var content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    var status: MessageStatus = MessageStatus.COMPLETE,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null
) {
    /**
     * 用於串流時追加內容
     */
    fun appendContent(delta: String) {
        content += delta
    }
    
    /**
     * 標記串流完成
     */
    fun markComplete() {
        status = MessageStatus.COMPLETE
    }
}

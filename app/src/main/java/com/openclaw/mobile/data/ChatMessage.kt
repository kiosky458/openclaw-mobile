package com.openclaw.mobile.data

/**
 * ChatMessage - 聊天訊息資料類別
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val isSystem: Boolean = false  // 系統訊息（連接狀態等）
)

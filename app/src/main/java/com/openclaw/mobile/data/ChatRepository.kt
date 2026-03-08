package com.openclaw.mobile.data

/**
 * 聊天訊息倉庫
 * 負責管理各 Agent 的訊息紀錄，Tab 切換時不會遺失
 */
object ChatRepository {
    
    // 各 Agent 的訊息列表
    private val messagesByAgent = mutableMapOf<String, MutableList<ChatMessage>>()
    
    // 目前串流中的訊息 (agentId -> messageId)
    private val streamingMessages = mutableMapOf<String, String>()
    
    /**
     * 取得指定 Agent 的訊息列表
     */
    fun getMessages(agentId: String): MutableList<ChatMessage> {
        return messagesByAgent.getOrPut(agentId) { mutableListOf() }
    }
    
    /**
     * 新增訊息
     */
    fun addMessage(message: ChatMessage): Int {
        val messages = getMessages(message.agentId)
        messages.add(message)
        return messages.size - 1
    }
    
    /**
     * 開始串流訊息
     */
    fun startStreamingMessage(agentId: String): ChatMessage {
        val message = ChatMessage(
            agentId = agentId,
            content = "",
            isUser = false,
            status = MessageStatus.STREAMING
        )
        addMessage(message)
        streamingMessages[agentId] = message.id
        return message
    }
    
    /**
     * 追加串流內容
     */
    fun appendStreamContent(agentId: String, delta: String): Int? {
        val messageId = streamingMessages[agentId] ?: return null
        val messages = getMessages(agentId)
        
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            messages[index].appendContent(delta)
            return index
        }
        return null
    }
    
    /**
     * 完成串流
     */
    fun completeStream(agentId: String): Int? {
        val messageId = streamingMessages.remove(agentId) ?: return null
        val messages = getMessages(agentId)
        
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            messages[index].markComplete()
            return index
        }
        return null
    }
    
    /**
     * 取得串流中的訊息 ID
     */
    fun getStreamingMessageId(agentId: String): String? {
        return streamingMessages[agentId]
    }
    
    /**
     * 清除指定 Agent 的訊息
     */
    fun clearMessages(agentId: String) {
        messagesByAgent[agentId]?.clear()
        streamingMessages.remove(agentId)
    }
    
    /**
     * 清除所有訊息
     */
    fun clearAll() {
        messagesByAgent.clear()
        streamingMessages.clear()
    }
    
    /**
     * 更新訊息狀態
     */
    fun updateMessageStatus(agentId: String, messageId: String, status: MessageStatus): Int? {
        val messages = getMessages(agentId)
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            messages[index] = messages[index].copy(status = status)
            return index
        }
        return null
    }
}

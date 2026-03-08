package com.openclaw.mobile.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.R
import com.openclaw.mobile.ui.chat.ChatFragment.Message

/**
 * MessageAdapter - 訊息列表適配器
 * 
 * 處理三種訊息類型：
 * - USER訊息：靠右，來源色
 * - AGENT 訊息：靠左，灰色
 * - SYSTEM 訊息：特殊樣式
 */
class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AGENT = 1
        private const val TYPE_SYSTEM = 2
    }
    
    private val messages = mutableListOf<Message>()
    
    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            Message.Type.USER -> TYPE_USER
            Message.Type.AGENT -> TYPE_AGENT
            Message.Type.SYSTEM -> TYPE_SYSTEM
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = when (viewType) {
            TYPE_USER -> R.layout.item_message_user
            TYPE_AGENT -> R.layout.item_message_agent
            TYPE_SYSTEM -> R.layout.item_message_system
            else -> R.layout.item_message_agent
        }
        
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        
        return when (viewType) {
            TYPE_USER -> MessageViewHolder(view, Message.Type.USER)
            TYPE_AGENT -> MessageViewHolder(view, Message.Type.AGENT)
            TYPE_SYSTEM -> MessageViewHolder(view, Message.Type.SYSTEM)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        if (holder is MessageViewHolder) {
            holder.bind(message)
        }
    }
    
    override fun getItemCount(): Int = messages.size
    
    /**
     * 添加訊息並滾動到底部
     */
    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
        
        // 自動滾動到底部
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
            recyclerView.post {
                layoutManager.scrollToPosition(messages.size - 1)
            }
        }
    }
    
    /**
     * 清空訊息
     */
    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }
}

/**
 * MessageViewHolder - 訊息 Item 視圖持有者
 */
class MessageViewHolder(
    itemView: View,
    private val messageType: Message.Type
) : RecyclerView.ViewHolder(itemView) {
    
    private val messageText: TextView = itemView.findViewById(R.id.tv_message_text)
    
    init {
        // 設置背景顏色
        val backgroundColor = when (messageType) {
            Message.Type.USER -> ContextCompat.getColor(itemView.context, R.color.message_user_bg)
            Message.Type.AGENT -> ContextCompat.getColor(itemView.context, R.color.message_agent_bg)
            Message.Type.SYSTEM -> ContextCompat.getColor(itemView.context, R.color.message_system_bg)
        }
        
        itemView.setBackgroundColor(backgroundColor)
    }
    
    fun bind(message: Message) {
        messageText.text = message.content
    }
}

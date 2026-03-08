package com.openclaw.mobile.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.R
import com.openclaw.mobile.data.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

/**
 * MessageAdapter - 訊息列表適配器
 */
class MessageAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    
    companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_AGENT = 2
        const val VIEW_TYPE_SYSTEM = 3
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isSystem -> VIEW_TYPE_SYSTEM
            message.isFromUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AGENT
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_SYSTEM -> R.layout.item_message_system
            VIEW_TYPE_USER -> R.layout.item_message_user
            else -> R.layout.item_message_agent
        }
        
        val view = LayoutInflater.from(parent.context)
            .inflate(layout, parent, false)
        
        return MessageViewHolder(view, viewType)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    /**
     * MessageViewHolder - 訊息項目 ViewHolder
     */
    class MessageViewHolder(
        itemView: View,
        private val viewType: Int
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timeText: TextView? = itemView.findViewById(R.id.time_text)
        
        fun bind(message: ChatMessage) {
            messageText.text = message.content
            
            // 系統訊息不顯示時間
            if (viewType != VIEW_TYPE_SYSTEM) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeText?.text = timeFormat.format(Date(message.timestamp))
            }
        }
    }
}

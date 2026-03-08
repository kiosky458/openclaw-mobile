package com.openclaw.mobile.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.R
import com.openclaw.mobile.data.ChatMessage
import com.openclaw.mobile.data.MessageStatus
import com.openclaw.mobile.data.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AGENT = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.type == MessageType.SYSTEM -> VIEW_TYPE_SYSTEM
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AGENT
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_USER -> R.layout.item_message_user
            VIEW_TYPE_AGENT -> R.layout.item_message_agent
            else -> R.layout.item_message_system
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view, viewType)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount() = messages.size
    
    /**
     * 通知特定訊息更新（用於串流）
     */
    fun notifyMessageChanged(position: Int) {
        if (position in 0 until messages.size) {
            notifyItemChanged(position, "content_update")
        }
    }
    
    inner class MessageViewHolder(
        itemView: View,
        private val viewType: Int
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val textContent: TextView = itemView.findViewById(R.id.textContent)
        private val textTime: TextView? = itemView.findViewById(R.id.textTime)
        private val statusIndicator: ImageView? = itemView.findViewById(R.id.statusIndicator)
        private val streamingIndicator: ProgressBar? = itemView.findViewById(R.id.streamingIndicator)
        private val fileContainer: View? = itemView.findViewById(R.id.fileContainer)
        private val fileName: TextView? = itemView.findViewById(R.id.fileName)
        private val fileSize: TextView? = itemView.findViewById(R.id.fileSize)
        
        fun bind(message: ChatMessage) {
            // 設定內容
            textContent.text = if (message.content.isEmpty() && message.status == MessageStatus.STREAMING) {
                "▌" // 串流中顯示游標
            } else if (message.status == MessageStatus.STREAMING) {
                message.content + "▌"
            } else {
                message.content
            }
            
            // 設定時間
            textTime?.text = dateFormat.format(Date(message.timestamp))
            
            // 設定狀態指示器
            when (message.status) {
                MessageStatus.SENDING -> {
                    statusIndicator?.visibility = View.VISIBLE
                    statusIndicator?.setImageResource(R.drawable.ic_sending)
                    streamingIndicator?.visibility = View.GONE
                }
                MessageStatus.STREAMING -> {
                    statusIndicator?.visibility = View.GONE
                    streamingIndicator?.visibility = View.VISIBLE
                }
                MessageStatus.ERROR -> {
                    statusIndicator?.visibility = View.VISIBLE
                    statusIndicator?.setImageResource(R.drawable.ic_error)
                    streamingIndicator?.visibility = View.GONE
                }
                else -> {
                    statusIndicator?.visibility = View.GONE
                    streamingIndicator?.visibility = View.GONE
                }
            }
            
            // 處理檔案訊息
            if (message.type == MessageType.FILE || message.type == MessageType.IMAGE) {
                fileContainer?.visibility = View.VISIBLE
                fileName?.text = message.fileName ?: "Unknown file"
                fileSize?.text = formatFileSize(message.fileSize ?: 0)
            } else {
                fileContainer?.visibility = View.GONE
            }
        }
        
        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            }
        }
    }
}

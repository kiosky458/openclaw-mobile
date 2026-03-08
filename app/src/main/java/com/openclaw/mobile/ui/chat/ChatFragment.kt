package com.openclaw.mobile.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.openclaw.mobile.R
import com.openclaw.mobile.data.ChatMessage
import com.openclaw.mobile.data.ChatRepository
import com.openclaw.mobile.data.MessageStatus
import com.openclaw.mobile.data.MessageType
import com.openclaw.mobile.websocket.WebSocketManager
import org.json.JSONObject

class ChatFragment : Fragment(), WebSocketManager.MessageListener {
    
    private lateinit var agentId: String
    private lateinit var agentName: String
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var attachButton: ImageButton
    private lateinit var cameraButton: ImageButton
    
    private lateinit var chatAdapter: ChatAdapter
    private val messages: MutableList<ChatMessage>
        get() = ChatRepository.getMessages(agentId)
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    companion object {
        private const val ARG_AGENT_ID = "agent_id"
        private const val ARG_AGENT_NAME = "agent_name"
        
        fun newInstance(agentId: String, agentName: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_AGENT_ID, agentId)
                    putString(ARG_AGENT_NAME, agentName)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            agentId = it.getString(ARG_AGENT_ID, "spark")
            agentName = it.getString(ARG_AGENT_NAME, "Spark")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView(view)
        setupInputArea(view)
        setupWebSocket()
        
        // 如果沒有訊息，顯示歡迎訊息
        if (messages.isEmpty()) {
            addWelcomeMessage()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 重新設置 WebSocket 監聽
        WebSocketManager.getInstance().setMessageListener(this)
    }
    
    override fun onPause() {
        super.onPause()
        // 移除監聽避免內存洩漏
    }
    
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewMessages)
        chatAdapter = ChatAdapter(messages)
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
            
            // 滾動到最新訊息
            if (messages.isNotEmpty()) {
                scrollToPosition(messages.size - 1)
            }
        }
    }
    
    private fun setupInputArea(view: View) {
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)
        attachButton = view.findViewById(R.id.attachButton)
        cameraButton = view.findViewById(R.id.cameraButton)
        
        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                messageInput.text.clear()
            }
        }
        
        attachButton.setOnClickListener {
            // TODO: Phase 4 實作
        }
        
        cameraButton.setOnClickListener {
            // TODO: Phase 4 實作
        }
    }
    
    private fun setupWebSocket() {
        val wsManager = WebSocketManager.getInstance()
        wsManager.setMessageListener(this)
        
        // 如果尚未連接，連接 WebSocket（暫時用 demo token）
        // 正式版會從配對機制取得 token
        // wsManager.connect("demo_token")
    }
    
    private fun addWelcomeMessage() {
        val welcomeText = when (agentId) {
            "spark" -> "👋 Hi! I'm Spark, your creative assistant. How can I help you today?"
            "data" -> "📊 你好！我是 Data，你的數據專家。需要什麼資料？"
            "numberone" -> "🎯 Hello! I'm NumberOne, ready to assist you. What's on your mind?"
            else -> "Hello! How can I help you?"
        }
        
        val message = ChatMessage(
            agentId = agentId,
            content = welcomeText,
            isUser = false,
            type = MessageType.SYSTEM
        )
        
        ChatRepository.addMessage(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
    }
    
    private fun sendMessage(text: String) {
        // 建立使用者訊息
        val userMessage = ChatMessage(
            agentId = agentId,
            content = text,
            isUser = true,
            status = MessageStatus.SENDING
        )
        
        val position = ChatRepository.addMessage(userMessage)
        chatAdapter.notifyItemInserted(position)
        recyclerView.scrollToPosition(position)
        
        // 發送到 WebSocket
        WebSocketManager.getInstance().sendMessage(agentId, text)
        
        // 更新狀態為已發送
        mainHandler.postDelayed({
            ChatRepository.updateMessageStatus(agentId, userMessage.id, MessageStatus.SENT)
            chatAdapter.notifyMessageChanged(position)
        }, 100)
        
        // 模擬串流回覆（正式版由 WebSocket 觸發）
        simulateStreamingResponse(text)
    }
    
    /**
     * 模擬串流回覆效果
     * 正式版會由 WebSocket 的 onTextMessage 觸發
     */
    private fun simulateStreamingResponse(userMessage: String) {
        mainHandler.postDelayed({
            // 開始串流
            val streamMessage = ChatRepository.startStreamingMessage(agentId)
            val position = messages.size - 1
            chatAdapter.notifyItemInserted(position)
            recyclerView.scrollToPosition(position)
            
            // 模擬回覆內容
            val response = generateDemoResponse(userMessage)
            var currentIndex = 0
            
            // 逐字顯示
            val streamRunnable = object : Runnable {
                override fun run() {
                    if (currentIndex < response.length) {
                        // 每次追加 1-3 個字元
                        val chunkSize = (1..3).random()
                        val endIndex = minOf(currentIndex + chunkSize, response.length)
                        val chunk = response.substring(currentIndex, endIndex)
                        
                        ChatRepository.appendStreamContent(agentId, chunk)
                        chatAdapter.notifyMessageChanged(position)
                        recyclerView.scrollToPosition(position)
                        
                        currentIndex = endIndex
                        mainHandler.postDelayed(this, (20..50).random().toLong())
                    } else {
                        // 串流完成
                        ChatRepository.completeStream(agentId)
                        chatAdapter.notifyMessageChanged(position)
                    }
                }
            }
            
            mainHandler.post(streamRunnable)
            
        }, 300)
    }
    
    private fun generateDemoResponse(userMessage: String): String {
        return when (agentId) {
            "spark" -> "Thanks for your message! I received: \"$userMessage\"\n\nThis is a demo response from Spark. In the real app, responses will come from the server via WebSocket."
            "data" -> "收到你的訊息：「$userMessage」\n\n這是 Data 的示範回覆。正式版本會透過 WebSocket 從伺服器接收回應。"
            "numberone" -> "Got it! Your message: \"$userMessage\"\n\nThis is NumberOne's demo response. Real responses will stream from the server."
            else -> "Response to: $userMessage"
        }
    }
    
    // ========== WebSocket MessageListener ==========
    
    override fun onTextMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type")
            val msgAgentId = json.optString("agentId")
            
            // 只處理當前 Agent 的訊息
            if (msgAgentId != agentId) return
            
            mainHandler.post {
                when (type) {
                    "stream" -> handleStreamMessage(json)
                    "message" -> handleCompleteMessage(json)
                    "error" -> handleErrorMessage(json)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onBinaryMessage(data: ByteArray) {
        // 處理二進制訊息（檔案下載等）
    }
    
    private fun handleStreamMessage(json: JSONObject) {
        val delta = json.optString("delta", "")
        val done = json.optBoolean("done", false)
        
        if (ChatRepository.getStreamingMessageId(agentId) == null) {
            // 開始新的串流訊息
            ChatRepository.startStreamingMessage(agentId)
            chatAdapter.notifyItemInserted(messages.size - 1)
        }
        
        // 追加內容
        val position = ChatRepository.appendStreamContent(agentId, delta)
        position?.let {
            chatAdapter.notifyMessageChanged(it)
            recyclerView.scrollToPosition(it)
        }
        
        // 串流完成
        if (done) {
            val completePosition = ChatRepository.completeStream(agentId)
            completePosition?.let {
                chatAdapter.notifyMessageChanged(it)
            }
        }
    }
    
    private fun handleCompleteMessage(json: JSONObject) {
        val content = json.optString("content", "")
        
        val message = ChatMessage(
            agentId = agentId,
            content = content,
            isUser = false
        )
        
        val position = ChatRepository.addMessage(message)
        chatAdapter.notifyItemInserted(position)
        recyclerView.scrollToPosition(position)
    }
    
    private fun handleErrorMessage(json: JSONObject) {
        val errorText = json.optString("error", "Unknown error")
        
        val message = ChatMessage(
            agentId = agentId,
            content = "❌ Error: $errorText",
            isUser = false,
            type = MessageType.SYSTEM,
            status = MessageStatus.ERROR
        )
        
        val position = ChatRepository.addMessage(message)
        chatAdapter.notifyItemInserted(position)
        recyclerView.scrollToPosition(position)
    }
}

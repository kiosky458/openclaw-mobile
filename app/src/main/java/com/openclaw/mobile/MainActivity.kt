package com.openclaw.mobile

import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.openclaw.mobile.data.ChatMessage
import com.openclaw.mobile.ui.chat.MessageAdapter
import com.openclaw.mobile.websocket.SocketIOManager

/**
 * MainActivity - WebSocket 即時版本
 * 
 * 功能：
 * - 單一 Spark Chat 介面
 * - WebSocket 即時雙向通訊（取代 HTTP 輪詢）
 * - 流式訊息接收
 */
class MainActivity : AppCompatActivity(), SocketIOManager.MessageListener {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputField: TextInputEditText
    private lateinit var sendButton: FloatingActionButton
    private lateinit var socketIOManager: SocketIOManager
    
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var deviceId: String
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 取得裝置 ID（Android ID）
        deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        // 設定 Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Spark Chat (WebSocket)"
        
        initializeViews()
        setupRecyclerView()
        setupWebSocket()
        setupListeners()
        
        // 測試訊息
        addSystemMessage("正在連接 WebSocket...")
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.messages_recycler_view)
        inputField = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.send_button)
    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }
    
    private fun setupWebSocket() {
        // WebSocket 伺服器位置
        val serverUrl = "http://59.125.35.234:5001"
        
        socketIOManager = SocketIOManager(
            serverUrl = serverUrl,
            deviceId = "android_$deviceId",
            listener = this
        )
        
        // 開始連接
        socketIOManager.connect()
    }
    
    private fun setupListeners() {
        sendButton.setOnClickListener {
            val message = inputField.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                inputField.text?.clear()
            }
        }
    }
    
    // ==================== 發送訊息 ====================
    
    private fun sendMessage(message: String) {
        // 顯示用戶訊息
        addUserMessage(message)
        
        // 發送到 Agent
        socketIOManager.sendMessage("spark", message)
    }
    
    // ==================== SocketIOManager.MessageListener 實現 ====================
    
    override fun onConnected() {
        runOnUiThread {
            addSystemMessage("✅ 已連接")
            supportActionBar?.subtitle = "已連接"
            Toast.makeText(this, "已連接到 Server", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDisconnected() {
        runOnUiThread {
            addSystemMessage("❌ 連接斷開")
            supportActionBar?.subtitle = "未連接"
            Toast.makeText(this, "連接斷開", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onMessageReceived(message: SocketIOManager.ChatMessage) {
        runOnUiThread {
            // 將 SocketIO 消息轉為 UI 消息
            val chatMessage = ChatMessage(
                id = message.id.toLong(),
                content = message.content,
                isFromUser = (message.from == "user"),
                timestamp = System.currentTimeMillis()
            )
            
            messages.add(chatMessage)
            messageAdapter.notifyItemInserted(messages.size - 1)
            recyclerView.scrollToPosition(messages.size - 1)
        }
    }
    
    override fun onError(error: String) {
        runOnUiThread {
            addSystemMessage("❌ 錯誤：$error")
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onAgentProcessing() {
        runOnUiThread {
            addSystemMessage("⏳ Agent 處理中...")
        }
    }
    
    override fun onAgentDone(messageCount: Int) {
        runOnUiThread {
            addSystemMessage("✅ Agent 完成（$messageCount 條訊息）")
        }
    }
    
    // ==================== UI 輔助方法 ====================
    
    private fun addUserMessage(content: String) {
        val message = ChatMessage(
            id = System.currentTimeMillis(),
            content = content,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun addSystemMessage(content: String) {
        val message = ChatMessage(
            id = System.currentTimeMillis(),
            content = content,
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    // ==================== 選單 ====================
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                messages.clear()
                messageAdapter.notifyDataSetChanged()
                addSystemMessage("訊息已清空")
                true
            }
            R.id.action_reconnect -> {
                socketIOManager.disconnect()
                socketIOManager.connect()
                addSystemMessage("正在重新連接...")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // ==================== 生命週期 ====================
    
    override fun onDestroy() {
        super.onDestroy()
        socketIOManager.disconnect()
    }
}

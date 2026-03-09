package com.openclaw.mobile

import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.openclaw.mobile.data.ChatMessage
import com.openclaw.mobile.ui.chat.MessageAdapter
import com.openclaw.mobile.http.HTTPPollManager

/**
 * MainActivity - HTTP 輪詢版本
 * 
 * 功能：
 * - 單一 Spark Chat 介面
 * - HTTP 輪詢連接（取代 WebSocket）
 * - 基本訊息收發
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputField: TextInputEditText
    private lateinit var sendButton: FloatingActionButton
    private lateinit var httpPollManager: HTTPPollManager
    
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
        supportActionBar?.title = "Spark Chat"
        
        initializeViews()
        setupRecyclerView()
        setupHTTPPoll()
        setupListeners()
        
        // 測試訊息
        addSystemMessage("連接至 Spark Agent...")
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
    
    private fun setupHTTPPoll() {
        // HTTP 伺服器位置（可設定）
        val serverUrl = "http://59.125.35.234:5001"  // 或使用 artiforge.studio
        
        httpPollManager = HTTPPollManager(
            baseUrl = serverUrl,
            deviceId = "android_$deviceId",
            listener = object : HTTPPollManager.MessageListener {
                override fun onMessage(message: String, from: String) {
                    runOnUiThread {
                        if (from == "agent") {
                            addAgentMessage(message)
                        } else {
                            addSystemMessage(message)
                        }
                    }
                }
                
                override fun onConnected() {
                    runOnUiThread {
                        addSystemMessage("已連接")
                        updateConnectionStatus(true)
                    }
                }
                
                override fun onDisconnected() {
                    runOnUiThread {
                        addSystemMessage("連接中斷")
                        updateConnectionStatus(false)
                    }
                }
                
                override fun onError(error: String) {
                    runOnUiThread {
                        addSystemMessage("錯誤: $error")
                    }
                }
            }
        )
        
        httpPollManager.start()
    }
    
    private fun setupListeners() {
        sendButton.setOnClickListener {
            sendMessage()
        }
        
        inputField.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }
    
    private fun sendMessage() {
        val text = inputField.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) return
        
        // 顯示用戶訊息
        addUserMessage(text)
        
        // 發送到 Server（HTTP POST）
        httpPollManager.sendMessage(text, "spark")
        
        // 清空輸入框
        inputField.text?.clear()
    }
    
    private fun addUserMessage(text: String) {
        val message = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun addAgentMessage(text: String) {
        val message = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun addSystemMessage(text: String) {
        val message = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            isSystem = true
        )
        messages.add(message)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
    
    private fun updateConnectionStatus(connected: Boolean) {
        supportActionBar?.subtitle = if (connected) "已連接（HTTP）" else "未連接"
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reconnect -> {
                httpPollManager.stop()
                httpPollManager.start()
                true
            }
            R.id.action_clear -> {
                messages.clear()
                messageAdapter.notifyDataSetChanged()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        httpPollManager.stop()
    }
}

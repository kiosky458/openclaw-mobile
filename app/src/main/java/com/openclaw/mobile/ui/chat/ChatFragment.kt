package com.openclaw.mobile.ui.chat

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.openclaw.mobile.R
import com.openclaw.mobile.websocket.WebSocketListener
import com.openclaw.mobile.websocket.WebSocketManager
import org.json.JSONObject

/**
 * ChatFragment - Agent 聊天介面
 * 
 * 支援：
 * - 即時串流顯示
 * - 訊息泡泡
 * - 檔案附件
 * - 拍照功能
 */
class ChatFragment(private val agentId: String) : Fragment(), WebSocketListener {
    
    companion object {
        private const val TAG = "ChatFragment"
        private const val REQUEST_CAMERA_PERMISSION = 1001
        private const val REQUEST_FILE_PICKER = 1002
    }
    
    // UI 元件
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendMessageButton: ImageButton
    private lateinit var attachButton: ImageButton
    private lateinit var cameraButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateView: View
    
    // 系統元件
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var messageAdapter: MessageAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeWebView()
        setupUI(view)
        setupRecyclerView()
        setupInputControls()
        
        // 檢查配對狀態
        if (!com.openclaw.mobile.auth.TokenManager(requireContext()).isPaired()) {
            showPairingRequiredMessage()
        }
    }
    
    private fun initializeWebSocket() {
        webSocketManager = WebSocketManager(this)
        webSocketManager.connect()
    }
    
    private fun setupUI(view: View) {
        recyclerView = view.findViewById(R.id.recycler_messages)
        messageEditText = view.findViewById(R.id.et_message_input)
        sendMessageButton = view.findViewById(R.id.btn_send_message)
        attachButton = view.findViewById(R.id.btn_attach_file)
        cameraButton = view.findViewById(R.id.btn_take_photo)
        progressBar = view.findViewById(R.id.pb_typing_indicator)
        emptyStateView = view.findViewById(R.id.empty_state_view)
        
        attachButton.setOnClickListener {
            pickFile()
        }
        
        cameraButton.setOnClickListener {
            requestCameraPermission()
        }
    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messageAdapter
        }
    }
    
    private fun setupInputControls() {
        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
        
        sendMessageButton.setOnClickListener {
            sendMessage()
        }
    }
    
    private fun sendMessage() {
        val text = messageEditText.text.toString().trim()
        if (text.isEmpty()) return
        
        // 顯示用戶訊息
        messageAdapter.addMessage(Message(text, Message.Type.USER))
        
        // 清空輸入框
        messageEditText.text.clear()
        
        // 發送 WebSocket 訊息
        webSocketManager.sendMessageToAgent(agentId, text)
    }
    
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                PackageManager.PERMISSION CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(PackageManager.PERMISSION CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            launchCamera()
        }
    }
    
    private fun pickFile() {
        // 開啟文件選擇器
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(
            Intent.createChooser(intent, "選擇檔案"),
            REQUEST_FILE_PICKER
        )
    }
    
    private fun launchCamera() {
        // 開啟相機
        // TODO: 實現相機邏輯
        Snackbar.make(requireView(), "相機功能開發中", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun showPairingRequiredMessage() {
        emptyStateView.visibility = VISIBLE
        emptyStateView.findViewById<TextView>(R.id.tv_empty_message)
            .text = "請先與 OpenClaw 主機配對"
    }
    
    override fun onConnected() {
        runOnUiThread {
            progressBar.visibility = INVISIBLE
        }
    }
    
    override fun onMessageReceived(jsonString: String) {
        runOnUiThread {
            try {
                val json = JSONObject(jsonString)
                val type = json.getString("type")
                val content = json.optString("content", "")
                
                when (type) {
                    "message" -> {
                        messageAdapter.addMessage(Message(content, Message.Type.AGENT))
                    }
                    "stream" -> {
                        // 更新串流訊息
                        val delta = json.optString("delta", "")
                        val done = json.optBoolean("done", false)
                        // TODO: 實現串流顯示
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    override fun onStatsReceived(json: org.json.JSONObject) {
        // 處理系統監控數據
        // 應該更新 DashboardFragment
    }
    
    override fun onAuthSuccess() {
        runOnUiThread {
            progressBar.visibility = INVISIBLE
        }
    }
    
    override fun onAuthFailed(reason: String) {
        runOnUiThread {
            Toast.makeText(context, "配對失敗：$reason", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onError(message: String) {
        runOnUiThread {
            Snackbar.make(requireView(), "錯誤：$message", Snackbar.LENGTH_LONG).show()
        }
    }
    
    override fun onConnectionError(error: Throwable) {
        runOnUiThread {
            Snackbar.make(
                requireView(),
                "連線錯誤：${error.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 可以暫停 WebSocket
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        webSocketManager.disconnect()
    }
    
    /**
     * 訊息資料類別
     */
    data class Message(
        val content: String,
        val type: Type
    ) {
        enum class Type {
            USER, AGENT, SYSTEM
        }
    }
}

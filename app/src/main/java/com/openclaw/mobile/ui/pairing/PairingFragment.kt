package com.openclaw.mobile.ui.pairing

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.openclaw.mobile.R
import com.openclaw.mobile.auth.DeviceInfo
import com.openclaw.mobile.auth.TokenManager
import com.openclaw.mobile.WebSocket.WebSocketListener
import com.openclaw.mobile.WebSocket.WebSocketManager

/**
 * PairingFragment - 配對流程介面
 * 
 * 用於與主機配對認證
 * Telegram-style pairing code 流程
 */
class PairingFragment : Fragment(), WebSocketListener {
    
    companion object {
        private const val TAG = "PairingFragment"
    }
    
    // UI 元件
    private lateinit var pairingCodeText: TextView
    private lateinit var pairingInstructions: TextView
    private lateinit var statusImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var retryButton: Button
    private lateinit var progressCircle: ImageView
    
    // 系統元件
    private lateinit var tokenManager: TokenManager
    private lateinit var webSocketManager: WebSocketManager
    private var pairingCode: String = ""
    
    // Activity Result Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startPairing()
        } else {
            showError("需要權限才能配對")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pairing, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tokenManager = TokenManager(requireContext())
        webSocketManager = WebSocketManager(this)
        
        bindingPairingUI(view)
        initializeUI()
    }
    
    private fun bindingPairingUI(view: View) {
        pairingCodeText = view.findViewById(R.id.tv_pairing_code)
        pairingInstructions = view.findViewById(R.id.tv_pairing_instructions)
        statusImage = view.findViewById(R.id.iv_pairing_status)
        statusText = view.findViewById(R.id.tv_pairing_status)
        retryButton = view.findViewById(R.id.btn_retry_pairing)
        progressCircle = view.findViewById(R.id.iv_pairing_progress)
        
        retryButton.setOnClickListener {
            startPairing()
        }
    }
    
    private fun initializeUI() {
        // 檢查是否已配對
        if (tokenManager.isPaired()) {
            showConnectedState()
        } else {
            showPairingState()
        }
    }
    
    /**
     * 開始配對流程
     */
    private fun startPairing() {
        // 生成配對碼
        pairingCode = generatePairingCode()
        
        showConnectingState()
        updateCodeDisplay()
        
        // 連接 WebSocket
        webSocketManager.connect()
        
        // 發送配對請求
        webSocketManager.sendPairingRequest(pairingCode)
    }
    
    /**
     * 生成配對碼（6-8 位數字）
     */
    private fun generatePairingCode(): String {
        val digits = (6..8).flatMap { (0..9).map { it.toString() } }
        val random = java.util.Random(System.currentTimeMillis())
        return StringBuilder().apply {
            repeat(6) {
                append(random.nextInt(10))
            }
        }.toString()
    }
    
    /**
     * 更新配對碼顯示
     */
    private fun updateCodeDisplay() {
        pairingCodeText.text = pairingCode
    }
    
    /**
     * 顯示「等待配對」狀態
     */
    private fun showPairingState() {
        pairingInstructions.text = getString(R.string.pairing_instructions)
        statusImage.setImageResource(R.drawable.ic_pairing_wait)
        statusText.text = "請在電腦端輸入配對碼"
        progressCircle.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
    }
    
    /**
     * 顯示「已連線」狀態
     */
    private fun showConnectedState() {
        pairingInstructions.text = getString(R.string.paired_instructions)
        statusImage.setImageResource(R.drawable.ic_connected)
        statusText.text = "已連接到 OpenClaw"
        progressCircle.visibility = View.GONE
        retryButton.visibility = View.GONE
        retryButton.text = "斷開連線"
        pairingCodeText.text = "配對碼：***（已隱藏）"
    }
    
    /**
     * 顯示「連接中」狀態
     */
    private fun showConnectingState() {
        pairingInstructions.text = "正在連接...\n請稍候"
        statusImage.setImageResource(R.drawable.ic_pairing_connecting)
        statusText.text = "等待連線"
        progressCircle.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
    }
    
    /**
     * 顯示錯誤訊息
     */
    private fun showError(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
        statusText.text = message
    }
    
    // ========== WebSocketListener 實現 ==========
    
    override fun onConnected() {
        runOnUiThread {
            statusText.text = "已連接到服务器"
        }
    }
    
    override fun onPairingSuccess(pairingCode: String, token: String) {
        val deviceInfo = DeviceInfo.collect()
        tokenManager.saveToken(token, deviceInfo.deviceId)
        
        runOnUiThread {
            showConnectedState()
            Snackbar.make(
                requireView(),
                "配對成功！準備使用 OpenClaw",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onAuthPending(pairingCode: String) {
        runOnUiThread {
            statusText.text = "配對碼已發送，請等待主機確認"
        }
    }
    
    override fun onAuthSuccess() {
        runOnUiThread {
            showConnectedState()
        }
    }
    
    override fun onAuthFailed(reason: String) {
        runOnUiThread {
            showError("配對失敗：$reason")
            showPairingState()
        }
    }
    
    override fun onMessageReceived(jsonString: String) {
        // 處理聊天訊息或串流數據
        // 這裡應該更新 UI 顯示
    }
    
    override fun onStatsReceived(json: org.json.JSONObject) {
        // 處理系統監控數據
        // 應該更新 DashboardFragment
    }
    
    override fun onError(message: String) {
        runOnUiThread {
            showError(message)
        }
    }
    
    override fun onConnectionError(error: Throwable) {
        runOnUiThread {
            showError("連線錯誤：${error.message}")
            showPairingState()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (tokenManager.isPaired()) {
            showConnectedState()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 可以在暫停時暫停網路活動
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        webSocketManager.disconnect()
    }
}

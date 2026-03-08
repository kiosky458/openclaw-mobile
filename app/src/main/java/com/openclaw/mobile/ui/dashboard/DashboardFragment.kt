package com.openclaw.mobile.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.openclaw.mobile.R
import com.openclaw.mobile.websocket.WebSocketListener
import com.openclaw.mobile.websocket.WebSocketManager
import org.json.JSONObject
import java.text.NumberFormat

/**
 * DashboardFragment - 系統資源監控介面
 * 
 * 顯示即時監控數據：
 * - CPU 使用率
 * - GPU 使用率
 * - 記憶體使用量
 * - 溫度（CPU/GPU）
 * - 磁碟使用量
 */
class DashboardFragment : Fragment(), WebSocketListener {
    
    companion object {
        private const val TAG = "DashboardFragment"
        private const val UPDATE_INTERVAL_MS = 2000L // 2 秒更新
    }
    
    // UI 元件
    private lateinit var cpuUsageText: TextView
    private lateinit var gpuUsageText: TextView
    private lateinit var memoryUsedText: TextView
    private lateinit var temperatureCpuText: TextView
    private lateinit var temperatureGpuText: TextView
    private lateinit var diskUsedText: TextView
    
    private lateinit var cpuProgressBar: ProgressBar
    private lateinit var gpuProgressBar: ProgressBar
    
    // 系統元件
    private lateinit var webSocketManager: WebSocketManager
    private var statsTimer: android.os.Handler? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        bindingDashboardUI(view)
        initializeWebSocket()
        startStatsPolling()
    }
    
    private fun bindingDashboardUI(view: View) {
        cpuUsageText = view.findViewById(R.id.tv_cpu_usage)
        gpuUsageText = view.findViewById(R.id.tv_gpu_usage)
        memoryUsedText = view.findViewById(R.id.tv_memory_used)
        temperatureCpuText = view.findViewById(R.id.tv_temp_cpu)
        temperatureGpuText = view.findViewById(R.id.tv_temp_gpu)
        diskUsedText = view.findViewById(R.id.tv_disk_used)
        
        cpuProgressBar = view.findViewById(R.id.pb_cpu_progress)
        gpuProgressBar = view.findViewById(R.id.pb_gpu_progress)
    }
    
    private fun initializeWebSocket() {
        // 檢查是否已配對
        val tokenManager = com.openclaw.mobile.auth.TokenManager(requireContext())
        if (!tokenManager.isPaired()) {
            showConnectedError()
            return
        }
        
        webSocketManager = WebSocketManager(this)
        webSocketManager.connect()
        
        // 持續請求 stats
        statsTimer = android.os.Handler(requireContext().mainLooper)
        statsTimer?.postDelayed(object : Runnable {
            override fun run() {
                webSocketManager.requestSystemStats()
                statsTimer?.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }, UPDATE_INTERVAL_MS)
    }
    
    private fun startStatsPolling() {
        requestSystemStats()
    }
    
    private fun requestSystemStats() {
        webSocketManager.requestSystemStats()
    }
    
    private fun showConnectedError() {
        cpuUsageText.text = "請先配對"
        gpuUsageText.text = "請先配對"
        memoryUsedText.text = "請先配對"
    }
    
    /**
     * 更新 CPU 使用率顯示
     */
    private fun updateCpuUsage(cpuPercent: Double) {
        runOnUiThread {
            val format = NumberFormat.getNumberInstance()
            cpuUsageText.text = format.format(cpuPercent) + "%"
            cpuProgressBar.progress = cpuPercent.toInt()
        }
    }
    
    /**
     * 更新 GPU 使用率顯示
     */
    private fun updateGpuUsage(gpuPercent: Double) {
        runOnUiThread {
            val format = NumberFormat.getNumberInstance()
            gpuUsageText.text = format.format(gpuPercent) + "%"
            gpuProgressBar.progress = gpuPercent.toInt()
        }
    }
    
    /**
     * 更新記憶體使用顯示
     */
    private fun updateMemoryUsage(used: Long, total: Long) {
        runOnUiThread {
            val usedGB = used / (1024.0 * 1024 * 1024)
            val totalGB = total / (1024.0 * 1024 * 1024)
            memoryUsedText.text = "%.1f / %.1f GB".format(usedGB, totalGB)
        }
    }
    
    /**
     * 更新 CPU 溫度顯示
     */
    private fun updateCpuTemp(tempCelsius: Int) {
        runOnUiThread {
            temperatureCpuText.text = "%d°C".format(tempCelsius)
        }
    }
    
    /**
     * 更新 GPU 溫度顯示
     */
    private fun updateGpuTemp(tempCelsius: Int) {
        runOnUiThread {
            temperatureGpuText.text = "%d°C".format(tempCelsius)
        }
    }
    
    /**
     * 更新磁碟使用顯示
     */
    private fun updateDiskUsage(used: Long, total: Long) {
        runOnUiThread {
            val usedGB = used / (1024.0 * 1024 * 1024)
            val totalGB = total / (1024.0 * 1024 * 1024)
            diskUsedText.text = "%.1f / %.1f GB".format(usedGB, totalGB)
        }
    }
    
    // ========== WebSocketListener 實現 ==========
    
    override fun onConnected() {
        runOnUiThread {
            // 顯示已連線
        }
    }
    
    override fun onPairingSuccess(pairingCode: String, token: String) {
        runOnUiThread {
            // 配對成功
        }
    }
    
    override fun onAuthSuccess() {
        runOnUiThread {
            // 驗證成功
            cpuUsageText.text = "0.0%"
            gpuUsageText.text = "0.0%"
            memoryUsedText.text = "0.0 / 0.0 GB"
        }
    }
    
    override fun onAuthFailed(reason: String) {
        runOnUiThread {
            // 驗證失敗
        }
    }
    
    override fun onMessageReceived(jsonString: String) {
        // 處理聊天訊息
    }
    
    override fun onStatsReceived(json: JSONObject) {
        try {
            val cpu = json.optDouble("cpu", 0.0)
            val gpu = json.optDouble("gpu", 0.0)
            
            val memory = json.optJSONObject("memory")
            val memoryUsed = memory?.optLong("used", 0) ?: 0
            val memoryTotal = memory?.optLong("total", 1) ?: 1
            
            val temp = json.optJSONObject("temp")
            val cpuTemp = temp?.optInt("cpu", 0) ?: 0
            val gpuTemp = temp?.optInt("gpu", 0) ?: 0
            
            val disk = json.optJSONObject("disk")
            val diskUsed = disk?.optLong("used", 0) ?: 0
            val diskTotal = disk?.optLong("total", 1) ?: 1
            
            updateCpuUsage(cpu)
            updateGpuUsage(gpu)
            updateMemoryUsage(memoryUsed, memoryTotal)
            updateCpuTemp(cpuTemp)
            updateGpuTemp(gpuTemp)
            updateDiskUsage(diskUsed, diskTotal)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onError(message: String) {
        runOnUiThread {
            // 顯示錯誤
        }
    }
    
    override fun onConnectionError(error: Throwable) {
        runOnUiThread {
            // 顯示連線錯誤
        }
    }
    
    override fun onPause() {
        super.onPause()
        statsTimer?.removeCallbacksAndMessages(null)
        webSocketManager?.disconnect()
    }
    
    override fun onResume() {
        super.onResume()
        if (com.openclaw.mobile.auth.TokenManager(requireContext()).isPaired()) {
            initializeWebSocket()
        }
    }
}

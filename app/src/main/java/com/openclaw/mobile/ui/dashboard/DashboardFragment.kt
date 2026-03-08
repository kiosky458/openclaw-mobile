package com.openclaw.mobile.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.openclaw.mobile.R

class DashboardFragment : Fragment() {
    
    private lateinit var cpuText: TextView
    private lateinit var gpuText: TextView
    private lateinit var memoryText: TextView
    private lateinit var tempText: TextView
    private lateinit var diskText: TextView
    
    companion object {
        fun newInstance() = DashboardFragment()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        cpuText = view.findViewById(R.id.cpuValue)
        gpuText = view.findViewById(R.id.gpuValue)
        memoryText = view.findViewById(R.id.memoryValue)
        tempText = view.findViewById(R.id.tempValue)
        diskText = view.findViewById(R.id.diskValue)
        
        // 模擬資料更新
        updateSystemInfo(
            cpu = 45.2f,
            gpu = 30.1f,
            memory = "8.2 / 16.0 GB",
            temp = "CPU: 65°C | GPU: 58°C",
            disk = "512 / 1024 GB"
        )
        
        // TODO: 連接 WebSocket 接收即時資料
    }
    
    fun updateSystemInfo(
        cpu: Float,
        gpu: Float,
        memory: String,
        temp: String,
        disk: String
    ) {
        cpuText.text = "${cpu}%"
        gpuText.text = "${gpu}%"
        memoryText.text = memory
        tempText.text = temp
        diskText.text = disk
    }
}

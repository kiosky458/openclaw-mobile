package com.openclaw.mobile.auth

import android.provider.Settings
import android.os.Build
import java.util.UUID

/**
 * DeviceInfo - 裝置資訊收集
 * 
 * 用於裝置識別和配對機制
 */
data class DeviceInfo(
    val deviceId: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val cpuAbi: String,
    val firstPairedTime: Long,
    val lastActiveTime: Long,
    val appVersion: String
) {
    companion object {
        fun collect(appVersion: String = "1.0.0"): DeviceInfo {
            val deviceId = Settings.Secure.ANDROID_ID
            
            return DeviceInfo(
                deviceId = deviceId,
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                androidVersion = Build.VERSION.RELEASE,
                cpuAbi = Build.CPU_ABI,
                firstPairedTime = System.currentTimeMillis(),
                lastActiveTime = System.currentTimeMillis(),
                appVersion = appVersion
            )
        }
        
        // 生成唯一裝置 ID（如果 ANDROID_ID 不可用）
        fun generateFallbackId(): String {
            return UUID.randomUUID().toString().replace("-", "")
        }
    }
}

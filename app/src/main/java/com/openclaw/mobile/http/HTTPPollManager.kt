package com.openclaw.mobile.http

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTPPollManager - HTTP 輪詢管理器
 * 
 * 取代 WebSocket，使用 HTTP GET/POST + 輪詢機制
 * 優勢：
 * - 斷線時訊息不會丟失（Server 端緩存）
 * - Server 端更新不影響 Client（只需修改 Server）
 * - 更簡單的錯誤處理
 */
class HTTPPollManager(
    private val baseUrl: String,
    private val deviceId: String,
    private val listener: MessageListener
) {
    
    companion object {
        private const val TAG = "HTTPPollManager"
        private const val POLL_INTERVAL = 3000L  // 3 秒輪詢一次
        private const val TIMEOUT = 10L           // 10 秒超時
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false
    private var lastMessageId = 0.0
    
    interface MessageListener {
        fun onMessage(message: String, from: String)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
    
    /**
     * 開始輪詢
     */
    fun start() {
        if (isPolling) {
            Log.w(TAG, "已在輪詢中，跳過")
            return
        }
        
        isPolling = true
        Log.i(TAG, "開始 HTTP 輪詢：$baseUrl")
        
        // 先註冊裝置
        registerDevice()
        
        // 開始輪詢循環
        schedulePoll()
    }
    
    /**
     * 停止輪詢
     */
    fun stop() {
        isPolling = false
        handler.removeCallbacksAndMessages(null)
        listener.onDisconnected()
        Log.i(TAG, "停止 HTTP 輪詢")
    }
    
    /**
     * 發送訊息
     */
    fun sendMessage(message: String, agentId: String = "spark") {
        val json = JSONObject().apply {
            put("device_id", deviceId)
            put("agent_id", agentId)
            put("message", message)
        }
        
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/api/mobile/send")
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "發送訊息失敗：${e.message}")
                listener.onError("發送失敗：${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.i(TAG, "訊息發送成功")
                    } else {
                        Log.e(TAG, "訊息發送失敗：${it.code}")
                        listener.onError("發送失敗：${it.code}")
                    }
                }
            }
        })
    }
    
    /**
     * 註冊裝置
     */
    private fun registerDevice() {
        val json = JSONObject().apply {
            put("device_id", deviceId)
        }
        
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/api/mobile/register")
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "裝置註冊失敗：${e.message}")
                listener.onError("註冊失敗：${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.i(TAG, "裝置註冊成功")
                        listener.onConnected()
                    } else {
                        Log.e(TAG, "裝置註冊失敗：${it.code}")
                        listener.onError("註冊失敗：${it.code}")
                    }
                }
            }
        })
    }
    
    /**
     * 輪詢新訊息
     */
    private fun poll() {
        if (!isPolling) return
        
        val json = JSONObject().apply {
            put("device_id", deviceId)
            put("last_message_id", lastMessageId)
        }
        
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/api/mobile/poll")
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "輪詢失敗：${e.message}")
                listener.onError("連線中斷：${e.message}")
                schedulePoll()  // 繼續輪詢
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string() ?: "{}"
                        processMessages(responseBody)
                    } else {
                        Log.e(TAG, "輪詢失敗：${it.code}")
                    }
                }
                schedulePoll()  // 繼續輪詢
            }
        })
    }
    
    /**
     * 處理收到的訊息
     */
    private fun processMessages(responseBody: String) {
        try {
            val json = JSONObject(responseBody)
            val messagesArray = json.optJSONArray("messages") ?: return
            
            for (i in 0 until messagesArray.length()) {
                val msg = messagesArray.getJSONObject(i)
                val id = msg.optDouble("id", 0.0)
                val content = msg.optString("content", "")
                val from = msg.optString("from", "agent")
                
                if (id > lastMessageId) {
                    lastMessageId = id
                    
                    handler.post {
                        listener.onMessage(content, from)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析訊息失敗：${e.message}")
        }
    }
    
    /**
     * 排程下一次輪詢
     */
    private fun schedulePoll() {
        if (!isPolling) return
        
        handler.postDelayed({
            poll()
        }, POLL_INTERVAL)
    }
}

package com.openclaw.mobile.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.json.JSONObject
import java.util.Base64
import java.util.Date

/**
 * TokenManager - JWT Token 管理
 * 
 * 使用 Android Keystore 加密儲存 Token
 * 支持自動簽名、驗證、更新機制
 */
class TokenManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "openclaw_auth"
        private const val TOUCHPOINT_KEY = "touchpoint_token"
        private const val DEVICE_KEY = "device_key"
        private const val PAIRED_TIME = "paired_time"
        private const val EXPIRY_TIME = "expiry_time"
        
        // Token 有效期：30 天
        private const val TOKEN_EXPIRY_DAYS = 30
    }
    
    private val prefs: SharedPreferences by lazy {
        // 使用 EncryptedSharedPreferences 加密儲存
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    private var deviceKey: KeyPair? = null
    private var validToken: String? = null
    private var tokenExpiry: Long? = null
    
    init {
        loadDeviceKey()
        loadToken()
    }
    
    /**
     * 建立或取得裝置金鑰對
     */
    private fun loadDeviceKey() {
        try {
            val keyExists = prefs.contains(DEVICE_KEY)
            if (!keyExists) {
                deviceKey = generateKeyPair()
                saveDeviceKey()
            } else {
                deviceKey = retrieveDeviceKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 生成 RSA 金鑰對（用於簽名）
     */
    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            "AndroidKeyStore"
        )
        
        val cipher = Cipher.getInstance(KeyProperties.IDENTITY_CIPHER_ALGORITHM)
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            DEVICE_KEY,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_PKCS1)
            .build()
        
        keyPairGenerator.initialize(keyGenParameterSpec)
        return keyPairGenerator.generateKeyPair()
    }
    
    /**
     * 保存裝置金鑰到 SharedPreferences（序列化的公鑰）
     */
    private fun saveDeviceKey() {
        deviceKey?.let { pair ->
            try {
                val publicKey = pair.public
                val publicBytes = publicKey.encoded
                val encoded = Base64.encodeToString(publicBytes, Base64.NO_WRAP)
                prefs.edit().putString(DEVICE_KEY, encoded).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 從 SharedPreferences 獲取裝置金鑰
     */
    private fun retrieveDeviceKey(): KeyPair {
        // 這裡簡化處理，實際應用需要從 Keystore 重新生成
        // 由於 Keystore 每次啟動都會重新加載
        loadDeviceKey()
        return deviceKey ?: throw IllegalStateException("Failed to retrieve device key")
    }
    
    /**
     * 簽名 Token（使用裝置私鑰）
     */
    fun signToken(token: String): String {
        deviceKey?.let { pair ->
            try {
                val signature = Signature.getInstance("SHA256withRSA")
                signature.initSign(pair.private)
                signature.update(token.toByteArray())
                val signedBytes = signature.sign()
                val base64Sig = Base64.getUrlEncoder().encodeToString(signedBytes)
                return "$token.BASE64:$base64Sig"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return token
    }
    
    /**
     * 驗證 Token 簽名
     */
    fun verifyToken(tokenWithSig: String): Boolean {
        try {
            val parts = tokenWithSig.split(".BASE64:")
            if (parts.size != 2) return false
            
            val token = parts[0]
            val signature = Base64.getUrlDecoder().decode(parts[1])
            
            deviceKey?.let { pair ->
                val signatureVerifier = Signature.getInstance("SHA256withRSA")
                signatureVerifier.initVerify(pair.public)
                signatureVerifier.update(token.toByteArray())
                return signatureVerifier.verify(signature)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    /**
     * 生成新的 Token
     */
    fun generateToken(deviceId: String): String {
        val now = System.currentTimeMillis()
        
        val tokenData = JSONObject().apply {
            put("deviceId", deviceId)
            put("issuedAt", now)
            put("expiresAt", now + TOKEN_EXPIRY_DAYS * 24 * 60 * 60 * 1000) // 30 天
            put("type", "mobile_auth")
        }
        
        val token = Base64.getUrlEncoder().encodeToString(tokenData.toString().toByteArray())
        return signToken(token)
    }
    
    /**
     * 驗證 Token 是否過期
     */
    fun validateToken(token: String): Boolean {
        try {
            val parts = token.split(".BASE64:")
            if (parts.size != 2) return false
            
            val tokenData = String(Base64.getUrlDecoder().decode(parts[0]))
            val json = JSONObject(tokenData)
            
            val expiresAt = json.getLong("expiresAt")
            val now = System.currentTimeMillis()
            
            return now < expiresAt
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * 儲存 Token
     */
    fun saveToken(token: String, deviceId: String) {
        validToken = token
        tokenExpiry = System.currentTimeMillis() + TOKEN_EXPIRY_DAYS * 24 * 60 * 60 * 1000
        
        prefs.edit().apply {
            putString(TOUCHPOINT_KEY, token)
            putString(PAIRED_TIME, System.currentTimeMillis().toString())
            putLong(EXPIRY_TIME, tokenExpiry)
            apply()
        }
    }
    
    /**
     * 獲取 Token
     */
    fun getToken(): String? {
        // 檢查 Token 是否過期
        if (tokenExpiry != null && System.currentTimeMillis() > tokenExpiry) {
            validToken = null
            tokenExpiry = null
            prefs.edit().remove(TOUCHPOINT_KEY).remove(EXPIRY_TIME).apply()
        }
        
        return validToken ?: prefs.getString(TOUCHPOINT_KEY, null)
    }
    
    /**
     * 檢查是否已配對
     */
    fun isPaired(): Boolean {
        return getToken() != null && validateToken(getToken()!!)
    }
    
    /**
     * 檢查 Token 是否即將過期（<7 天）
     */
    fun isTokenExpiringSoon(): Boolean {
        val expiry = tokenExpiry ?: return false
        val daysUntilExpiry = (expiry - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
        return daysUntilExpiry <= 7
    }
    
    /**
     * 清除配對資料
     */
    fun clearPairing() {
        prefs.edit().apply {
            remove(TOUCHPOINT_KEY)
            remove(DEVICE_KEY)
            remove(PAIRED_TIME)
            remove(EXPIRY_TIME)
            apply()
        }
        validToken = null
        tokenExpiry = null
    }
    
    /**
     * 獲取配對時間
     */
    fun getPairedTime(): Long? {
        return prefs.getLong(PAIRED_TIME, 0)
    }
}

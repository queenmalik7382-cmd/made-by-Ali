package com.example.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig

class SecureKeyStore(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "maya_encrypted_secrets",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            appContext.getSharedPreferences("maya_fallback_secrets_test", Context.MODE_PRIVATE)
        }
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API, key.trim()).apply()
    }

    fun getGeminiApiKey(): String {
        val stored = prefs.getString(KEY_GEMINI_API, "")?.trim().orEmpty()
        if (stored.isNotEmpty()) return stored
        val bc = runCatching { BuildConfig.GEMINI_API_KEY.trim() }.getOrDefault("")
        return if (bc.isBlank() || bc.startsWith("PLACEHOLDER_") || bc == "MY_GEMINI_API_KEY") "" else bc
    }

    fun hasGeminiApiKey(): Boolean = getGeminiApiKey().isNotBlank()

    fun saveYouTubeAccessToken(token: String, expiryEpochMs: Long) {
        prefs.edit()
            .putString(KEY_YT_ACCESS_TOKEN, token.trim())
            .putLong(KEY_YT_TOKEN_EXPIRY, expiryEpochMs)
            .apply()
    }

    fun getYouTubeAccessToken(): String {
        val token = prefs.getString(KEY_YT_ACCESS_TOKEN, "").orEmpty()
        val expiry = prefs.getLong(KEY_YT_TOKEN_EXPIRY, 0L)
        if (token.isNotBlank() && (expiry == 0L || System.currentTimeMillis() < expiry)) {
            return token
        }
        return ""
    }

    fun saveGoogleClientId(clientId: String) {
        prefs.edit().putString(KEY_GOOGLE_CLIENT_ID, clientId.trim()).apply()
    }

    fun getGoogleClientId(): String {
        val stored = prefs.getString(KEY_GOOGLE_CLIENT_ID, "").orEmpty()
        if (stored.isNotBlank()) return stored
        val bc = runCatching { BuildConfig.GOOGLE_OAUTH_CLIENT_ID.trim() }.getOrDefault("")
        return if (bc.isBlank() || bc.startsWith("PLACEHOLDER_")) "" else bc
    }

    fun clearAllSecrets() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_YT_ACCESS_TOKEN = "yt_oauth_access_token"
        private const val KEY_YT_TOKEN_EXPIRY = "yt_oauth_expiry_ms"
        private const val KEY_GOOGLE_CLIENT_ID = "google_oauth_client_id"
    }
}

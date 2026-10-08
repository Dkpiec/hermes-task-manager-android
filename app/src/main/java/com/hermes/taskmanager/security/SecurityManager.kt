package com.hermes.taskmanager.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

const val CLOUD_SERVER_URL = "https://outcome-lift-future-holdings.trycloudflare.com"

class SecurityManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "hermes_secure_prefs_v2",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("hermes_fallback_prefs_v2", Context.MODE_PRIVATE)
    }

    var serverUrl: String
        get() {
            val url = prefs.getString("server_url", CLOUD_SERVER_URL) ?: CLOUD_SERVER_URL
            return if (url.contains("161.118.184.188") || url.contains("sslip.io") || url.contains("librarian-sink") || url.isBlank()) {
                CLOUD_SERVER_URL
            } else {
                url
            }
        }
        set(value) = prefs.edit().putString("server_url", value).apply()

    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var pinCode: String?
        get() = prefs.getString("pin_code", null)
        set(value) = prefs.edit().putString("pin_code", value).apply()

    val isLoggedIn: Boolean
        get() = !authToken.isNullOrEmpty()

    val hasPin: Boolean
        get() = !pinCode.isNullOrEmpty()

    fun verifyPin(inputPin: String): Boolean {
        return pinCode != null && pinCode == inputPin
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

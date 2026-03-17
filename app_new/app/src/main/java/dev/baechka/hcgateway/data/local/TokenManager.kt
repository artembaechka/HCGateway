package dev.baechka.hcgateway.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.Instant

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiry: String) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_TOKEN_EXPIRY, expiry)
        }.apply()
    }

    suspend fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    suspend fun isTokenExpired(): Boolean {
        val expiry = sharedPreferences.getString(KEY_TOKEN_EXPIRY, null) ?: return true
        return isTokenExpired(expiry)
    }

    suspend fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    suspend fun isLoggedIn(): Boolean {
        val token = getAccessToken()
        val refreshToken = getRefreshToken()
        return !token.isNullOrEmpty() || !refreshToken.isNullOrEmpty()
    }

    suspend fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }

    private fun isTokenExpired(expiry: String): Boolean {
        return try {
            val expiryInstant = Instant.parse(expiry)
            Instant.now().isAfter(expiryInstant)
        } catch (e: Exception) {
            true
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }
}

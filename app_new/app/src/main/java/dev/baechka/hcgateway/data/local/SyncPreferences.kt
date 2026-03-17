package dev.baechka.hcgateway.data.local

import android.content.Context
import android.content.SharedPreferences

class SyncPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    fun saveLastSyncTime(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
    }

    fun getLastSyncTime(): Long? {
        val time = sharedPreferences.getLong(KEY_LAST_SYNC_TIME, -1L)
        return if (time == -1L) null else time
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_SYNC_ENABLED, false)
    }

    companion object {
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    }
}

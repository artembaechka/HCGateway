package dev.baechka.hcgateway.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.baechka.hcgateway.data.api.ApiService
import dev.baechka.hcgateway.data.api.model.SleepMetadata
import dev.baechka.hcgateway.data.api.model.SleepSyncItem
import dev.baechka.hcgateway.data.api.model.SyncRequest
import dev.baechka.hcgateway.data.local.SyncPreferences
import java.time.Instant

class SleepRepository(
    private val context: Context,
    private val apiService: ApiService,
    private val syncPreferences: SyncPreferences
) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    suspend fun isHealthConnectAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(getRequiredPermissions())
        } catch (_: Exception) {
            false
        }
    }

    fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
    }

    suspend fun syncSleepData(startTime: Instant, endTime: Instant): Result<Int> {
        return try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = healthConnectClient.readRecords(request)
            val records = response.records

            if (records.isEmpty()) {
                return Result.success(0)
            }

            val sleepItems = records.map { record ->
                SleepSyncItem(
                    metadata = SleepMetadata(
                        id = record.metadata.id,
                        dataOrigin = record.metadata.dataOrigin.packageName
                    ),
                    startTime = record.startTime.toString(),
                    endTime = record.endTime.toString()
                )
            }

            val syncRequest = SyncRequest(data = sleepItems)
            val apiResponse = apiService.syncSleepSession(syncRequest)

            if (apiResponse.isSuccessful) {
                syncPreferences.saveLastSyncTime(System.currentTimeMillis())
                Result.success(records.size)
            } else {
                Result.failure(Exception("Ошибка синхронизации: ${apiResponse.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLastSyncTime(): Long? {
        return syncPreferences.getLastSyncTime()
    }

    fun isAutoSyncEnabled(): Boolean {
        return syncPreferences.isAutoSyncEnabled()
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        syncPreferences.setAutoSyncEnabled(enabled)
    }
}

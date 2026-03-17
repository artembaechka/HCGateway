package dev.baechka.hcgateway.data.api.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String,
    val fcmToken: String = ""
)

data class LoginResponse(
    val token: String,
    val refresh: String,
    val expiry: String
)

data class RefreshRequest(
    val refresh: String
)

data class SyncRequest(
    val data: List<SleepSyncItem>
)

data class SleepSyncItem(
    val metadata: SleepMetadata,
    val startTime: String,
    val endTime: String
)

data class SleepMetadata(
    val id: String,
    val dataOrigin: String
)

data class SyncResponse(
    val success: Boolean
)

data class ErrorResponse(
    val error: String? = null,
    val message: String? = null
)

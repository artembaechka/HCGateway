package dev.baechka.hcgateway.data.api

import dev.baechka.hcgateway.data.api.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<LoginResponse>

    @DELETE("revoke")
    suspend fun revokeToken(): Response<Unit>

    @POST("sync/sleepSession")
    suspend fun syncSleepSession(@Body request: SyncRequest): Response<SyncResponse>
}

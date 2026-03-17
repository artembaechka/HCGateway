package dev.baechka.hcgateway.data.repository

import dev.baechka.hcgateway.data.api.ApiService
import dev.baechka.hcgateway.data.api.model.LoginRequest
import dev.baechka.hcgateway.data.local.TokenManager

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun login(username: String, password: String, fcmToken: String = ""): Result<Unit> {
        return try {
            val response = apiService.login(LoginRequest(username, password, fcmToken))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    tokenManager.saveTokens(body.token, body.refresh, body.expiry)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Неверный логин или пароль"
                    403 -> "Доступ запрещён"
                    else -> "Ошибка входа: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            apiService.revokeToken()
            tokenManager.clearTokens()
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.success(Unit)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
}

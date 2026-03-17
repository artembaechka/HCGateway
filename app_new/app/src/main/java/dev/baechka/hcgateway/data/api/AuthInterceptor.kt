package dev.baechka.hcgateway.data.api

import dev.baechka.hcgateway.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Не добавляем токен к login запросу
        if (originalRequest.url.toString().endsWith("login")) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking {
            tokenManager.getAccessToken()
        }

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}

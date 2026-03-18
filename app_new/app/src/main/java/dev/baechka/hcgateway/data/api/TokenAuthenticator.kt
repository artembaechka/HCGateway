package dev.baechka.hcgateway.data.api

import com.google.gson.Gson
import dev.baechka.hcgateway.BuildConfig
import dev.baechka.hcgateway.data.api.model.LoginResponse
import dev.baechka.hcgateway.data.api.model.RefreshRequest
import dev.baechka.hcgateway.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Interceptor {
    private val gson = Gson()
    private val simpleClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        // Не пытаемся рефрешить если это сам запрос на refresh
        if (response.code != 401 && response.code != 403) return response
        if (originalRequest.url.toString().endsWith("refresh")) return response

        return runBlocking {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                return@runBlocking response
            }

            val currentToken = tokenManager.getAccessToken()

            try {
                val requestBody = gson.toJson(RefreshRequest(refreshToken))
                    .toRequestBody("application/json".toMediaType())

                val requestBuilder = Request.Builder()
                    .url("${BuildConfig.BASE_URL}refresh")
                    .post(requestBody)

                if (currentToken != null) {
                    requestBuilder.header("Authorization", "Bearer $currentToken")
                }

                val refreshResponse = simpleClient.newCall(requestBuilder.build()).execute()

                if (refreshResponse.isSuccessful) {
                    val loginResponse = gson.fromJson(
                        refreshResponse.body?.string(),
                        LoginResponse::class.java
                    )

                    if (loginResponse != null) {
                        tokenManager.saveTokens(
                            loginResponse.token,
                            loginResponse.refresh,
                            loginResponse.expiry
                        )

                        response.close()
                        return@runBlocking chain.proceed(
                            originalRequest.newBuilder()
                                .header("Authorization", "Bearer ${loginResponse.token}")
                                .build()
                        )
                    }
                }

                tokenManager.clearTokens()
            } catch (_: Exception) {
                tokenManager.clearTokens()
            }

            response
        }
    }
}

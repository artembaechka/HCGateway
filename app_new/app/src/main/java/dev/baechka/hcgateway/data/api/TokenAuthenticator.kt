package dev.baechka.hcgateway.data.api

import com.google.gson.Gson
import dev.baechka.hcgateway.BuildConfig
import dev.baechka.hcgateway.data.api.model.LoginResponse
import dev.baechka.hcgateway.data.api.model.RefreshRequest
import dev.baechka.hcgateway.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {
    private val gson = Gson()
    private val simpleClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code == 401 || response.code == 403) {
            return runBlocking {
                val refreshToken = tokenManager.getRefreshToken() ?: return@runBlocking null
                val currentToken = tokenManager.getAccessToken()

                try {
                    val refreshRequest = RefreshRequest(refreshToken)
                    val jsonBody = gson.toJson(refreshRequest)
                    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                    val requestBuilder = Request.Builder()
                        .url("${BuildConfig.BASE_URL}refresh")
                        .post(requestBody)

                    if (currentToken != null) {
                        requestBuilder.header("Authorization", "Bearer $currentToken")
                    }

                    val request = requestBuilder.build()

                    val refreshResponse = simpleClient.newCall(request).execute()

                    if (refreshResponse.isSuccessful) {
                        val body = refreshResponse.body?.string()
                        val loginResponse = gson.fromJson(body, LoginResponse::class.java)

                        if (loginResponse != null) {
                            tokenManager.saveTokens(
                                loginResponse.token,
                                loginResponse.refresh,
                                loginResponse.expiry
                            )

                            return@runBlocking response.request.newBuilder()
                                .header("Authorization", "Bearer ${loginResponse.token}")
                                .build()
                        }
                    } else {
                        tokenManager.clearTokens()
                    }
                } catch (e: Exception) {
                    tokenManager.clearTokens()
                }

                null
            }
        }

        return null
    }
}

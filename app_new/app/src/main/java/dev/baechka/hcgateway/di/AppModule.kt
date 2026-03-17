package dev.baechka.hcgateway.di

import android.content.Context
import dev.baechka.hcgateway.BuildConfig
import dev.baechka.hcgateway.data.api.ApiService
import dev.baechka.hcgateway.data.api.AuthInterceptor
import dev.baechka.hcgateway.data.api.TokenAuthenticator
import dev.baechka.hcgateway.data.local.SyncPreferences
import dev.baechka.hcgateway.data.local.TokenManager
import dev.baechka.hcgateway.data.repository.AuthRepository
import dev.baechka.hcgateway.data.repository.SleepRepository
import dev.baechka.hcgateway.domain.usecase.LoginUseCase
import dev.baechka.hcgateway.domain.usecase.SyncSleepUseCase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppModule {
    private var tokenManager: TokenManager? = null
    private var syncPreferences: SyncPreferences? = null
    private var apiService: ApiService? = null
    private var authRepository: AuthRepository? = null
    private var sleepRepository: SleepRepository? = null

    fun provideTokenManager(context: Context): TokenManager {
        return tokenManager ?: TokenManager(context.applicationContext).also {
            tokenManager = it
        }
    }

    fun provideSyncPreferences(context: Context): SyncPreferences {
        return syncPreferences ?: SyncPreferences(context.applicationContext).also {
            syncPreferences = it
        }
    }

    fun provideApiService(context: Context): ApiService {
        return apiService ?: createApiService(context).also {
            apiService = it
        }
    }

    private fun createApiService(context: Context): ApiService {
        val tokenManager = provideTokenManager(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(tokenManager))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: AuthRepository(
            provideApiService(context),
            provideTokenManager(context)
        ).also {
            authRepository = it
        }
    }

    fun provideSleepRepository(context: Context): SleepRepository {
        return sleepRepository ?: SleepRepository(
            context.applicationContext,
            provideApiService(context),
            provideSyncPreferences(context)
        ).also {
            sleepRepository = it
        }
    }

    fun provideLoginUseCase(context: Context): LoginUseCase {
        return LoginUseCase(provideAuthRepository(context))
    }

    fun provideSyncSleepUseCase(context: Context): SyncSleepUseCase {
        return SyncSleepUseCase(provideSleepRepository(context))
    }
}

package dev.baechka.hcgateway.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.baechka.hcgateway.di.AppModule
import dev.baechka.hcgateway.service.SleepSyncForegroundService

class SleepSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val syncSleepUseCase = AppModule.provideSyncSleepUseCase(applicationContext)

            val result = syncSleepUseCase.syncRecent()

            result.fold(
                onSuccess = { count ->
                    SleepSyncForegroundService.updateNotification(applicationContext)
                    Result.success()
                },
                onFailure = {
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

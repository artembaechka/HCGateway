package dev.baechka.hcgateway.domain.usecase

import dev.baechka.hcgateway.data.repository.SleepRepository
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class SyncSleepUseCase(
    private val sleepRepository: SleepRepository
) {
    suspend fun syncRecent(): Result<Int> {
        val lastSync = sleepRepository.getLastSyncTime()
        val startTime = if (lastSync != null) {
            Instant.ofEpochMilli(lastSync)
        } else {
            Instant.now().minus(24, ChronoUnit.HOURS)
        }
        val endTime = Instant.now()

        return sleepRepository.syncSleepData(startTime, endTime)
    }

    suspend fun syncPeriod(startDate: Long, endDate: Long): Result<Int> {
        val startTime = Instant.ofEpochMilli(startDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()

        val endTime = Instant.ofEpochMilli(endDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()

        return sleepRepository.syncSleepData(startTime, endTime)
    }
}

package dev.baechka.hcgateway.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.baechka.hcgateway.data.repository.AuthRepository
import dev.baechka.hcgateway.data.repository.SleepRepository
import dev.baechka.hcgateway.domain.usecase.SyncSleepUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val lastSyncTime: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val autoSyncEnabled: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoggingOut: Boolean = false,
    val hasPermissions: Boolean = false,
    val isHealthConnectAvailable: Boolean = false
)

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val count: Int) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

class MainViewModel(
    private val authRepository: AuthRepository,
    private val sleepRepository: SleepRepository,
    private val syncSleepUseCase: SyncSleepUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val lastSync = sleepRepository.getLastSyncTime()
            val autoSync = sleepRepository.isAutoSyncEnabled()
            val hasPermissions = sleepRepository.hasPermissions()
            val isAvailable = sleepRepository.isHealthConnectAvailable()

            _uiState.value = _uiState.value.copy(
                lastSyncTime = lastSync,
                autoSyncEnabled = autoSync,
                hasPermissions = hasPermissions,
                isHealthConnectAvailable = isAvailable
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Syncing)

            val result = syncSleepUseCase.syncRecent()

            result.fold(
                onSuccess = { count ->
                    val lastSync = sleepRepository.getLastSyncTime()
                    _uiState.value = _uiState.value.copy(
                        syncStatus = SyncStatus.Success(count),
                        lastSyncTime = lastSync,
                        successMessage = "Синхронизировано записей: $count"
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        syncStatus = SyncStatus.Error(exception.message ?: "Ошибка"),
                        errorMessage = exception.message ?: "Ошибка синхронизации"
                    )
                }
            )
        }
    }

    fun syncPeriod(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.Syncing)

            val result = syncSleepUseCase.syncPeriod(startDate, endDate)

            result.fold(
                onSuccess = { count ->
                    val lastSync = sleepRepository.getLastSyncTime()
                    _uiState.value = _uiState.value.copy(
                        syncStatus = SyncStatus.Success(count),
                        lastSyncTime = lastSync,
                        successMessage = if (count > 0) {
                            "Синхронизировано записей: $count"
                        } else {
                            "Нет данных за выбранный период"
                        }
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        syncStatus = SyncStatus.Error(exception.message ?: "Ошибка"),
                        errorMessage = exception.message ?: "Ошибка синхронизации"
                    )
                }
            )
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        sleepRepository.setAutoSyncEnabled(enabled)
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            authRepository.logout()
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun updatePermissionStatus(hasPermissions: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermissions = hasPermissions)
    }
}

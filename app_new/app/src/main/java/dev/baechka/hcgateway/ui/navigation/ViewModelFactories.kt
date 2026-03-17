package dev.baechka.hcgateway.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.baechka.hcgateway.data.repository.AuthRepository
import dev.baechka.hcgateway.data.repository.SleepRepository
import dev.baechka.hcgateway.domain.usecase.LoginUseCase
import dev.baechka.hcgateway.domain.usecase.SyncSleepUseCase
import dev.baechka.hcgateway.ui.login.LoginViewModel
import dev.baechka.hcgateway.ui.main.MainViewModel

class LoginViewModelFactory(
    private val loginUseCase: LoginUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(loginUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainViewModelFactory(
    private val authRepository: AuthRepository,
    private val sleepRepository: SleepRepository,
    private val syncSleepUseCase: SyncSleepUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(authRepository, sleepRepository, syncSleepUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

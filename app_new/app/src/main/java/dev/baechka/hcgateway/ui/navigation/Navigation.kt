package dev.baechka.hcgateway.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.baechka.hcgateway.data.repository.AuthRepository
import dev.baechka.hcgateway.di.AppModule
import dev.baechka.hcgateway.service.SleepSyncForegroundService
import dev.baechka.hcgateway.ui.login.LoginScreen
import dev.baechka.hcgateway.ui.login.LoginViewModel
import dev.baechka.hcgateway.ui.main.MainScreen
import dev.baechka.hcgateway.ui.main.MainViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authRepository = remember { AppModule.provideAuthRepository(context) }
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            startDestination = if (isLoggedIn) {
                Screen.Main.route
            } else {
                Screen.Login.route
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (startDestination != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination!!
        ) {
            composable(Screen.Login.route) {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(
                        AppModule.provideLoginUseCase(context)
                    )
                )

                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Main.route) {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(
                        AppModule.provideAuthRepository(context),
                        AppModule.provideSleepRepository(context),
                        AppModule.provideSyncSleepUseCase(context)
                    )
                )

                val uiState by mainViewModel.uiState.collectAsState()

                LaunchedEffect(uiState.autoSyncEnabled) {
                    if (uiState.autoSyncEnabled) {
                        SleepSyncForegroundService.start(context)
                    } else {
                        SleepSyncForegroundService.stop(context)
                    }
                }

                MainScreen(
                    viewModel = mainViewModel,
                    onLogout = {
                        SleepSyncForegroundService.stop(context)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

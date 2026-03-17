package dev.baechka.hcgateway.ui.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import dev.baechka.hcgateway.di.AppModule
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPeriodBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sleepRepository = remember { AppModule.provideSleepRepository(context) }

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        scope.launch {
            viewModel.updatePermissionStatus(sleepRepository.hasPermissions())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        val allGranted = sleepRepository.getRequiredPermissions().all { grantedPermissions.contains(it) }
        viewModel.updatePermissionStatus(allGranted)
    }

    fun requestPermissions() {
        try {
            val permissions = sleepRepository.getRequiredPermissions()
            permissionLauncher.launch(permissions)

            scope.launch {
                kotlinx.coroutines.delay(500)
                if (!sleepRepository.hasPermissions()) {
                    try {
                        val intent = PermissionController.createRequestPermissionResultContract()
                            .createIntent(context, permissions)
                        activityLauncher.launch(intent)
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("Откройте настройки Health Connect вручную")
                    }
                }
            }
        } catch (_: Exception) { }
    }

    LaunchedEffect(Unit) {
        if (!uiState.isHealthConnectAvailable) {
            // не показываем snackbar, карточка уже видна
        } else if (!uiState.hasPermissions) {
            requestPermissions()
        }
    }

    LaunchedEffect(uiState.isLoggingOut) {
        if (uiState.isLoggingOut) {
            onLogout()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val has = sleepRepository.hasPermissions()
                    if (has != uiState.hasPermissions) {
                        viewModel.updatePermissionStatus(has)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Sync") },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выйти")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.hasPermissions) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.syncNow() },
                    icon = {
                        if (uiState.syncStatus is SyncStatus.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    },
                    text = { Text("Синхронизировать") },
                    expanded = uiState.syncStatus !is SyncStatus.Syncing
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isHealthConnectAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Health Connect недоступен",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Для работы приложения необходим Health Connect. Убедитесь, что он установлен и обновлён.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse(
                                            "market://details?id=com.google.android.apps.healthdata"
                                        )
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) { }
                            }
                        ) {
                            Text("Открыть Google Play")
                        }
                    }
                }
            } else if (!uiState.hasPermissions) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Требуются разрешения",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Предоставьте доступ к данным сна",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { requestPermissions() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Предоставить доступ")
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                                        context.startActivity(intent)
                                    } catch (_: Exception) { }
                                }
                            ) {
                                Text("Настройки")
                            }
                        }
                    }
                }
            }

            SyncStatusCard(
                lastSyncTime = uiState.lastSyncTime,
                syncStatus = uiState.syncStatus
            )

            OutlinedButton(
                onClick = { showPeriodBottomSheet = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.hasPermissions && uiState.syncStatus !is SyncStatus.Syncing
            ) {
                Text("Синхронизация за период")
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Автосинхронизация",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Каждый час",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.autoSyncEnabled,
                        onCheckedChange = { viewModel.toggleAutoSync(it) },
                        enabled = uiState.hasPermissions
                    )
                }
            }
        }
    }

    if (showPeriodBottomSheet) {
        SyncPeriodBottomSheet(
            onDismiss = { showPeriodBottomSheet = false },
            onSync = { start, end ->
                viewModel.syncPeriod(start, end)
                showPeriodBottomSheet = false
            }
        )
    }
}

@Composable
fun SyncStatusCard(
    lastSyncTime: Long?,
    syncStatus: SyncStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Статус синхронизации",
                style = MaterialTheme.typography.titleMedium
            )

            when (syncStatus) {
                is SyncStatus.Idle -> {
                    Text(
                        if (lastSyncTime != null) "Синхронизировано" else "Ещё не синхронизировалось",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (lastSyncTime != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                is SyncStatus.Syncing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            "Синхронизация...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is SyncStatus.Success -> {
                    Text(
                        "Синхронизировано",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is SyncStatus.Error -> {
                    Text(
                        "Ошибка",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (lastSyncTime != null) {
                val formatter = DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                val formattedTime = formatter.format(Instant.ofEpochMilli(lastSyncTime))

                Text(
                    "Последняя синхронизация: $formattedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

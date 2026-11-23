package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.DayKey
import com.example.ui.MainViewModel
import com.example.ui.UiEvent
import com.example.ui.components.AppDestination
import com.example.ui.components.FloatingPillBottomBarWithFooter
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.SeFueLaLuzTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            SeFueLaLuzTheme(themeMode = themeMode) {
                SeFueLaLuzApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNow()
    }
}

@Composable
fun SeFueLaLuzApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.INICIO.route
    val currentDestination = AppDestination.entries.firstOrNull { it.route == currentRoute }
        ?: AppDestination.INICIO

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onNotificationPermissionResult(granted)
        }
    )

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val json = viewModel.buildExportJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("No se pudo abrir el archivo de destino")
                    }
                    snackbarHostState.showSnackbar("Historial exportado correctamente")
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar("No se pudo exportar el archivo JSON")
                }
            }
        }
    )

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                try {
                    val raw = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        } ?: error("No se pudo leer el archivo")
                    }
                    viewModel.importBackupJson(raw)
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar("No se pudo leer el archivo seleccionado")
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                UiEvent.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    val density = LocalDensity.current
    var bottomOverlayHeight by remember { mutableStateOf(128.dp) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.INICIO.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(AppDestination.INICIO.route) {
                    HomeScreen(
                        uiState = uiState,
                        bottomContentPadding = bottomOverlayHeight,
                        onStartOutage = viewModel::onStartOutageClicked,
                        onStopOutage = viewModel::onStopOutageClicked,
                        onCancelActiveOutage = viewModel::onCancelActiveOutageConfirmed,
                        onAddManualOutage = viewModel::addManualOutage
                    )
                }
                composable(AppDestination.HISTORIAL.route) {
                    HistoryScreen(
                        uiState = uiState,
                        bottomContentPadding = bottomOverlayHeight,
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        onUpdateOutage = viewModel::updateOutage,
                        onDeleteOutage = viewModel::deleteOutage
                    )
                }
                composable(AppDestination.ESTADISTICAS.route) {
                    StatsScreen(
                        uiState = uiState,
                        bottomContentPadding = bottomOverlayHeight
                    )
                }
                composable(AppDestination.AJUSTES.route) {
                    SettingsScreen(
                        uiState = uiState,
                        bottomContentPadding = bottomOverlayHeight,
                        onThemeModeChange = viewModel::setThemeMode,
                        onExportJsonClick = {
                            val today = DayKey.fromMillis(System.currentTimeMillis())
                            val filename = String.format(
                                Locale.US,
                                "SeFueLaLuz-respaldo-%04d%02d%02d.json",
                                today.year,
                                today.month,
                                today.dayOfMonth
                            )
                            exportJsonLauncher.launch(filename)
                        },
                        onImportJsonClick = {
                            importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        onDeleteAllConfirmed = viewModel::deleteAllData,
                        onShowMessage = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomOverlayHeight + 8.dp, start = 16.dp, end = 16.dp)
            )

            FloatingPillBottomBarWithFooter(
                currentDestination = currentDestination,
                onNavigate = { target ->
                    navController.navigate(target.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        bottomOverlayHeight = with(density) { size.height.toDp() }
                    }
            )
        }
    }
}

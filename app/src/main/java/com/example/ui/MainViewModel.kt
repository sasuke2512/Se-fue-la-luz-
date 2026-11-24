package com.example.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppThemeMode
import com.example.data.local.OutageDatabase
import com.example.data.local.OutageEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.ImportResult
import com.example.data.repository.OutageRepository
import com.example.domain.OutageStatsCalculator
import com.example.domain.OutageStatsSnapshot
import com.example.domain.SpanishFormatters
import com.example.notification.OutageNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class NoticeType {
    SUCCESS,
    INFO
}

data class AppNotice(
    val id: Long,
    val message: String,
    val type: NoticeType = NoticeType.INFO
)

data class MainUiState(
    val activeOutage: OutageEntity? = null,
    val allOutages: List<OutageEntity> = emptyList(),
    val completedOutages: List<OutageEntity> = emptyList(),
    val nowMillis: Long = System.currentTimeMillis(),
    val stats: OutageStatsSnapshot = OutageStatsCalculator.calculate(emptyList(), System.currentTimeMillis()),
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val searchQuery: String = "",
    val isLoading: Boolean = true
) {
    val isPowerOut: Boolean
        get() = activeOutage != null

    val activeElapsedMillis: Long
        get() = activeOutage?.let { (nowMillis - it.startTimeMillis).coerceAtLeast(0L) } ?: 0L
}

sealed interface UiEvent {
    data object RequestNotificationPermission : UiEvent
}

private data class BaseTrackingInputs(
    val activeOutage: OutageEntity?,
    val allOutages: List<OutageEntity>,
    val nowMillis: Long,
    val trackingStartMillis: Long?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = OutageDatabase.getInstance(application)
    private val repository = OutageRepository(database.outageDao())
    private val preferences = UserPreferencesRepository(application)

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    private val _searchQuery = MutableStateFlow("")
    private val _currentNotice = MutableStateFlow<AppNotice?>(null)
    val currentNotice: StateFlow<AppNotice?> = _currentNotice.asStateFlow()
    private var noticeDismissJob: Job? = null

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val themeMode: StateFlow<AppThemeMode> = preferences.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppThemeMode.SYSTEM
        )

    private val baseInputs = combine(
        repository.activeOutage,
        repository.allOutages,
        _nowMillis,
        preferences.trackingStartMillis
    ) { active, all, now, trackingStart ->
        BaseTrackingInputs(
            activeOutage = active,
            allOutages = all,
            nowMillis = now,
            trackingStartMillis = trackingStart
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        baseInputs,
        themeMode,
        _searchQuery
    ) { base, theme, query ->
        val completed = base.allOutages.filter { it.endTimeMillis != null }
        val stats = OutageStatsCalculator.calculate(
            outages = base.allOutages,
            nowMillis = base.nowMillis,
            trackingStartMillis = base.trackingStartMillis
        )
        MainUiState(
            activeOutage = base.activeOutage,
            allOutages = base.allOutages,
            completedOutages = completed,
            nowMillis = base.nowMillis,
            stats = stats,
            themeMode = theme,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            preferences.ensureTrackingStartInitialized(System.currentTimeMillis())
        }
        startClockTicker()
        syncActiveNotificationOnLaunch()
    }

    fun showNotice(message: String, type: NoticeType = NoticeType.INFO) {
        noticeDismissJob?.cancel()
        _currentNotice.value = AppNotice(
            id = System.nanoTime(),
            message = message,
            type = type
        )
        noticeDismissJob = viewModelScope.launch {
            delay(2_500L)
            _currentNotice.value = null
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                _nowMillis.value = now
                val hasActive = uiState.value.activeOutage != null
                delay(if (hasActive) 500L else 15_000L)
            }
        }
    }

    private fun syncActiveNotificationOnLaunch() {
        viewModelScope.launch {
            val active = repository.getActiveOutageOnce()
            val ctx = getApplication<Application>()
            if (active != null) {
                OutageNotificationHelper.showOngoingOutageNotification(ctx, active.startTimeMillis)
            } else {
                OutageNotificationHelper.cancelOutageNotification(ctx)
            }
        }
    }

    fun refreshNow() {
        _nowMillis.value = System.currentTimeMillis()
    }

    fun onStartOutageClicked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val insertedId = repository.startOutage(now)
            if (insertedId == null) {
                showNotice("Ya hay un apagón en curso.", NoticeType.INFO)
                return@launch
            }
            _nowMillis.value = now

            val ctx = getApplication<Application>()
            val askedBefore = preferences.hasAskedNotificationPermission.first()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !OutageNotificationHelper.hasNotificationPermission(ctx) &&
                !askedBefore
            ) {
                preferences.markNotificationPermissionAsked()
                _events.emit(UiEvent.RequestNotificationPermission)
            } else {
                OutageNotificationHelper.showOngoingOutageNotification(ctx, now)
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            viewModelScope.launch {
                val active = repository.getActiveOutageOnce()
                if (active != null) {
                    OutageNotificationHelper.showOngoingOutageNotification(
                        getApplication(),
                        active.startTimeMillis
                    )
                }
            }
        }
    }

    fun onStopOutageClicked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val completed = repository.stopActiveOutage(now)
            _nowMillis.value = now
            OutageNotificationHelper.cancelOutageNotification(getApplication())
            if (completed != null && completed.endTimeMillis != null) {
                val duration = SpanishFormatters.formatDurationReadable(
                    completed.endTimeMillis - completed.startTimeMillis
                )
                showNotice("Apagón guardado ($duration)", NoticeType.SUCCESS)
            }
        }
    }

    fun onCancelActiveOutageConfirmed() {
        viewModelScope.launch {
            repository.cancelActiveOutage()
            _nowMillis.value = System.currentTimeMillis()
            OutageNotificationHelper.cancelOutageNotification(getApplication())
            showNotice("Apagón activo cancelado", NoticeType.SUCCESS)
        }
    }

    fun addManualOutage(startMillis: Long, endMillis: Long, note: String) {
        viewModelScope.launch {
            try {
                repository.addManualOutage(startMillis, endMillis, note)
                _nowMillis.value = System.currentTimeMillis()
                showNotice("Apagón registrado en el historial", NoticeType.SUCCESS)
            } catch (e: IllegalArgumentException) {
                showNotice(e.message ?: "Horario inválido", NoticeType.INFO)
            }
        }
    }

    fun updateOutage(outage: OutageEntity) {
        viewModelScope.launch {
            try {
                repository.updateOutage(outage)
                _nowMillis.value = System.currentTimeMillis()
                showNotice("Cambios guardados", NoticeType.SUCCESS)
            } catch (e: IllegalArgumentException) {
                showNotice(e.message ?: "Horario inválido", NoticeType.INFO)
            }
        }
    }

    fun deleteOutage(id: Long) {
        viewModelScope.launch {
            repository.deleteOutage(id)
            _nowMillis.value = System.currentTimeMillis()
            showNotice("Apagón eliminado", NoticeType.SUCCESS)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    suspend fun buildExportJson(): String {
        return repository.exportCompletedToJson(appVersion = BuildConfig.VERSION_NAME)
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            try {
                val result: ImportResult = repository.importFromJson(rawJson)
                _nowMillis.value = System.currentTimeMillis()
                val msg = buildString {
                    append("Importados: ${result.addedCount}")
                    if (result.skippedDuplicateCount > 0) {
                        append(" · Omitidos (duplicados): ${result.skippedDuplicateCount}")
                    }
                    if (result.invalidCount > 0) {
                        append(" · Inválidos: ${result.invalidCount}")
                    }
                }
                showNotice(msg, NoticeType.SUCCESS)
            } catch (_: Exception) {
                showNotice("El archivo JSON no tiene un formato válido", NoticeType.INFO)
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllOutages()
            OutageNotificationHelper.cancelOutageNotification(getApplication())
            val resetNow = System.currentTimeMillis()
            preferences.clearTrackingStart()
            preferences.ensureTrackingStartInitialized(resetNow)
            _nowMillis.value = resetNow
            showNotice("Todos los datos fueron eliminados", NoticeType.SUCCESS)
        }
    }
}

package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.CodeCatcherApp
import com.example.data.local.AppSettings
import com.example.data.local.CopiedCodeEntity
import com.example.domain.MonitoredApp
import com.example.service.CodeCatcherService
import com.example.util.ClipboardHelper
import com.example.util.FeedbackHelper
import com.example.util.PermissionHelper
import com.example.util.PermissionStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val history: List<CopiedCodeEntity> = emptyList(),
    val permissions: PermissionStatus = PermissionStatus(),
    val showOnboardingDialog: Boolean = false,
    val showClearHistoryDialog: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CodeCatcherApp
    private val settingsRepo = app.container.settingsRepository
    private val historyRepo = app.container.historyRepository

    private val permissionFlow = MutableStateFlow(PermissionHelper.checkStatus(application))
    private val forceOnboardingFlow = MutableStateFlow(false)
    private val clearDialogFlow = MutableStateFlow(false)

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    val uiState: StateFlow<MainUiState> = combine(
        settingsRepo.settingsFlow,
        historyRepo.historyFlow,
        permissionFlow,
        forceOnboardingFlow,
        clearDialogFlow
    ) { settings, history, permissions, forceOnboarding, showClearDialog ->
        val autoShowOnboarding = !settings.onboardingDismissed && !permissions.allRecommendedGranted
        MainUiState(
            settings = settings,
            history = history,
            permissions = permissions,
            showOnboardingDialog = forceOnboarding || autoShowOnboarding,
            showClearHistoryDialog = showClearDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(permissions = PermissionHelper.checkStatus(application))
    )

    init {
        viewModelScope.launch {
            val snapshot = settingsRepo.getSnapshot()
            val perms = PermissionHelper.checkStatus(application)
            if (snapshot.hasAnyAppSelected && perms.overlayGranted) {
                CodeCatcherService.startOrSync(application)
            }
        }
    }

    fun refreshPermissions() {
        val updated = PermissionHelper.checkStatus(app)
        permissionFlow.value = updated
        viewModelScope.launch {
            val snapshot = settingsRepo.getSnapshot()
            if (snapshot.hasAnyAppSelected && updated.overlayGranted) {
                CodeCatcherService.startOrSync(app)
            }
        }
    }

    fun toggleApp(monitoredApp: MonitoredApp) {
        viewModelScope.launch {
            val current = settingsRepo.getSnapshot()
            val currentlyEnabled = when (monitoredApp) {
                MonitoredApp.WHATSAPP -> current.whatsAppEnabled
                MonitoredApp.INSTAGRAM -> current.instagramEnabled
            }
            val nextEnabled = !currentlyEnabled
            settingsRepo.setAppEnabled(monitoredApp, nextEnabled)

            val updated = settingsRepo.getSnapshot()
            val perms = PermissionHelper.checkStatus(app)
            permissionFlow.value = perms

            if (updated.hasAnyAppSelected) {
                if (!perms.allRequiredGranted) {
                    forceOnboardingFlow.value = true
                }
                if (perms.overlayGranted) {
                    CodeCatcherService.startOrSync(app)
                }
            } else {
                CodeCatcherService.stop(app)
            }
        }
    }

    fun toggleMasterMonitoring() {
        viewModelScope.launch {
            val snapshot = settingsRepo.getSnapshot()
            if (!snapshot.hasAnyAppSelected) return@launch
            settingsRepo.toggleMonitoringActive()
            if (PermissionHelper.isOverlayGranted(app)) {
                CodeCatcherService.startOrSync(app)
            }
        }
    }

    fun openOnboarding() {
        forceOnboardingFlow.value = true
    }

    fun dismissOnboarding() {
        forceOnboardingFlow.value = false
        viewModelScope.launch {
            settingsRepo.setOnboardingDismissed(true)
        }
    }

    fun setVibrateOnCopy(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setVibrateOnCopy(enabled) }
    }

    fun setSoundOnCopy(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSoundOnCopy(enabled) }
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoStartOnBoot(enabled) }
    }

    fun requestClearHistory() {
        clearDialogFlow.value = true
    }

    fun dismissClearHistory() {
        clearDialogFlow.value = false
    }

    fun confirmClearHistory() {
        clearDialogFlow.value = false
        viewModelScope.launch {
            historyRepo.clearHistory()
            _snackbarEvents.tryEmit("History cleared")
        }
    }

    fun copyCodeAgain(code: String) {
        viewModelScope.launch {
            val copied = ClipboardHelper.copyDirect(app, code)
            if (copied) {
                val settings = settingsRepo.getSnapshot()
                FeedbackHelper.triggerCopyFeedback(
                    context = app,
                    vibrate = settings.vibrateOnCopy,
                    sound = settings.soundOnCopy
                )
                _snackbarEvents.tryEmit("Copied $code to clipboard")
            }
        }
    }
}

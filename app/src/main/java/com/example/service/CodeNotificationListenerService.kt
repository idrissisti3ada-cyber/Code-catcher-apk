package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.CodeCatcherApp
import com.example.domain.CodeExtractor
import com.example.domain.MonitoredApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CodeNotificationListenerService : NotificationListenerService() {

    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        val pkg = statusBarNotification.packageName ?: return
        val monitoredApp = MonitoredApp.fromPackage(pkg) ?: return
        val notification = statusBarNotification.notification ?: return

        val app = application as? CodeCatcherApp ?: return
        val settingsRepo = app.container.settingsRepository
        val historyRepo = app.container.historyRepository

        listenerScope.launch {
            val settings = settingsRepo.getSnapshot()
            if (!settings.isMonitoringActive) return@launch
            if (!settings.isPackageAllowed(pkg)) return@launch

            val textSegments = CodeExtractor.collectNotificationTexts(notification)
            if (textSegments.isEmpty()) return@launch

            val candidateCodes = linkedSetOf<String>()
            for (segment in textSegments) {
                candidateCodes.addAll(CodeExtractor.extractCodes(segment))
            }
            if (candidateCodes.isEmpty()) return@launch

            val title = CodeExtractor.extractTitle(notification)
            for (code in candidateCodes) {
                val inserted = historyRepo.recordIfNew(
                    code = code,
                    app = monitoredApp,
                    sourcePackage = pkg,
                    senderTitle = title
                )
                if (inserted != null) {
                    CodeCatcherService.notifyCodeDetected(
                        context = applicationContext,
                        code = code,
                        sourceApp = monitoredApp.displayName
                    )
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        listenerScope.cancel()
        super.onDestroy()
    }
}

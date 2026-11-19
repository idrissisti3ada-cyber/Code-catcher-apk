package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.CodeCatcherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as? CodeCatcherApp
        if (app == null) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.container.settingsRepository.getSnapshot()
                if (settings.autoStartOnBoot && settings.hasAnyAppSelected) {
                    CodeCatcherService.startOrSync(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

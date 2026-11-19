package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.MonitoredApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "code_catcher_prefs")

data class AppSettings(
    val whatsAppEnabled: Boolean = false,
    val instagramEnabled: Boolean = false,
    val isMonitoringActive: Boolean = false,
    val vibrateOnCopy: Boolean = true,
    val soundOnCopy: Boolean = false,
    val autoStartOnBoot: Boolean = true,
    val onboardingDismissed: Boolean = false,
    val bubbleX: Int = 24,
    val bubbleY: Int = 360
) {
    val hasAnyAppSelected: Boolean
        get() = whatsAppEnabled || instagramEnabled

    fun isPackageAllowed(packageName: String): Boolean {
        val app = MonitoredApp.fromPackage(packageName) ?: return false
        return when (app) {
            MonitoredApp.WHATSAPP -> whatsAppEnabled
            MonitoredApp.INSTAGRAM -> instagramEnabled
        }
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val WHATSAPP_ENABLED = booleanPreferencesKey("whatsapp_enabled")
        val INSTAGRAM_ENABLED = booleanPreferencesKey("instagram_enabled")
        val MONITORING_ACTIVE = booleanPreferencesKey("monitoring_active")
        val VIBRATE_ON_COPY = booleanPreferencesKey("vibrate_on_copy")
        val SOUND_ON_COPY = booleanPreferencesKey("sound_on_copy")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val ONBOARDING_DISMISSED = booleanPreferencesKey("onboarding_dismissed")
        val BUBBLE_X = intPreferencesKey("bubble_x")
        val BUBBLE_Y = intPreferencesKey("bubble_y")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            whatsAppEnabled = prefs[Keys.WHATSAPP_ENABLED] ?: false,
            instagramEnabled = prefs[Keys.INSTAGRAM_ENABLED] ?: false,
            isMonitoringActive = prefs[Keys.MONITORING_ACTIVE] ?: false,
            vibrateOnCopy = prefs[Keys.VIBRATE_ON_COPY] ?: true,
            soundOnCopy = prefs[Keys.SOUND_ON_COPY] ?: false,
            autoStartOnBoot = prefs[Keys.AUTO_START_ON_BOOT] ?: true,
            onboardingDismissed = prefs[Keys.ONBOARDING_DISMISSED] ?: false,
            bubbleX = prefs[Keys.BUBBLE_X] ?: 24,
            bubbleY = prefs[Keys.BUBBLE_Y] ?: 360
        )
    }

    suspend fun getSnapshot(): AppSettings = settingsFlow.first()

    suspend fun setAppEnabled(app: MonitoredApp, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (app) {
                MonitoredApp.WHATSAPP -> prefs[Keys.WHATSAPP_ENABLED] = enabled
                MonitoredApp.INSTAGRAM -> prefs[Keys.INSTAGRAM_ENABLED] = enabled
            }
            val wa = if (app == MonitoredApp.WHATSAPP) enabled else (prefs[Keys.WHATSAPP_ENABLED] ?: false)
            val ig = if (app == MonitoredApp.INSTAGRAM) enabled else (prefs[Keys.INSTAGRAM_ENABLED] ?: false)
            if (enabled) {
                // Activating an app automatically activates monitoring
                prefs[Keys.MONITORING_ACTIVE] = true
            } else if (!wa && !ig) {
                prefs[Keys.MONITORING_ACTIVE] = false
            }
        }
    }

    suspend fun setMonitoringActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MONITORING_ACTIVE] = active
        }
    }

    suspend fun toggleMonitoringActive(): Boolean {
        var newState = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.MONITORING_ACTIVE] ?: false
            newState = !current
            prefs[Keys.MONITORING_ACTIVE] = newState
        }
        return newState
    }

    suspend fun setVibrateOnCopy(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.VIBRATE_ON_COPY] = enabled }
    }

    suspend fun setSoundOnCopy(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SOUND_ON_COPY] = enabled }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_START_ON_BOOT] = enabled }
    }

    suspend fun setOnboardingDismissed(dismissed: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_DISMISSED] = dismissed }
    }

    suspend fun saveBubblePosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUBBLE_X] = x
            prefs[Keys.BUBBLE_Y] = y
        }
    }
}

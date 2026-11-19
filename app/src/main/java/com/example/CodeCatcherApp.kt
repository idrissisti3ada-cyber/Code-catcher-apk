package com.example

import android.app.Application
import com.example.data.local.CodeCatcherDatabase
import com.example.data.local.CodeHistoryRepository
import com.example.data.local.SettingsRepository

class AppContainer(application: Application) {
    private val database: CodeCatcherDatabase = CodeCatcherDatabase.getInstance(application)
    val settingsRepository: SettingsRepository = SettingsRepository(application)
    val historyRepository: CodeHistoryRepository = CodeHistoryRepository(database.copiedCodeDao())
}

class CodeCatcherApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

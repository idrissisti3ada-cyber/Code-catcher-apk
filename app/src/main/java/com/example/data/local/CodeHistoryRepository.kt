package com.example.data.local

import com.example.domain.MonitoredApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CodeHistoryRepository(private val dao: CopiedCodeDao) {

    private val mutex = Mutex()
    private var hydrated = false
    private val seenCodes = HashSet<String>()

    val historyFlow: Flow<List<CopiedCodeEntity>> = dao.observeAll()

    /**
     * Atomically checks if [code] has already been copied; if new, persists it to Room and
     * returns the inserted entity. Returns null if [code] was already copied (duplicate).
     */
    suspend fun recordIfNew(
        code: String,
        app: MonitoredApp,
        sourcePackage: String,
        senderTitle: String
    ): CopiedCodeEntity? = mutex.withLock {
        if (!hydrated) {
            seenCodes.addAll(dao.getAllCodes())
            hydrated = true
        }
        if (code in seenCodes || dao.exists(code)) {
            seenCodes.add(code)
            return@withLock null
        }
        val entity = CopiedCodeEntity(
            code = code,
            sourceApp = app.displayName,
            sourcePackage = sourcePackage,
            senderTitle = senderTitle,
            copiedAt = System.currentTimeMillis()
        )
        val rowId = dao.insert(entity)
        if (rowId == -1L) {
            seenCodes.add(code)
            return@withLock null
        }
        seenCodes.add(code)
        entity.copy(id = rowId)
    }

    suspend fun clearHistory() = mutex.withLock {
        dao.clearAll()
        seenCodes.clear()
        hydrated = true
    }
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CopiedCodeDao {
    @Query("SELECT * FROM copied_codes ORDER BY copiedAt DESC")
    fun observeAll(): Flow<List<CopiedCodeEntity>>

    @Query("SELECT code FROM copied_codes")
    suspend fun getAllCodes(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM copied_codes WHERE code = :code)")
    suspend fun exists(code: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CopiedCodeEntity): Long

    @Query("DELETE FROM copied_codes")
    suspend fun clearAll()
}

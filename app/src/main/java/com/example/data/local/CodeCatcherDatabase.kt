package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CopiedCodeEntity::class], version = 1, exportSchema = false)
abstract class CodeCatcherDatabase : RoomDatabase() {
    abstract fun copiedCodeDao(): CopiedCodeDao

    companion object {
        @Volatile
        private var INSTANCE: CodeCatcherDatabase? = null

        fun getInstance(context: Context): CodeCatcherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CodeCatcherDatabase::class.java,
                    "code_catcher_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "copied_codes",
    indices = [Index(value = ["code"], unique = true)]
)
data class CopiedCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val sourceApp: String,
    val sourcePackage: String,
    val senderTitle: String,
    val copiedAt: Long = System.currentTimeMillis()
)

package com.example.imageanalyzer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// data/local/entity/HistoryEntity.kt
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "image_path") val imagePath: String?,
    @ColumnInfo(name = "prompt") val prompt: String,
    @ColumnInfo(name = "response") val response: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)


package com.example.imageanalyzer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// data/local/dao/HistoryDao.kt
@Dao
interface HistoryDao {
    @Insert
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistory(id: Long)
}

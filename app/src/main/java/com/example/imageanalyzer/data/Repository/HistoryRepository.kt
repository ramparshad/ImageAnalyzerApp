package com.example.imageanalyzer.data.Repository

import com.example.imageanalyzer.data.HistoryDao
import com.example.imageanalyzer.data.HistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun getAllHistory(): Flow<List<HistoryEntity>> {
        return historyDao.getAllHistory()
            .catch { e ->
                // Handle any errors in the Flow
                throw Exception("Failed to load history: ${e.message}")
            }
    }

    suspend fun insertHistory(history: HistoryEntity) {
        try {
            historyDao.insertHistory(history)
        } catch (e: Exception) {
            throw Exception("Failed to insert history: ${e.message}")
        }
    }

    suspend fun deleteHistory(id: Long) {
        try {
            historyDao.deleteHistory(id)
        } catch (e: Exception) {
            throw Exception("Failed to delete history: ${e.message}")
        }
    }
}
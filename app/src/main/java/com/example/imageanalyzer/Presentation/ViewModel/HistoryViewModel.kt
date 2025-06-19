package com.example.imageanalyzer.Presentation.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageanalyzer.data.HistoryEntity
import com.example.imageanalyzer.data.Repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HistoryState {
    object Loading : HistoryState()
    data class Success(val data: List<HistoryEntity>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState = _historyState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading
            try {
                historyRepository.getAllHistory()
                    .collect { historyList ->
                        _historyState.value = HistoryState.Success(historyList)
                    }
            } catch (e: Exception) {
                _historyState.value = HistoryState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            try {
                historyRepository.deleteHistory(id)
                // No need to manually update state here - Flow will emit new list
            } catch (e: Exception) {
                _historyState.value = HistoryState.Error(
                    "Failed to delete item: ${e.message}"
                )
            }
        }
    }
}
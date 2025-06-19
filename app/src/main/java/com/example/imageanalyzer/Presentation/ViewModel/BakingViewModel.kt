package com.example.imageanalyzer.Presentation.ViewModel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageanalyzer.Presentation.Screens.UiState
import com.example.imageanalyzer.data.HistoryEntity
import com.example.imageanalyzer.data.Repository.HistoryRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// presentation/baking/BakingViewModel.kt
@HiltViewModel
class BakingViewModel @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun sendPrompt(bitmap: Bitmap, prompt: String) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                    // Save to history
                    historyRepository.insertHistory(
                        HistoryEntity(
                            imagePath = null, // You might want to save image path here
                            prompt = prompt,
                            response = outputContent
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}
package com.puzzlella.app.ui.samples

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzlella.app.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SamplesUiState(
    val sampleImages: List<String> = emptyList(),
    val isLoading: Boolean = true
)

class SamplesViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SamplesUiState())
    val uiState: StateFlow<SamplesUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = SamplesUiState(
            sampleImages = imageRepository.getSampleImages(),
            isLoading = false
        )
    }

    fun onSampleSelected(assetName: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val savedPath = imageRepository.copySampleToStorage(assetName)
            onReady(savedPath)
        }
    }
}

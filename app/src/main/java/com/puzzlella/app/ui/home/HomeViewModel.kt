package com.puzzlella.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.puzzlella.app.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val sampleImages: List<String> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSamples()
    }

    private fun loadSamples() {
        _uiState.value = _uiState.value.copy(
            sampleImages = imageRepository.getSampleImages()
        )
    }
}

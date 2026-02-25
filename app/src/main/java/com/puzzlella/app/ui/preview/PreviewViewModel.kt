package com.puzzlella.app.ui.preview

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PreviewUiState(
    val selectedPieceCount: Int = 12,
    val isLoading: Boolean = false
)

class PreviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    fun selectPieceCount(count: Int) {
        _uiState.value = _uiState.value.copy(selectedPieceCount = count)
    }
}

package com.puzzlella.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzlella.app.data.model.PuzzleHistory
import com.puzzlella.app.data.repository.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val puzzles: List<PuzzleHistory> = emptyList(),
    val isLoading: Boolean = true,
    val showDeleteConfirm: Long? = null
)

class HistoryViewModel(
    private val puzzleRepository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            puzzleRepository.getAllHistory().collect { puzzles ->
                _uiState.value = _uiState.value.copy(
                    puzzles = puzzles,
                    isLoading = false
                )
            }
        }
    }

    fun showDeleteConfirm(id: Long) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = id)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
    }

    fun deletePuzzle(id: Long) {
        viewModelScope.launch {
            puzzleRepository.delete(id)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
        }
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}

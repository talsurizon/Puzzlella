package com.puzzlella.app.ui.puzzle

import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzlella.app.data.model.PuzzleConfig
import com.puzzlella.app.data.model.PuzzleHistory
import com.puzzlella.app.data.model.PuzzleStatus
import com.puzzlella.app.data.repository.ImageRepository
import com.puzzlella.app.data.repository.PuzzleRepository
import com.puzzlella.app.engine.PuzzleBoard
import com.puzzlella.app.engine.PuzzlePiece
import com.puzzlella.app.engine.PuzzlePieceGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PuzzleUiState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val isLoading: Boolean = true,
    val showHint: Boolean = false,
    val elapsedTimeMs: Long = 0,
    val moves: Int = 0,
    val isCompleted: Boolean = false,
    val showSuccess: Boolean = false,
    val boardWidth: Float = 0f,
    val boardHeight: Float = 0f,
    val draggedPieceId: Int? = null
)

class PuzzleViewModel(
    private val imagePath: String,
    private val pieceCount: Int,
    private val puzzleRepository: PuzzleRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    private var board: PuzzleBoard? = null
    private val generator = PuzzlePieceGenerator()
    private var timerJob: Job? = null
    private var historyId: Long = -1
    private var startTime: Long = System.currentTimeMillis()

    fun initializePuzzle(boardWidth: Float, boardHeight: Float) {
        if (board != null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val bitmap = BitmapFactory.decodeFile(imagePath) ?: return@launch
            val config = PuzzleConfig.forPieceCount(pieceCount)

            val pieces = generator.generatePieces(
                sourceBitmap = bitmap,
                rows = config.rows,
                cols = config.columns,
                boardWidth = boardWidth,
                boardHeight = boardHeight
            )

            board = PuzzleBoard(
                pieces = pieces,
                rows = config.rows,
                cols = config.columns,
                boardWidth = boardWidth,
                boardHeight = boardHeight
            )

            board?.shufflePieces(boardWidth * 1.5f, boardHeight * 1.5f)

            _uiState.value = _uiState.value.copy(
                pieces = pieces,
                isLoading = false,
                boardWidth = boardWidth,
                boardHeight = boardHeight
            )

            // Save to history
            val thumbnailPath = imageRepository.saveThumbnail(imagePath)
            historyId = puzzleRepository.insert(
                PuzzleHistory(
                    imagePath = imagePath,
                    thumbnailPath = thumbnailPath,
                    pieceCount = pieceCount,
                    status = PuzzleStatus.IN_PROGRESS,
                    elapsedTimeMs = 0,
                    moves = 0,
                    dateStarted = startTime
                )
            )

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                if (!_uiState.value.isCompleted) {
                    _uiState.value = _uiState.value.copy(
                        elapsedTimeMs = System.currentTimeMillis() - startTime
                    )
                }
            }
        }
    }

    fun onPieceDragStart(pieceId: Int) {
        board?.let { b ->
            val reordered = b.bringToFront(pieceId)
            _uiState.value = _uiState.value.copy(
                pieces = reordered,
                draggedPieceId = pieceId
            )
        }
    }

    fun onPieceDrag(pieceId: Int, offset: Offset) {
        board?.let { b ->
            val piece = b.pieces.find { it.id == pieceId } ?: return
            b.movePiece(pieceId, piece.currentPosition + offset)
            _uiState.value = _uiState.value.copy(
                pieces = b.pieces.toList()
            )
        }
    }

    fun onPieceDragEnd(pieceId: Int) {
        board?.let { b ->
            val snapped = b.trySnapPiece(pieceId)
            _uiState.value = _uiState.value.copy(
                pieces = b.pieces.toList(),
                moves = b.moves,
                draggedPieceId = null,
                isCompleted = b.isCompleted
            )

            if (b.isCompleted) {
                onPuzzleCompleted()
            }
        }
    }

    private fun onPuzzleCompleted() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(showSuccess = true)

        viewModelScope.launch {
            if (historyId > 0) {
                puzzleRepository.getById(historyId)?.let { history ->
                    puzzleRepository.update(
                        history.copy(
                            status = PuzzleStatus.COMPLETED,
                            elapsedTimeMs = _uiState.value.elapsedTimeMs,
                            moves = _uiState.value.moves,
                            dateCompleted = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun toggleHint() {
        _uiState.value = _uiState.value.copy(
            showHint = !_uiState.value.showHint
        )
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(showSuccess = false)
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        // Save progress
        viewModelScope.launch {
            if (historyId > 0 && !_uiState.value.isCompleted) {
                puzzleRepository.getById(historyId)?.let { history ->
                    puzzleRepository.update(
                        history.copy(
                            elapsedTimeMs = _uiState.value.elapsedTimeMs,
                            moves = _uiState.value.moves
                        )
                    )
                }
            }
        }
    }
}

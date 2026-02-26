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
    val draggedPieceId: Int? = null,
    val savedImagePath: String? = null
)

class PuzzleViewModel(
    private val imagePath: String,
    private val pieceCount: Int,
    private val resumedHistoryId: Long,
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
    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f

    fun initializePuzzle(canvasWidth: Float, canvasHeight: Float) {
        if (board != null) return

        this.canvasWidth = canvasWidth
        this.canvasHeight = canvasHeight

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val existingHistory = if (resumedHistoryId > 0) {
                puzzleRepository.getById(resumedHistoryId)
            } else {
                null
            }

            // Save content URI to file, or use path directly
            val savedPath = if (existingHistory != null) {
                existingHistory.imagePath
            } else {
                try {
                    val uri = android.net.Uri.parse(imagePath)
                    if (uri.scheme == "content" || uri.scheme == "file") {
                        imageRepository.saveImage(uri)
                    } else {
                        imagePath
                    }
                } catch (e: Exception) {
                    imagePath
                }
            }

            val bitmap = BitmapFactory.decodeFile(savedPath) ?: return@launch
            val config = PuzzleConfig.forPieceCount(pieceCount)

            // Calculate board dimensions maintaining image aspect ratio
            val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val maxBoardWidth = canvasWidth * 0.9f
            val maxBoardHeight = canvasHeight * 0.7f

            val boardWidth: Float
            val boardHeight: Float

            if (imageAspectRatio > maxBoardWidth / maxBoardHeight) {
                boardWidth = maxBoardWidth
                boardHeight = maxBoardWidth / imageAspectRatio
            } else {
                boardHeight = maxBoardHeight
                boardWidth = maxBoardHeight * imageAspectRatio
            }

            val boardOffsetX = (canvasWidth - boardWidth) / 2
            val boardOffsetY = (canvasHeight - boardHeight) / 2

            val pieces = generator.generatePieces(
                sourceBitmap = bitmap,
                rows = config.rows,
                cols = config.columns,
                boardWidth = boardWidth,
                boardHeight = boardHeight
            )

            // Offset correct positions to canvas coordinates
            pieces.forEach { piece ->
                piece.correctPosition = Offset(
                    piece.correctPosition.x + boardOffsetX,
                    piece.correctPosition.y + boardOffsetY
                )
            }

            board = PuzzleBoard(
                pieces = pieces,
                rows = config.rows,
                cols = config.columns,
                boardWidth = boardWidth,
                boardHeight = boardHeight
            )

            if (existingHistory?.status == PuzzleStatus.COMPLETED) {
                board?.markAsCompleted()
                board?.setMoves(existingHistory.moves)
                historyId = existingHistory.id
                _uiState.value = _uiState.value.copy(
                    pieces = board!!.pieces.toList(),
                    isLoading = false,
                    boardWidth = boardWidth,
                    boardHeight = boardHeight,
                    savedImagePath = savedPath,
                    elapsedTimeMs = existingHistory.elapsedTimeMs,
                    moves = existingHistory.moves,
                    isCompleted = true,
                    showSuccess = false
                )
                return@launch
            }

            board?.shufflePieces(canvasWidth, canvasHeight)

            if (existingHistory != null) {
                historyId = existingHistory.id
                board?.setMoves(existingHistory.moves)
                startTime = System.currentTimeMillis() - existingHistory.elapsedTimeMs
                _uiState.value = _uiState.value.copy(
                    pieces = board!!.pieces.toList(),
                    isLoading = false,
                    boardWidth = boardWidth,
                    boardHeight = boardHeight,
                    savedImagePath = savedPath,
                    elapsedTimeMs = existingHistory.elapsedTimeMs,
                    moves = existingHistory.moves
                )
                startTimer()
                return@launch
            }

            startTime = System.currentTimeMillis()

            _uiState.value = _uiState.value.copy(
                pieces = board!!.pieces.toList(),
                isLoading = false,
                boardWidth = boardWidth,
                boardHeight = boardHeight,
                savedImagePath = savedPath
            )

            // Save to history
            val thumbnailPath = imageRepository.saveThumbnail(savedPath)
            historyId = puzzleRepository.insert(
                PuzzleHistory(
                    imagePath = savedPath,
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
            b.bringToFront(pieceId)
            _uiState.value = _uiState.value.copy(
                pieces = b.pieces.toList(),
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

    fun getConnectedPieceIds(pieceId: Int): Set<Int> {
        return board?.getGroupPieceIds(pieceId) ?: emptySet()
    }

    fun resetPuzzle() {
        val b = board ?: return
        if (canvasWidth <= 0f || canvasHeight <= 0f) return

        b.resetPieces(canvasWidth, canvasHeight)
        startTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            pieces = b.pieces.toList(),
            elapsedTimeMs = 0,
            moves = 0,
            isCompleted = false,
            showSuccess = false,
            draggedPieceId = null
        )
        startTimer()

        viewModelScope.launch {
            if (historyId > 0) {
                puzzleRepository.getById(historyId)?.let { history ->
                    puzzleRepository.update(
                        history.copy(
                            status = PuzzleStatus.IN_PROGRESS,
                            elapsedTimeMs = 0,
                            moves = 0,
                            dateStarted = startTime,
                            dateCompleted = null
                        )
                    )
                }
            }
        }
    }

    fun onPieceDragEnd(pieceId: Int) {
        board?.let { b ->
            b.trySnapPiece(pieceId)
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

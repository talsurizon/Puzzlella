package com.puzzlella.app.data.model

data class PuzzleHistory(
    val id: Long = 0,
    val imagePath: String,
    val thumbnailPath: String,
    val pieceCount: Int,
    val status: PuzzleStatus,
    val elapsedTimeMs: Long,
    val moves: Int,
    val dateStarted: Long,
    val dateCompleted: Long? = null
)

enum class PuzzleStatus {
    IN_PROGRESS,
    COMPLETED
}

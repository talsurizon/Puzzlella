package com.puzzlella.app.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt
import kotlin.random.Random

class PuzzleBoard(
    pieces: List<PuzzlePiece>,
    val rows: Int,
    val cols: Int,
    val boardWidth: Float,
    val boardHeight: Float
) {
    private val _pieces = pieces.toMutableList()
    val pieces: List<PuzzlePiece> get() = _pieces

    var moves: Int = 0
        private set

    var isCompleted: Boolean = false
        private set

    private val snapThreshold: Float = minOf(boardWidth / cols, boardHeight / rows) * 0.35f

    fun shufflePieces(areaWidth: Float, areaHeight: Float) {
        val pieceWidth = boardWidth / cols
        val pieceHeight = boardHeight / rows
        val margin = 20f

        _pieces.forEach { piece ->
            if (!piece.isLocked) {
                piece.currentPosition = Offset(
                    x = Random.nextFloat() * (areaWidth - pieceWidth - margin * 2) + margin,
                    y = Random.nextFloat() * (areaHeight - pieceHeight - margin * 2) + margin
                )
            }
        }
    }

    fun movePiece(pieceId: Int, newPosition: Offset) {
        val piece = _pieces.find { it.id == pieceId } ?: return
        if (piece.isLocked) return
        piece.currentPosition = newPosition
    }

    fun trySnapPiece(pieceId: Int): Boolean {
        val piece = _pieces.find { it.id == pieceId } ?: return false
        if (piece.isLocked) return false
        moves++

        if (piece.isAtCorrectPosition(snapThreshold)) {
            piece.currentPosition = piece.correctPosition
            piece.isLocked = true
            checkCompletion()
            return true
        }
        return false
    }

    fun checkCompletion(): Boolean {
        isCompleted = _pieces.all { it.isAtCorrectPosition(1f) }
        return isCompleted
    }

    fun getPieceAt(position: Offset): PuzzlePiece? {
        return _pieces.lastOrNull { piece ->
            val dx = position.x - piece.currentPosition.x
            val dy = position.y - piece.currentPosition.y
            dx in 0f..piece.width && dy in 0f..piece.height
        }
    }

    fun bringToFront(pieceId: Int) {
        val piece = _pieces.find { it.id == pieceId } ?: return
        _pieces.remove(piece)
        _pieces.add(piece)
    }
}

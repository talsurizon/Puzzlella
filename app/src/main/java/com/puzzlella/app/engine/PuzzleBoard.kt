package com.puzzlella.app.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt
import kotlin.random.Random

class PuzzleBoard(
    val pieces: List<PuzzlePiece>,
    val rows: Int,
    val cols: Int,
    val boardWidth: Float,
    val boardHeight: Float
) {
    var moves: Int = 0
        private set

    var isCompleted: Boolean = false
        private set

    private val snapThreshold: Float = minOf(boardWidth / cols, boardHeight / rows) * 0.35f

    fun shufflePieces(areaWidth: Float, areaHeight: Float) {
        val pieceWidth = boardWidth / cols
        val pieceHeight = boardHeight / rows
        val margin = 20f

        pieces.forEach { piece ->
            piece.currentPosition = Offset(
                x = Random.nextFloat() * (areaWidth - pieceWidth - margin * 2) + margin,
                y = Random.nextFloat() * (areaHeight - pieceHeight - margin * 2) + margin
            )
        }
    }

    fun movePiece(pieceId: Int, newPosition: Offset) {
        val piece = pieces.find { it.id == pieceId } ?: return
        piece.currentPosition = newPosition
    }

    fun trySnapPiece(pieceId: Int): Boolean {
        val piece = pieces.find { it.id == pieceId } ?: return false
        moves++

        if (piece.isAtCorrectPosition(snapThreshold)) {
            piece.currentPosition = piece.correctPosition
            checkCompletion()
            return true
        }
        return false
    }

    fun checkCompletion(): Boolean {
        isCompleted = pieces.all { it.isAtCorrectPosition(1f) }
        return isCompleted
    }

    fun getPieceAt(position: Offset): PuzzlePiece? {
        // Return topmost piece (last in list) that contains the touch point
        return pieces.lastOrNull { piece ->
            val dx = position.x - piece.currentPosition.x
            val dy = position.y - piece.currentPosition.y
            dx in 0f..piece.width && dy in 0f..piece.height
        }
    }

    fun bringToFront(pieceId: Int): List<PuzzlePiece> {
        val piece = pieces.find { it.id == pieceId } ?: return pieces
        val mutablePieces = pieces.toMutableList()
        mutablePieces.remove(piece)
        mutablePieces.add(piece)
        return mutablePieces
    }
}

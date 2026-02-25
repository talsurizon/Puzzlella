package com.puzzlella.app.data.model

data class PuzzleConfig(
    val pieceCount: Int,
    val columns: Int,
    val rows: Int
) {
    companion object {
        val SIZES = listOf(
            PuzzleConfig(12, 4, 3),
            PuzzleConfig(24, 6, 4),
            PuzzleConfig(36, 6, 6),
            PuzzleConfig(48, 8, 6),
            PuzzleConfig(60, 10, 6),
            PuzzleConfig(80, 10, 8),
            PuzzleConfig(100, 10, 10)
        )

        fun forPieceCount(count: Int): PuzzleConfig =
            SIZES.first { it.pieceCount == count }
    }
}

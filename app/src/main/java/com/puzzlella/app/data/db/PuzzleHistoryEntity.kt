package com.puzzlella.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puzzle_history")
data class PuzzleHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val thumbnailPath: String,
    val pieceCount: Int,
    val status: String,
    val elapsedTimeMs: Long,
    val moves: Int,
    val dateStarted: Long,
    val dateCompleted: Long? = null
)

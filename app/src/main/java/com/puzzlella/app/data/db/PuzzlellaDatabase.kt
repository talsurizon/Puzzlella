package com.puzzlella.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PuzzleHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PuzzlellaDatabase : RoomDatabase() {
    abstract fun puzzleHistoryDao(): PuzzleHistoryDao
}

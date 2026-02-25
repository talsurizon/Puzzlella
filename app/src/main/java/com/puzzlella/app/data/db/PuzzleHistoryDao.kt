package com.puzzlella.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleHistoryDao {
    @Query("SELECT * FROM puzzle_history ORDER BY dateStarted DESC")
    fun getAll(): Flow<List<PuzzleHistoryEntity>>

    @Query("SELECT * FROM puzzle_history WHERE id = :id")
    suspend fun getById(id: Long): PuzzleHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PuzzleHistoryEntity): Long

    @Update
    suspend fun update(entity: PuzzleHistoryEntity)

    @Delete
    suspend fun delete(entity: PuzzleHistoryEntity)

    @Query("DELETE FROM puzzle_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}

package com.puzzlella.app.data.repository

import com.puzzlella.app.data.db.PuzzleHistoryDao
import com.puzzlella.app.data.db.PuzzleHistoryEntity
import com.puzzlella.app.data.model.PuzzleHistory
import com.puzzlella.app.data.model.PuzzleStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PuzzleRepository(private val dao: PuzzleHistoryDao) {

    fun getAllHistory(): Flow<List<PuzzleHistory>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getById(id: Long): PuzzleHistory? =
        dao.getById(id)?.toDomain()

    suspend fun insert(history: PuzzleHistory): Long =
        dao.insert(history.toEntity())

    suspend fun update(history: PuzzleHistory) =
        dao.update(history.toEntity())

    suspend fun delete(id: Long) =
        dao.deleteById(id)

    private fun PuzzleHistoryEntity.toDomain() = PuzzleHistory(
        id = id,
        imagePath = imagePath,
        thumbnailPath = thumbnailPath,
        pieceCount = pieceCount,
        status = PuzzleStatus.valueOf(status),
        elapsedTimeMs = elapsedTimeMs,
        moves = moves,
        dateStarted = dateStarted,
        dateCompleted = dateCompleted
    )

    private fun PuzzleHistory.toEntity() = PuzzleHistoryEntity(
        id = id,
        imagePath = imagePath,
        thumbnailPath = thumbnailPath,
        pieceCount = pieceCount,
        status = status.name,
        elapsedTimeMs = elapsedTimeMs,
        moves = moves,
        dateStarted = dateStarted,
        dateCompleted = dateCompleted
    )
}

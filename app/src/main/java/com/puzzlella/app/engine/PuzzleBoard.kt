package com.puzzlella.app.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
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
    private val groupParents = mutableMapOf<Int, Int>()

    var moves: Int = 0
        private set

    var isCompleted: Boolean = false
        private set

    private val snapThreshold: Float = minOf(boardWidth / cols, boardHeight / rows) * 0.35f

    init {
        _pieces.forEach { piece ->
            groupParents[piece.id] = piece.id
        }
    }

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
        val delta = Offset(
            x = newPosition.x - piece.currentPosition.x,
            y = newPosition.y - piece.currentPosition.y
        )
        moveGroupBy(pieceId, delta)
    }

    fun trySnapPiece(pieceId: Int): Boolean {
        val piece = _pieces.find { it.id == pieceId } ?: return false
        if (piece.isLocked) return false
        moves++

        val snappedToNeighbors = snapGroupToMatchingNeighbors(pieceId)
        val snappedToBoard = snapGroupToCorrectPosition(pieceId)
        checkCompletion()
        return snappedToNeighbors || snappedToBoard
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
        val groupIds = getGroupMembers(pieceId).map { it.id }.toSet()
        if (groupIds.isEmpty()) return
        val groupPieces = _pieces.filter { it.id in groupIds }
        _pieces.removeAll { it.id in groupIds }
        _pieces.addAll(groupPieces)
    }

    fun getGroupPieceIds(pieceId: Int): Set<Int> {
        return getGroupMembers(pieceId).map { it.id }.toSet()
    }

    fun setMoves(value: Int) {
        moves = value
    }

    fun markAsCompleted() {
        _pieces.forEach { piece ->
            piece.currentPosition = piece.correctPosition
            piece.isLocked = true
            groupParents[piece.id] = piece.id
        }
        isCompleted = true
    }

    fun resetPieces(areaWidth: Float, areaHeight: Float) {
        _pieces.forEach { piece ->
            piece.isLocked = false
            piece.currentPosition = piece.correctPosition
            groupParents[piece.id] = piece.id
        }
        moves = 0
        isCompleted = false
        shufflePieces(areaWidth, areaHeight)
    }

    private fun findRoot(pieceId: Int): Int {
        val parent = groupParents[pieceId] ?: return pieceId
        if (parent == pieceId) return pieceId
        val root = findRoot(parent)
        groupParents[pieceId] = root
        return root
    }

    private fun unionGroups(firstPieceId: Int, secondPieceId: Int) {
        val firstRoot = findRoot(firstPieceId)
        val secondRoot = findRoot(secondPieceId)
        if (firstRoot != secondRoot) {
            groupParents[secondRoot] = firstRoot
        }
    }

    private fun getGroupMembers(pieceId: Int): List<PuzzlePiece> {
        val root = findRoot(pieceId)
        return _pieces.filter { findRoot(it.id) == root }
    }

    private fun moveGroupBy(pieceId: Int, delta: Offset) {
        val groupMembers = getGroupMembers(pieceId)
        if (groupMembers.any { it.isLocked }) return
        groupMembers.forEach { member ->
            member.currentPosition = Offset(
                x = member.currentPosition.x + delta.x,
                y = member.currentPosition.y + delta.y
            )
        }
    }

    private fun snapGroupToMatchingNeighbors(pieceId: Int): Boolean {
        var snappedAny = false
        var snappedInPass: Boolean

        do {
            snappedInPass = false
            val groupMembers = getGroupMembers(pieceId)
            if (groupMembers.any { it.isLocked }) break
            val groupIds = groupMembers.map { it.id }.toSet()

            outer@ for (member in groupMembers) {
                for (neighbor in _pieces) {
                    if (neighbor.id in groupIds) continue
                    if (!areAdjacentPieces(member, neighbor)) continue

                    val targetMemberPosition = Offset(
                        x = neighbor.currentPosition.x - (neighbor.correctPosition.x - member.correctPosition.x),
                        y = neighbor.currentPosition.y - (neighbor.correctPosition.y - member.correctPosition.y)
                    )

                    if (isWithinThreshold(member.currentPosition, targetMemberPosition, snapThreshold)) {
                        val delta = Offset(
                            x = targetMemberPosition.x - member.currentPosition.x,
                            y = targetMemberPosition.y - member.currentPosition.y
                        )
                        moveGroupBy(pieceId, delta)
                        unionGroups(member.id, neighbor.id)
                        snappedAny = true
                        snappedInPass = true
                        break@outer
                    }
                }
            }
        } while (snappedInPass)

        return snappedAny
    }

    private fun snapGroupToCorrectPosition(pieceId: Int): Boolean {
        val groupMembers = getGroupMembers(pieceId)
        val anchor = groupMembers.firstOrNull { it.isAtCorrectPosition(snapThreshold) } ?: return false
        val delta = Offset(
            x = anchor.correctPosition.x - anchor.currentPosition.x,
            y = anchor.correctPosition.y - anchor.currentPosition.y
        )
        moveGroupBy(pieceId, delta)
        lockGroup(pieceId)
        return true
    }

    private fun lockGroup(pieceId: Int) {
        getGroupMembers(pieceId).forEach { member ->
            member.currentPosition = member.correctPosition
            member.isLocked = true
        }
    }

    private fun areAdjacentPieces(first: PuzzlePiece, second: PuzzlePiece): Boolean {
        val rowDiff = abs(first.row - second.row)
        val colDiff = abs(first.col - second.col)
        return rowDiff + colDiff == 1
    }

    private fun isWithinThreshold(first: Offset, second: Offset, threshold: Float): Boolean {
        val dx = first.x - second.x
        val dy = first.y - second.y
        return dx * dx + dy * dy <= threshold * threshold
    }
}

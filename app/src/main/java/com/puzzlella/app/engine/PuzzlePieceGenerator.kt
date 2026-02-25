package com.puzzlella.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset

data class PuzzlePiece(
    val id: Int,
    val row: Int,
    val col: Int,
    val bitmap: Bitmap,
    val correctPosition: Offset,
    var currentPosition: Offset,
    val width: Float,
    val height: Float
) {
    fun isAtCorrectPosition(threshold: Float = 30f): Boolean {
        val dx = currentPosition.x - correctPosition.x
        val dy = currentPosition.y - correctPosition.y
        return dx * dx + dy * dy < threshold * threshold
    }
}

enum class ConnectorType { TAB, BLANK, FLAT }

data class PieceEdges(
    val top: ConnectorType,
    val right: ConnectorType,
    val bottom: ConnectorType,
    val left: ConnectorType
)

class PuzzlePieceGenerator {

    fun generatePieces(
        sourceBitmap: Bitmap,
        rows: Int,
        cols: Int,
        boardWidth: Float,
        boardHeight: Float
    ): List<PuzzlePiece> {
        val pieceWidth = boardWidth / cols
        val pieceHeight = boardHeight / rows
        val tabSize = minOf(pieceWidth, pieceHeight) * 0.18f

        val edges = generateEdgeMap(rows, cols)
        val pieces = mutableListOf<PuzzlePiece>()

        val srcPieceW = sourceBitmap.width.toFloat() / cols
        val srcPieceH = sourceBitmap.height.toFloat() / rows

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val pieceEdges = edges[row][col]
                val path = createPiecePath(
                    pieceWidth, pieceHeight, tabSize, pieceEdges
                )

                val pieceBitmap = clipBitmapToPiece(
                    sourceBitmap, path, row, col,
                    srcPieceW, srcPieceH, pieceWidth, pieceHeight, tabSize
                )

                val correctX = col * pieceWidth
                val correctY = row * pieceHeight

                pieces.add(
                    PuzzlePiece(
                        id = row * cols + col,
                        row = row,
                        col = col,
                        bitmap = pieceBitmap,
                        correctPosition = Offset(correctX, correctY),
                        currentPosition = Offset(correctX, correctY),
                        width = pieceWidth + tabSize * 2,
                        height = pieceHeight + tabSize * 2
                    )
                )
            }
        }
        return pieces
    }

    private fun generateEdgeMap(rows: Int, cols: Int): Array<Array<PieceEdges>> {
        // Horizontal edges (between columns): true = tab on right side
        val hEdges = Array(rows) { BooleanArray(cols - 1) { Math.random() > 0.5 } }
        // Vertical edges (between rows): true = tab on bottom side
        val vEdges = Array(rows - 1) { BooleanArray(cols) { Math.random() > 0.5 } }

        return Array(rows) { row ->
            Array(cols) { col ->
                PieceEdges(
                    top = when {
                        row == 0 -> ConnectorType.FLAT
                        vEdges[row - 1][col] -> ConnectorType.BLANK
                        else -> ConnectorType.TAB
                    },
                    right = when {
                        col == cols - 1 -> ConnectorType.FLAT
                        hEdges[row][col] -> ConnectorType.TAB
                        else -> ConnectorType.BLANK
                    },
                    bottom = when {
                        row == rows - 1 -> ConnectorType.FLAT
                        vEdges[row][col] -> ConnectorType.TAB
                        else -> ConnectorType.BLANK
                    },
                    left = when {
                        col == 0 -> ConnectorType.FLAT
                        hEdges[row][col - 1] -> ConnectorType.BLANK
                        else -> ConnectorType.TAB
                    }
                )
            }
        }
    }

    fun createPiecePath(
        width: Float,
        height: Float,
        tabSize: Float,
        edges: PieceEdges
    ): Path {
        val path = Path()
        val ox = tabSize  // offset to leave room for tabs
        val oy = tabSize

        path.moveTo(ox, oy)

        // Top edge
        drawEdge(path, ox, oy, ox + width, oy, edges.top, tabSize, isHorizontal = true)

        // Right edge
        drawEdge(path, ox + width, oy, ox + width, oy + height, edges.right, tabSize, isHorizontal = false)

        // Bottom edge (reversed)
        drawEdge(path, ox + width, oy + height, ox, oy + height, edges.bottom, tabSize, isHorizontal = true, reversed = true)

        // Left edge (reversed)
        drawEdge(path, ox, oy + height, ox, oy, edges.left, tabSize, isHorizontal = false, reversed = true)

        path.close()
        return path
    }

    private fun drawEdge(
        path: Path,
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        type: ConnectorType,
        tabSize: Float,
        isHorizontal: Boolean,
        reversed: Boolean = false
    ) {
        if (type == ConnectorType.FLAT) {
            path.lineTo(endX, endY)
            return
        }

        val sign = if (type == ConnectorType.TAB) -1f else 1f
        val actualSign = if (reversed) -sign else sign

        if (isHorizontal) {
            val midX = (startX + endX) / 2
            val len = endX - startX
            val third = len / 3

            path.lineTo(startX + third, startY)

            // Bezier curve for tab/blank
            path.cubicTo(
                startX + third, startY + actualSign * tabSize * 0.2f,
                midX - tabSize * 0.5f, startY + actualSign * tabSize,
                midX, startY + actualSign * tabSize
            )
            path.cubicTo(
                midX + tabSize * 0.5f, startY + actualSign * tabSize,
                startX + 2 * third, startY + actualSign * tabSize * 0.2f,
                startX + 2 * third, startY
            )

            path.lineTo(endX, endY)
        } else {
            val midY = (startY + endY) / 2
            val len = endY - startY
            val third = len / 3

            path.lineTo(startX, startY + third)

            path.cubicTo(
                startX + actualSign * tabSize * 0.2f, startY + third,
                startX + actualSign * tabSize, midY - tabSize * 0.5f,
                startX + actualSign * tabSize, midY
            )
            path.cubicTo(
                startX + actualSign * tabSize, midY + tabSize * 0.5f,
                startX + actualSign * tabSize * 0.2f, startY + 2 * third,
                startX, startY + 2 * third
            )

            path.lineTo(endX, endY)
        }
    }

    private fun clipBitmapToPiece(
        source: Bitmap,
        piecePath: Path,
        row: Int, col: Int,
        srcPieceW: Float, srcPieceH: Float,
        dstPieceW: Float, dstPieceH: Float,
        tabSize: Float
    ): Bitmap {
        val bmpW = (dstPieceW + tabSize * 2).toInt()
        val bmpH = (dstPieceH + tabSize * 2).toInt()
        val result = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw the path as mask
        canvas.drawPath(piecePath, paint)

        // Apply source bitmap with SRC_IN to clip
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcLeft = col * srcPieceW - (tabSize * srcPieceW / dstPieceW)
        val srcTop = row * srcPieceH - (tabSize * srcPieceH / dstPieceH)
        val srcRight = srcLeft + bmpW * (srcPieceW / dstPieceW) * (bmpW.toFloat() / dstPieceW) * (dstPieceW / bmpW.toFloat())
        val srcBottom = srcTop + bmpH * (srcPieceH / dstPieceH) * (bmpH.toFloat() / dstPieceH) * (dstPieceH / bmpH.toFloat())

        val srcRect = android.graphics.Rect(
            maxOf(0, srcLeft.toInt()),
            maxOf(0, srcTop.toInt()),
            minOf(source.width, (srcLeft + bmpW * srcPieceW / dstPieceW).toInt()),
            minOf(source.height, (srcTop + bmpH * srcPieceH / dstPieceH).toInt())
        )
        val dstRect = android.graphics.Rect(0, 0, bmpW, bmpH)

        canvas.drawBitmap(source, srcRect, dstRect, paint)

        // Draw outline
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = android.graphics.Color.argb(80, 0, 0, 0)
            xfermode = null
        }
        canvas.drawPath(piecePath, outlinePaint)

        return result
    }
}

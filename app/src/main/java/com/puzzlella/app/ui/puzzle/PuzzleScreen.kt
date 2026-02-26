package com.puzzlella.app.ui.puzzle

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.puzzlella.app.R
import com.puzzlella.app.ui.components.SuccessDialog
import com.puzzlella.app.ui.theme.YellowAccent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    imagePath: String,
    pieceCount: Int,
    windowSizeClass: WindowSizeClass,
    onBack: () -> Unit,
    onNewPuzzle: () -> Unit,
    viewModel: PuzzleViewModel = koinViewModel { parametersOf(imagePath, pieceCount) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            viewModel.initializePuzzle(
                canvasSize.width.toFloat(),
                canvasSize.height.toFloat()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Timer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = stringResource(R.string.timer_label),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = viewModel.formatTime(uiState.elapsedTimeMs),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // Moves
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.moves_label)}: ${uiState.moves}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconToggleButton(
                        checked = uiState.showHint,
                        onCheckedChange = { viewModel.toggleHint() }
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = stringResource(R.string.hint_label),
                            tint = if (uiState.showHint) YellowAccent
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isTablet) {
                // Tablet: side-by-side layout
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Reference image on the side
                    Box(
                        modifier = Modifier
                            .weight(0.3f)
                            .padding(16.dp)
                    ) {
                        val bitmap = remember(uiState.savedImagePath) {
                            uiState.savedImagePath?.let { BitmapFactory.decodeFile(it) }
                        }
                        bitmap?.let {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                drawImage(
                                    image = it.asImageBitmap(),
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                                )
                            }
                        }
                    }

                    // Puzzle board
                    PuzzleBoardCanvas(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(0.7f)
                            .onSizeChanged { canvasSize = it }
                    )
                }
            } else {
                // Phone: full screen puzzle
                Box(modifier = Modifier.fillMaxSize()) {
                    PuzzleBoardCanvas(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { canvasSize = it }
                    )

                    // Hint overlay
                    AnimatedVisibility(
                        visible = uiState.showHint,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val bitmap = remember(uiState.savedImagePath) {
                            uiState.savedImagePath?.let { BitmapFactory.decodeFile(it) }
                        }
                        bitmap?.let {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.3f)
                            ) {
                                val bw = uiState.boardWidth
                                val bh = uiState.boardHeight
                                val offsetX = (size.width - bw) / 2
                                val offsetY = (size.height - bh) / 2
                                translate(offsetX, offsetY) {
                                    drawImage(
                                        image = it.asImageBitmap(),
                                        dstSize = IntSize(bw.toInt(), bh.toInt())
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Success dialog
            if (uiState.showSuccess) {
                SuccessDialog(
                    time = viewModel.formatTime(uiState.elapsedTimeMs),
                    moves = uiState.moves,
                    onNewPuzzle = {
                        viewModel.dismissSuccess()
                        onNewPuzzle()
                    },
                    onDismiss = { viewModel.dismissSuccess() }
                )
            }
        }
    }
}

@Composable
private fun PuzzleBoardCanvas(
    uiState: PuzzleUiState,
    viewModel: PuzzleViewModel,
    modifier: Modifier = Modifier
) {
    // Cache ImageBitmaps — avoids re-wrapping Android Bitmap on every draw frame
    val imageBitmaps = remember(uiState.pieces.size) {
        uiState.pieces.associate { it.id to it.bitmap.asImageBitmap() }
    }

    // Local drag state keeps movement smooth without per-frame ViewModel updates.
    var activeDragId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var draggedGroupIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val currentPieces = viewModel.uiState.value.pieces
                        val piece = currentPieces.lastOrNull { p ->
                            !p.isLocked &&
                            (offset.x - p.currentPosition.x) in 0f..p.width &&
                            (offset.y - p.currentPosition.y) in 0f..p.height
                        }
                        piece?.let {
                            activeDragId = it.id
                            dragOffset = Offset.Zero
                            viewModel.onPieceDragStart(it.id)
                            draggedGroupIds = viewModel.getConnectedPieceIds(it.id)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (activeDragId != null) {
                            dragOffset += Offset(dragAmount.x, dragAmount.y)
                        }
                    },
                    onDragEnd = {
                        activeDragId?.let { id ->
                            if (dragOffset != Offset.Zero) {
                                viewModel.onPieceDrag(id, dragOffset)
                            }
                            viewModel.onPieceDragEnd(id)
                        }
                        activeDragId = null
                        dragOffset = Offset.Zero
                        draggedGroupIds = emptySet()
                    },
                    onDragCancel = {
                        activeDragId?.let { id ->
                            if (dragOffset != Offset.Zero) {
                                viewModel.onPieceDrag(id, dragOffset)
                            }
                            viewModel.onPieceDragEnd(id)
                        }
                        activeDragId = null
                        dragOffset = Offset.Zero
                        draggedGroupIds = emptySet()
                    }
                )
            }
    ) {
        // Draw board background
        val boardW = uiState.boardWidth
        val boardH = uiState.boardHeight
        if (boardW > 0 && boardH > 0) {
            val offsetX = (size.width - boardW) / 2
            val offsetY = (size.height - boardH) / 2

            drawRect(
                color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.2f),
                topLeft = Offset(offsetX, offsetY),
                size = androidx.compose.ui.geometry.Size(boardW, boardH)
            )
        }

        // Draw pieces
        uiState.pieces.forEach { piece ->
            val bitmap = imageBitmaps[piece.id] ?: return@forEach
            val pos = if (piece.id in draggedGroupIds) {
                piece.currentPosition + dragOffset
            } else {
                piece.currentPosition
            }
            translate(pos.x, pos.y) {
                drawImage(
                    image = bitmap,
                    dstSize = IntSize(piece.width.toInt(), piece.height.toInt())
                )
            }
        }
    }
}

package com.puzzlella.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzlella.app.R
import com.puzzlella.app.ui.theme.*

@Composable
fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    onImageSelected: (Uri) -> Unit,
    onHistoryClick: () -> Unit
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // For camera, we'd need to save to file and get URI
        // This is simplified — in production, use FileProvider
    }

    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            PrimaryContainerLight.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            // History button top-left (RTL: top-right)
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = stringResource(R.string.history_title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isTablet) 80.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "🧩",
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Source buttons
                if (isTablet) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SourceCard(
                            title = stringResource(R.string.source_gallery),
                            icon = Icons.Default.PhotoLibrary,
                            color = SecondaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = { galleryLauncher.launch("image/*") }
                        )
                        SourceCard(
                            title = stringResource(R.string.source_camera),
                            icon = Icons.Default.CameraAlt,
                            color = TertiaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = { /* Camera launch */ }
                        )
                        SourceCard(
                            title = stringResource(R.string.source_samples),
                            icon = Icons.Default.Image,
                            color = PrimaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = { /* Navigate to samples */ }
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        SourceCard(
                            title = stringResource(R.string.source_gallery),
                            icon = Icons.Default.PhotoLibrary,
                            color = SecondaryLight,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { galleryLauncher.launch("image/*") }
                        )
                        SourceCard(
                            title = stringResource(R.string.source_camera),
                            icon = Icons.Default.CameraAlt,
                            color = TertiaryLight,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { /* Camera launch */ }
                        )
                        SourceCard(
                            title = stringResource(R.string.source_samples),
                            icon = Icons.Default.Image,
                            color = PrimaryLight,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { /* Navigate to samples */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(50),
                ambientColor = Color.Gray.copy(alpha = 0.2f),
                spotColor = Color.Gray.copy(alpha = 0.3f)
            )
            .animateContentSize(),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
    }
}

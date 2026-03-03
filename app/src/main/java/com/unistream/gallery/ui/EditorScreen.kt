package com.unistream.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.unistream.gallery.viewmodel.GalleryViewModel

enum class EditorTool {
    CROP, ROTATE, BRIGHTNESS, CONTRAST, SATURATION, WARMTH, HIGHLIGHTS,
    SHADOWS, SHARPNESS, BLUR, FILTERS, TEXT, STICKERS, DRAWING, MOSAIC
}

@Composable
fun EditorScreen(
    mediaId: Long,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val media = uiState.allMedia.find { it.id == mediaId }
    var selectedTool by remember { mutableStateOf<EditorTool?>(null) }
    var sliderValue by remember { mutableStateOf(0.5f) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                "Edit",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            )
            TextButton(onClick = {}) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Image Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (media != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Editing",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Adjustment Slider (shown when tool selected)
        if (selectedTool in listOf(
                EditorTool.BRIGHTNESS, EditorTool.CONTRAST, EditorTool.SATURATION,
                EditorTool.WARMTH, EditorTool.HIGHLIGHTS, EditorTool.SHADOWS,
                EditorTool.SHARPNESS, EditorTool.BLUR
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = selectedTool?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.7f))
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Tools Tabs: Basic | Adjustments | Advanced
        var tabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf("Basic", "Adjustments", "Advanced")

        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        // Tool buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color(0xFF1A1A1A))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val tools = when (tabIndex) {
                0 -> listOf(
                    EditorTool.CROP to "Crop" to Icons.Default.Crop,
                    EditorTool.ROTATE to "Rotate" to Icons.Default.RotateRight,
                )
                1 -> listOf(
                    EditorTool.BRIGHTNESS to "Bright" to Icons.Default.BrightnessHigh,
                    EditorTool.CONTRAST to "Contrast" to Icons.Default.Contrast,
                    EditorTool.SATURATION to "Saturation" to Icons.Default.InvertColors,
                    EditorTool.WARMTH to "Warmth" to Icons.Default.WbSunny,
                    EditorTool.HIGHLIGHTS to "Highlights" to Icons.Default.LightMode,
                    EditorTool.SHADOWS to "Shadows" to Icons.Default.DarkMode,
                    EditorTool.SHARPNESS to "Sharpen" to Icons.Default.AutoFixHigh,
                    EditorTool.BLUR to "Blur" to Icons.Default.BlurOn
                )
                else -> listOf(
                    EditorTool.FILTERS to "Filters" to Icons.Default.FilterVintage,
                    EditorTool.TEXT to "Text" to Icons.Default.TextFields,
                    EditorTool.STICKERS to "Sticker" to Icons.Default.EmojiEmotions,
                    EditorTool.DRAWING to "Draw" to Icons.Default.Brush,
                    EditorTool.MOSAIC to "Mosaic" to Icons.Default.GridView
                )
            }

            tools.forEach { (toolPair, icon) ->
                val (tool, label) = toolPair
                EditorToolButton(
                    icon = icon,
                    label = label,
                    isSelected = selectedTool == tool,
                    onClick = {
                        selectedTool = if (selectedTool == tool) null else tool
                        sliderValue = 0.5f
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else Color.Transparent
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
            )
        )
    }
}

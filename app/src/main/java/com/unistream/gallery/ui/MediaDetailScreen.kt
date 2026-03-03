package com.unistream.gallery.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.unistream.gallery.data.MediaItem
import com.unistream.gallery.viewmodel.GalleryViewModel

@Composable
fun MediaDetailScreen(
    mediaId: Long,
    onNavigateToEditor: () -> Unit,
    onNavigateToWallpaper: () -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val media = uiState.allMedia.find { it.id == mediaId }
    var showControls by remember { mutableStateOf(true) }
    val isFavorite = uiState.favorites.contains(mediaId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (media != null) {
            // Zoomable Image
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = media.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showControls = !showControls })
                    },
                contentScale = ContentScale.Fit
            )

            // Top controls
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = media.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Row {
                            IconButton(onClick = { viewModel.toggleFavorite(mediaId) }) {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color(0xFFE91E8C) else Color.White
                                )
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Bottom controls
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit button
                        MediaActionButton(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            onClick = onNavigateToEditor
                        )

                        // Wallpaper button
                        MediaActionButton(
                            icon = Icons.Default.Wallpaper,
                            label = "Wallpaper",
                            onClick = onNavigateToWallpaper
                        )

                        // Hide in vault
                        MediaActionButton(
                            icon = Icons.Default.Lock,
                            label = "Hide",
                            onClick = {
                                viewModel.toggleSelection(mediaId)
                                viewModel.hideSelectedMedia()
                                onBack()
                            }
                        )

                        // Share
                        MediaActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = {}
                        )

                        // Delete
                        MediaActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            onClick = {},
                            tint = Color(0xFFEF5350)
                        )
                    }
                }
            }
        } else {
            Text(
                "Media not found",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun MediaActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = tint)
        )
    }
}


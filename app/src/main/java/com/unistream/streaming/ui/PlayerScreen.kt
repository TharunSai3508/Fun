package com.unistream.streaming.ui

import android.app.Activity
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.unistream.streaming.viewmodel.StreamingViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    sourceType: String,
    sourceId: String,
    onBack: () -> Unit,
    viewModel: StreamingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    if (sourceId.isBlank()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(sourceId))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Update position
    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(500)
        }
    }

    // Save progress when leaving
    DisposableEffect(player) {
        onDispose {
            val uri = sourceId
            val progress = player.currentPosition
            if (progress > 5000) {
                viewModel.updateProgress(uri, progress)
            }
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) {
                            player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                        } else {
                            player.seekTo(player.currentPosition + 10_000)
                        }
                    }
                )
            }
    ) {
        // ExoPlayer Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false  // Custom controls
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar
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
                        Row {
                            IconButton(onClick = { showSpeedSheet = true }) {
                                Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Cast, contentDescription = "Cast", tint = Color.White)
                            }
                        }
                    }
                }

                // Center controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "-10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (player.isPlaying) player.pause() else player.play()
                        },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    IconButton(
                        onClick = { player.seekTo(player.currentPosition + 10_000) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward10,
                            contentDescription = "+10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    // Time display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition), color = Color.White, style = MaterialTheme.typography.labelMedium)
                        if (playbackSpeed != 1f) {
                            Text("${playbackSpeed}x", color = Color(0xFFE50914), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(formatTime(duration), color = Color.White.copy(0.6f), style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Seek bar
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = { fraction ->
                            player.seekTo((fraction * duration).toLong())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE50914),
                            activeTrackColor = Color(0xFFE50914),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }

    // Speed selection sheet
    if (showSpeedSheet) {
        SpeedSelectionSheet(
            currentSpeed = playbackSpeed,
            onSpeedSelected = { speed ->
                playbackSpeed = speed
                player.setPlaybackSpeed(speed)
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelectionSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Playback Speed",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                ListItem(
                    headlineContent = {
                        Text(
                            if (speed == 1.0f) "Normal" else "${speed}x",
                            color = if (currentSpeed == speed) Color(0xFFE50914) else Color.White
                        )
                    },
                    trailingContent = {
                        if (currentSpeed == speed) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFFE50914))
                        }
                    },
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onSpeedSelected(speed) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000) % 60
    val m = (ms / 1000 / 60) % 60
    val h = ms / 1000 / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

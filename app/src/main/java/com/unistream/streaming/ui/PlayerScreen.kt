package com.unistream.streaming.ui

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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.unistream.streaming.viewmodel.StreamingViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    sourceType: String,
    sourceId: String,               // Already decoded in AppNavigation
    onBack: () -> Unit,
    viewModel: StreamingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }

    // Build the correct URI depending on source type
    val mediaUri = remember(sourceId) {
        when {
            sourceId.startsWith("content://") ||
            sourceId.startsWith("file://") -> Uri.parse(sourceId)
            sourceId.startsWith("http://") ||
            sourceId.startsWith("https://") -> Uri.parse(sourceId)
            else -> Uri.parse(sourceId)
        }
    }

    val player = remember(mediaUri) {
        ExoPlayer.Builder(context)
            .build()
            .also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        playerError = "Playback error: ${error.errorCodeName}. " +
                            "Check that the file exists and the URL is accessible."
                        isBuffering = false
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> isBuffering = true
                            Player.STATE_READY -> { isBuffering = false; playerError = null }
                            Player.STATE_ENDED -> isBuffering = false
                            Player.STATE_IDLE -> isBuffering = false
                        }
                    }
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
                exo.setMediaItem(MediaItem.fromUri(mediaUri))
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    // Auto-hide controls after 4 s of inactivity
    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Polling for seek-bar position (500 ms)
    LaunchedEffect(player) {
        while (true) {
            if (player.playbackState == Player.STATE_READY) {
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Release player and save progress on exit
    DisposableEffect(Unit) {
        onDispose {
            val progress = player.currentPosition
            if (progress > 5_000L) viewModel.updateProgress(sourceId, progress)
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
                        val seekAmount = 10_000L
                        if (offset.x < size.width / 2) {
                            player.seekTo((player.currentPosition - seekAmount).coerceAtLeast(0))
                        } else {
                            player.seekTo(player.currentPosition + seekAmount)
                        }
                        showControls = true
                    }
                )
            }
    ) {
        // ── Video surface ─────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Buffering indicator ───────────────────────────────────────────
        if (isBuffering && playerError == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFE50914),
                strokeWidth = 3.dp
            )
        }

        // ── Error state ───────────────────────────────────────────────────
        if (playerError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    playerError ?: "Playback failed",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = {
                        playerError = null
                        player.prepare()
                        player.playWhenReady = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
                TextButton(onClick = onBack) {
                    Text("Go Back", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        // ── Controls overlay (visible by tap) ─────────────────────────────
        AnimatedVisibility(
            visible = showControls && playerError == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Top gradient + bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent),
                                endY = 160f
                            )
                        )
                        .padding(start = 4.dp, end = 4.dp, top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                        Row {
                            IconButton(onClick = { showSpeedSheet = true }) {
                                Icon(Icons.Default.Speed, "Speed", tint = Color.White)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Subtitles, "Subtitles", tint = Color.White)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Cast, "Cast", tint = Color.White)
                            }
                        }
                    }
                }

                // Center playback controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10, "-10s",
                            tint = Color.White, modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (player.isPlaying) player.pause() else player.play() },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.seekTo(player.currentPosition + 10_000) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward10, "+10s",
                            tint = Color.White, modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Bottom gradient + seek bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 0f,
                                endY = 200f
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (playbackSpeed != 1f) {
                            Text(
                                "${playbackSpeed}x",
                                color = Color(0xFFE50914),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            formatTime(duration),
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
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

    // Speed sheet
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1E1E1E)) {
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
                            if (speed == 1.0f) "Normal (1.0x)" else "${speed}x",
                            color = if (currentSpeed == speed) Color(0xFFE50914) else Color.White
                        )
                    },
                    trailingContent = {
                        if (currentSpeed == speed)
                            Icon(Icons.Default.Check, null, tint = Color(0xFFE50914))
                    },
                    modifier = Modifier.pointerInput(speed) {
                        detectTapGestures(onTap = { onSpeedSelected(speed) })
                    },
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

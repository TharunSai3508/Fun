package com.unistream.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.unistream.core.ui.theme.StreamingTheme
import com.unistream.streaming.data.VideoSource
import com.unistream.streaming.data.WatchHistoryEntity
import com.unistream.streaming.viewmodel.StreamingViewModel

@Composable
fun StreamingHomeScreen(
    onNavigateToPlayer: (String, String) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: StreamingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    StreamingTheme {
        Scaffold(
            containerColor = Color(0xFF0D0D0D),
            topBar = {
                StreamingTopBar(
                    onBack = onBack,
                    onAddUrl = { viewModel.showUrlDialog(true) }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Hero Banner
                item {
                    StreamingHeroBanner(
                        videos = uiState.localVideos.take(5),
                        onPlayClick = { source ->
                            viewModel.recordWatch(source)
                            onNavigateToPlayer("local", source.uri)
                        }
                    )
                }

                // Continue Watching
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        ContentSection(
                            title = "Continue Watching",
                            icon = Icons.Default.PlayCircle
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.continueWatching) { history ->
                                    ContinueWatchingCard(
                                        history = history,
                                        onClick = {
                                            onNavigateToPlayer(
                                                history.sourceType,
                                                history.sourceUri
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // My Playlists
                if (uiState.playlists.isNotEmpty()) {
                    item {
                        ContentSection(title = "My Playlists", icon = Icons.Default.VideoLibrary) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.playlists) { playlist ->
                                    PlaylistCard(
                                        name = playlist.name,
                                        thumbnailUri = playlist.thumbnailUri,
                                        onClick = { onNavigateToPlaylist(playlist.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Local Videos
                if (uiState.localVideos.isNotEmpty()) {
                    item {
                        ContentSection(title = "From Device", icon = Icons.Default.Smartphone) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.localVideos.take(20)) { source ->
                                    VideoCard(
                                        source = source,
                                        onClick = {
                                            viewModel.recordWatch(source)
                                            onNavigateToPlayer("local", source.uri)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty state
                if (!uiState.isLoading && uiState.localVideos.isEmpty()) {
                    item {
                        StreamingEmptyState(
                            onAddUrl = { viewModel.showUrlDialog(true) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // URL Input Dialog
        if (uiState.showUrlDialog) {
            StreamUrlDialog(
                urlInput = uiState.urlInput,
                onUrlChange = viewModel::setUrlInput,
                onPlay = { url ->
                    val source = viewModel.prepareUrlSource(url)
                    if (source != null) {
                        viewModel.showUrlDialog(false)
                        viewModel.recordWatch(source)
                        onNavigateToPlayer(source.type.name, source.uri)
                    }
                },
                onDismiss = { viewModel.showUrlDialog(false) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamingTopBar(onBack: () -> Unit, onAddUrl: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "STREAM",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = onAddUrl) {
                Icon(Icons.Default.AddLink, contentDescription = "Add URL", tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D))
    )
}

@Composable
private fun StreamingHeroBanner(
    videos: List<VideoSource>,
    onPlayClick: (VideoSource) -> Unit
) {
    if (videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MovieFilter, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp))
        }
        return
    }

    val featured = videos.first()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Thumbnail
        AsyncImage(
            model = featured.thumbnailUri ?: featured.uri,
            contentDescription = featured.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xDD000000))
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = featured.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onPlayClick(featured) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.5f))
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("My List", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ContentSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        content()
    }
}

@Composable
private fun VideoCard(source: VideoSource, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = source.thumbnailUri ?: source.uri,
                contentDescription = source.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Icon(
                Icons.Default.PlayCircleOutline,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = source.title,
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContinueWatchingCard(history: WatchHistoryEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(112.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            AsyncImage(
                model = history.thumbnailUri,
                contentDescription = history.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Progress bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(history.progressPercent)
                        .fillMaxHeight()
                        .background(Color(0xFFE50914))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            history.title,
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistCard(name: String, thumbnailUri: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlaylistPlay, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(40.dp))
        }
        Text(name, style = MaterialTheme.typography.labelMedium.copy(color = Color.White), maxLines = 1)
    }
}

@Composable
private fun StreamingEmptyState(onAddUrl: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.VideoLibrary,
            null,
            modifier = Modifier.size(80.dp),
            tint = Color.White.copy(0.2f)
        )
        Text(
            "No videos found",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(0.6f))
        )
        Button(
            onClick = onAddUrl,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
        ) {
            Icon(Icons.Default.AddLink, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stream from URL")
        }
    }
}

@Composable
private fun StreamUrlDialog(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onPlay: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        icon = {
            Icon(Icons.Default.Link, null, tint = Color(0xFFE50914))
        },
        title = {
            Text("Stream from URL", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste a video URL, Google Drive link, HLS (.m3u8), or DASH stream",
                    color = Color.White.copy(0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    placeholder = { Text("https://...", color = Color.White.copy(0.4f)) },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.White.copy(0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPlay(urlInput) },
                enabled = urlInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
            ) {
                Text("Play")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(0.7f))
            }
        }
    )
}

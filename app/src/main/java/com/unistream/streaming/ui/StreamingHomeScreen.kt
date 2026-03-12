package com.unistream.streaming.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import kotlinx.coroutines.launch

private val StreamBg = Color(0xFF0D0D0D)
private val StreamSurface = Color(0xFF1A1A1A)
private val StreamRed = Color(0xFFE50914)

private val CATEGORIES = listOf("All", "Movies", "Series", "Sports", "Music", "Kids")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamingHomeScreen(
    onNavigateToPlayer: (String, String) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: StreamingViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableIntStateOf(0) }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredVideos = remember(uiState.localVideos, selectedCategory) {

        when (CATEGORIES[selectedCategory]) {

            "Movies" ->
                uiState.localVideos.filter { it.title.contains("movie", true) }

            "Series" ->
                uiState.localVideos.filter {
                    it.title.contains("episode", true) ||
                            it.title.contains("series", true)
                }

            "Sports" ->
                uiState.localVideos.filter { it.title.contains("sport", true) }

            "Music" ->
                uiState.localVideos.filter {
                    it.title.contains("music", true) ||
                            it.title.contains("song", true)
                }

            "Kids" ->
                uiState.localVideos.filter {
                    it.title.contains("kids", true) ||
                            it.title.contains("cartoon", true)
                }

            else -> uiState.localVideos
        }
    }

    StreamingTheme {

        Scaffold(
            containerColor = StreamBg,

            topBar = {

                Column {

                    StreamingTopBar(
                        searchExpanded = searchExpanded,
                        searchQuery = searchQuery,
                        onSearchToggle = {
                            searchExpanded = !searchExpanded
                            if (!searchExpanded) searchQuery = ""
                        },
                        onSearchChange = { searchQuery = it },
                        onBack = onBack,
                        onAddUrl = { viewModel.showUrlDialog(true) },
                        onCreatePlaylist = { viewModel.createPlaylist("New Playlist") }
                    )

                    AnimatedVisibility(visible = !searchExpanded) {

                        StreamingCategoryTabs(
                            categories = CATEGORIES,
                            selectedIndex = selectedCategory,
                            onTabSelected = { selectedCategory = it }
                        )
                    }
                }
            }
        ) { paddingValues ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                item {

                    StreamingHeroBanner(
                        videos = filteredVideos.take(5),
                        onPlayClick = { source ->

                            viewModel.recordWatch(source)

                            onNavigateToPlayer(
                                "local",
                                source.uri
                            )
                        }
                    )
                }

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

                if (filteredVideos.isNotEmpty()) {

                    item {

                        ContentSection(
                            title = "From Device",
                            icon = Icons.Default.Smartphone
                        ) {

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                items(filteredVideos.take(20)) { source ->

                                    VideoCard(
                                        source = source,
                                        onClick = {

                                            viewModel.recordWatch(source)

                                            onNavigateToPlayer(
                                                "local",
                                                source.uri
                                            )
                                        },
                                        onAddToPlaylist = {

                                            viewModel.addToPlaylist(
                                                playlistId = uiState.playlists.firstOrNull()?.id
                                                    ?: return@VideoCard,
                                                source = source
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }

        if (uiState.showUrlDialog) {

            StreamUrlDialog(
                urlInput = uiState.urlInput,
                onUrlChange = viewModel::setUrlInput,

                onPlay = { url ->

                    scope.launch {

                        val source = viewModel.prepareUrlSource(url)

                        if (source != null) {

                            viewModel.showUrlDialog(false)

                            viewModel.recordWatch(source)

                            onNavigateToPlayer(
                                source.type.name,
                                source.uri
                            )
                        }
                    }
                },

                onDownload = { url ->

                    scope.launch {
                        viewModel.downloadVideo(url)
                        viewModel.showUrlDialog(false)
                    }
                },

                onDismiss = { viewModel.showUrlDialog(false) }
            )
        }

        uiState.errorMessage?.let { error ->

            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}


// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamingTopBar(
    searchExpanded: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    onAddUrl: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = searchExpanded,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "top_bar_content"
            ) { expanded ->
                if (expanded) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search videos…", color = Color.White.copy(0.4f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = StreamRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "STREAM",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        )
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = if (searchExpanded) onSearchToggle else onBack) {
                Icon(
                    if (searchExpanded) Icons.Default.Close else Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchToggle) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
            IconButton(onClick = onAddUrl) {
                Icon(Icons.Default.AddLink, contentDescription = "Add URL", tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = StreamBg)
    )
}

// ─── Category Tabs ────────────────────────────────────────────────────────────

@Composable
private fun StreamingCategoryTabs(
    categories: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = StreamBg,
        contentColor = Color.White,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = StreamRed,
                    height = 2.dp
                )
            }
        },
        divider = {}
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        category,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                selectedContentColor = Color.White,
                unselectedContentColor = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

// ─── Hero Banner ──────────────────────────────────────────────────────────────

@Composable
private fun StreamingHeroBanner(
    videos: List<VideoSource>,
    onPlayClick: (VideoSource) -> Unit
) {
    if (videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(StreamSurface),
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
        AsyncImage(
            model = featured.thumbnailUri ?: featured.uri,
            contentDescription = featured.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xEE000000))
                    )
                )
        )

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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.5f))
                ) {
                    Icon(Icons.Default.Info, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Info", color = Color.White)
                }
            }
        }
    }
}

// ─── Content Section ──────────────────────────────────────────────────────────

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
            Icon(icon, contentDescription = null, tint = StreamRed, modifier = Modifier.size(20.dp))
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

// ─── Video Card ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoCard(
    source: VideoSource,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {

    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {

        Box(
            modifier = Modifier
                .width(160.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StreamSurface),
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
                null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = source.title,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {

            DropdownMenuItem(
                text = { Text("Play") },
                onClick = { showMenu = false; onClick() }
            )

            DropdownMenuItem(
                text = { Text("Add to Playlist") },
                onClick = { showMenu = false; onAddToPlaylist() }
            )
        }
    }
}

// ─── Continue Watching Card ───────────────────────────────────────────────────

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
                .background(StreamSurface)
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
                    .background(Color.White.copy(0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(history.progressPercent)
                        .fillMaxHeight()
                        .background(StreamRed)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            history.title,
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Playlist Card ────────────────────────────────────────────────────────────

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

// ─── Empty State ──────────────────────────────────────────────────────────────

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
        Text(
            "Add local videos or stream from a URL",
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(0.4f))
        )
        Button(
            onClick = onAddUrl,
            colors = ButtonDefaults.buttonColors(containerColor = StreamRed)
        ) {
            Icon(Icons.Default.AddLink, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stream from URL")
        }
    }
}

// ─── URL Dialog ───────────────────────────────────────────────────────────────

@Composable
private fun StreamUrlDialog(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onPlay: (String) -> Unit,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Stream from URL")
        },

        text = {

            Column {

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {

            Row {

                Button(
                    onClick = { onPlay(urlInput) },
                    enabled = urlInput.isNotBlank()
                ) {
                    Text("Play")
                }

                Spacer(Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { onDownload(urlInput) },
                    enabled = urlInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Download")
                }
            }
        },

        dismissButton = {

            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
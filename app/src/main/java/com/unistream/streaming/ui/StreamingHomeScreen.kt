package com.unistream.streaming.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontStyle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val StreamBg = Color(0xFF0D0D0D)
private val StreamSurface = Color(0xFF1A1A1A)
private val StreamRed = Color(0xFFE50914)
private val StreamDarkGray = Color(0xFF141414)
private val StreamGold = Color(0xFFFFD700)

private val CATEGORIES = listOf("All", "Movies", "Series", "Sports", "Music", "Kids")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamingHomeScreen(
    onNavigateToPlayer: (String, String) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToHiddenVideos: () -> Unit = {},
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
            "Movies" -> uiState.localVideos.filter { it.title.contains("movie", true) }
            "Series" -> uiState.localVideos.filter {
                it.title.contains("episode", true) || it.title.contains("series", true)
            }
            "Sports" -> uiState.localVideos.filter { it.title.contains("sport", true) }
            "Music" -> uiState.localVideos.filter {
                it.title.contains("music", true) || it.title.contains("song", true)
            }
            "Kids" -> uiState.localVideos.filter {
                it.title.contains("kids", true) || it.title.contains("cartoon", true)
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
                        onCreatePlaylist = { viewModel.createPlaylist("New Playlist") },
                        onHiddenVideos = onNavigateToHiddenVideos
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

                // Hero Banner with pager
                item {
                    NetflixHeroBanner(
                        videos = filteredVideos.take(5),
                        onPlayClick = { source ->
                            viewModel.recordWatch(source)
                            onNavigateToPlayer("local", source.uri)
                        },
                        onMyListClick = {
                            val playlist = uiState.playlists.firstOrNull()
                            if (playlist != null) {
                                viewModel.addToPlaylist(playlist.id, it)
                            }
                        }
                    )
                }

                // Continue Watching
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        ContentSection(title = "Continue Watching", icon = Icons.Default.PlayCircle) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.continueWatching) { history ->
                                    ContinueWatchingCard(
                                        history = history,
                                        onClick = {
                                            onNavigateToPlayer(history.sourceType, history.sourceUri)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Top 10 row
                if (filteredVideos.size >= 3) {
                    item {
                        ContentSection(title = "Top 10 on Device", icon = Icons.Default.TrendingUp) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(filteredVideos.take(10)) { index, source ->
                                    Top10Card(
                                        rank = index + 1,
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

                // My List (playlists)
                if (uiState.playlists.isNotEmpty()) {
                    item {
                        ContentSection(title = "My List", icon = Icons.Default.BookmarkBorder) {
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

                // From Device
                if (filteredVideos.isNotEmpty()) {
                    item {
                        ContentSection(title = "From Device", icon = Icons.Default.Smartphone) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredVideos.take(20)) { source ->
                                    VideoCard(
                                        source = source,
                                        onClick = {
                                            viewModel.recordWatch(source)
                                            onNavigateToPlayer("local", source.uri)
                                        },
                                        onAddToPlaylist = {
                                            viewModel.addToPlaylist(
                                                playlistId = uiState.playlists.firstOrNull()?.id
                                                    ?: return@VideoCard,
                                                source = source
                                            )
                                        },
                                        onHide = { viewModel.hideVideo(source) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Watch History
                if (uiState.watchHistory.isNotEmpty()) {
                    item {
                        ContentSection(title = "Recently Watched", icon = Icons.Default.History) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.watchHistory.take(15)) { history ->
                                    ContinueWatchingCard(
                                        history = history,
                                        onClick = {
                                            onNavigateToPlayer(history.sourceType, history.sourceUri)
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

        // URL Dialog
        if (uiState.showUrlDialog) {
            StreamUrlDialog(
                urlInput = uiState.urlInput,
                isDownloading = uiState.isDownloading,
                onUrlChange = viewModel::setUrlInput,
                onPlay = { url ->
                    scope.launch {
                        val source = viewModel.prepareUrlSource(url)
                        if (source != null) {
                            viewModel.showUrlDialog(false)
                            viewModel.recordWatch(source)
                            onNavigateToPlayer(source.type.name, source.uri)
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

        // Error/Success snackbar
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = StreamSurface,
                shape = RoundedCornerShape(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = StreamRed)
                    }
                }
            ) { Text(error, color = Color.White) }
        }

        uiState.successMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = Color(0xFF1B5E20),
                shape = RoundedCornerShape(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = Color.White)
                    }
                }
            ) { Text(msg, color = Color.White) }
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
    onCreatePlaylist: () -> Unit,
    onHiddenVideos: () -> Unit = {}
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
                        placeholder = { Text("Search videos...", color = Color.White.copy(0.4f)) },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Netflix "N" style logo
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(StreamRed, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "S",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
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
            IconButton(onClick = onHiddenVideos) {
                Icon(Icons.Default.Lock, contentDescription = "Hidden Videos", tint = Color.White)
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

// ─── Hero Banner with Pager ──────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NetflixHeroBanner(
    videos: List<VideoSource>,
    onPlayClick: (VideoSource) -> Unit,
    onMyListClick: (VideoSource) -> Unit
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

    val pagerState = rememberPagerState(pageCount = { videos.size })

    // Auto-scroll
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % videos.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val video = videos[page]
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = video.thumbnailUri ?: video.uri,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(StreamBg.copy(0.3f), Color.Transparent, StreamBg)
                            )
                        )
                )
            }
        }

        // Content overlay
        val currentVideo = videos.getOrNull(pagerState.currentPage) ?: videos.first()
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = currentVideo.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (currentVideo.durationMs > 0) {
                Text(
                    formatDuration(currentVideo.durationMs),
                    color = Color.White.copy(0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Play button
                Button(
                    onClick = { onPlayClick(currentVideo) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                // My List button
                OutlinedButton(
                    onClick = { onMyListClick(currentVideo) },
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.5f)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("My List", color = Color.White)
                }
            }
        }

        // Page indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(videos.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) StreamRed
                            else Color.White.copy(0.4f)
                        )
                )
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

// ─── Top 10 Card ──────────────────────────────────────────────────────────────

@Composable
private fun Top10Card(
    rank: Int,
    source: VideoSource,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Bottom
    ) {
        // Large rank number
        Text(
            text = "$rank",
            style = MaterialTheme.typography.displayLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 72.sp,
                fontStyle = FontStyle.Italic
            ),
            modifier = Modifier.offset(x = 8.dp, y = 8.dp)
        )

        // Thumbnail
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(140.dp)
                .offset(x = (-12).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StreamSurface)
        ) {
            AsyncImage(
                model = source.thumbnailUri ?: source.uri,
                contentDescription = source.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Netflix red top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(StreamRed)
            )
        }
    }
}

// ─── Video Card ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoCard(
    source: VideoSource,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onHide: () -> Unit = {}
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
                .clip(RoundedCornerShape(6.dp))
                .background(StreamSurface),
            contentAlignment = Alignment.Center
        ) {

            AsyncImage(
                model = source.thumbnailUri ?: source.uri,
                contentDescription = source.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Play icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Duration badge
            if (source.durationMs > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(0.75f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        formatDuration(source.durationMs),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = source.title,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Play") },
                leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                onClick = { showMenu = false; onClick() }
            )
            DropdownMenuItem(
                text = { Text("Add to My List") },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                onClick = { showMenu = false; onAddToPlaylist() }
            )
            DropdownMenuItem(
                text = { Text("Hide Video") },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = { showMenu = false; onHide() }
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
                .clip(RoundedCornerShape(6.dp))
                .background(StreamSurface)
        ) {
            AsyncImage(
                model = history.thumbnailUri,
                contentDescription = history.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Play icon center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            // Progress bar at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(history.progressPercent)
                        .fillMaxHeight()
                        .background(StreamRed)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            history.title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            ),
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
                .clip(RoundedCornerShape(6.dp))
                .background(StreamSurface),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailUri != null) {
                AsyncImage(
                    model = thumbnailUri,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Icon(Icons.Default.PlaylistPlay, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    isDownloading: Boolean = false,
    onUrlChange: (String) -> Unit,
    onPlay: (String) -> Unit,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StreamSurface,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLink, null, tint = StreamRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stream from URL", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    placeholder = { Text("https://...", color = Color.White.copy(0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = StreamRed,
                        unfocusedBorderColor = Color.White.copy(0.3f),
                        cursorColor = StreamRed
                    ),
                    singleLine = true
                )
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = StreamRed,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloading...", color = Color.White.copy(0.6f), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = { onPlay(urlInput) },
                    enabled = urlInput.isNotBlank() && !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onDownload(urlInput) },
                    enabled = urlInput.isNotBlank() && !isDownloading,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.3f))
                ) {
                    Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(0.6f))
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    val hours = ms / 1000 / 3600
    return if (hours > 0)
        "%d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%d:%02d".format(minutes, seconds)
}

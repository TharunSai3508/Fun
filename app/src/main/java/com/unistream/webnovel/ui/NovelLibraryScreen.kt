package com.unistream.webnovel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.unistream.core.ui.theme.NovelTheme
import com.unistream.webnovel.data.NovelEntity
import com.unistream.webnovel.viewmodel.NovelViewModel

@Composable
fun NovelLibraryScreen(
    onNavigateToReader: (Long) -> Unit,
    onNavigateToImport: () -> Unit,
    onBack: () -> Unit,
    viewModel: NovelViewModel = hiltViewModel()
) {
    val state by viewModel.libraryState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    NovelTheme {
        Scaffold(
            topBar = {
                NovelTopBar(onBack = onBack, onImport = onNavigateToImport)
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToImport,
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Import Novel") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tabs: Library | Favorites | History
                val tabs = listOf("Library", "Favorites", "History")
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { i, tab ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = { Text(tab) }
                        )
                    }
                }

                val novels = when (selectedTab) {
                    0 -> state.novels
                    1 -> state.favoriteNovels
                    else -> state.novels
                }

                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    novels.isEmpty() -> {
                        NovelEmptyState(
                            tab = selectedTab,
                            onImport = onNavigateToImport
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Continue Reading carousel (Library tab only)
                            if (selectedTab == 0 && novels.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "Continue Reading")
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(novels.take(5)) { novel ->
                                            CompactNovelCard(
                                                novel = novel,
                                                onClick = { onNavigateToReader(novel.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SectionHeader(title = "All Novels")
                                }
                            }

                            items(novels) { novel ->
                                NovelListCard(
                                    novel = novel,
                                    onClick = { onNavigateToReader(novel.id) },
                                    onFavoriteClick = { viewModel.toggleFavorite(novel) },
                                    onDeleteClick = { viewModel.deleteNovel(novel) }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) } // FAB clearance
                        }
                    }
                }
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovelTopBar(onBack: () -> Unit, onImport: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "NOVELS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Sort, contentDescription = "Sort")
            }
        }
    )
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    )
}

// ─── Novel List Card ──────────────────────────────────────────────────────────

@Composable
private fun NovelListCard(
    novel: NovelEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Fake reading progress based on downloaded chapters
    val progress = if (novel.totalChapters > 0)
        novel.downloadedChapters.toFloat() / novel.totalChapters.toFloat()
    else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cover thumbnail
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(112.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = novel.coverUrl,
                        contentDescription = novel.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (novel.coverUrl == null) {
                        Icon(
                            Icons.Default.MenuBook,
                            null,
                            modifier = Modifier.align(Alignment.Center).size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                        )
                    }
                    // Status badge
                    if (novel.status == "completed") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "DONE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }
                }

                // Info column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        novel.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        novel.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )

                    // Genre chips
                    if (novel.genre.isNotBlank()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val genres = novel.genre.split(",").map { it.trim() }.take(3)
                            items(genres.size) { i ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(genres[i], style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Chapter progress
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                        Text(
                            "${novel.downloadedChapters}/${novel.totalChapters} chapters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                        if (novel.isFavorite) {
                            Icon(
                                Icons.Default.Favorite,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // More menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (novel.isFavorite) "Unfavorite" else "Add to Favorites") },
                            leadingIcon = {
                                Icon(
                                    if (novel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null
                                )
                            },
                            onClick = { onFavoriteClick(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Update chapters") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            onClick = { showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { onDeleteClick(); showMenu = false }
                        )
                    }
                }
            }

            // Reading progress bar
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

// ─── Compact Novel Card (carousel) ───────────────────────────────────────────

@Composable
private fun CompactNovelCard(novel: NovelEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = novel.coverUrl,
                contentDescription = novel.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Bottom gradient with title overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(0.6f)),
                            startY = 70f
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            novel.title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun NovelEmptyState(tab: Int, onImport: () -> Unit) {
    val (icon, message, sub) = when (tab) {
        1 -> Triple(Icons.Default.FavoriteBorder, "No favorites yet", "Mark novels as favorite to see them here")
        2 -> Triple(Icons.Default.History, "No reading history", "Start reading to track your progress")
        else -> Triple(Icons.Default.LibraryBooks, "Your library is empty", "Paste a novel URL to import it to your library.\nSupports Webnovel, RoyalRoad, WuxiaWorld, and more.")
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onBackground.copy(0.2f))
            Text(message, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(
                sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                textAlign = TextAlign.Center
            )
            if (tab == 0) {
                Button(onClick = onImport) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Novel")
                }
            }
        }
    }
}

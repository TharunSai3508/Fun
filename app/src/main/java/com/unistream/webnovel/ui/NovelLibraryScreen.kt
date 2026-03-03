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
                FloatingActionButton(
                    onClick = onNavigateToImport,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import Novel")
                }
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

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (novels.isEmpty()) {
                    NovelEmptyState(onImport = onNavigateToImport)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Currently Reading section
                        if (selectedTab == 0 && novels.isNotEmpty()) {
                            item {
                                Text(
                                    "Continue Reading",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(novels.take(5)) { novel ->
                                        CompactNovelCard(
                                            novel = novel,
                                            onClick = { onNavigateToReader(novel.id) }
                                        )
                                    }
                                }
                            }
                            item {
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "All Novels",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
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
                    }
                }
            }
        }
    }
}

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
            IconButton(onClick = onImport) {
                Icon(Icons.Default.Add, contentDescription = "Import")
            }
        }
    )
}

@Composable
private fun NovelListCard(
    novel: NovelEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
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
            }

            // Info
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
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${novel.downloadedChapters}/${novel.totalChapters} ch", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Download, null, modifier = Modifier.size(12.dp)) }
                    )
                    if (novel.status == "completed") {
                        AssistChip(
                            onClick = {},
                            label = { Text("Complete", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Actions
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (novel.isFavorite) "Unfavorite" else "Favorite") },
                            leadingIcon = {
                                Icon(
                                    if (novel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null
                                )
                            },
                            onClick = { onFavoriteClick(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { onDeleteClick(); showMenu = false }
                        )
                    }
                }
            }
        }
    }
}

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
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = novel.coverUrl,
                contentDescription = novel.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
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

@Composable
private fun NovelEmptyState(onImport: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.LibraryBooks,
                null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(0.2f)
            )
            Text(
                "Your library is empty",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Paste a novel URL to import it to your library. Supports Webnovel, RoyalRoad, WuxiaWorld, and more.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = onImport) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Novel")
            }
        }
    }
}

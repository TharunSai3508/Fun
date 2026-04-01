package com.unistream.webnovel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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

private val WebnovelBlue = Color(0xFF1565C0)
private val WebnovelLightBlue = Color(0xFF42A5F5)
private val WebnovelBg = Color(0xFFFAFAFA)
private val WebnovelDark = Color(0xFF1A1A1A)
private val WebnovelGray = Color(0xFF757575)
private val WebnovelOrange = Color(0xFFFF6D00)

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
            containerColor = WebnovelBg,
            topBar = {
                WebnovelTopBar(onBack = onBack, onImport = onNavigateToImport)
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToImport,
                    containerColor = WebnovelBlue,
                    contentColor = Color.White,
                    shape = CircleShape
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
                // Webnovel-style tabs
                val tabs = listOf("Library", "Favorites", "History")
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = WebnovelBlue,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = WebnovelBlue,
                                height = 3.dp
                            )
                        }
                    },
                    divider = { HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp) }
                ) {
                    tabs.forEachIndexed { i, tab ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = {
                                Text(
                                    tab,
                                    fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            selectedContentColor = WebnovelBlue,
                            unselectedContentColor = WebnovelGray
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
                            CircularProgressIndicator(color = WebnovelBlue)
                        }
                    }
                    novels.isEmpty() -> {
                        NovelEmptyState(tab = selectedTab, onImport = onNavigateToImport)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            // Featured novel banner (Library tab only)
                            if (selectedTab == 0 && novels.isNotEmpty()) {
                                item {
                                    FeaturedNovelBanner(
                                        novel = novels.first(),
                                        onClick = { onNavigateToReader(novels.first().id) }
                                    )
                                }
                            }

                            // Continue Reading carousel
                            if (selectedTab == 0 && novels.size > 1) {
                                item {
                                    SectionHeader(title = "Continue Reading")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(novels.take(6)) { novel ->
                                            ContinueReadingCard(
                                                novel = novel,
                                                onClick = { onNavigateToReader(novel.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            // Book grid section header
                            item {
                                SectionHeader(
                                    title = when (selectedTab) {
                                        0 -> "All Novels"
                                        1 -> "Favorite Novels"
                                        else -> "Reading History"
                                    }
                                )
                            }

                            // 3-column book cover grid
                            val rows = novels.chunked(3)
                            items(rows.size) { rowIndex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rows[rowIndex].forEach { novel ->
                                        BookCoverCard(
                                            novel = novel,
                                            onClick = { onNavigateToReader(novel.id) },
                                            onFavoriteClick = { viewModel.toggleFavorite(novel) },
                                            onDeleteClick = { viewModel.deleteNovel(novel) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // Fill remaining slots in last row
                                    repeat(3 - rows[rowIndex].size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
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
private fun WebnovelTopBar(onBack: () -> Unit, onImport: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(WebnovelBlue, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("W", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "NOVELS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        color = WebnovelDark
                    )
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WebnovelDark)
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = WebnovelDark)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = WebnovelDark)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

// ─── Featured Novel Banner ───────────────────────────────────────────────────

@Composable
private fun FeaturedNovelBanner(novel: NovelEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(WebnovelBlue, WebnovelLightBlue)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Book cover
            Box(
                modifier = Modifier
                    .width(85.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(0.2f))
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
                        tint = Color.White.copy(0.5f)
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "FEATURED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    novel.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    novel.author,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(0.8f))
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (novel.genre.isNotBlank()) {
                        val firstGenre = novel.genre.split(",").firstOrNull()?.trim() ?: ""
                        if (firstGenre.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(firstGenre, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${novel.totalChapters} Ch",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ─── Section Header ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = WebnovelDark
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─── Continue Reading Card ───────────────────────────────────────────────────

@Composable
private fun ContinueReadingCard(novel: NovelEntity, onClick: () -> Unit) {
    val progress = if (novel.totalChapters > 0)
        novel.downloadedChapters.toFloat() / novel.totalChapters.toFloat()
    else 0f

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

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(0.6f))
                        )
                    )
            )

            // Reading progress circular indicator
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(22.dp),
                        color = WebnovelOrange,
                        trackColor = Color.White.copy(0.2f),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        "${(progress * 100).toInt()}",
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Status badge
            if (novel.status == "completed") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(WebnovelBlue, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("END", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            novel.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = WebnovelDark
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Book Cover Card (Grid) ──────────────────────────────────────────────────

@Composable
private fun BookCoverCard(
    novel: NovelEntity,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val progress = if (novel.totalChapters > 0)
        novel.downloadedChapters.toFloat() / novel.totalChapters.toFloat()
    else 0f

    Column(modifier = modifier.clickable(onClick = onClick)) {
        // 3:4 book cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = WebnovelGray.copy(0.4f)
                    )
                    Text(
                        novel.title.take(12),
                        color = WebnovelGray.copy(0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Progress bar at bottom
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(WebnovelOrange)
                    )
                }
            }

            // Status badge
            if (novel.status == "completed") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(WebnovelBlue, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("END", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Favorite indicator
            if (novel.isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp),
                    tint = WebnovelOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            novel.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = WebnovelDark
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        // Author
        Text(
            novel.author,
            style = MaterialTheme.typography.labelSmall.copy(color = WebnovelGray),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Chapter count
        Text(
            "${novel.downloadedChapters}/${novel.totalChapters} ch",
            style = MaterialTheme.typography.labelSmall.copy(
                color = WebnovelBlue,
                fontWeight = FontWeight.Medium
            )
        )

        // Genre tags
        if (novel.genre.isNotBlank()) {
            val genres = novel.genre.split(",").map { it.trim() }.take(2)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                genres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .background(WebnovelBlue.copy(0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            genre,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WebnovelBlue,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
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
            Icon(
                icon, null,
                modifier = Modifier.size(72.dp),
                tint = WebnovelGray.copy(0.2f)
            )
            Text(
                message,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WebnovelDark
                )
            )
            Text(
                sub,
                style = MaterialTheme.typography.bodyMedium,
                color = WebnovelGray,
                textAlign = TextAlign.Center
            )
            if (tab == 0) {
                Button(
                    onClick = onImport,
                    colors = ButtonDefaults.buttonColors(containerColor = WebnovelBlue),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Novel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

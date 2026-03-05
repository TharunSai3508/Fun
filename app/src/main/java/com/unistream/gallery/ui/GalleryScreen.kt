package com.unistream.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
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
import coil.request.videoFrameMillis
import com.unistream.core.ui.theme.GalleryTheme
import com.unistream.gallery.data.MediaFilter
import com.unistream.gallery.data.MediaItem
import com.unistream.gallery.viewmodel.GalleryViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onNavigateToAlbums: () -> Unit,
    onNavigateToMedia: (Long) -> Unit,
    onNavigateToHiddenVault: () -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    GalleryTheme {
        Scaffold(
            topBar = {
                GalleryTopBar(
                    title = "Gallery",
                    searchQuery = uiState.searchQuery,
                    searchExpanded = searchExpanded,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedCount = uiState.selectedMedia.size,
                    onSearchClick = { searchExpanded = !searchExpanded },
                    onSearchChange = viewModel::search,
                    onFilterClick = { showFilterSheet = true },
                    onAlbumsClick = onNavigateToAlbums,
                    onHideSelected = viewModel::hideSelectedMedia,
                    onClearSelection = viewModel::clearSelection,
                    onHiddenVaultClick = onNavigateToHiddenVault,
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        // Keep filter chips visible even if current filter has no content
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalItemSpacing = 4.dp
                        ) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                GalleryFilterChips(
                                    currentFilter = uiState.currentFilter,
                                    onFilterSelected = viewModel::setFilter
                                )
                            }

                            if (uiState.filteredMedia.isEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    EmptyGalleryPlaceholder()
                                }
                            } else {
                                items(
                                    items = uiState.filteredMedia,
                                    key = { it.id }
                                ) { media ->
                                    GalleryMediaCard(
                                        media = media,
                                        isSelected = uiState.selectedMedia.contains(media.id),
                                        isFavorite = uiState.favorites.contains(media.id),
                                        isSelectionMode = uiState.isSelectionMode,
                                        onClick = {
                                            if (uiState.isSelectionMode) viewModel.toggleSelection(media.id)
                                            else onNavigateToMedia(media.id)
                                        },
                                        onLongClick = { viewModel.toggleSelection(media.id) },
                                        onFavoriteClick = { viewModel.toggleFavorite(media.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            GalleryFilterSheet(
                currentFilter = uiState.currentFilter,
                currentSort = uiState.currentSort,
                onFilterSelected = { viewModel.setFilter(it); showFilterSheet = false },
                onSortSelected = { viewModel.setSort(it); showFilterSheet = false },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(
    title: String,
    searchQuery: String,
    searchExpanded: Boolean,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onSearchClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onHideSelected: () -> Unit,
    onClearSelection: () -> Unit,
    onHiddenVaultClick: () -> Unit,
    onBack: () -> Unit
) {
    if (isSelectionMode) {
        TopAppBar(
            title = { Text("$selectedCount selected") },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                }
            },
            actions = {
                IconButton(onClick = onHideSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Hide selected"
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        )
    } else {
        Column {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search photos, videos...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(title, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (searchExpanded) {
                        IconButton(onClick = { onSearchChange(""); onSearchClick() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onAlbumsClick) {
                        Icon(Icons.Default.PhotoAlbum, contentDescription = "Albums")
                    }
                    IconButton(onClick = onHiddenVaultClick) {
                        Icon(Icons.Default.Lock, contentDescription = "Hidden Vault")
                    }
                    IconButton(onClick = onFilterClick) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryMediaCard(
    media: MediaItem,
    isSelected: Boolean,
    isFavorite: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current
    val aspectRatio = if (media.height > 0 && media.width > 0)
        media.width.toFloat() / media.height.toFloat()
    else 0.75f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val imageRequest = ImageRequest.Builder(context)
            .data(media.uri)
            .apply {
                if (media.isVideo) videoFrameMillis(1000)
            }
            .crossfade(true)
            .build()

        AsyncImage(
            model = imageRequest,
            contentDescription = media.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Selection overlay
        AnimatedVisibility(visible = isSelected, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                contentAlignment = Alignment.TopEnd
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Video indicator
        if (media.isVideo) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = formatDuration(media.duration),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                    )
                }
            }
        }

        // GIF indicator
        if (media.isGif) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = "GIF",
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF7043).copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Favorite indicator
        if (isFavorite) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = Color(0xFFE91E8C),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryFilterChips(
    currentFilter: MediaFilter,
    onFilterSelected: (MediaFilter) -> Unit
) {
    val filters = listOf(
        MediaFilter.ALL to "All",
        MediaFilter.IMAGES to "Photos",
        MediaFilter.VIDEOS to "Videos",
        MediaFilter.GIFS to "GIFs",
        MediaFilter.SCREEN_RECORDINGS to "Recordings"
    )

    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters.size) { i ->
            val (filter, label) = filters[i]
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryFilterSheet(
    currentFilter: MediaFilter,
    currentSort: com.unistream.gallery.data.SortOrder,
    onFilterSelected: (MediaFilter) -> Unit,
    onSortSelected: (com.unistream.gallery.data.SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Filter by Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // Filter options
            listOf(
                MediaFilter.ALL to "All Media",
                MediaFilter.IMAGES to "Images Only",
                MediaFilter.VIDEOS to "Videos Only",
                MediaFilter.GIFS to "GIFs Only",
                MediaFilter.LARGE_FILES to "Large Files",
                MediaFilter.SCREEN_RECORDINGS to "Screen Recordings"
            ).forEach { (filter, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        if (currentFilter == filter) Icon(Icons.Default.Check, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onFilterSelected(filter) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Sort by", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                com.unistream.gallery.data.SortOrder.DATE_DESC to "Newest First",
                com.unistream.gallery.data.SortOrder.DATE_ASC to "Oldest First",
                com.unistream.gallery.data.SortOrder.NAME_ASC to "Name A-Z",
                com.unistream.gallery.data.SortOrder.SIZE_DESC to "Largest First"
            ).forEach { (sort, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        if (currentSort == sort) Icon(Icons.Default.Check, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onSortSelected(sort) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EmptyGalleryPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No media found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Text(
                "Grant storage permission to browse your media",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    val hours = ms / 1000 / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

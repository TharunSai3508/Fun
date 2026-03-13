package com.unistream.gallery.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.unistream.core.ui.theme.GalleryTheme
import com.unistream.gallery.data.MediaFilter
import com.unistream.gallery.data.MediaItem
import com.unistream.gallery.data.SortOrder
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
    var showImportDialog by remember { mutableStateOf(false) }

    GalleryTheme {

        Scaffold(

            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showImportDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import URL")
                }
            },

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
                    onClearSelection = viewModel::clearSelection,
                    onHiddenVaultClick = onNavigateToHiddenVault,
                    onBack = onBack
                )
            },

            bottomBar = {

                AnimatedVisibility(
                    visible = uiState.isSelectionMode && uiState.selectedMedia.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {

                    SelectionActionBar(
                        selectedCount = uiState.selectedMedia.size,
                        onHide = viewModel::hideSelectedMedia,
                        onShare = { },
                        onDelete = { },
                        onSelectAll = { viewModel.selectAll() },
                        onClear = viewModel::clearSelection
                    )
                }
            }

        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {

                if (uiState.isLoading) {
                    GalleryLoadingState()
                } else {

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

                                EmptyGalleryPlaceholder(
                                    currentFilter = uiState.currentFilter
                                )
                            }

                        } else {

                            items(
                                items = uiState.filteredMedia,
                                key = { media -> media.id }
                            ) { media ->

                                GalleryMediaCard(
                                    media = media,
                                    isSelected = uiState.selectedMedia.contains(media.id),
                                    isFavorite = uiState.favorites.contains(media.id),
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        if (uiState.isSelectionMode)
                                            viewModel.toggleSelection(media.id)
                                        else
                                            onNavigateToMedia(media.id)
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(media.id)
                                    },
                                    onFavoriteClick = {
                                        viewModel.toggleFavorite(media.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {

            GalleryFilterSheet(
                currentFilter = uiState.currentFilter,
                currentSort = uiState.currentSort,
                onFilterSelected = {
                    viewModel.setFilter(it)
                    showFilterSheet = false
                },
                onSortSelected = {
                    viewModel.setSort(it)
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }

        if (showImportDialog) {

            var url by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                confirmButton = {

                    TextButton(
                        onClick = {
                            viewModel.importFromUrl(url)
                            showImportDialog = false
                        }
                    ) {
                        Text("Download")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Import Image from URL") },
                text = {

                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("https://example.com/image.jpg") },
                        singleLine = true
                    )
                }
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onHide: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            ActionBarButton(Icons.Default.SelectAll, "All", onClick = onSelectAll)
            ActionBarButton(Icons.Outlined.Lock, "Hide", onClick = onHide)
            ActionBarButton(Icons.Default.Share, "Share", onClick = onShare)
            ActionBarButton(Icons.Default.Delete, "Delete", onClick = onDelete)
        }
    }
}

@Composable
private fun ActionBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(icon, contentDescription = label, tint = tint)
        Text(label, style = MaterialTheme.typography.labelSmall)
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
    onClearSelection: () -> Unit,
    onHiddenVaultClick: () -> Unit,
    onBack: () -> Unit
) {

    TopAppBar(

        title = {

            if (searchExpanded) {

                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search media") }
                )

            } else {

                Text(title)
            }
        },

        navigationIcon = {

            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null)
            }
        },

        actions = {

            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, null)
            }

            IconButton(onClick = onAlbumsClick) {
                Icon(Icons.Default.PhotoAlbum, null)
            }

            IconButton(onClick = onHiddenVaultClick) {
                Icon(Icons.Default.Lock, null)
            }

            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.FilterList, null)
            }
        }
    )
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

    val aspectRatio =
        if (media.height > 0 && media.width > 0)
            media.width.toFloat() / media.height.toFloat()
        else 0.75f

    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {

        val request = ImageRequest.Builder(context)
            .data(media.uri)
            .apply {
                if (media.isVideo) videoFrameMillis(1000)
            }
            .crossfade(true)
            .build()

        AsyncImage(
            model = request,
            contentDescription = media.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun GalleryFilterChips(
    currentFilter: MediaFilter,
    onFilterSelected: (MediaFilter) -> Unit
) {

    val filters = listOf(
        MediaFilter.ALL,
        MediaFilter.IMAGES,
        MediaFilter.VIDEOS,
        MediaFilter.GIFS,
        MediaFilter.SCREEN_RECORDINGS
    )

    LazyRow(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(filters) { filter ->

            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.name) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryFilterSheet(
    currentFilter: MediaFilter,
    currentSort: SortOrder,
    onFilterSelected: (MediaFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {

    ModalBottomSheet(onDismissRequest = onDismiss) {

        Column(modifier = Modifier.padding(16.dp)) {

            Text("Filter")

            MediaFilter.values().forEach {

                ListItem(
                    headlineContent = { Text(it.name) },
                    modifier = Modifier.clickable { onFilterSelected(it) }
                )
            }
        }
    }
}

@Composable
private fun GalleryLoadingState() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyGalleryPlaceholder(currentFilter: MediaFilter) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "No media found",
            style = MaterialTheme.typography.titleMedium
        )
    }
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
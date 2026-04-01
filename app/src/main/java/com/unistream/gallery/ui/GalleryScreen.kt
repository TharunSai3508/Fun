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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.unistream.core.ui.theme.GalleryTheme
import com.unistream.gallery.data.MediaFilter
import com.unistream.gallery.data.MediaItem
import com.unistream.gallery.data.SortOrder
import com.unistream.gallery.viewmodel.GalleryViewModel

private val PinterestRed = Color(0xFFE60023)
private val PinterestBg = Color(0xFFFFFFFF)
private val PinterestDarkText = Color(0xFF111111)
private val PinterestGray = Color(0xFF767676)
private val PinterestLightGray = Color(0xFFEFEFEF)

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

            containerColor = PinterestBg,

            floatingActionButton = {
                // Pinterest-style FAB
                AnimatedVisibility(
                    visible = !uiState.isSelectionMode,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { showImportDialog = true },
                        containerColor = PinterestRed,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Import URL")
                    }
                }
            },

            topBar = {
                PinterestTopBar(
                    searchQuery = uiState.searchQuery,
                    searchExpanded = searchExpanded,
                    isSelectionMode = uiState.isSelectionMode,
                    selectedCount = uiState.selectedMedia.size,
                    onSearchClick = { searchExpanded = !searchExpanded },
                    onSearchChange = viewModel::search,
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
                    PinterestSelectionBar(
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
                    .background(PinterestBg)
            ) {

                if (uiState.isLoading) {
                    PinterestLoadingState()
                } else {

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {

                        // Pinterest-style filter chips
                        item(span = StaggeredGridItemSpan.FullLine) {
                            PinterestFilterChips(
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

                                PinterestPinCard(
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

                // Import status snackbars
                if (uiState.isImporting) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .background(PinterestDarkText, RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Downloading...", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                uiState.errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = PinterestDarkText,
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("OK", color = PinterestRed)
                            }
                        }
                    ) {
                        Text(error, color = Color.White)
                    }
                }
            }
        }

        if (showFilterSheet) {
            PinterestFilterSheet(
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
            PinterestImportDialog(
                isImporting = uiState.isImporting,
                onImport = { url ->
                    viewModel.importFromUrl(url)
                    showImportDialog = false
                },
                onDismiss = { showImportDialog = false }
            )
        }
    }
}

// ─── Pinterest Top Bar ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinterestTopBar(
    searchQuery: String,
    searchExpanded: Boolean,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onSearchClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onAlbumsClick: () -> Unit,
    onClearSelection: () -> Unit,
    onHiddenVaultClick: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = PinterestBg,
        shadowElevation = if (searchExpanded) 2.dp else 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Logo
                IconButton(onClick = if (isSelectionMode) onClearSelection else onBack) {
                    if (isSelectionMode) {
                        Icon(Icons.Default.Close, null, tint = PinterestDarkText)
                    } else {
                        Icon(Icons.Default.ArrowBack, null, tint = PinterestDarkText)
                    }
                }

                if (isSelectionMode) {
                    Text(
                        "$selectedCount selected",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PinterestDarkText
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    // Pinterest-style search bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(PinterestLightGray)
                            .clickable { if (!searchExpanded) onSearchClick() },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchExpanded) {
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = {
                                    Text("Search your pins", color = PinterestGray)
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = PinterestDarkText,
                                    cursorColor = PinterestRed
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, null, tint = PinterestGray)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchChange("") }) {
                                            Icon(Icons.Default.Close, null, tint = PinterestGray)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    null,
                                    tint = PinterestGray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Search your pins",
                                    color = PinterestGray,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Action icons
                if (!isSelectionMode) {
                    IconButton(onClick = onAlbumsClick) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = "Boards",
                            tint = PinterestDarkText
                        )
                    }
                    IconButton(onClick = onHiddenVaultClick) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Vault",
                            tint = PinterestDarkText
                        )
                    }
                }
            }
        }
    }
}

// ─── Pinterest Pin Card ──────────────────────────────────────────────────────

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
    PinterestPinCard(media, isSelected, isFavorite, isSelectionMode, onClick, onLongClick, onFavoriteClick)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinterestPinCard(
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
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isSelected) PinterestRed.copy(0.3f) else Color.Black.copy(0.1f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
                    .clip(RoundedCornerShape(16.dp))
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

                // Bottom gradient overlay for info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(64.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(0.5f))
                            )
                        )
                )

                // Video duration badge
                if (media.isVideo && media.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            formatDuration(media.duration),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Video play icon
                if (media.isVideo) {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        null,
                        tint = Color.White.copy(0.85f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                    )
                }

                // GIF badge
                if (media.mimeType.contains("gif", true)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "GIF",
                            color = PinterestDarkText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Save/Heart button (Pinterest style)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFavorite) PinterestRed else Color.Black.copy(0.4f)
                        )
                        .clickable { onFavoriteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Selection checkmark
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) PinterestRed else Color.Black.copy(0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Pin info below image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    media.displayName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PinterestDarkText
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (media.bucketName.isNotBlank()) {
                    Text(
                        media.bucketName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PinterestGray
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Pinterest Filter Chips ──────────────────────────────────────────────────

@Composable
private fun PinterestFilterChips(
    currentFilter: MediaFilter,
    onFilterSelected: (MediaFilter) -> Unit
) {
    val filters = listOf(
        MediaFilter.ALL to "All Pins",
        MediaFilter.IMAGES to "Photos",
        MediaFilter.VIDEOS to "Videos",
        MediaFilter.GIFS to "GIFs",
        MediaFilter.SCREEN_RECORDINGS to "Recordings"
    )

    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(filters) { (filter, label) ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        label,
                        fontWeight = if (currentFilter == filter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PinterestDarkText,
                    selectedLabelColor = Color.White,
                    containerColor = PinterestLightGray,
                    labelColor = PinterestDarkText
                ),
                border = null
            )
        }
    }
}

// ─── Pinterest Selection Bar ─────────────────────────────────────────────────

@Composable
private fun PinterestSelectionBar(
    selectedCount: Int,
    onHide: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PinterestDarkText,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionButton(Icons.Default.SelectAll, "All", Color.White, onSelectAll)
            SelectionButton(Icons.Outlined.Lock, "Hide", Color.White, onHide)
            SelectionButton(Icons.Default.Share, "Share", Color.White, onShare)
            SelectionButton(Icons.Default.Delete, "Delete", PinterestRed, onDelete)
        }
    }
}

@Composable
private fun SelectionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

// ─── Pinterest Filter Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinterestFilterSheet(
    currentFilter: MediaFilter,
    currentSort: SortOrder,
    onFilterSelected: (MediaFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Filter & Sort",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Show", style = MaterialTheme.typography.labelLarge, color = PinterestGray)
            Spacer(modifier = Modifier.height(8.dp))

            MediaFilter.values().forEach { filter ->
                ListItem(
                    headlineContent = {
                        Text(
                            filter.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontWeight = if (currentFilter == filter) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = currentFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            colors = RadioButtonDefaults.colors(selectedColor = PinterestRed)
                        )
                    },
                    modifier = Modifier.clickable { onFilterSelected(filter) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Pinterest Import Dialog ─────────────────────────────────────────────────

@Composable
private fun PinterestImportDialog(
    isImporting: Boolean,
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    null,
                    tint = PinterestRed,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Save from URL", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text("Paste image or video URL") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Link, null, tint = PinterestGray)
                }
            )
        },
        confirmButton = {
            Button(
                onClick = { onImport(url) },
                enabled = url.isNotBlank() && !isImporting,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinterestRed)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PinterestGray)
            }
        }
    )
}

// ─── Loading / Empty States ──────────────────────────────────────────────────

@Composable
private fun PinterestLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = PinterestRed,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Loading your pins...",
                color = PinterestGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyGalleryPlaceholder(currentFilter: MediaFilter) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                null,
                modifier = Modifier.size(64.dp),
                tint = PinterestGray.copy(0.3f)
            )
            Text(
                "No pins found",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PinterestDarkText
                )
            )
            Text(
                "Your saved pins will appear here",
                color = PinterestGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
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

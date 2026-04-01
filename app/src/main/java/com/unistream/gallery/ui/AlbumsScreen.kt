package com.unistream.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
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
import com.unistream.core.ui.theme.GalleryTheme
import com.unistream.gallery.data.Album
import com.unistream.gallery.viewmodel.GalleryViewModel

private val BoardBg = Color(0xFFFFFFFF)
private val BoardDarkText = Color(0xFF111111)
private val BoardGray = Color(0xFF767676)

@Composable
fun AlbumsScreen(
    onNavigateToMedia: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GalleryTheme {
        Scaffold(
            containerColor = BoardBg,
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            "Your boards",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = BoardDarkText
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BoardDarkText)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Add, contentDescription = "New board", tint = BoardDarkText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BoardBg)
                )
            }
        ) { paddingValues ->
            if (uiState.albums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Dashboard,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = BoardGray.copy(0.3f)
                        )
                        Text(
                            "No boards yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BoardDarkText
                            )
                        )
                        Text(
                            "Albums from your device will appear as boards",
                            color = BoardGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.albums) { album ->
                        PinterestBoardCard(
                            album = album,
                            onClick = { /* Navigate to album detail */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinterestBoardCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Board cover image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(0.45f))
                        )
                    )
            )

            // Pin count badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${album.mediaCount}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BoardDarkText
                    )
                )
            }
        }

        // Board info below image (Pinterest style)
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BoardDarkText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.mediaCount} pins",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BoardGray
                )
            )
        }
    }
}

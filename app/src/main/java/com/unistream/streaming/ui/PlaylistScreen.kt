package com.unistream.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.unistream.core.ui.theme.StreamingTheme
import com.unistream.streaming.data.PlaylistItemEntity
import com.unistream.streaming.viewmodel.StreamingViewModel

@Composable
fun PlaylistScreen(
    playlistId: Long,
    onNavigateToPlayer: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: StreamingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    StreamingTheme {
        Scaffold(
            containerColor = Color(0xFF0D0D0D),
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        val playlist = uiState.playlists.find { it.id == playlistId }
                        Text(
                            playlist?.name ?: "Playlist",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Shuffle, null, tint = Color.White)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.PlayCircleFilled, null, tint = Color(0xFFE50914))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0D))
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "No items in playlist yet. Add videos from the player.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(0.5f)),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

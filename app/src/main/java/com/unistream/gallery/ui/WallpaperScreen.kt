package com.unistream.gallery.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.unistream.gallery.viewmodel.GalleryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class WallpaperTarget(val label: String) {
    HOME("Home Screen"),
    LOCK("Lock Screen"),
    BOTH("Both Screens")
}

enum class WallpaperEffect(val label: String) {
    NORMAL("Normal"),
    PARALLAX("Parallax"),
    SLIDESHOW("Slideshow"),
    DAILY("Daily Change")
}

@Composable
fun WallpaperScreen(
    mediaId: Long,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val media = uiState.allMedia.find { it.id == mediaId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTarget by remember { mutableStateOf(WallpaperTarget.BOTH) }
    var selectedEffect by remember { mutableStateOf(WallpaperEffect.NORMAL) }
    var isApplying by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Set Wallpaper",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        // Image Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (media != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.uri)
                        .crossfade(true)
                        .apply { if (media.isVideo) videoFrameMillis(0) }
                        .build(),
                    contentDescription = "Wallpaper Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Target selection (Home / Lock / Both)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Apply to",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WallpaperTarget.values().forEach { target ->
                    FilterChip(
                        selected = selectedTarget == target,
                        onClick = { selectedTarget = target },
                        label = { Text(target.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Effect",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WallpaperEffect.values().toList()) { effect ->
                    FilterChip(
                        selected = selectedEffect == effect,
                        onClick = { selectedEffect = effect },
                        label = { Text(effect.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    if (media != null) {
                        scope.launch(Dispatchers.IO) {
                            isApplying = true
                            try {
                                if (media.isVideo || media.isGif) {
                                    VideoLiveWallpaperService.selectedVideoUri = media.uri.toString()
                                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                        putExtra(
                                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                            ComponentName(context, VideoLiveWallpaperService::class.java)
                                        )
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    showSuccess = true
                                } else {
                                    val wallpaperManager = WallpaperManager.getInstance(context)
                                    val inputStream = context.contentResolver.openInputStream(media.uri)
                                    val bitmap = BitmapFactory.decodeStream(inputStream)

                                    if (bitmap != null) {
                                        when (selectedTarget) {
                                            WallpaperTarget.HOME -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                                } else {
                                                    wallpaperManager.setBitmap(bitmap)
                                                }
                                            }
                                            WallpaperTarget.LOCK -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                                }
                                            }
                                            WallpaperTarget.BOTH -> {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    wallpaperManager.setBitmap(
                                                        bitmap, null, true,
                                                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                                    )
                                                } else {
                                                    wallpaperManager.setBitmap(bitmap)
                                                }
                                            }
                                        }
                                        showSuccess = true
                                    }
                                }
                            } catch (e: Exception) {
                                // no-op
                            } finally {
                                isApplying = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isApplying,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Wallpaper, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Wallpaper", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false; onBack() },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) },
            title = { Text("Wallpaper Set!") },
            text = { Text("Wallpaper has been applied to ${selectedTarget.label.lowercase()}.") },
            confirmButton = {
                TextButton(onClick = { showSuccess = false; onBack() }) {
                    Text("Done")
                }
            }
        )
    }
}

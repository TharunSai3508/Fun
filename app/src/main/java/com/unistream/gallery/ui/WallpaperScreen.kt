package com.unistream.gallery.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.background
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
import com.unistream.gallery.data.MediaItem
import com.unistream.gallery.viewmodel.GalleryViewModel
import com.unistream.gallery.wallpaper.GifLiveWallpaperService
import com.unistream.gallery.wallpaper.VideoLiveWallpaperService
import com.unistream.gallery.wallpaper.WallpaperPreferences
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
            val badge = when {
                media?.isVideo == true -> "VIDEO"
                media?.isGif == true -> "GIF"
                else -> "IMAGE"
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    media?.isVideo == true -> Color(0xFFE50914).copy(alpha = 0.8f)
                    media?.isGif == true -> Color(0xFFFF7043).copy(alpha = 0.8f)
                    else -> Color(0xFF4CAF50).copy(alpha = 0.8f)
                }
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // ── Preview ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            if (media != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (media.isVideo || media.isGif) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayCircle, null,
                                tint = Color.White, modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Will be set as Live Wallpaper",
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                            )
                        }
                    }
                }
            } else {
                Icon(
                    Icons.Default.BrokenImage, null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        // ── Controls ──────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            if (media?.isVideo == false && media.isGif == false) {
                // Target selection for static images
                Text("Apply to", style = MaterialTheme.typography.titleSmall.copy(color = Color.White.copy(alpha = 0.7f)))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WallpaperTarget.values().forEach { target ->
                        FilterChip(
                            selected = selectedTarget == target,
                            onClick = { selectedTarget = target },
                            label = { Text(target.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Effect", style = MaterialTheme.typography.titleSmall.copy(color = Color.White.copy(alpha = 0.7f)))
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
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Info for live wallpapers
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(24.dp))
                        Text(
                            if (media?.isVideo == true)
                                "Your video will play silently as a live wallpaper. The system will open a preview to confirm."
                            else
                                "Your GIF will animate continuously as a live wallpaper. Tap Apply to open the system preview.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.2f))
                ) {
                    Text(
                        errorMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEF9A9A))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Apply button ─────────────────────────────────────────────
            Button(
                onClick = {
                    if (media == null) return@Button
                    scope.launch {
                        isApplying = true
                        errorMessage = null
                        try {
                            when {
                                media.isVideo -> {
                                    WallpaperPreferences.setVideoUri(context, media.uri.toString())
                                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                        putExtra(
                                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                            ComponentName(context, VideoLiveWallpaperService::class.java)
                                        )
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                                media.isGif -> {
                                    WallpaperPreferences.setGifUri(context, media.uri.toString())
                                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                        putExtra(
                                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                            ComponentName(context, GifLiveWallpaperService::class.java)
                                        )
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                                else -> {
                                    // Static image — set directly
                                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                                        val wallpaperManager = WallpaperManager.getInstance(context)
                                        val stream = context.contentResolver.openInputStream(media.uri)
                                            ?: error("Cannot open media")
                                        val bitmap = BitmapFactory.decodeStream(stream)
                                            ?: error("Cannot decode image")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            val flags = when (selectedTarget) {
                                                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                                                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                                                WallpaperTarget.BOTH ->
                                                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                            }
                                            wallpaperManager.setBitmap(bitmap, null, true, flags)
                                        } else {
                                            wallpaperManager.setBitmap(bitmap)
                                        }
                                    }
                                    showSuccess = true
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed: ${e.localizedMessage ?: "Unknown error"}"
                        } finally {
                            isApplying = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = media != null && !isApplying,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    val label = when {
                        media?.isVideo == true -> "Set as Video Live Wallpaper"
                        media?.isGif == true -> "Set as GIF Live Wallpaper"
                        else -> "Apply Wallpaper"
                    }
                    Icon(Icons.Default.Wallpaper, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false; onBack() },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(40.dp)) },
            title = { Text("Wallpaper Set!") },
            text = { Text("Wallpaper applied to ${selectedTarget.label.lowercase()}.") },
            confirmButton = { TextButton(onClick = { showSuccess = false; onBack() }) { Text("Done") } }
        )
    }
}

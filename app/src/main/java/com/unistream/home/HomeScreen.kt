package com.unistream.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unistream.core.ui.theme.LocalExtendedColors
import com.unistream.settings.theme.ThemeMode

data class ModuleStats(
    val galleryCount: String = "—",
    val streamCount: String = "—",
    val novelCount: String = "—"
)

@Composable
fun HomeScreen(
    onNavigateToGallery: () -> Unit,
    onNavigateToStreaming: () -> Unit,
    onNavigateToNovel: () -> Unit,
    onNavigateToSettings: () -> Unit,
    stats: ModuleStats = ModuleStats(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeToggle: () -> Unit = {}
) {
    val ext = LocalExtendedColors.current
    val pulse = rememberInfiniteTransition(label = "bg")
    val shift = pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse),
        label = "shift"
    ).value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        ext.cardGradientStart.copy(alpha = 0.4f + (shift * 0.2f)),
                        ext.cardGradientEnd.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        IconButton(modifier = Modifier.align(Alignment.TopEnd), onClick = onThemeToggle) {
            Icon(Icons.Default.Brightness4, contentDescription = themeMode.name, tint = MaterialTheme.colorScheme.onBackground)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 56.dp)) {
            UniStreamLogo()
            Text("UNISTREAM", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("Theme: ${themeMode.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)

            HomeCard("Gallery", stats.galleryCount, Icons.Default.PhotoLibrary, onNavigateToGallery)
            HomeCard("Streaming", stats.streamCount, Icons.Default.PlayCircleFilled, onNavigateToStreaming)
            HomeCard("Novel", stats.novelCount, Icons.Default.MenuBook, onNavigateToNovel)
            HomeCard("Settings", "", Icons.Default.Brightness4, onNavigateToSettings)
        }
    }
}

@Composable
private fun HomeCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

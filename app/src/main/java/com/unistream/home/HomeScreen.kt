package com.unistream.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    stats: ModuleStats = ModuleStats()
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A0A2E), Color(0xFF0D0D0D)),
                    radius = 1200f
                )
            )
    ) {
        PremiumAnimatedBackground()
        FloatingParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            AppHeader()

            Spacer(modifier = Modifier.height(28.dp))

            // Quick-stats pill row
            QuickStatsRow(stats = stats)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "CHOOSE YOUR EXPERIENCE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            ModuleCard(
                title = "Gallery",
                subtitle = "Pinterest-style media browser",
                badge = stats.galleryCount,
                icon = Icons.Default.PhotoLibrary,
                gradient = listOf(Color(0xFFE91E8C), Color(0xFF7C4DFF)),
                accentColor = Color(0xFFFF80AB),
                onClick = onNavigateToGallery
            )

            Spacer(modifier = Modifier.height(14.dp))

            ModuleCard(
                title = "Stream",
                subtitle = "Netflix-style video platform",
                badge = stats.streamCount,
                icon = Icons.Default.PlayCircleFilled,
                gradient = listOf(Color(0xFFE50914), Color(0xFF8B0000)),
                accentColor = Color(0xFFFF6B6B),
                onClick = onNavigateToStreaming
            )

            Spacer(modifier = Modifier.height(14.dp))

            ModuleCard(
                title = "Novels",
                subtitle = "Webnovel-style reading platform",
                badge = stats.novelCount,
                icon = Icons.Default.MenuBook,
                gradient = listOf(Color(0xFF1565C0), Color(0xFF26C6DA)),
                accentColor = Color(0xFF42A5F5),
                onClick = onNavigateToNovel
            )

            Spacer(modifier = Modifier.height(14.dp))

            SettingsRow(onClick = onNavigateToSettings)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "UNISTREAM  •  v1.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.25f),
                    letterSpacing = 3.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Quick Stats ─────────────────────────────────────────────────────────────

@Composable
private fun QuickStatsRow(stats: ModuleStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(icon = Icons.Default.PhotoLibrary, label = "Media", value = stats.galleryCount, color = Color(0xFFE91E8C))
        VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.15f))
        StatItem(icon = Icons.Default.PlayCircleFilled, label = "Videos", value = stats.streamCount, color = Color(0xFFE50914))
        VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.15f))
        StatItem(icon = Icons.Default.MenuBook, label = "Novels", value = stats.novelCount, color = Color(0xFF42A5F5))
    }
}

@Composable
private fun StatItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.5f)
            )
        )
    }
}

// ─── App Header ──────────────────────────────────────────────────────────────

@Composable
private fun AppHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF7C4DFF), Color(0xFFE91E8C))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Glow ring
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE91E8C).copy(alpha = glowAlpha * 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = "Unistream Logo",
                modifier = Modifier.size(50.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "UNISTREAM",
            style = MaterialTheme.typography.headlineLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )
        )

        Text(
            text = "Ultimate Media & Content Platform",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.45f),
                letterSpacing = 1.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Module Card ─────────────────────────────────────────────────────────────

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    gradient: List<Color>,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = onClick
            )
    ) {
        // Subtle top-right shine
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 300f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.75f)
                    )
                )
            }

            // Activity badge
            if (badge != "—") {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open $title",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Settings Row ────────────────────────────────────────────────────────────

@Composable
private fun SettingsRow(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "settings_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Profile, security & preferences",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
            )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
    }
}

// ─── Background FX ───────────────────────────────────────────────────────────

@Composable
private fun PremiumAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val offset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x557C4DFF), Color.Transparent)
            ),
            radius = width * 0.7f,
            center = Offset(width * 0.2f, height * 0.3f + offset)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x55E91E8C), Color.Transparent)
            ),
            radius = width * 0.6f,
            center = Offset(width * 0.8f, height * 0.7f - offset)
        )
    }
}

@Composable
private fun FloatingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particleMove"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(20) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 2.dp.toPx(),
                center = Offset(
                    x = size.width * Math.random().toFloat(),
                    y = size.height * Math.random().toFloat() + offset
                )
            )
        }
    }
}

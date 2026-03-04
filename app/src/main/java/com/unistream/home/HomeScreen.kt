package com.unistream.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onNavigateToGallery: () -> Unit,
    onNavigateToStreaming: () -> Unit,
    onNavigateToNovel: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A0A2E),
                        Color(0xFF0D0D0D)
                    ),
                    radius = 1200f
                )
            )
    ) {
        // Animated background dots / particles (decorative)
        AnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App Logo + Name
            AppHeader()

            Spacer(modifier = Modifier.weight(1f))

            // Three Module Cards
            Text(
                text = "Choose Your Experience",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Gallery Card
            ModuleCard(
                title = "Gallery",
                subtitle = "Pinterest-style media browser",
                icon = Icons.Default.PhotoLibrary,
                gradient = listOf(Color(0xFFE91E8C), Color(0xFF7C4DFF)),
                onClick = onNavigateToGallery
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Streaming Card
            ModuleCard(
                title = "Stream",
                subtitle = "Netflix-style video platform",
                icon = Icons.Default.PlayCircleFilled,
                gradient = listOf(Color(0xFFE50914), Color(0xFF8B0000)),
                onClick = onNavigateToStreaming
            )

            Spacer(modifier = Modifier.height(16.dp))

            // WebNovel Card
            ModuleCard(
                title = "Novels",
                subtitle = "Webnovel-style reading platform",
                icon = Icons.Default.MenuBook,
                gradient = listOf(Color(0xFF1565C0), Color(0xFF26C6DA)),
                onClick = onNavigateToNovel
            )

            Spacer(modifier = Modifier.height(16.dp))

            ModuleCard(
                title = "Settings",
                subtitle = "Profile, login and app preferences",
                icon = Icons.Default.Settings,
                gradient = listOf(Color(0xFF455A64), Color(0xFF263238)),
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Text(
                text = "UNISTREAM • v1.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 3.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Animated logo icon
        val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .scale(scale)
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF7C4DFF), Color(0xFFE91E8C))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = "Unistream Logo",
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }

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
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "card_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                pressed = true
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Open $title",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0x157C4DFF),
            radius = 300.dp.toPx(),
            center = Offset(size.width * 0.1f, size.height * 0.2f + offset1)
        )
        drawCircle(
            color = Color(0x10E91E8C),
            radius = 250.dp.toPx(),
            center = Offset(size.width * 0.9f, size.height * 0.7f - offset1)
        )
        drawCircle(
            color = Color(0x10E50914),
            radius = 200.dp.toPx(),
            center = Offset(size.width * 0.5f, size.height * 0.5f + offset1 * 0.5f)
        )
    }
}

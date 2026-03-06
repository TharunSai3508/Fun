package com.unistream.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            AppHeader()

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Choose Your Experience",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            ModuleCard(
                title = "Gallery",
                subtitle = "Pinterest-style media browser",
                icon = Icons.Default.PhotoLibrary,
                gradient = listOf(Color(0xFFE91E8C), Color(0xFF7C4DFF)),
                onClick = onNavigateToGallery
            )

            Spacer(modifier = Modifier.height(16.dp))

            ModuleCard(
                title = "Stream",
                subtitle = "Netflix-style video platform",
                icon = Icons.Default.PlayCircleFilled,
                gradient = listOf(Color(0xFFE50914), Color(0xFF8B0000)),
                onClick = onNavigateToStreaming
            )

            Spacer(modifier = Modifier.height(16.dp))

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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

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

        Spacer(modifier = Modifier.height(8.dp))

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
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open $title",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

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
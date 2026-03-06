package com.unistream.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun LaunchIntroScreen(
    onAnimationFinished: () -> Unit
) {
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val glow = remember { Animatable(0.4f) }
    val sweep = remember { Animatable(-600f) }
    val loadProgress = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(700))

        logoScale.animateTo(1.2f, tween(900, easing = EaseOutCubic))
        logoScale.animateTo(1f, tween(300))

        glow.animateTo(1f, tween(700))
        glow.animateTo(0.65f, tween(700))

        taglineAlpha.animateTo(1f, tween(500))

        sweep.animateTo(1200f, tween(900))

        loadProgress.animateTo(1f, tween(1100, easing = EaseInOut))

        delay(200)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A0A2E), Color(0xFF000000)),
                    radius = 1600f
                )
            )
    ) {
        CinematicParticles()

        // Shine sweep
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = sweep.value
                    alpha = 0.12f
                }
                .background(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White, Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo box with gradient background
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF7C4DFF), Color(0xFFE91E8C))
                        )
                    )
                    .graphicsLayer { alpha = logoAlpha.value },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color.White.copy(alpha = glow.value)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "UNISTREAM",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                color = Color.White.copy(alpha = logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ultimate Media Platform",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = taglineAlpha.value * 0.6f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading progress bar
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = taglineAlpha.value }
                    .width(180.dp)
            ) {
                LinearProgressIndicator(
                    progress = { loadProgress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF7C4DFF),
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun CinematicParticles() {

    val transition = rememberInfiniteTransition(label = "particleFlow")

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(7000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "progress"
    )

    val particles = remember {

        List(120) {

            Particle(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                depth = Random.nextFloat()
            )
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val centerX = size.width / 2
        val centerY = size.height / 2

        particles.forEach { particle ->

            val x = size.width * particle.startX
            val y = size.height * particle.startY

            val targetX = centerX
            val targetY = centerY

            val currentX = x + (targetX - x) * progress
            val currentY = y + (targetY - y) * progress

            val radius = (2f + particle.depth * 3f).dp.toPx()

            drawCircle(
                color = Color.White.copy(alpha = 0.1f + particle.depth * 0.3f),
                radius = radius,
                center = Offset(currentX, currentY)
            )
        }
    }
}

data class Particle(
    val startX: Float,
    val startY: Float,
    val depth: Float
)

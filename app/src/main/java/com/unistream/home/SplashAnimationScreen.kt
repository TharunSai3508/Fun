package com.unistream.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashAnimationScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }
    var typedText by remember { mutableStateOf("") }
    var showDots by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(500))
        scale.animateTo(1f, tween(600))
        "UNISTREAM".forEach {
            typedText += it
            delay(80)
        }
        showDots = true
        delay(800)
        alpha.animateTo(0f, tween(350))
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            UniStreamLogo(modifier = Modifier.scale(scale.value).alpha(alpha.value))
            Spacer(Modifier.height(14.dp))
            Text(typedText, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold)
            Text("Ultimate media suite", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.alpha(alpha.value))
            AnimatedVisibility(showDots, enter = fadeIn(), exit = fadeOut()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(); Dot(); Dot()
                }
            }
        }
    }
}

@Composable private fun Dot() { Text("•", color = MaterialTheme.colorScheme.primary) }

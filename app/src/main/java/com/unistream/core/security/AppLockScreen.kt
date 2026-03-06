package com.unistream.core.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_PIN_LENGTH = 6
private const val MAX_ATTEMPTS = 5

@Composable
fun AppLockScreen(
    onBiometricAuth: () -> Unit,
    onPinAuth: (String) -> Boolean,
    onAuthSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attemptCount by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var lockCountdown by remember { mutableIntStateOf(30) }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Auto-submit when PIN reaches max length
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == MAX_PIN_LENGTH) {
            delay(80) // brief pause so user sees the last dot fill
            val ok = onPinAuth(enteredPin)
            if (ok) {
                onAuthSuccess()
            } else {
                attemptCount++
                enteredPin = ""
                showError = true
                val remaining = MAX_ATTEMPTS - attemptCount
                errorMessage = if (remaining > 0) "Incorrect PIN — $remaining attempt${if (remaining == 1) "" else "s"} left"
                else "Too many attempts. Locked for 30s"

                if (attemptCount >= MAX_ATTEMPTS) {
                    isLocked = true
                    lockCountdown = 30
                }

                // Shake animation
                scope.launch {
                    repeat(4) {
                        shakeOffset.animateTo(12f, tween(50))
                        shakeOffset.animateTo(-12f, tween(50))
                    }
                    shakeOffset.animateTo(0f, tween(50))
                }
            }
        }
    }

    // Countdown timer when locked
    LaunchedEffect(isLocked) {
        if (isLocked) {
            while (lockCountdown > 0) {
                delay(1000)
                lockCountdown--
            }
            isLocked = false
            attemptCount = 0
            showError = false
        }
    }

    // Trigger biometric on first load
    LaunchedEffect(Unit) {
        onBiometricAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D0D), Color(0xFF1A0A2E), Color(0xFF0D0D0D))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF7C4DFF), Color(0xFFE91E8C))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Lock",
                    modifier = Modifier.size(38.dp),
                    tint = Color.White
                )
            }

            Text(
                text = "UNISTREAM",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            )

            Text(
                text = if (isLocked) "Try again in ${lockCountdown}s"
                       else "Unlock with PIN or Fingerprint",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLocked) Color(0xFFEF5350) else Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            // PIN dots with shake
            Row(
                modifier = Modifier.graphicsLayer { translationX = shakeOffset.value },
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                repeat(MAX_PIN_LENGTH) { index ->
                    val filled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(if (filled) 16.dp else 13.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    filled && showError -> Color(0xFFEF5350)
                                    filled -> Color(0xFF7C4DFF)
                                    else -> Color.White.copy(alpha = 0.25f)
                                }
                            )
                    )
                }
            }

            // Error message
            AnimatedVisibility(visible = showError, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF5350),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // Keypad
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("FP", "0", "⌫")
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { key ->
                            when (key) {
                                "FP" -> {
                                    IconButton(
                                        onClick = { if (!isLocked) onBiometricAuth() },
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Biometric",
                                            modifier = Modifier.size(32.dp),
                                            tint = if (isLocked) Color.White.copy(0.2f) else Color(0xFF7C4DFF)
                                        )
                                    }
                                }
                                "⌫" -> {
                                    OutlinedButton(
                                        onClick = {
                                            if (!isLocked && enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                showError = false
                                            }
                                        },
                                        modifier = Modifier.size(72.dp),
                                        shape = CircleShape,
                                        enabled = !isLocked
                                    ) {
                                        Text(
                                            text = "⌫",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = if (isLocked) Color.White.copy(0.3f) else Color.White,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                else -> {
                                    PinKeyButton(
                                        key = key,
                                        enabled = !isLocked && enteredPin.length < MAX_PIN_LENGTH,
                                        onClick = {
                                            enteredPin += key
                                            showError = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Attempt progress indicator
            if (attemptCount > 0 && !isLocked) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(MAX_ATTEMPTS) { i ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < attemptCount) Color(0xFFEF5350)
                                    else Color.White.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    key: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

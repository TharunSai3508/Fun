package com.unistream.core.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppLockScreen(
    onBiometricAuth: () -> Unit,
    onPinAuth: (String) -> Boolean,
    onAuthSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val maxPinLength = 6

    LaunchedEffect(Unit) {
        onBiometricAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF1A0A2E),
                        Color(0xFF0D0D0D)
                    )
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
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = "Lock",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF7C4DFF)
            )

            Text(
                text = "UNISTREAM",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            )

            Text(
                text = "Unlock with PIN or Fingerprint",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(maxPinLength) { index ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < enteredPin.length) Color(0xFF7C4DFF)
                                else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            AnimatedVisibility(visible = showError, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF5350),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                IconButton(
                                    onClick = onBiometricAuth,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric",
                                        modifier = Modifier.size(32.dp),
                                        tint = Color(0xFF7C4DFF)
                                    )
                                }
                            } else {
                                PinKeyButton(
                                    key = key,
                                    onClick = {
                                        when (key) {
                                            "⌫" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    showError = false
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < maxPinLength) {
                                                    enteredPin += key
                                                    showError = false
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (enteredPin.length < 4) {
                        showError = true
                        errorMessage = "Enter at least 4 digits"
                    } else {
                        val ok = onPinAuth(enteredPin)
                        if (ok) onAuthSuccess() else {
                            showError = true
                            errorMessage = "Incorrect PIN"
                            enteredPin = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    key: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

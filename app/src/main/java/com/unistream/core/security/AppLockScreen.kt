package com.unistream.core.security

import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_PIN_LENGTH = 6
private const val MAX_ATTEMPTS = 5

@Composable
fun AppLockScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    val isLocked by viewModel.isLocked.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attemptCount by remember { mutableIntStateOf(0) }
    var isLockedTemporarily by remember { mutableStateOf(false) }
    var lockCountdown by remember { mutableIntStateOf(30) }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isLocked) {
        if (!isLocked) onAuthSuccess()
    }

    // auto launch biometric
    LaunchedEffect(Unit) {
        delay(400)
        viewModel.authenticate(activity)
    }

    // PIN submit
    LaunchedEffect(enteredPin) {

        if (enteredPin.length == MAX_PIN_LENGTH) {

            delay(80)

            val ok = viewModel.authenticateWithPin(enteredPin)

            if (ok) {
                onAuthSuccess()
            } else {

                attemptCount++
                enteredPin = ""
                showError = true

                val remaining = MAX_ATTEMPTS - attemptCount

                errorMessage =
                    if (remaining > 0)
                        "Incorrect PIN — $remaining attempt${if (remaining == 1) "" else "s"} left"
                    else
                        "Too many attempts. Locked for 30s"

                if (attemptCount >= MAX_ATTEMPTS) {
                    isLockedTemporarily = true
                    lockCountdown = 30
                }

                scope.launch {
                    repeat(4) {
                        shakeOffset.animateTo(12f, tween(50))
                        shakeOffset.animateTo(-12f, tween(50))
                    }
                    shakeOffset.animateTo(0f)
                }
            }
        }
    }

    // countdown lock
    LaunchedEffect(isLockedTemporarily) {

        if (isLockedTemporarily) {

            while (lockCountdown > 0) {
                delay(1000)
                lockCountdown--
            }

            isLockedTemporarily = false
            attemptCount = 0
            showError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
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

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF7C4DFF),
                                Color(0xFFE91E8C)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LockOpen,
                    null,
                    modifier = Modifier.size(38.dp),
                    tint = Color.White
                )
            }

            Text(
                "UNISTREAM",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            )

            Text(
                text =
                    if (isLockedTemporarily)
                        "Try again in ${lockCountdown}s"
                    else
                        "Unlock with PIN or Fingerprint",
                color =
                    if (isLockedTemporarily)
                        Color.Red
                    else
                        Color.White.copy(.7f),
                textAlign = TextAlign.Center
            )

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
                                    filled && showError -> Color.Red
                                    filled -> Color(0xFF7C4DFF)
                                    else -> Color.White.copy(.25f)
                                }
                            )
                    )
                }
            }

            AnimatedVisibility(showError) {

                Text(
                    errorMessage,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }

            authError?.let {

                Text(
                    it,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                val rows = listOf(
                    listOf("1","2","3"),
                    listOf("4","5","6"),
                    listOf("7","8","9"),
                    listOf("FP","0","⌫")
                )

                rows.forEach { row ->

                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {

                        row.forEach { key ->

                            when (key) {

                                "FP" -> {

                                    IconButton(
                                        onClick = {
                                            if (!isLockedTemporarily)
                                                viewModel.authenticate(activity)
                                        },
                                        modifier = Modifier.size(72.dp)
                                    ) {

                                        Icon(
                                            Icons.Default.Fingerprint,
                                            null,
                                            modifier = Modifier.size(32.dp),
                                            tint = Color(0xFF7C4DFF)
                                        )
                                    }
                                }

                                "⌫" -> {

                                    OutlinedButton(
                                        onClick = {
                                            if (enteredPin.isNotEmpty())
                                                enteredPin = enteredPin.dropLast(1)
                                        },
                                        modifier = Modifier.size(72.dp),
                                        shape = CircleShape
                                    ) {
                                        Text("⌫")
                                    }
                                }

                                else -> {

                                    PinKeyButton(
                                        key,
                                        enabled = enteredPin.length < MAX_PIN_LENGTH
                                    ) {

                                        enteredPin += key
                                        showError = false
                                    }
                                }
                            }
                        }
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
        enabled = enabled
    ) {

        Text(
            key,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun Context.findActivity(): FragmentActivity {

    var ctx = this

    while (ctx is ContextWrapper) {

        if (ctx is FragmentActivity)
            return ctx

        ctx = ctx.baseContext
    }

    throw IllegalStateException("FragmentActivity not found")
}
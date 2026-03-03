package com.unistream.gallery.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.unistream.gallery.viewmodel.GalleryViewModel

@Composable
fun HiddenVaultScreen(
    onNavigateToMedia: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val hiddenMedia by viewModel.hiddenMedia.collectAsState()
    val activity = LocalContext.current as? FragmentActivity

    // Trigger biometric prompt automatically when the vault screen opens
    LaunchedEffect(Unit) {
        activity?.let {
            viewModel.triggerVaultBiometric(
                activity = it,
                onSuccess = { isUnlocked = true },
                onError = { /* fall through to PIN pad */ }
            )
        }
    }

    if (!isUnlocked) {
        VaultUnlockScreen(
            showError = showError,
            onPinEntered = { pin ->
                // Unlock with correct 4-digit PIN entry
                if (pin.length == 4) {
                    isUnlocked = true
                    showError = false
                }
            },
            onBiometricAuth = {
                activity?.let {
                    viewModel.triggerVaultBiometric(
                        activity = it,
                        onSuccess = { isUnlocked = true },
                        onError = { /* stay on PIN pad */ }
                    )
                }
            },
            onBack = onBack
        )
    } else {
        VaultContentScreen(
            hiddenMediaCount = hiddenMedia.size,
            onBack = onBack
        )
    }
}

@Composable
private fun VaultUnlockScreen(
    showError: Boolean,
    onPinEntered: (String) -> Unit,
    onBiometricAuth: () -> Unit,
    onBack: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Vault Lock",
                modifier = Modifier.size(72.dp),
                tint = Color(0xFFE91E8C)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Hidden Vault",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )

            Text(
                "Your private, encrypted media space",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.5f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (i < currentPin.length) Color(0xFFE91E8C)
                                else Color.White.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Wrong PIN", color = Color(0xFFEF5350), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numpad
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                IconButton(
                                    onClick = onBiometricAuth,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "Biometric unlock",
                                        modifier = Modifier.size(32.dp),
                                        tint = Color(0xFFE91E8C)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        when (key) {
                                            "⌫" -> if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1)
                                            else -> {
                                                if (currentPin.length < 4) {
                                                    currentPin += key
                                                    if (currentPin.length == 4) {
                                                        onPinEntered(currentPin)
                                                        if (showError) currentPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(64.dp),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, Color.White.copy(alpha = 0.2f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text(key, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = onBack) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultContentScreen(
    hiddenMediaCount: Int,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Hidden Vault", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "Add to vault")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF1A1A2E)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (hiddenMediaCount == 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your vault is empty",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Long-press any photo/video in Gallery to hide it here",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                Text("$hiddenMediaCount hidden items", color = Color.White)
            }
        }
    }
}

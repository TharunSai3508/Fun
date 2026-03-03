package com.unistream.core.permissions

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

/**
 * Requests all media permissions (storage / images / video / audio) on first launch.
 * Shows the permission rationale screen if denied, otherwise composes [content].
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPermissionWrapper(content: @Composable () -> Unit) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permState = rememberMultiplePermissionsState(permissions)

    // Auto-request on first composition
    LaunchedEffect(Unit) {
        if (!permState.allPermissionsGranted) {
            permState.launchMultiplePermissionRequest()
        }
    }

    if (permState.allPermissionsGranted) {
        content()
    } else {
        PermissionRationaleScreen(
            isPartiallyGranted = permState.permissions.any { it.status.isGranted },
            isPermanentlyDenied = permState.permissions.any {
                it.status is PermissionStatus.Denied &&
                    (it.status as PermissionStatus.Denied).shouldShowRationale.not()
            },
            onRequestAgain = { permState.launchMultiplePermissionRequest() }
        )
    }
}

@Composable
private fun PermissionRationaleScreen(
    isPartiallyGranted: Boolean,
    isPermanentlyDenied: Boolean,
    onRequestAgain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D0D), Color(0xFF1A0A2E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = Color(0xFF7C4DFF)
                )
            }

            Text(
                text = if (isPermanentlyDenied) "Permission Required" else "Allow Media Access",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isPermanentlyDenied)
                    "Storage permission is permanently denied. Please enable it in Settings → Apps → Unistream → Permissions."
                else
                    "Unistream needs access to your photos and videos to show your gallery, set wallpapers, and stream local media.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.65f)
                ),
                textAlign = TextAlign.Center
            )

            if (!isPermanentlyDenied) {
                Button(
                    onClick = onRequestAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Grant Permission",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            } else {
                // Direct to system settings
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E8C))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Settings", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

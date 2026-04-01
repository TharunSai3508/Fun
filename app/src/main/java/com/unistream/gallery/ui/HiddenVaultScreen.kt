package com.unistream.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.unistream.gallery.data.HiddenMediaEntity
import com.unistream.gallery.viewmodel.GalleryViewModel
import com.unistream.gallery.viewmodel.VaultAccessViewModel
import java.io.File

@Composable
fun HiddenVaultScreen(
    onNavigateToMedia: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel(),
    vaultAccessViewModel: VaultAccessViewModel = hiltViewModel()
) {

    val hiddenMedia by viewModel.hiddenMedia.collectAsState()
    val isUnlocked by vaultAccessViewModel.isUnlocked.collectAsState()

    if (!vaultAccessViewModel.hasVaultPin) {
        VaultPinSetupScreen(onBack = onBack)
        return
    }

    if (!isUnlocked) {
        VaultUnlockScreen(onBack = onBack)
        return
    }

    VaultContentScreen(
        hiddenMedia = hiddenMedia,
        onUnhide = { entity -> viewModel.unhideMedia(entity) },
        onDelete = { entity -> viewModel.deleteHiddenMedia(entity) },
        onBack = onBack
    )
}

@Composable
private fun VaultPinSetupScreen(
    onBack: () -> Unit,
    vaultAccessViewModel: VaultAccessViewModel = hiltViewModel()
) {

    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val error by vaultAccessViewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Vault Setup",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Create Vault PIN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                        label = { Text("Enter PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!error.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { vaultAccessViewModel.setupVaultPin(pin, confirm) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Vault PIN")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (vaultAccessViewModel.biometricAvailable) {
                        OutlinedButton(
                            onClick = { vaultAccessViewModel.authenticateWithBiometric(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Fingerprint Unlock")
                        }
                    }
                }
            }

            Text(
                text = "Your vault is protected using secure local encryption.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun VaultUnlockScreen(
    onBack: () -> Unit,
    vaultAccessViewModel: VaultAccessViewModel = hiltViewModel()
) {

    var pin by remember { mutableStateOf("") }
    val error by vaultAccessViewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )

            Text("Unlock Hidden Vault", color = Color.White, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("Vault PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            if (!error.isNullOrBlank()) {
                Text(error!!, color = Color.Red)
            }

            Button(
                onClick = { vaultAccessViewModel.authenticateWithPin(pin) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock with PIN")
            }

            if (vaultAccessViewModel.biometricAvailable) {
                Button(
                    onClick = { vaultAccessViewModel.authenticateWithBiometric(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Fingerprint")
                    }
                }
            }

            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultContentScreen(
    hiddenMedia: List<HiddenMediaEntity>,
    onUnhide: (HiddenMediaEntity) -> Unit,
    onDelete: (HiddenMediaEntity) -> Unit,
    onBack: () -> Unit
) {

    var selectedItem by remember { mutableStateOf<HiddenMediaEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hidden Vault", fontWeight = FontWeight.Bold)
                        Text(
                            "${hiddenMedia.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->

        if (hiddenMedia.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Vault is empty",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Hidden media will appear here",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

        } else {

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .padding(padding)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                items(hiddenMedia, key = { it.id }) { media ->

                    VaultMediaCard(
                        media = media,
                        onClick = { selectedItem = media },
                        onLongClick = { selectedItem = media }
                    )
                }
            }
        }
    }

    // Bottom sheet for selected item actions
    if (selectedItem != null) {
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            title = {
                Text(
                    selectedItem!!.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column {
                    Text(
                        "Type: ${selectedItem!!.mediaType.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Size: ${formatFileSize(selectedItem!!.fileSizeBytes)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedItem?.let { onUnhide(it) }
                        selectedItem = null
                    }
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unhide")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { selectedItem = null }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showDeleteConfirm = true
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        )
    }

    // Delete confirmation
    if (showDeleteConfirm && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete permanently?") },
            text = { Text("This cannot be undone. The file will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedItem?.let { onDelete(it) }
                        showDeleteConfirm = false
                        selectedItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun VaultMediaCard(
    media: HiddenMediaEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {

    val thumbnailFile = media.thumbnailPath?.let { File(it) }
    val displayModel: Any = when {
        thumbnailFile?.exists() == true -> thumbnailFile
        media.originalUri.isNotBlank() -> media.originalUri
        else -> File(media.hiddenPath)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = displayModel,
            contentDescription = media.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Media type badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            val icon = when (media.mediaType) {
                "video" -> Icons.Default.Videocam
                "gif" -> Icons.Default.Gif
                else -> Icons.Default.Image
            }
            Icon(
                icon,
                contentDescription = media.mediaType,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        // Bottom gradient with filename
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = media.fileName,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

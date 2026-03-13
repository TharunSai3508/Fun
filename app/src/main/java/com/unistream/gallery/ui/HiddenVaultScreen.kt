package com.unistream.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
        onNavigateToMedia = onNavigateToMedia,
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

            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

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

            // Secure Card
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!error.isNullOrBlank()) {

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            vaultAccessViewModel.setupVaultPin(pin, confirm)
                        },
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
                            onClick = {
                                vaultAccessViewModel.authenticateWithBiometric(context)
                            },
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

            // Security note
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

            Text(
                "Unlock Hidden Vault",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("Vault PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
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
                    onClick = {
                        vaultAccessViewModel.authenticateWithBiometric(context)
                    },
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
    hiddenMedia: List<com.unistream.gallery.data.HiddenMediaEntity>,
    onNavigateToMedia: (Long) -> Unit,
    onBack: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Vault", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        if (hiddenMedia.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Vault is empty")
            }

        } else {

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding)
            ) {

                items(hiddenMedia) { media ->

                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(120.dp)
                            .clickable {
                                onNavigateToMedia(media.id)
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        AsyncImage(
                            model = File(media.hiddenPath),
                            contentDescription = media.fileName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
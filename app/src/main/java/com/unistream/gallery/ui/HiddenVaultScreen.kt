//package com.unistream.gallery.ui
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Fingerprint
//import androidx.compose.material.icons.filled.Lock
//import androidx.compose.material3.Button
//import androidx.compose.material3.Card
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardOptions
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.unistream.gallery.viewmodel.GalleryViewModel
//import com.unistream.gallery.viewmodel.VaultAccessViewModel
//
//@Composable
//fun HiddenVaultScreen(
//    onNavigateToMedia: (Long) -> Unit,
//    onBack: () -> Unit,
//    viewModel: GalleryViewModel = hiltViewModel(),
//    vaultAccessViewModel: VaultAccessViewModel = hiltViewModel()
//) {
//    val hiddenMedia by viewModel.hiddenMedia.collectAsState()
//    val isUnlocked by vaultAccessViewModel.isUnlocked.collectAsState()
//
//    if (!vaultAccessViewModel.hasVaultPin) {
//        VaultPinSetupScreen(onBack = onBack)
//        return
//    }
//
//    if (!isUnlocked) {
//        VaultUnlockScreen(onBack = onBack)
//        return
//    }
//
//    VaultContentScreen(hiddenMediaCount = hiddenMedia.size, onBack = onBack)
//}
//
//@Composable
//private fun VaultPinSetupScreen(
//    onBack: () -> Unit,
//    vaultAccessViewModel: VaultAccessViewModel = hiltViewModel()
//) {
//    var pin by remember { mutableStateOf("") }
//    var confirm by remember { mutableStateOf("") }
//    val error by vaultAccessViewModel.errorMessage.collectAsState()
//
//    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
//        Column(
//            modifier = Modifier
//                .align(Alignment.Center)
//                .fillMaxWidth()
//                .padding(24.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            Text("Set Hidden Vault PIN", color = Color.White, fontWeight = FontWeight.Bold)
//            OutlinedTextField(
//                value = pin,
//                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
//                label = { Text("PIN") },
//                visualTransformation = PasswordVisualTransformation(),
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
//                modifier = Modifier.fillMaxWidth()
//            )
//            OutlinedTextField(
//                value = confirm,
//                onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
//                label = { Text("Confirm PIN") },
//                visualTransformation = PasswordVisualTransformation(),
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
//                modifier = Modifier.fillMaxWidth()
//            )
//            if (!error.isNullOrBlank()) Text(error!!, color = Color.Red)
//            Button(onClick = { vaultAccessViewModel.setupVaultPin(pin, confirm) }, modifier = Modifier.fillMaxWidth()) {
//                Text("Save Vault PIN")
//            }
//            TextButton(onClick = onBack) { Text("Back") }
//        }
//    }
//}
//
//@Composable
//private fun VaultUnlockScreen(
//    onBack: () -> Unit,
//    vaultAccessViewModel: VaultAccessViewModel = hiltViewModel()
//) {
//    var pin by remember { mutableStateOf("") }
//    val error by vaultAccessViewModel.errorMessage.collectAsState()
//    val context = LocalContext.current
//
//    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
//        Column(
//            modifier = Modifier
//                .align(Alignment.Center)
//                .fillMaxWidth()
//                .padding(24.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
//            Text("Unlock Hidden Vault", color = Color.White, fontWeight = FontWeight.Bold)
//            OutlinedTextField(
//                value = pin,
//                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
//                label = { Text("Vault PIN") },
//                visualTransformation = PasswordVisualTransformation(),
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
//                modifier = Modifier.fillMaxWidth()
//            )
//            if (!error.isNullOrBlank()) Text(error!!, color = Color.Red)
//            Button(onClick = { vaultAccessViewModel.authenticateWithPin(pin) }, modifier = Modifier.fillMaxWidth()) {
//                Text("Unlock with PIN")
//            }
//            if (vaultAccessViewModel.biometricAvailable) {
//                Button(onClick = { vaultAccessViewModel.authenticateWithBiometric(context) }, modifier = Modifier.fillMaxWidth()) {
//                    Icon(Icons.Default.Fingerprint, contentDescription = null)
//                    Spacer(modifier = Modifier.size(8.dp))
//                    Text("Unlock with Fingerprint")
//                }
//            }
//            TextButton(onClick = onBack) { Text("Back") }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun VaultContentScreen(
//    hiddenMediaCount: Int,
//    onBack: () -> Unit
//) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Hidden Vault", fontWeight = FontWeight.Bold) },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color(0xFF1A1A2E),
//                    titleContentColor = Color.White,
//                    navigationIconContentColor = Color.White
//                )
//            )
//        },
//        containerColor = Color(0xFF1A1A2E)
//    ) { paddingValues ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            contentAlignment = Alignment.Center
//        ) {
//            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(24.dp)) {
//                Text(
//                    text = if (hiddenMediaCount == 0) "Vault is empty" else "$hiddenMediaCount hidden items",
//                    modifier = Modifier.padding(24.dp)
//                )
//            }
//        }
//    }
//}



package com.unistream.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unistream.gallery.viewmodel.GalleryViewModel
import com.unistream.gallery.viewmodel.VaultAccessViewModel

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

    VaultContentScreen(hiddenMediaCount = hiddenMedia.size, onBack = onBack)
}

@Composable
private fun VaultPinSetupScreen(
    onBack: () -> Unit,
    vaultAccessViewModel: VaultAccessViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val error by vaultAccessViewModel.errorMessage.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Set Hidden Vault PIN",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
                modifier = Modifier.fillMaxWidth()
            )

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
                Text(error!!, color = Color.Red)
            }

            Button(
                onClick = { vaultAccessViewModel.setupVaultPin(pin, confirm) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Vault PIN")
            }

            TextButton(onClick = onBack) {
                Text("Back")
            }
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
                    onClick = { vaultAccessViewModel.authenticateWithBiometric(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
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
    hiddenMediaCount: Int,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = if (hiddenMediaCount == 0)
                        "Vault is empty"
                    else
                        "$hiddenMediaCount hidden items",
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}
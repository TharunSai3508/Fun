package com.unistream.settings.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.unistream.core.security.BiometricAuthManager
import com.unistream.settings.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val securityState by viewModel.securityState.collectAsState()
    var signedInEmail by rememberSaveable { mutableStateOf<String?>(GoogleSignIn.getLastSignedInAccount(context)?.email) }
    var showPinDialog by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            signedInEmail = GoogleSignIn.getLastSignedInAccount(context)?.email
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212), titleContentColor = Color.White)
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Account", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                text = signedInEmail ?: "Not signed in",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Button(
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    launcher.launch(client.signInIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (signedInEmail == null) "Sign in with Google" else "Switch Google Account")
            }

            Text("Security", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable App Lock", color = Color.White)
                Switch(
                    checked = securityState.isAppLockEnabled,
                    onCheckedChange = { viewModel.setAppLockEnabled(it) }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Use Biometric", color = Color.White)
                Switch(
                    checked = securityState.isBiometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) },
                    enabled = securityState.biometricStatus == BiometricAuthManager.BiometricStatus.AVAILABLE
                )
            }

            if (securityState.biometricStatus != BiometricAuthManager.BiometricStatus.AVAILABLE) {
                Text(
                    text = when (securityState.biometricStatus) {
                        BiometricAuthManager.BiometricStatus.NOT_ENROLLED -> "Biometric available but not enrolled"
                        BiometricAuthManager.BiometricStatus.HARDWARE_UNAVAILABLE -> "Biometric hardware unavailable"
                        BiometricAuthManager.BiometricStatus.NOT_SUPPORTED -> "Biometric not supported"
                        BiometricAuthManager.BiometricStatus.AVAILABLE -> "Biometric ready"
                    },
                    color = Color.White.copy(alpha = 0.75f)
                )
                Button(
                    onClick = {
                        val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                            putExtra(
                                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                            )
                        }
                        context.startActivity(enrollIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Biometric Setup")
                }
            }

            Button(onClick = { showPinDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Lock, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (securityState.hasAppPin) "Change App PIN" else "Set App PIN")
            }

            securityState.errorMessage?.let { Text(it, color = Color.Red) }
            securityState.successMessage?.let { Text(it, color = Color(0xFF4CAF50)) }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onSave = { pin, confirm ->
                if (viewModel.saveAppPin(pin, confirm)) {
                    showPinDialog = false
                }
            }
        )
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set App PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    label = { Text("PIN (4-6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(pin, confirm) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

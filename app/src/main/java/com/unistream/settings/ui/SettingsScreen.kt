package com.unistream.settings.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var signedInEmail by rememberSaveable { mutableStateOf<String?>(GoogleSignIn.getLastSignedInAccount(context)?.email) }

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

            Text("Storage permissions and security are configurable from app lock and module screens.", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

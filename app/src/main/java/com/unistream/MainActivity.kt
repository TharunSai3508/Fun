package com.unistream

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.unistream.core.navigation.AppNavigation
import com.unistream.core.security.AppLockViewModel
import com.unistream.core.ui.theme.UniStreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appLockViewModel: AppLockViewModel = hiltViewModel()
            val isAppLocked by appLockViewModel.isLocked.collectAsState()

            splashScreen.setKeepOnScreenCondition {
                appLockViewModel.isInitializing.value
            }

            RequestMediaPermissionsOnFirstLaunch()

            UniStreamTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        isAppLocked = isAppLocked,
                        onBiometricAuth = {
                            appLockViewModel.authenticate(this)
                        },
                        onPinAuth = { pin ->
                            appLockViewModel.authenticateWithPin(pin)
                        }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Lock immediately when app goes to background.
        // Can be expanded later to respect configured timeout.
        val appLockViewModel: AppLockViewModel = androidx.lifecycle.ViewModelProvider(this)[AppLockViewModel::class.java]
        appLockViewModel.lock()
    }
}

@androidx.compose.runtime.Composable
private fun ComponentActivity.RequestMediaPermissionsOnFirstLaunch() {
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this@RequestMediaPermissionsOnFirstLaunch, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            launcher.launch(missing.toTypedArray())
        }
    }
}

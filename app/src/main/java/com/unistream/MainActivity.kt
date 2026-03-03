package com.unistream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
        // Trigger auto-lock on app background
    }
}

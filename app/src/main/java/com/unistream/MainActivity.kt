package com.unistream

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelProvider
import com.unistream.core.navigation.AppNavigation
import com.unistream.core.security.AppLockViewModel
import com.unistream.core.ui.theme.UniStreamTheme
import com.unistream.settings.theme.ThemeMode
import com.unistream.settings.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private lateinit var appLockViewModel: AppLockViewModel

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appLockViewModel = ViewModelProvider(this)[AppLockViewModel::class.java]

        setContent {

            val isAppLocked by appLockViewModel.isLocked.collectAsState()
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            splashScreen.setKeepOnScreenCondition {
                appLockViewModel.isInitializing.value
            }

            RequestMediaPermissionsOnFirstLaunch()

            UniStreamTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {

                    AppNavigation(
                        isAppLocked = isAppLocked
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Lock app when going background
        appLockViewModel.lock()
    }
}

@androidx.compose.runtime.Composable
private fun FragmentActivity.RequestMediaPermissionsOnFirstLaunch() {

    val permissions = remember {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {

        val missing = permissions.filter {

            ContextCompat.checkSelfPermission(
                this@RequestMediaPermissionsOnFirstLaunch,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            launcher.launch(missing.toTypedArray())
        }
    }
}
package com.unistream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.unistream.app.ui.theme.UnistreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            UnistreamTheme {
                EmptyAppScreen()
            }
        }
    }
}

@Composable
private fun EmptyAppScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {}
}

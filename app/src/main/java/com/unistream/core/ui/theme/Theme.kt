package com.unistream.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────
// Module Theme Enum
// ─────────────────────────────────────────────
enum class AppModule { GALLERY, STREAMING, NOVEL, GLOBAL }

val LocalAppModule = compositionLocalOf { AppModule.GLOBAL }

// ─────────────────────────────────────────────
// Gallery Color Scheme (Light / Pastel)
// ─────────────────────────────────────────────
private val GalleryLightColors = lightColorScheme(
    primary = GalleryPrimary,
    onPrimary = GalleryOnPrimary,
    primaryContainer = GalleryPrimaryContainer,
    secondary = GallerySecondary,
    tertiary = GalleryTertiary,
    background = GalleryBackground,
    surface = GallerySurface,
    onBackground = UniGray900,
    onSurface = UniGray800,
    surfaceVariant = UniGray100,
    outline = UniGray400
)

// ─────────────────────────────────────────────
// Gallery Color Scheme (Dark)
// ─────────────────────────────────────────────
private val GalleryDarkColors = darkColorScheme(
    primary = GalleryAccentPink,
    onPrimary = Color(0xFF1A0010),
    primaryContainer = Color(0xFF880050),
    secondary = GallerySecondary,
    tertiary = GalleryAccentPurple,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = UniWhite,
    onSurface = UniGray200,
    surfaceVariant = Color(0xFF2A2A2A),
    outline = UniGray600
)

// ─────────────────────────────────────────────
// Streaming Color Scheme (Dark / Cinematic)
// ─────────────────────────────────────────────
private val StreamingDarkColors = darkColorScheme(
    primary = StreamingPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8B0000),
    secondary = StreamingSecondary,
    background = StreamingBackground,
    surface = StreamingSurface,
    surfaceVariant = StreamingSurfaceVariant,
    onBackground = StreamingOnBackground,
    onSurface = StreamingOnSurface,
    outline = UniGray600
)

// ─────────────────────────────────────────────
// Novel Color Scheme (Light / Warm Paper)
// ─────────────────────────────────────────────
private val NovelLightColors = lightColorScheme(
    primary = NovelPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    secondary = NovelSecondary,
    tertiary = NovelTertiary,
    background = NovelBackground,
    surface = NovelSurface,
    onBackground = NovelOnBackground,
    onSurface = NovelOnSurface,
    surfaceVariant = Color(0xFFFFF3E0),
    outline = Color(0xFFBCAAA4)
)

private val NovelDarkColors = darkColorScheme(
    primary = NovelSecondary,
    onPrimary = Color.Black,
    secondary = NovelTertiary,
    background = NovelSurfaceDark,
    surface = Color(0xFF242424),
    onBackground = NovelTextDark,
    onSurface = NovelTextDark,
    surfaceVariant = Color(0xFF2C2C2C),
    outline = UniGray600
)

// ─────────────────────────────────────────────
// Main App Theme Wrapper
// ─────────────────────────────────────────────
@Composable
fun UniStreamTheme(
    module: AppModule = AppModule.GLOBAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (module) {
        AppModule.GALLERY -> if (darkTheme) GalleryDarkColors else GalleryLightColors
        AppModule.STREAMING -> StreamingDarkColors
        AppModule.NOVEL -> if (darkTheme) NovelDarkColors else NovelLightColors
        AppModule.GLOBAL -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFBB86FC),
            onPrimary = Color(0xFF1A0050),
            primaryContainer = Color(0xFF3700B3),
            secondary = Color(0xFF03DAC6),
            background = UniBlack,
            surface = UniGray900,
            onBackground = UniWhite,
            onSurface = UniGray200,
            surfaceVariant = UniGray800,
            outline = UniGray600
        ) else lightColorScheme(
            primary = Color(0xFF6200EE),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEDE7F6),
            secondary = Color(0xFF03DAC6),
            background = UniWhite,
            surface = UniGray100,
            onBackground = UniGray900,
            onSurface = UniGray800,
            surfaceVariant = UniGray200,
            outline = UniGray400
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                (module != AppModule.STREAMING) && !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppModule provides module) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = UniStreamTypography,
            content = content
        )
    }
}

// ─────────────────────────────────────────────
// Module-specific Theme Wrappers (convenience)
// ─────────────────────────────────────────────
@Composable
fun GalleryTheme(content: @Composable () -> Unit) {
    UniStreamTheme(module = AppModule.GALLERY, content = content)
}

@Composable
fun StreamingTheme(content: @Composable () -> Unit) {
    UniStreamTheme(module = AppModule.STREAMING, content = content)
}

@Composable
fun NovelTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    UniStreamTheme(module = AppModule.NOVEL, darkTheme = darkTheme, content = content)
}

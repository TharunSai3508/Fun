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

enum class AppModule { GALLERY, STREAMING, NOVEL, GLOBAL }

data class ExtendedColors(
    val accent: Color,
    val accentAlt: Color,
    val cardGradientStart: Color,
    val cardGradientEnd: Color,
)

val LocalAppModule = compositionLocalOf { AppModule.GLOBAL }
val LocalExtendedColors = compositionLocalOf {
    ExtendedColors(
        accent = UniPrimaryLight,
        accentAlt = UniSecondaryLight,
        cardGradientStart = UniPrimaryLight,
        cardGradientEnd = UniSecondaryLight
    )
}

private fun lightScheme(module: AppModule) = when (module) {
    AppModule.GALLERY -> lightColorScheme(
        primary = GalleryPrimaryLight,
        secondary = GallerySecondaryLight,
        tertiary = GalleryTertiaryLight,
        background = GalleryBackgroundLight,
        surface = GallerySurfaceLight,
        onPrimary = UniWhite,
        onBackground = UniGray900,
        onSurface = UniGray800,
        outline = UniGray400
    )
    AppModule.STREAMING -> lightColorScheme(
        primary = StreamingPrimaryLight,
        secondary = StreamingSecondaryLight,
        background = StreamingBackgroundLight,
        surface = StreamingSurfaceLight,
        onPrimary = UniWhite,
        onBackground = UniWhite,
        onSurface = UniGray200,
        outline = UniGray600
    )
    AppModule.NOVEL -> lightColorScheme(
        primary = NovelPrimaryLight,
        secondary = NovelSecondaryLight,
        background = NovelBackgroundLight,
        surface = NovelSurfaceLight,
        onPrimary = UniWhite,
        onBackground = UniGray900,
        onSurface = UniGray800,
        outline = UniGray600
    )
    AppModule.GLOBAL -> lightColorScheme(
        primary = UniPrimaryLight,
        secondary = UniSecondaryLight,
        background = UniWhite,
        surface = UniGray100,
        onPrimary = UniWhite,
        onBackground = UniGray900,
        onSurface = UniGray800,
        outline = UniGray400
    )
}

private fun darkScheme(module: AppModule) = when (module) {
    AppModule.GALLERY -> darkColorScheme(
        primary = GalleryPrimaryDark,
        secondary = GallerySecondaryDark,
        tertiary = GalleryTertiaryDark,
        background = GalleryBackgroundDark,
        surface = GallerySurfaceDark,
        onPrimary = UniBlack,
        onBackground = UniWhite,
        onSurface = UniGray200,
        outline = UniGray600
    )
    AppModule.STREAMING -> darkColorScheme(
        primary = StreamingPrimaryDark,
        secondary = StreamingSecondaryDark,
        background = StreamingBackgroundDark,
        surface = StreamingSurfaceDark,
        onPrimary = UniBlack,
        onBackground = UniWhite,
        onSurface = UniGray200,
        outline = UniGray600
    )
    AppModule.NOVEL -> darkColorScheme(
        primary = NovelPrimaryDark,
        secondary = NovelSecondaryDark,
        background = NovelBackgroundDark,
        surface = NovelSurfaceDark,
        onPrimary = UniBlack,
        onBackground = NovelDarkText,
        onSurface = NovelDarkText,
        outline = UniGray600
    )
    AppModule.GLOBAL -> darkColorScheme(
        primary = UniPrimaryDark,
        secondary = UniSecondaryDark,
        background = UniBlack,
        surface = UniGray900,
        onPrimary = UniBlack,
        onBackground = UniWhite,
        onSurface = UniGray200,
        outline = UniGray600
    )
}

private fun extended(module: AppModule, darkTheme: Boolean): ExtendedColors = when (module) {
    AppModule.GALLERY -> ExtendedColors(
        accent = if (darkTheme) GalleryPrimaryDark else GalleryPrimaryLight,
        accentAlt = if (darkTheme) GalleryTertiaryDark else GalleryTertiaryLight,
        cardGradientStart = if (darkTheme) GalleryPrimaryDark else GalleryPrimaryLight,
        cardGradientEnd = if (darkTheme) GalleryTertiaryDark else GalleryTertiaryLight
    )
    AppModule.STREAMING -> ExtendedColors(
        accent = if (darkTheme) StreamingPrimaryDark else StreamingPrimaryLight,
        accentAlt = if (darkTheme) StreamingSecondaryDark else StreamingSecondaryLight,
        cardGradientStart = if (darkTheme) StreamingPrimaryDark else StreamingPrimaryLight,
        cardGradientEnd = if (darkTheme) StreamingSecondaryDark else StreamingSecondaryLight
    )
    AppModule.NOVEL -> ExtendedColors(
        accent = if (darkTheme) NovelPrimaryDark else NovelPrimaryLight,
        accentAlt = if (darkTheme) NovelSecondaryDark else NovelSecondaryLight,
        cardGradientStart = if (darkTheme) NovelPrimaryDark else NovelPrimaryLight,
        cardGradientEnd = if (darkTheme) NovelSecondaryDark else NovelSecondaryLight
    )
    AppModule.GLOBAL -> ExtendedColors(
        accent = if (darkTheme) UniPrimaryDark else UniPrimaryLight,
        accentAlt = if (darkTheme) UniSecondaryDark else UniSecondaryLight,
        cardGradientStart = if (darkTheme) UniPrimaryDark else UniPrimaryLight,
        cardGradientEnd = if (darkTheme) UniSecondaryDark else UniSecondaryLight
    )
}

@Composable
fun UniStreamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    module: AppModule = AppModule.GLOBAL,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkScheme(module) else lightScheme(module)
    val ext = extended(module, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppModule provides module, LocalExtendedColors provides ext) {
        MaterialTheme(colorScheme = colorScheme, typography = UniStreamTypography, content = content)
    }
}

@Composable
fun GalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = UniStreamTheme(darkTheme = darkTheme, module = AppModule.GALLERY, content = content)

@Composable
fun StreamingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = UniStreamTheme(darkTheme = darkTheme, module = AppModule.STREAMING, content = content)

@Composable
fun NovelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = UniStreamTheme(darkTheme = darkTheme, module = AppModule.NOVEL, content = content)

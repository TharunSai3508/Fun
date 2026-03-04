package com.unistream.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unistream.core.security.AppLockScreen
import com.unistream.gallery.ui.GalleryScreen
import com.unistream.gallery.ui.AlbumsScreen
import com.unistream.gallery.ui.MediaDetailScreen
import com.unistream.gallery.ui.HiddenVaultScreen
import com.unistream.gallery.ui.EditorScreen
import com.unistream.gallery.ui.WallpaperScreen
import com.unistream.home.HomeScreen
import com.unistream.settings.ui.SettingsScreen
import com.unistream.streaming.ui.StreamingHomeScreen
import com.unistream.streaming.ui.PlayerScreen
import com.unistream.streaming.ui.PlaylistScreen
import com.unistream.webnovel.ui.NovelLibraryScreen
import com.unistream.webnovel.ui.NovelReaderScreen
import com.unistream.webnovel.ui.NovelImportScreen

sealed class Screen(val route: String) {
    // App Lock
    object AppLock : Screen("app_lock")

    // Home
    object Home : Screen("home")

    // Gallery
    object Gallery : Screen("gallery")
    object Albums : Screen("albums")
    object MediaDetail : Screen("media_detail/{mediaId}") {
        fun createRoute(mediaId: Long) = "media_detail/$mediaId"
    }
    object HiddenVault : Screen("hidden_vault")
    object Editor : Screen("editor/{mediaId}") {
        fun createRoute(mediaId: Long) = "editor/$mediaId"
    }
    object Wallpaper : Screen("wallpaper/{mediaId}") {
        fun createRoute(mediaId: Long) = "wallpaper/$mediaId"
    }

    // Streaming
    object StreamingHome : Screen("streaming_home")
    object Player : Screen("player/{sourceType}/{sourceId}") {
        fun createRoute(sourceType: String, sourceId: String) = "player/$sourceType/${Uri.encode(sourceId)}"
    }
    object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    // WebNovel
    object NovelLibrary : Screen("novel_library")
    object NovelReader : Screen("novel_reader/{novelId}/{chapterId}") {
        fun createRoute(novelId: Long, chapterId: Long = 0L) = "novel_reader/$novelId/$chapterId"
    }
    object NovelImport : Screen("novel_import")

    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    isAppLocked: Boolean,
    onBiometricAuth: () -> Unit,
    onPinAuth: (String) -> Boolean
) {
    val navController = rememberNavController()

    val startDestination = if (isAppLocked) Screen.AppLock.route else Screen.Home.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        // App Lock Screen
        composable(Screen.AppLock.route) {
            AppLockScreen(
                onBiometricAuth = onBiometricAuth,
                onPinAuth = onPinAuth,
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.AppLock.route) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                onNavigateToStreaming = { navController.navigate(Screen.StreamingHome.route) },
                onNavigateToNovel = { navController.navigate(Screen.NovelLibrary.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Gallery Screens
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                onNavigateToMedia = { mediaId ->
                    navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                },
                onNavigateToHiddenVault = { navController.navigate(Screen.HiddenVault.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Albums.route) {
            AlbumsScreen(
                onNavigateToMedia = { mediaId ->
                    navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MediaDetail.route) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L
            MediaDetailScreen(
                mediaId = mediaId,
                onNavigateToEditor = { navController.navigate(Screen.Editor.createRoute(mediaId)) },
                onNavigateToWallpaper = { navController.navigate(Screen.Wallpaper.createRoute(mediaId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HiddenVault.route) {
            HiddenVaultScreen(
                onNavigateToMedia = { mediaId ->
                    navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Editor.route) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L
            EditorScreen(
                mediaId = mediaId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Wallpaper.route) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L
            WallpaperScreen(
                mediaId = mediaId,
                onBack = { navController.popBackStack() }
            )
        }

        // Streaming Screens
        composable(Screen.StreamingHome.route) {
            StreamingHomeScreen(
                onNavigateToPlayer = { sourceType, sourceId ->
                    navController.navigate(Screen.Player.createRoute(sourceType, sourceId))
                },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate(Screen.Playlist.createRoute(playlistId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Player.route) { backStackEntry ->
            val sourceType = backStackEntry.arguments?.getString("sourceType") ?: "local"
            val sourceId = Uri.decode(backStackEntry.arguments?.getString("sourceId") ?: "")
            PlayerScreen(
                sourceType = sourceType,
                sourceId = sourceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Playlist.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L
            PlaylistScreen(
                playlistId = playlistId,
                onNavigateToPlayer = { sourceType, sourceId ->
                    navController.navigate(Screen.Player.createRoute(sourceType, sourceId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // WebNovel Screens
        composable(Screen.NovelLibrary.route) {
            NovelLibraryScreen(
                onNavigateToReader = { novelId ->
                    navController.navigate(Screen.NovelReader.createRoute(novelId))
                },
                onNavigateToImport = { navController.navigate(Screen.NovelImport.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NovelReader.route) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getString("novelId")?.toLongOrNull() ?: 0L
            val chapterId = backStackEntry.arguments?.getString("chapterId")?.toLongOrNull() ?: 0L
            NovelReaderScreen(
                novelId = novelId,
                chapterId = chapterId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NovelImport.route) {
            NovelImportScreen(
                onBack = { navController.popBackStack() },
                onImportSuccess = { novelId ->
                    navController.navigate(Screen.NovelReader.createRoute(novelId)) {
                        popUpTo(Screen.NovelImport.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

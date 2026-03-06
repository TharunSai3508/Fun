package com.unistream.core.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unistream.core.security.AppLockScreen
import com.unistream.gallery.ui.*
import com.unistream.home.HomeScreen
import com.unistream.home.LaunchIntroScreen
import com.unistream.settings.ui.SettingsScreen
import com.unistream.streaming.ui.*
import com.unistream.webnovel.ui.*

sealed class Screen(val route: String) {

    object Intro : Screen("intro")

    object AppLock : Screen("app_lock")

    object Home : Screen("home")

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

    object StreamingHome : Screen("streaming_home")

    object Player : Screen("player/{sourceType}/{sourceId}") {
        fun createRoute(sourceType: String, sourceId: String) =
            "player/$sourceType/${Uri.encode(sourceId)}"
    }

    object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    object NovelLibrary : Screen("novel_library")

    object NovelReader : Screen("novel_reader/{novelId}/{chapterId}") {
        fun createRoute(novelId: Long, chapterId: Long = 0L) =
            "novel_reader/$novelId/$chapterId"
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

    NavHost(
        navController = navController,
        startDestination = Screen.Intro.route,

        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300)
                    )
        },

        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300)
                    )
        },

        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300)
                    )
        },

        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300)
                    )
        }
    ) {

        composable(Screen.Intro.route) {

            LaunchIntroScreen(
                onAnimationFinished = {

                    val next =
                        if (isAppLocked) Screen.AppLock.route
                        else Screen.Home.route

                    navController.navigate(next) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                    }
                }
            )
        }

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

        composable(Screen.Home.route) {

            HomeScreen(
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                onNavigateToStreaming = { navController.navigate(Screen.StreamingHome.route) },
                onNavigateToNovel = { navController.navigate(Screen.NovelLibrary.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Gallery.route) {

            GalleryScreen(
                onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                onNavigateToMedia = { mediaId ->
                    navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                },
                onNavigateToHiddenVault = {
                    navController.navigate(Screen.HiddenVault.route)
                },
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

        composable(Screen.MediaDetail.route) { entry ->

            val mediaId =
                entry.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L

            MediaDetailScreen(
                mediaId = mediaId,
                onNavigateToEditor = {
                    navController.navigate(Screen.Editor.createRoute(mediaId))
                },
                onNavigateToWallpaper = {
                    navController.navigate(Screen.Wallpaper.createRoute(mediaId))
                },
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

        composable(Screen.Editor.route) { entry ->

            val mediaId =
                entry.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L

            EditorScreen(
                mediaId = mediaId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Wallpaper.route) { entry ->

            val mediaId =
                entry.arguments?.getString("mediaId")?.toLongOrNull() ?: 0L

            WallpaperScreen(
                mediaId = mediaId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StreamingHome.route) {

            StreamingHomeScreen(
                onNavigateToPlayer = { type, id ->
                    navController.navigate(Screen.Player.createRoute(type, id))
                },
                onNavigateToPlaylist = { id ->
                    navController.navigate(Screen.Playlist.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Player.route) { entry ->

            val type = entry.arguments?.getString("sourceType") ?: "local"
            val id = Uri.decode(entry.arguments?.getString("sourceId") ?: "")

            PlayerScreen(
                sourceType = type,
                sourceId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Playlist.route) { entry ->

            val playlistId =
                entry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L

            PlaylistScreen(
                playlistId = playlistId,
                onNavigateToPlayer = { type, id ->
                    navController.navigate(Screen.Player.createRoute(type, id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NovelLibrary.route) {

            NovelLibraryScreen(
                onNavigateToReader = { novelId ->
                    navController.navigate(Screen.NovelReader.createRoute(novelId))
                },
                onNavigateToImport = {
                    navController.navigate(Screen.NovelImport.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NovelReader.route) { entry ->

            val novelId =
                entry.arguments?.getString("novelId")?.toLongOrNull() ?: 0L

            val chapterId =
                entry.arguments?.getString("chapterId")?.toLongOrNull() ?: 0L

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

                    navController.navigate(
                        Screen.NovelReader.createRoute(novelId)
                    ) {
                        popUpTo(Screen.NovelImport.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {

            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
package com.unistream.core.navigation

import android.util.Base64
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unistream.core.permissions.MediaPermissionWrapper
import com.unistream.core.security.AppLockScreen
import com.unistream.gallery.ui.AlbumsScreen
import com.unistream.gallery.ui.EditorScreen
import com.unistream.gallery.ui.GalleryScreen
import com.unistream.gallery.ui.HiddenVaultScreen
import com.unistream.gallery.ui.MediaDetailScreen
import com.unistream.gallery.ui.WallpaperScreen
import com.unistream.home.HomeScreen
import com.unistream.streaming.ui.PlayerScreen
import com.unistream.streaming.ui.PlaylistScreen
import com.unistream.streaming.ui.StreamingHomeScreen
import com.unistream.webnovel.ui.NovelImportScreen
import com.unistream.webnovel.ui.NovelLibraryScreen
import com.unistream.webnovel.ui.NovelReaderScreen

// ─────────────────────────────────────────────────────────────────────────────
// Helpers: encode/decode URI/URL values safe for use as NavHost path segments.
// Base64 URL-safe avoids slashes, question marks, and other special characters.
// ─────────────────────────────────────────────────────────────────────────────
fun encodeNavParam(value: String): String =
    Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)

fun decodeNavParam(encoded: String): String =
    runCatching {
        String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
    }.getOrDefault(encoded)  // fallback: return as-is if not encoded

// ─────────────────────────────────────────────────────────────────────────────
// Screens
// ─────────────────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object AppLock : Screen("app_lock")
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

    // Streaming — sourceId is Base64-encoded to avoid URI path collisions
    object StreamingHome : Screen("streaming_home")
    object Player : Screen("player/{sourceType}/{sourceId}") {
        fun createRoute(sourceType: String, sourceId: String) =
            "player/$sourceType/${encodeNavParam(sourceId)}"
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
}

// ─────────────────────────────────────────────────────────────────────────────
// App Navigation
// ─────────────────────────────────────────────────────────────────────────────
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
        // ── App Lock ───────────────────────────────────────────────────────
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

        // ── Home ───────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                onNavigateToStreaming = { navController.navigate(Screen.StreamingHome.route) },
                onNavigateToNovel = { navController.navigate(Screen.NovelLibrary.route) }
            )
        }

        // ── Gallery (wrapped in permission check) ─────────────────────────
        composable(Screen.Gallery.route) {
            MediaPermissionWrapper {
                GalleryScreen(
                    onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                    onNavigateToMedia = { mediaId ->
                        navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                    },
                    onNavigateToHiddenVault = { navController.navigate(Screen.HiddenVault.route) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Albums.route) {
            MediaPermissionWrapper {
                AlbumsScreen(
                    onNavigateToMedia = { mediaId ->
                        navController.navigate(Screen.MediaDetail.createRoute(mediaId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
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

        // ── Streaming (wrapped in permission check for local media) ────────
        composable(Screen.StreamingHome.route) {
            MediaPermissionWrapper {
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
        }

        composable(Screen.Player.route) { backStackEntry ->
            val sourceType = backStackEntry.arguments?.getString("sourceType") ?: "local"
            // Decode the Base64-encoded sourceId back to the original URI/URL
            val encodedId = backStackEntry.arguments?.getString("sourceId") ?: ""
            val sourceId = decodeNavParam(encodedId)
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

        // ── WebNovel ───────────────────────────────────────────────────────
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
    }
}

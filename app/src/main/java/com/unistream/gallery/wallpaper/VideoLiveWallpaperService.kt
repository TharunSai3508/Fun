package com.unistream.gallery.wallpaper

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/**
 * Live wallpaper service that renders a video (MP4 / local content URI) on the
 * home/lock screen using [MediaPlayer]. The video URI is stored in SharedPreferences
 * so the service can retrieve it after device reboot.
 */
class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoWallpaperEngine()

    inner class VideoWallpaperEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            startVideo(holder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            // Re-start if surface dimensions changed
            stopVideo()
            startVideo(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopVideo()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopVideo()
            super.onDestroy()
        }

        private fun startVideo(holder: SurfaceHolder) {
            val uriString = WallpaperPreferences.getVideoUri(this@VideoLiveWallpaperService)
                ?: return

            try {
                mediaPlayer = MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(
                        this@VideoLiveWallpaperService,
                        Uri.parse(uriString)
                    )
                    isLooping = true
                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    setOnPreparedListener { mp ->
                        // Mute: wallpaper should not produce sound
                        mp.setVolume(0f, 0f)
                        mp.start()
                    }
                    setOnErrorListener { _, _, _ ->
                        stopVideo()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                stopVideo()
            }
        }

        private fun stopVideo() {
            mediaPlayer?.runCatching {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Preferences helpers for live wallpaper URI storage
// ─────────────────────────────────────────────────────────────────────────────
object WallpaperPreferences {
    private const val PREFS_NAME = "unistream_wallpaper"
    private const val KEY_VIDEO_URI = "video_wallpaper_uri"
    private const val KEY_GIF_URI = "gif_wallpaper_uri"

    fun setVideoUri(context: Context, uri: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_VIDEO_URI, uri).apply()
    }

    fun getVideoUri(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_VIDEO_URI, null)

    fun setGifUri(context: Context, uri: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_GIF_URI, uri).apply()
    }

    fun getGifUri(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GIF_URI, null)
}

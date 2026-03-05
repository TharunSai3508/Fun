package com.unistream.gallery.ui

import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoWallpaperEngine()

    inner class VideoWallpaperEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            startPlayback(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) mediaPlayer?.start() else mediaPlayer?.pause()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        private fun startPlayback(holder: SurfaceHolder) {
            val uriString = selectedVideoUri ?: return
            releasePlayer()
            mediaPlayer = MediaPlayer().apply {
                setSurface(holder.surface)
                isLooping = true
                setDataSource(applicationContext, Uri.parse(uriString))
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }

        private fun releasePlayer() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    companion object {
        @Volatile
        var selectedVideoUri: String? = null
    }
}

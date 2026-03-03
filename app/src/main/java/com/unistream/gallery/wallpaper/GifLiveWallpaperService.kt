package com.unistream.gallery.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi

/**
 * Live wallpaper service that animates a GIF on the home/lock screen.
 *
 * API 28+: Uses [ImageDecoder] with [android.graphics.drawable.AnimatedImageDrawable].
 * API < 28: Falls back to the deprecated [Movie] class.
 *
 * The GIF URI is persisted via [WallpaperPreferences] so it survives reboots.
 */
class GifLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = GifWallpaperEngine()

    inner class GifWallpaperEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var running = false

        // ── API 28+ path ──────────────────────────────────────────────────
        private var animatedDrawable: android.graphics.drawable.AnimatedImageDrawable? = null

        // ── Legacy path (Movie) ───────────────────────────────────────────
        private var gifMovie: Movie? = null
        private var movieStart = 0L

        private val drawRunnable = object : Runnable {
            override fun run() {
                if (running) {
                    drawFrame()
                    handler.postDelayed(this, FRAME_DELAY_MS)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            loadGif()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            surfaceWidth = width
            surfaceHeight = height
            super.onSurfaceChanged(holder, format, width, height)
            loadGif()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopDrawing()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopDrawing()
            super.onDestroy()
        }

        // ── Load GIF ──────────────────────────────────────────────────────
        private fun loadGif() {
            val uriString = WallpaperPreferences.getGifUri(this@GifLiveWallpaperService) ?: return
            val uri = Uri.parse(uriString)
            stopDrawing()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    loadAnimatedDrawable(uri)
                } else {
                    loadMovieGif(uri)
                }
                startDrawing()
            } catch (e: Exception) {
                // If loading fails, draw a blank black surface
                drawBlank()
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun loadAnimatedDrawable(uri: Uri) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            val drawable = ImageDecoder.decodeDrawable(source) as?
                android.graphics.drawable.AnimatedImageDrawable
            drawable?.let {
                it.repeatCount = android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                animatedDrawable = it
            }
        }

        private fun loadMovieGif(uri: Uri) {
            contentResolver.openInputStream(uri)?.use { stream ->
                gifMovie = Movie.decodeStream(stream)
            }
        }

        // ── Draw loop ─────────────────────────────────────────────────────
        private fun startDrawing() {
            running = true
            movieStart = System.currentTimeMillis()
            handler.post(drawRunnable)

            // For AnimatedImageDrawable, start animation on its Drawable callback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                animatedDrawable?.start()
            }
        }

        private fun stopDrawing() {
            running = false
            handler.removeCallbacks(drawRunnable)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                animatedDrawable?.stop()
                animatedDrawable = null
            }
            gifMovie = null
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                canvas.drawColor(Color.BLACK)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    drawAnimatedDrawable(canvas)
                } else {
                    drawMovieFrame(canvas)
                }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun drawAnimatedDrawable(canvas: Canvas) {
            val drawable = animatedDrawable ?: return
            // Scale drawable to fill the surface while maintaining aspect ratio
            val drawableW = drawable.intrinsicWidth.takeIf { it > 0 } ?: surfaceWidth
            val drawableH = drawable.intrinsicHeight.takeIf { it > 0 } ?: surfaceHeight
            val scale = maxOf(
                surfaceWidth.toFloat() / drawableW,
                surfaceHeight.toFloat() / drawableH
            )
            val scaledW = (drawableW * scale).toInt()
            val scaledH = (drawableH * scale).toInt()
            val left = (surfaceWidth - scaledW) / 2
            val top = (surfaceHeight - scaledH) / 2
            drawable.setBounds(left, top, left + scaledW, top + scaledH)
            drawable.draw(canvas)
        }

        private fun drawMovieFrame(canvas: Canvas) {
            val movie = gifMovie ?: return
            val duration = movie.duration().takeIf { it > 0 } ?: 1
            val elapsed = ((System.currentTimeMillis() - movieStart) % duration).toInt()
            movie.setTime(elapsed)

            val scaleX = surfaceWidth.toFloat() / movie.width().coerceAtLeast(1)
            val scaleY = surfaceHeight.toFloat() / movie.height().coerceAtLeast(1)
            val scale = maxOf(scaleX, scaleY)

            canvas.save()
            canvas.scale(scale, scale)
            movie.draw(canvas, 0f, 0f)
            canvas.restore()
        }

        private fun drawBlank() {
            val holder = surfaceHolder
            val canvas = holder.lockCanvas() ?: return
            try {
                canvas.drawColor(Color.BLACK)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    companion object {
        private const val FRAME_DELAY_MS = 33L  // ~30 fps
    }
}

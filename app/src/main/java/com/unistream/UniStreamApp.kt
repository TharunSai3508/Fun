package com.unistream

import android.app.Application
import android.os.Build
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

@HiltAndroidApp
class UniStreamApp : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * WorkManager configuration (Hilt integration)
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /**
     * Coil Image Loader (global)
     */
    override fun newImageLoader(): ImageLoader {

        val httpCache = Cache(
            File(cacheDir, "http_cache"),
            50L * 1024 * 1024 // 50MB
        )

        val client = okHttpClient.newBuilder()
            .cache(httpCache)
            .build()

        return ImageLoader.Builder(this)

            // Memory cache
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.18)
                    .build()
            }

            // Disk cache
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.08)
                    .build()
            }

            .okHttpClient(client)

            .components {

                // GIF support
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }

                // Video thumbnails
                add(VideoFrameDecoder.Factory())
            }

            .crossfade(300)

            .build()
    }
}
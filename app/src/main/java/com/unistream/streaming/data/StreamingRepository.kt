package com.unistream.streaming.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val okHttpClient: OkHttpClient
) {
    // Local video browsing
    suspend fun getLocalVideos(): List<VideoSource> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoSource>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
        )
        context.contentResolver.query(collection, projection, null, null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                videos.add(
                    VideoSource(
                        type = VideoSourceType.LOCAL,
                        uri = uri.toString(),
                        title = cursor.getString(nameCol) ?: "Video",
                        durationMs = cursor.getLong(durCol)
                    )
                )
            }
        }
        videos
    }

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>> = playlistDao.getPlaylistItems(playlistId)

    suspend fun createPlaylist(name: String, description: String = ""): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name, description = description))

    suspend fun addToPlaylist(playlistId: Long, source: VideoSource, order: Int = 0): Long =
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceType = source.type.name,
                sourceUri = source.uri,
                title = source.title,
                durationMs = source.durationMs,
                sortOrder = order
            )
        )

    fun getWatchHistory(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getWatchHistory()
    fun getContinueWatching(): Flow<List<WatchHistoryEntity>> = watchHistoryDao.getContinueWatching()

    suspend fun recordWatch(source: VideoSource) {
        watchHistoryDao.upsertHistory(
            WatchHistoryEntity(
                sourceType = source.type.name,
                sourceUri = source.uri,
                title = source.title,
                thumbnailUri = source.thumbnailUri,
                durationMs = source.durationMs
            )
        )
    }

    suspend fun updateProgress(uri: String, progressMs: Long) {
        watchHistoryDao.updateProgress(uri, progressMs, System.currentTimeMillis())
    }

    fun detectSourceType(url: String): VideoSourceType {
        return when {
            url.contains("drive.google.com") -> VideoSourceType.DRIVE
            url.endsWith(".m3u8", ignoreCase = true) -> VideoSourceType.URL_HLS
            url.endsWith(".mpd", ignoreCase = true) -> VideoSourceType.URL_DASH
            url.startsWith("content://") -> VideoSourceType.LOCAL
            else -> VideoSourceType.URL_MP4
        }
    }

    fun convertDriveUrl(shareUrl: String): String {
        val fileIdRegex = Regex("/file/d/([a-zA-Z0-9_-]+)")
        val match = fileIdRegex.find(shareUrl)
        val fileId = match?.groupValues?.get(1) ?: return shareUrl
        return "https://drive.google.com/uc?export=download&id=$fileId"
    }

    suspend fun resolvePlayableUrl(url: String): String? = withContext(Dispatchers.IO) {
        if (url.startsWith("content://") || url.endsWith(".mp4") || url.endsWith(".m3u8") || url.endsWith(".mpd")) {
            return@withContext url
        }

        runCatching {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(12000)
                .get()

            val direct = doc.select("meta[property=og:video], meta[property=og:video:url], source[src]")
                .firstOrNull()
                ?.attr("content")
                ?.ifBlank { doc.select("source[src]").firstOrNull()?.attr("src") ?: "" }
                ?.trim()

            val normalized = when {
                direct.isNullOrBlank() -> null
                direct.startsWith("//") -> "https:$direct"
                direct.startsWith("http") -> direct
                else -> URL(URL(url), direct).toString()
            }
            normalized
        }.getOrNull()
    }

    suspend fun downloadFromUrl(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
                val file = File(dir, "stream_${System.currentTimeMillis()}.mp4")
                body.byteStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file.absolutePath
            }
        }.getOrNull()
    }
}

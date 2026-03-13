package com.unistream.streaming.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
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

    /**
     * Download a video from [url] into permanent user-visible storage.
     *
     * Previous bug: files were saved to context.cacheDir which is:
     *  - Volatile (cleared by system at any time)
     *  - Not visible in Files app or any media picker
     *  - Never indexed by MediaStore
     *
     * Fix: use MediaStore.Downloads (API 29+) with the IS_PENDING lifecycle,
     * or getExternalFilesDir on older devices.
     *
     * Returns the display name of the saved file on success, null on failure.
     */
    suspend fun downloadFromUrl(url: String, title: String = ""): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "video/*,*/*;q=0.8")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null

                val contentType = response.header("Content-Type") ?: "video/mp4"
                val mimeType = contentType.substringBefore(";").trim().ifBlank { "video/mp4" }
                val ext = when {
                    mimeType.contains("mp4") -> "mp4"
                    mimeType.contains("webm") -> "webm"
                    mimeType.contains("x-matroska") -> "mkv"
                    url.endsWith(".mkv", ignoreCase = true) -> "mkv"
                    url.endsWith(".webm", ignoreCase = true) -> "webm"
                    else -> "mp4"
                }
                val safeTitle = title.ifBlank { "stream_${System.currentTimeMillis()}" }
                    .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val fileName = "$safeTitle.$ext"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToDownloadsQ(body, fileName, mimeType)
                } else {
                    saveToDownloadsLegacy(body, fileName, mimeType)
                }
            }
        }.getOrNull()
    }

    private fun saveToDownloadsQ(
        body: okhttp3.ResponseBody,
        fileName: String,
        mimeType: String
    ): String? {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val cv = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/UniStream")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri: Uri = context.contentResolver.insert(collection, cv) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                body.byteStream().copyTo(out)
            }
            // Clear IS_PENDING to publish the file — without this the file stays invisible
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null
            )
            fileName
        } catch (e: Exception) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToDownloadsLegacy(
        body: okhttp3.ResponseBody,
        fileName: String,
        mimeType: String
    ): String? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.resolve("UniStream")
            ?: return null
        dir.mkdirs()
        val file = File(dir, fileName)
        return try {
            file.outputStream().use { out -> body.byteStream().copyTo(out) }
            android.media.MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf(mimeType), null
            )
            fileName
        } catch (e: Exception) {
            file.delete()
            null
        }
    }
}

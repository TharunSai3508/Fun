package com.unistream.streaming.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.unistream.core.network.UniversalMediaResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val okHttpClient: OkHttpClient,
    private val mediaResolver: UniversalMediaResolver
) {

    // ---------------------------------------------------------
    // LOCAL VIDEO BROWSING
    // ---------------------------------------------------------

    suspend fun getLocalVideos(): List<VideoSource> = withContext(Dispatchers.IO) {

        val videos = mutableListOf<VideoSource>()

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
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

    // ---------------------------------------------------------
    // PLAYLIST
    // ---------------------------------------------------------

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> =
        playlistDao.getAllPlaylists()

    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>> =
        playlistDao.getPlaylistItems(playlistId)

    suspend fun createPlaylist(
        name: String,
        description: String = ""
    ): Long {
        return playlistDao.insertPlaylist(
            PlaylistEntity(name = name, description = description)
        )
    }

    suspend fun addToPlaylist(
        playlistId: Long,
        source: VideoSource,
        order: Int = 0
    ): Long {

        return playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                sourceType = source.type.name,
                sourceUri = source.uri,
                title = source.title,
                durationMs = source.durationMs,
                sortOrder = order
            )
        )
    }

    // ---------------------------------------------------------
    // WATCH HISTORY
    // ---------------------------------------------------------

    fun getWatchHistory(): Flow<List<WatchHistoryEntity>> =
        watchHistoryDao.getWatchHistory()

    fun getContinueWatching(): Flow<List<WatchHistoryEntity>> =
        watchHistoryDao.getContinueWatching()

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

    suspend fun updateProgress(
        uri: String,
        progressMs: Long
    ) {
        watchHistoryDao.updateProgress(
            uri,
            progressMs,
            System.currentTimeMillis()
        )
    }

    // ---------------------------------------------------------
    // SOURCE TYPE DETECTION
    // ---------------------------------------------------------

    fun detectSourceType(url: String): VideoSourceType {

        return when {

            url.startsWith("content://") ->
                VideoSourceType.LOCAL

            url.contains("drive.google.com") ->
                VideoSourceType.DRIVE

            url.endsWith(".m3u8", true) ->
                VideoSourceType.URL_HLS

            url.endsWith(".mpd", true) ->
                VideoSourceType.URL_DASH

            else ->
                VideoSourceType.URL_MP4
        }
    }

    // ---------------------------------------------------------
    // GOOGLE DRIVE LINK CONVERTER
    // ---------------------------------------------------------

    fun convertDriveUrl(shareUrl: String): String {

        val fileIdRegex = Regex("/file/d/([a-zA-Z0-9_-]+)")

        val match = fileIdRegex.find(shareUrl)

        val fileId = match?.groupValues?.get(1)
            ?: return shareUrl

        return "https://drive.google.com/uc?export=download&id=$fileId"
    }

    // ---------------------------------------------------------
    // RESOLVE PLAYABLE URL
    // ---------------------------------------------------------

    /**
     * Resolves any URL (webpage, share link, direct CDN link) to a playable stream URL.
     *
     * Previously used basic Jsoup extraction which only found og:video on simple pages.
     * Now delegates to UniversalMediaResolver which handles Reddit, Imgur, Redgifs,
     * Streamable, Pixeldrain, Google Drive, and generic HTML with multiple selector
     * fallbacks — the same approach used by SaveFrom / cobalt.tools.
     */
    suspend fun resolvePlayableUrl(url: String): String? = withContext(Dispatchers.IO) {
        // Direct playable URLs need no resolution
        if (url.startsWith("content://") || mediaResolver.detectMimeFromUrl(url) != null) {
            return@withContext url
        }

        val candidates = mediaResolver.resolve(url)

        // Prefer video streams; fall back to any media; return null if nothing found
        return@withContext candidates
            .sortedWith(compareByDescending<com.unistream.core.network.ResolvedMedia> { it.isVideo }
                .thenByDescending { it.quality.contains("HD", ignoreCase = true) })
            .firstOrNull()
            ?.url
    }

    // ---------------------------------------------------------
    // PLAY FROM URL
    // ---------------------------------------------------------

    fun playFromUrl(url: String): String {

        return if (url.contains("drive.google.com"))
            convertDriveUrl(url)
        else
            url
    }

    /**
     * Download a video from [url] into permanent user-visible storage.
     *
     * Previous bug: files were saved to context.cacheDir which is volatile,
     * not visible in Files app, and never indexed by MediaStore.
     *
     * Fix: use MediaStore.Downloads (API 29+) with the IS_PENDING lifecycle.
     * IS_PENDING=1 reserves the row; IS_PENDING=0 publishes it after bytes are written.
     * On failure the orphaned pending row is deleted so no ghost entries appear.
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

    private fun saveToDownloadsQ(body: okhttp3.ResponseBody, fileName: String, mimeType: String): String? {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val cv = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/UniStream")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri: Uri = context.contentResolver.insert(collection, cv) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)?.use { body.byteStream().copyTo(it) }
            context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            fileName
        } catch (e: Exception) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToDownloadsLegacy(body: okhttp3.ResponseBody, fileName: String, mimeType: String): String? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve("UniStream") ?: return null
        dir.mkdirs()
        val file = File(dir, fileName)
        return try {
            file.outputStream().use { body.byteStream().copyTo(it) }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
            fileName
        } catch (e: Exception) {
            file.delete()
            null
        }
    }
}

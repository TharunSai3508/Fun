package com.unistream.streaming.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistDao: PlaylistDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val hiddenVideoDao: HiddenVideoDao,
    private val okHttpClient: OkHttpClient
) {

    private val vaultDir: File
        get() {
            val dir = File(context.filesDir, "hidden_videos")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val thumbnailDir: File
        get() {
            val dir = File(context.filesDir, "hidden_video_thumbs")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

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
        val lower = url.lowercase()
        return when {
            lower.startsWith("content://") -> VideoSourceType.LOCAL
            lower.contains("drive.google.com") -> VideoSourceType.DRIVE
            lower.contains(".m3u8") -> VideoSourceType.URL_HLS
            lower.contains(".mpd") -> VideoSourceType.URL_DASH
            else -> VideoSourceType.URL_MP4
        }
    }

    // ---------------------------------------------------------
    // GOOGLE DRIVE LINK CONVERTER
    // ---------------------------------------------------------

    fun convertDriveUrl(shareUrl: String): String {
        val patterns = listOf(
            Regex("/file/d/([a-zA-Z0-9_-]+)"),
            Regex("[?&]id=([a-zA-Z0-9_-]+)"),
            Regex("/d/([a-zA-Z0-9_-]+)")
        )
        val fileId = patterns.firstNotNullOfOrNull { it.find(shareUrl)?.groupValues?.getOrNull(1) }
            ?: return shareUrl
        return "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
    }

    // ---------------------------------------------------------
    // RESOLVE PLAYABLE URL
    // ---------------------------------------------------------

    suspend fun resolvePlayableUrl(url: String): String? =
        withContext(Dispatchers.IO) {

            val videoExtensions = listOf(".mp4", ".webm", ".mkv", ".avi", ".mov", ".m3u8", ".mpd")
            if (url.startsWith("content://") || videoExtensions.any { url.lowercase().contains(it) }) {
                return@withContext url
            }

            runCatching {

                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .followRedirects(true)
                    .get()

                // Try multiple strategies to find video URL

                // 1. <video> or <source> tags
                val videoSrc = doc.select(
                    "video[src], video source[src], " +
                    "meta[property=og:video], meta[property=og:video:url], meta[property=og:video:secure_url], " +
                    "meta[name=twitter:player:stream]"
                ).firstOrNull()

                var raw = when {
                    videoSrc == null -> null
                    videoSrc.hasAttr("src") -> videoSrc.attr("src")
                    videoSrc.hasAttr("content") -> videoSrc.attr("content")
                    else -> null
                }

                // 2. Check for iframe embeds (common on sites)
                if (raw.isNullOrBlank()) {
                    val iframe = doc.select("iframe[src*=video], iframe[src*=embed], iframe[src*=player]").firstOrNull()
                    raw = iframe?.attr("src")
                }

                // 3. Look for direct video links in the page
                if (raw.isNullOrBlank()) {
                    val allLinks = doc.select("a[href]")
                    raw = allLinks.firstOrNull { link ->
                        val href = link.attr("href").lowercase()
                        videoExtensions.any { href.endsWith(it) }
                    }?.attr("href")
                }

                // 4. Search page source for video URLs via regex
                if (raw.isNullOrBlank()) {
                    val pageHtml = doc.html()
                    val urlPattern = Regex("""(https?://[^\s"'<>]+\.(mp4|m3u8|webm|mkv))""", RegexOption.IGNORE_CASE)
                    raw = urlPattern.find(pageHtml)?.value
                }

                // Normalize relative URLs
                val normalized = when {
                    raw.isNullOrBlank() -> null
                    raw.startsWith("//") -> "https:$raw"
                    raw.startsWith("http") -> raw
                    else -> URL(URL(url), raw).toString()
                }

                normalized

            }.getOrNull()
        }

    // ---------------------------------------------------------
    // PLAY FROM URL
    // ---------------------------------------------------------

    fun playFromUrl(url: String): String {
        return when {
            url.contains("drive.google.com") -> convertDriveUrl(url)
            url.contains("dropbox.com") -> url.replace("dl=0", "dl=1").replace("www.dropbox.com", "dl.dropboxusercontent.com")
            else -> url
        }
    }

    // ---------------------------------------------------------
    // DOWNLOAD STREAM FROM URL
    // ---------------------------------------------------------

    suspend fun downloadFromUrl(url: String): Uri? =
        withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", url)
                .build()

            okHttpClient.newCall(request).execute().use { response ->

                if (!response.isSuccessful) return@withContext null

                val body = response.body ?: return@withContext null

                val contentType = body.contentType()?.toString() ?: "video/mp4"
                val ext = when {
                    contentType.contains("webm") -> "webm"
                    contentType.contains("mkv") -> "mkv"
                    contentType.contains("avi") -> "avi"
                    contentType.contains("mov") -> "mov"
                    else -> "mp4"
                }

                val fileName = "VIDEO_${System.currentTimeMillis()}.$ext"

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/$ext")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Unistream")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: return@withContext null

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    body.byteStream().copyTo(output)
                }

                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)

                context.contentResolver.update(uri, values, null, null)

                uri
            }
        }

    // ---------------------------------------------------------
    // HIDDEN VIDEOS VAULT
    // ---------------------------------------------------------

    fun getHiddenVideos(): Flow<List<HiddenVideoEntity>> =
        hiddenVideoDao.getAllHiddenVideos()

    suspend fun hideVideo(source: VideoSource): Long = withContext(Dispatchers.IO) {
        val uri = Uri.parse(source.uri)
        val uuid = UUID.randomUUID().toString().take(8)
        val safeFileName = "${uuid}_${source.title}"
        val destFile = File(vaultDir, safeFileName)

        // Copy file from MediaStore to vault
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Cannot open video file")

        // Generate thumbnail
        val thumbFile = File(thumbnailDir, "thumb_$safeFileName.jpg")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bitmap = context.contentResolver.loadThumbnail(uri, Size(300, 300), null)
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bitmap.recycle()
            }
        } catch (_: Exception) { }

        // Delete original from MediaStore
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) { }

        // Save to database
        hiddenVideoDao.insertHiddenVideo(
            HiddenVideoEntity(
                originalUri = source.uri,
                hiddenPath = destFile.absolutePath,
                mimeType = "video/mp4",
                fileName = source.title,
                fileSizeBytes = destFile.length(),
                durationMs = source.durationMs,
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else null
            )
        )
    }

    suspend fun unhideVideo(entity: HiddenVideoEntity): Boolean = withContext(Dispatchers.IO) {
        val hiddenFile = File(entity.hiddenPath)
        if (!hiddenFile.exists()) {
            hiddenVideoDao.deleteHiddenVideo(entity)
            return@withContext false
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, entity.fileName)
            put(MediaStore.Video.Media.MIME_TYPE, entity.mimeType)
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Unistream")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return@withContext false

        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                hiddenFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            return@withContext false
        }

        hiddenFile.delete()
        entity.thumbnailPath?.let { File(it).delete() }
        hiddenVideoDao.deleteHiddenVideo(entity)
        true
    }

    suspend fun deleteHiddenVideoPermanently(entity: HiddenVideoEntity) = withContext(Dispatchers.IO) {
        File(entity.hiddenPath).delete()
        entity.thumbnailPath?.let { File(it).delete() }
        hiddenVideoDao.deleteHiddenVideo(entity)
    }
}
package com.unistream.streaming.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
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

    suspend fun resolvePlayableUrl(url: String): String? =
        withContext(Dispatchers.IO) {

            if (
                url.startsWith("content://") ||
                url.endsWith(".mp4", true) ||
                url.endsWith(".m3u8", true) ||
                url.endsWith(".mpd", true)
            ) {
                return@withContext url
            }

            runCatching {

                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get()

                val videoSrc =
                    doc.select("video source[src], meta[property=og:video], meta[property=og:video:url]")
                        .firstOrNull()

                val raw = when {

                    videoSrc == null -> null

                    videoSrc.hasAttr("src") ->
                        videoSrc.attr("src")

                    videoSrc.hasAttr("content") ->
                        videoSrc.attr("content")

                    else -> null
                }

                val normalized = when {

                    raw.isNullOrBlank() ->
                        null

                    raw.startsWith("//") ->
                        "https:$raw"

                    raw.startsWith("http") ->
                        raw

                    else ->
                        URL(URL(url), raw).toString()
                }

                normalized

            }.getOrNull()
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

    // ---------------------------------------------------------
    // DOWNLOAD STREAM FROM URL
    // ---------------------------------------------------------

    suspend fun downloadFromUrl(url: String): Uri? =
        withContext(Dispatchers.IO) {

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->

                if (!response.isSuccessful) return@withContext null

                val body = response.body ?: return@withContext null

                val fileName = "VIDEO_${System.currentTimeMillis()}.mp4"

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
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
}
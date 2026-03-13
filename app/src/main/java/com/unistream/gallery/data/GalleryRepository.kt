package com.unistream.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.unistream.core.network.ResolvedMedia
import com.unistream.core.network.UniversalMediaResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hiddenMediaDao: HiddenMediaDao,
    private val okHttpClient: OkHttpClient,
    private val resolver: UniversalMediaResolver
) {
    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun getAllMedia(
        filter: MediaFilter = MediaFilter.ALL,
        sortOrder: SortOrder = SortOrder.DATE_DESC
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaItems = mutableListOf<MediaItem>()

        if (filter == MediaFilter.ALL || filter == MediaFilter.IMAGES || filter == MediaFilter.GIFS) {
            mediaItems.addAll(queryImages(filter))
        }

        if (filter == MediaFilter.ALL || filter == MediaFilter.VIDEOS || filter == MediaFilter.SCREEN_RECORDINGS) {
            mediaItems.addAll(queryVideos(filter))
        }

        when (sortOrder) {
            SortOrder.DATE_DESC -> mediaItems.sortByDescending { it.dateAdded }
            SortOrder.DATE_ASC -> mediaItems.sortBy { it.dateAdded }
            SortOrder.NAME_ASC -> mediaItems.sortBy { it.displayName }
            SortOrder.SIZE_DESC -> mediaItems.sortByDescending { it.size }
        }

        mediaItems
    }

    private fun queryImages(filter: MediaFilter): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
        )

        val selection = if (filter == MediaFilter.GIFS) "${MediaStore.Images.Media.MIME_TYPE} = ?" else null
        val selectionArgs = if (filter == MediaFilter.GIFS) arrayOf("image/gif") else null

        contentResolver.query(
            collection, projection, selection, selectionArgs,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                items.add(
                    MediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = cursor.getString(nameCol) ?: "",
                        mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                        dateAdded = cursor.getLong(dateAddedCol),
                        dateModified = cursor.getLong(dateModCol),
                        size = cursor.getLong(sizeCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                        bucketId = cursor.getLong(bucketIdCol)
                    )
                )
            }
        }
        return items
    }

    private fun queryVideos(filter: MediaFilter): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID
        )

        contentResolver.query(
            collection, projection, null, null,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: ""
                val isScreenRecording = filter == MediaFilter.SCREEN_RECORDINGS &&
                    (name.contains("screen", ignoreCase = true) || name.contains("record", ignoreCase = true))
                if (filter == MediaFilter.SCREEN_RECORDINGS && !isScreenRecording) continue

                items.add(
                    MediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = name,
                        mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                        dateAdded = cursor.getLong(dateAddedCol),
                        dateModified = cursor.getLong(dateModCol),
                        size = cursor.getLong(sizeCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        duration = cursor.getLong(durationCol),
                        bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                        bucketId = cursor.getLong(bucketIdCol)
                    )
                )
            }
        }
        return items
    }

    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumMap = mutableMapOf<Long, Album>()
        getAllMedia().forEach { media ->
            val existing = albumMap[media.bucketId]
            if (existing == null) {
                albumMap[media.bucketId] = Album(
                    id = media.bucketId, name = media.bucketName,
                    coverUri = media.uri, mediaCount = 1, bucketId = media.bucketId
                )
            } else {
                albumMap[media.bucketId] = existing.copy(mediaCount = existing.mediaCount + 1)
            }
        }
        albumMap.values.sortedByDescending { it.mediaCount }
    }

    fun getHiddenMedia(): Flow<List<HiddenMediaEntity>> = hiddenMediaDao.getAllHiddenMedia()

    suspend fun hideMedia(media: MediaItem, hiddenPath: String): Long {
        return hiddenMediaDao.insertHiddenMedia(
            HiddenMediaEntity(
                originalUri = media.uri.toString(),
                hiddenPath = hiddenPath,
                mediaType = if (media.isVideo) "video" else if (media.isGif) "gif" else "image",
                mimeType = media.mimeType,
                fileName = media.displayName,
                fileSizeBytes = media.size
            )
        )
    }

    suspend fun unhideMedia(entity: HiddenMediaEntity) {
        hiddenMediaDao.deleteHiddenMedia(entity)
    }

    /**
     * Step 1 of gallery URL import: resolve [url] to a list of downloadable media options.
     * The user can then pick which item to save.
     */
    suspend fun resolveMediaFromUrl(url: String): List<ResolvedMedia> =
        resolver.resolve(url)

    /**
     * Step 2 of gallery URL import: download a [ResolvedMedia] item obtained from [resolveMediaFromUrl].
     * Delegates directly to [downloadMediaFromUrl] with the resolved direct URL.
     */
    suspend fun downloadResolvedMedia(media: ResolvedMedia): Boolean =
        downloadMediaFromUrl(media.url, media.mimeType)

    /**
     * Download media from a URL into the device gallery.
     *
     * Root cause of blank black image bug: IS_PENDING=1 was set on MediaStore insert but
     * never cleared to 0 after bytes were written. MediaStore retained a 0-byte pending
     * placeholder that the gallery rendered as a solid black thumbnail.
     *
     * Fix: write all bytes first, then clear IS_PENDING=0. On any exception, delete the
     * orphaned pending row so no black placeholder ever appears in the gallery.
     */
    suspend fun downloadMediaFromUrl(url: String, knownMime: String? = null): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "image/*,video/*,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false

                val contentType = response.header("Content-Type") ?: ""
                val mimeType = when {
                    contentType.contains("video/") -> contentType.substringBefore(";").trim()
                    contentType.contains("image/") -> contentType.substringBefore(";").trim()
                    knownMime != null -> knownMime
                    else -> resolver.detectMimeFromUrl(url) ?: "image/jpeg"
                }
                val isVideo = mimeType.startsWith("video/")
                val ext = mimeType.substringAfter("/").replace("jpeg", "jpg").substringBefore(";")
                val fileName = "unistream_${System.currentTimeMillis()}.$ext"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveViaMediaStoreQ(body, fileName, mimeType, isVideo)
                } else {
                    saveViaLegacyExternalStorage(body, fileName, mimeType, isVideo)
                }
            }
        }.getOrDefault(false)
    }

    private fun saveViaMediaStoreQ(
        body: okhttp3.ResponseBody,
        fileName: String,
        mimeType: String,
        isVideo: Boolean
    ): Boolean {
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/UniStream" else "Pictures/UniStream")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri: Uri = contentResolver.insert(collection, cv) ?: return false
        return try {
            contentResolver.openOutputStream(uri)?.use { body.byteStream().copyTo(it) }
            // CRITICAL: clear IS_PENDING — without this the file stays as an invisible 0-byte placeholder
            contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            true
        } catch (e: Exception) {
            runCatching { contentResolver.delete(uri, null, null) }
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyExternalStorage(
        body: okhttp3.ResponseBody,
        fileName: String,
        mimeType: String,
        isVideo: Boolean
    ): Boolean {
        val dir = if (isVideo) {
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
        } else {
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
        }.resolve("UniStream")
        dir.mkdirs()
        val file = java.io.File(dir, fileName)
        return try {
            file.outputStream().use { body.byteStream().copyTo(it) }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
            true
        } catch (e: Exception) {
            file.delete()
            false
        }
    }
}

enum class MediaFilter { ALL, IMAGES, VIDEOS, GIFS, LARGE_FILES, SCREEN_RECORDINGS }
enum class SortOrder { DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC }

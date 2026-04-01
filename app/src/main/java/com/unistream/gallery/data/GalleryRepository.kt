package com.unistream.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hiddenMediaDao: HiddenMediaDao,
    private val okHttpClient: OkHttpClient
) {

    private val contentResolver: ContentResolver = context.contentResolver

    private val vaultDir: File
        get() {
            val dir = File(context.filesDir, "hidden_vault")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val thumbnailDir: File
        get() {
            val dir = File(context.filesDir, "hidden_thumbs")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    // ---------------------------------------------------------
    // MEDIA QUERIES
    // ---------------------------------------------------------

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

        val hiddenUris = hiddenMediaDao.getAllHiddenMediaOnce().map { it.originalUri }.toSet()
        val visibleMedia = mediaItems.filterNot { it.uri.toString() in hiddenUris }.toMutableList()

        when (sortOrder) {
            SortOrder.DATE_DESC -> visibleMedia.sortByDescending { it.dateAdded }
            SortOrder.DATE_ASC -> visibleMedia.sortBy { it.dateAdded }
            SortOrder.NAME_ASC -> visibleMedia.sortBy { it.displayName }
            SortOrder.SIZE_DESC -> visibleMedia.sortByDescending { it.size }
        }

        visibleMedia
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

        val selection = if (filter == MediaFilter.GIFS)
            "${MediaStore.Images.Media.MIME_TYPE}=?"
        else null

        val selectionArgs = if (filter == MediaFilter.GIFS)
            arrayOf("image/gif")
        else null

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
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
                val uri = ContentUris.withAppendedId(collection, id)

                items.add(
                    MediaItem(
                        id = id,
                        uri = uri,
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
            collection,
            projection,
            null,
            null,
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
                val uri = ContentUris.withAppendedId(collection, id)

                val name = cursor.getString(nameCol) ?: ""

                val isScreenRecording =
                    filter == MediaFilter.SCREEN_RECORDINGS &&
                            (name.contains("screen", true) || name.contains("record", true))

                if (filter == MediaFilter.SCREEN_RECORDINGS && !isScreenRecording) continue

                items.add(
                    MediaItem(
                        id = id,
                        uri = uri,
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
        val allMedia = getAllMedia()

        allMedia.forEach { media ->

            val existing = albumMap[media.bucketId]

            if (existing == null) {
                albumMap[media.bucketId] = Album(
                    id = media.bucketId,
                    name = media.bucketName,
                    coverUri = media.uri,
                    mediaCount = 1,
                    bucketId = media.bucketId
                )
            } else {
                albumMap[media.bucketId] =
                    existing.copy(mediaCount = existing.mediaCount + 1)
            }
        }

        albumMap.values.sortedByDescending { it.mediaCount }
    }

    // ---------------------------------------------------------
    // HIDDEN VAULT — FILE OPERATIONS
    // ---------------------------------------------------------

    fun getHiddenMedia(): Flow<List<HiddenMediaEntity>> =
        hiddenMediaDao.getAllHiddenMedia()

    suspend fun hideMedia(media: MediaItem, hiddenPath: String): Long =
        withContext(Dispatchers.IO) {
            val uuid = UUID.randomUUID().toString().take(8)
            val safeFileName = "${uuid}_${media.displayName}"
            val destFile = File(vaultDir, safeFileName)

            // Copy file from MediaStore to vault directory
            try {
                contentResolver.openInputStream(media.uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Cannot open media file")
            } catch (e: Exception) {
                if (destFile.exists()) destFile.delete()
                throw e
            }

            // Generate thumbnail
            val thumbFile = File(thumbnailDir, "thumb_$safeFileName.jpg")
            try {
                if (media.isImage || media.isGif) {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentResolver.loadThumbnail(media.uri, Size(300, 300), null)
                    } else {
                        MediaStore.Images.Thumbnails.getThumbnail(
                            contentResolver, media.id,
                            MediaStore.Images.Thumbnails.MINI_KIND, null
                        )
                    }
                    bitmap?.let {
                        FileOutputStream(thumbFile).use { out ->
                            it.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        }
                        it.recycle()
                    }
                } else if (media.isVideo) {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentResolver.loadThumbnail(media.uri, Size(300, 300), null)
                    } else {
                        ThumbnailUtils.createVideoThumbnail(
                            destFile.absolutePath,
                            MediaStore.Video.Thumbnails.MINI_KIND
                        )
                    }
                    bitmap?.let {
                        FileOutputStream(thumbFile).use { out ->
                            it.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        }
                        it.recycle()
                    }
                }
            } catch (_: Exception) {
                // Thumbnail generation is best-effort
            }

            // Delete original from MediaStore
            try {
                contentResolver.delete(media.uri, null, null)
            } catch (_: Exception) {
                // On API 30+ may need user permission via createDeleteRequest
                // For now we keep the original if deletion fails
            }

            // Save to database
            hiddenMediaDao.insertHiddenMedia(
                HiddenMediaEntity(
                    originalUri = media.uri.toString(),
                    hiddenPath = destFile.absolutePath,
                    mediaType = when {
                        media.isVideo -> "video"
                        media.isGif -> "gif"
                        else -> "image"
                    },
                    mimeType = media.mimeType,
                    fileName = media.displayName,
                    fileSizeBytes = media.size,
                    thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else null
                )
            )
        }

    suspend fun unhideMedia(entity: HiddenMediaEntity): Boolean =
        withContext(Dispatchers.IO) {
            val hiddenFile = File(entity.hiddenPath)
            if (!hiddenFile.exists()) {
                hiddenMediaDao.deleteHiddenMedia(entity)
                return@withContext false
            }

            // Restore to MediaStore
            val isVideo = entity.mediaType == "video"
            val collection = if (isVideo)
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val relativePath = if (isVideo) "Movies/Unistream" else "Pictures/Unistream"

            val values = ContentValues().apply {
                if (isVideo) {
                    put(MediaStore.Video.Media.DISPLAY_NAME, entity.fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, entity.mimeType)
                    put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                } else {
                    put(MediaStore.Images.Media.DISPLAY_NAME, entity.fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, entity.mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(collection, values)
                ?: return@withContext false

            try {
                contentResolver.openOutputStream(uri)?.use { output ->
                    hiddenFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                values.clear()
                values.put(
                    if (isVideo) MediaStore.Video.Media.IS_PENDING
                    else MediaStore.Images.Media.IS_PENDING, 0
                )
                contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                contentResolver.delete(uri, null, null)
                return@withContext false
            }

            // Clean up vault files
            hiddenFile.delete()
            entity.thumbnailPath?.let { File(it).delete() }

            // Remove from DB
            hiddenMediaDao.deleteHiddenMedia(entity)

            true
        }

    suspend fun deleteHiddenMediaPermanently(entity: HiddenMediaEntity) =
        withContext(Dispatchers.IO) {
            File(entity.hiddenPath).delete()
            entity.thumbnailPath?.let { File(it).delete() }
            hiddenMediaDao.deleteHiddenMedia(entity)
        }

    // ---------------------------------------------------------
    // IMPORT FROM URL
    // ---------------------------------------------------------

    suspend fun importImageFromUrl(url: String): Uri = withContext(Dispatchers.IO) {

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Referer", url)
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Failed to download: HTTP ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty response")

        val contentType = body.contentType()?.toString() ?: "image/jpeg"

        // Determine if it's a video or image
        val isVideo = contentType.startsWith("video/")

        val extension = when {
            contentType.contains("gif") -> "gif"
            contentType.contains("png") -> "png"
            contentType.contains("webp") -> "webp"
            contentType.contains("mp4") -> "mp4"
            contentType.contains("webm") -> "webm"
            contentType.contains("video") -> "mp4"
            else -> "jpg"
        }

        val timestamp = System.currentTimeMillis()
        val prefix = if (isVideo) "VID" else "IMG"
        val fileName = "${prefix}_${timestamp}.$extension"

        val collection = if (isVideo)
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val relativePath = if (isVideo) "Movies/Unistream" else "Pictures/Unistream"
        val mimeType = if (isVideo) "video/$extension" else contentType

        val values = ContentValues().apply {
            if (isVideo) {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            } else {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(collection, values)
            ?: throw Exception("MediaStore insert failed")

        contentResolver.openOutputStream(uri)?.use { output ->
            body.byteStream().copyTo(output)
        }

        values.clear()
        values.put(
            if (isVideo) MediaStore.Video.Media.IS_PENDING
            else MediaStore.Images.Media.IS_PENDING, 0
        )
        contentResolver.update(uri, values, null, null)

        uri
    }
}

enum class MediaFilter { ALL, IMAGES, VIDEOS, GIFS, LARGE_FILES, SCREEN_RECORDINGS }
enum class SortOrder { DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC }

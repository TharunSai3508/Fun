package com.unistream.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.unistream.core.network.UrlDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hiddenMediaDao: HiddenMediaDao
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

    fun getHiddenMedia(): Flow<List<HiddenMediaEntity>> =
        hiddenMediaDao.getAllHiddenMedia()

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
     * Import image or GIF from URL into MediaStore
     */
    suspend fun importImageFromUrl(url: String): Uri = withContext(Dispatchers.IO) {

        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Referer", url)
            .build()

        val client = okhttp3.OkHttpClient()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Failed to download image")
        }

        val body = response.body ?: throw Exception("Empty response")

        val mime = body.contentType()?.toString() ?: "image/jpeg"

        val extension = when {
            mime.contains("gif") -> "gif"
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }

        val fileName = "IMG_${System.currentTimeMillis()}.$extension"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Unistream")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw Exception("MediaStore insert failed")

        contentResolver.openOutputStream(uri)?.use { output ->
            body.byteStream().copyTo(output)
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)

        contentResolver.update(uri, values, null, null)

        uri
    }
}

enum class MediaFilter { ALL, IMAGES, VIDEOS, GIFS, LARGE_FILES, SCREEN_RECORDINGS }
enum class SortOrder { DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC }
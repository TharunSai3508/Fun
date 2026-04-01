package com.unistream.gallery.data

import androidx.room.*

// ─────────────────────────────────────────────
// Hidden Media Entity (Encrypted Vault)
// ─────────────────────────────────────────────
@Entity(tableName = "hidden_media")
data class HiddenMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalUri: String,
    val hiddenPath: String,           // Path inside encrypted storage
    val mediaType: String,            // "image" | "video" | "gif"
    val mimeType: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val dateHidden: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null
)

// ─────────────────────────────────────────────
// Gallery Media Model (MediaStore query result)
// ─────────────────────────────────────────────
data class MediaItem(
    val id: Long,
    val uri: android.net.Uri,
    val displayName: String,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Long = 0,          // For videos (ms)
    val bucketName: String = "",     // Album name
    val bucketId: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isGif: Boolean get() = mimeType == "image/gif"
    val isImage: Boolean get() = mimeType.startsWith("image/") && !isGif
}

data class Album(
    val id: Long,
    val name: String,
    val coverUri: android.net.Uri?,
    val mediaCount: Int,
    val bucketId: Long
)

// ─────────────────────────────────────────────
// Hidden Media DAO
// ─────────────────────────────────────────────
@Dao
interface HiddenMediaDao {
    @Query("SELECT * FROM hidden_media ORDER BY dateHidden DESC")
    fun getAllHiddenMedia(): kotlinx.coroutines.flow.Flow<List<HiddenMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenMedia(media: HiddenMediaEntity): Long

    @Delete
    suspend fun deleteHiddenMedia(media: HiddenMediaEntity)

    @Query("SELECT COUNT(*) FROM hidden_media")
    fun getHiddenMediaCount(): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT * FROM hidden_media WHERE id = :id")
    suspend fun getHiddenMediaById(id: Long): HiddenMediaEntity?

    @Query("SELECT * FROM hidden_media")
    suspend fun getAllHiddenMediaOnce(): List<HiddenMediaEntity>
}

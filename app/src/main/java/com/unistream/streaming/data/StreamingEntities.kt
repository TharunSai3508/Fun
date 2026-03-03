package com.unistream.streaming.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
// Playlist Entity
// ─────────────────────────────────────────────
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val thumbnailUri: String? = null
)

// ─────────────────────────────────────────────
// Playlist Item Entity
// ─────────────────────────────────────────────
@Entity(
    tableName = "playlist_items",
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val sourceType: String,         // "local" | "drive" | "url"
    val sourceUri: String,
    val title: String,
    val thumbnailUri: String? = null,
    val durationMs: Long = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

// ─────────────────────────────────────────────
// Watch History Entity
// ─────────────────────────────────────────────
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String,
    val sourceUri: String,
    val title: String,
    val thumbnailUri: String? = null,
    val watchedAt: Long = System.currentTimeMillis(),
    val progressMs: Long = 0,
    val durationMs: Long = 0,
    val profileId: String = "default"
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (progressMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

// ─────────────────────────────────────────────
// Video Source Model (in-memory)
// ─────────────────────────────────────────────
data class VideoSource(
    val type: VideoSourceType,
    val uri: String,
    val title: String,
    val thumbnailUri: String? = null,
    val durationMs: Long = 0,
    val subtitleUri: String? = null
)

enum class VideoSourceType { LOCAL, DRIVE, URL_MP4, URL_HLS, URL_DASH }

// ─────────────────────────────────────────────
// Playlist DAO
// ─────────────────────────────────────────────
@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylistItem(item: PlaylistItemEntity)

    @Query("UPDATE playlist_items SET sortOrder = :order WHERE id = :itemId")
    suspend fun updateItemOrder(itemId: Long, order: Int)
}

// ─────────────────────────────────────────────
// Watch History DAO
// ─────────────────────────────────────────────
@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY watchedAt DESC LIMIT 50")
    fun getWatchHistory(profileId: String = "default"): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE progressPercent < 0.95 AND profileId = :profileId ORDER BY watchedAt DESC LIMIT 20")
    fun getContinueWatching(profileId: String = "default"): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: WatchHistoryEntity)

    @Query("UPDATE watch_history SET progressMs = :progressMs, watchedAt = :watchedAt WHERE sourceUri = :uri AND profileId = :profileId")
    suspend fun updateProgress(uri: String, progressMs: Long, watchedAt: Long, profileId: String = "default")

    @Query("DELETE FROM watch_history WHERE watchedAt < :before")
    suspend fun clearOldHistory(before: Long)

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun clearAllHistory(profileId: String = "default")
}

// Extension for progressPercent in DAO query - computed in Kotlin
private val WatchHistoryEntity.progressPercent: Float
    get() = if (durationMs > 0) progressMs.toFloat() / durationMs else 0f

package com.unistream.webnovel.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
// Novel Entity
// ─────────────────────────────────────────────
@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "Unknown",
    val coverUrl: String? = null,
    val description: String = "",
    val sourceUrl: String,
    val totalChapters: Int = 0,
    val downloadedChapters: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val status: String = "ongoing",      // ongoing | completed
    val genre: String = "",
    val autoUpdate: Boolean = true
)

// ─────────────────────────────────────────────
// Chapter Entity
// ─────────────────────────────────────────────
@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = NovelEntity::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("novelId"), Index("novelId", "chapterNumber", unique = true)]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val chapterNumber: Float,
    val title: String,
    val content: String,            // Full chapter text
    val sourceUrl: String = "",
    val wordCount: Int = 0,
    val isDownloaded: Boolean = false,
    val publishedAt: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
// Bookmark Entity
// ─────────────────────────────────────────────
@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = NovelEntity::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long,
    val chapterId: Long,
    val chapterNumber: Float,
    val textPosition: Int = 0,      // Character offset in chapter
    val highlightedText: String = "",
    val note: String = "",
    val color: String = "#FFD700",  // Highlight color
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
// Reading Progress Entity
// ─────────────────────────────────────────────
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val novelId: Long,
    val lastChapterId: Long = 0,
    val lastChapterNumber: Float = 0f,
    val scrollOffset: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────
// Reader Settings (persisted via DataStore)
// ─────────────────────────────────────────────
data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontSize: Float = 16f,
    val lineSpacing: Float = 1.6f,
    val fontFamily: ReaderFont = ReaderFont.SERIF,
    val autoScrollSpeed: Int = 0,   // 0 = disabled
    val isTextToSpeechEnabled: Boolean = false
)

enum class ReaderTheme(val label: String) {
    LIGHT("Light"), SEPIA("Sepia"), DARK("Dark"), NIGHT("Night")
}

enum class ReaderFont(val label: String) {
    DEFAULT("System"), SERIF("Serif"), SANS("Sans-Serif"), MONO("Monospace")
}

// ─────────────────────────────────────────────
// Novel DAO
// ─────────────────────────────────────────────
@Dao
interface NovelDao {
    @Query("SELECT * FROM novels ORDER BY lastUpdated DESC")
    fun getAllNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteNovels(): Flow<List<NovelEntity>>

    @Query("SELECT * FROM novels WHERE id = :id")
    suspend fun getNovelById(id: Long): NovelEntity?

    @Query("SELECT * FROM novels WHERE sourceUrl = :url LIMIT 1")
    suspend fun getNovelBySource(url: String): NovelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: NovelEntity): Long

    @Update
    suspend fun updateNovel(novel: NovelEntity)

    @Delete
    suspend fun deleteNovel(novel: NovelEntity)

    @Query("UPDATE novels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE novels SET totalChapters = :total, lastUpdated = :updatedAt WHERE id = :id")
    suspend fun updateChapterCount(id: Long, total: Int, updatedAt: Long)

    @Query("UPDATE novels SET downloadedChapters = downloadedChapters + 1 WHERE id = :id")
    suspend fun incrementDownloadedChapters(id: Long)
}


// ─────────────────────────────────────────────
// Chapter DAO
// ─────────────────────────────────────────────
@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    fun getChapters(novelId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND chapterNumber = :number LIMIT 1")
    suspend fun getChapterByNumber(novelId: Long, number: Float): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC LIMIT 1 OFFSET :offset")
    suspend fun getChapterAtOffset(novelId: Long, offset: Int): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId")
    suspend fun getChapterCount(novelId: Long): Int
}

// ─────────────────────────────────────────────
// Bookmark DAO
// ─────────────────────────────────────────────
@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE novelId = :novelId ORDER BY createdAt DESC")
    fun getBookmarks(novelId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM reading_progress WHERE novelId = :novelId")
    suspend fun getReadingProgress(novelId: Long): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgressEntity)
}

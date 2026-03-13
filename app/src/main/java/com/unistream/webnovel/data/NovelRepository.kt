package com.unistream.webnovel.data

import com.unistream.webnovel.parser.NovelParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovelRepository @Inject constructor(
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val parser: NovelParser
) {

    // --------------------------------------------------
    // Library
    // --------------------------------------------------

    fun getAllNovels(): Flow<List<NovelEntity>> =
        novelDao.getAllNovels()

    fun getFavoriteNovels(): Flow<List<NovelEntity>> =
        novelDao.getFavoriteNovels()

    fun getChapters(novelId: Long): Flow<List<ChapterEntity>> =
        chapterDao.getChapters(novelId)

    fun getBookmarks(novelId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarks(novelId)

    // --------------------------------------------------
    // Import novel from URL
    // --------------------------------------------------

    suspend fun importNovel(url: String): Long? =
        withContext(Dispatchers.IO) {

            val parsed = parser.parseNovelPage(url) ?: return@withContext null

            // prevent duplicates
            val existing = novelDao.getNovelBySource(url)
            if (existing != null) return@withContext existing.id

            val novelId = novelDao.insertNovel(
                NovelEntity(
                    title = parsed.title,
                    author = parsed.author,
                    coverUrl = parsed.coverUrl,
                    description = parsed.description,
                    sourceUrl = url,
                    totalChapters = parsed.chapters.size
                )
            )

            val chapters = parsed.chapters.map {
                ChapterEntity(
                    novelId = novelId,
                    chapterNumber = it.number,
                    title = it.title,
                    content = "",
                    sourceUrl = it.url,
                    isDownloaded = false
                )
            }

            chapterDao.insertChapters(chapters)

            novelId
        }

    // --------------------------------------------------
    // Chapter download
    // --------------------------------------------------

    suspend fun downloadChapter(chapter: ChapterEntity): ChapterEntity =
        withContext(Dispatchers.IO) {
            if (chapter.isDownloaded && chapter.content.isNotBlank())
                return@withContext chapter

            val content = parser.parseChapterContent(chapter.sourceUrl)

            // Do not mark as downloaded if content is blank — parser failed
            if (content.isBlank()) return@withContext chapter

            val updated = chapter.copy(
                content = content,
                isDownloaded = true,
                wordCount = content.split("\\s+".toRegex()).size
            )

            chapterDao.insertChapter(updated)

            // Track downloaded chapter count on the parent novel
            novelDao.incrementDownloadedChapters(chapter.novelId)

            updated
        }

    suspend fun getChapterById(id: Long): ChapterEntity? =
        chapterDao.getChapterById(id)

    // --------------------------------------------------
    // Reading progress
    // --------------------------------------------------

    suspend fun getReadingProgress(novelId: Long): ReadingProgressEntity? =
        bookmarkDao.getReadingProgress(novelId)

    suspend fun saveReadingProgress(
        novelId: Long,
        chapterId: Long,
        chapterNumber: Float,
        scrollOffset: Int
    ) {
        bookmarkDao.upsertReadingProgress(
            ReadingProgressEntity(
                novelId = novelId,
                lastChapterId = chapterId,
                lastChapterNumber = chapterNumber,
                scrollOffset = scrollOffset
            )
        )
    }

    // --------------------------------------------------
    // Bookmarks
    // --------------------------------------------------

    suspend fun addBookmark(
        novelId: Long,
        chapterId: Long,
        chapterNumber: Float,
        text: String,
        note: String
    ): Long {

        return bookmarkDao.insertBookmark(
            BookmarkEntity(
                novelId = novelId,
                chapterId = chapterId,
                chapterNumber = chapterNumber,
                highlightedText = text,
                note = note
            )
        )
    }

    // --------------------------------------------------
    // Favorites
    // --------------------------------------------------

    suspend fun toggleFavorite(
        novelId: Long,
        isFavorite: Boolean
    ) {
        novelDao.setFavorite(novelId, isFavorite)
    }

    // --------------------------------------------------
    // Delete
    // --------------------------------------------------

    suspend fun deleteNovel(novel: NovelEntity) {
        novelDao.deleteNovel(novel)
    }
}
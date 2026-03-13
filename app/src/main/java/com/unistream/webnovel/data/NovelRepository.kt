package com.unistream.webnovel.data

import com.unistream.webnovel.parser.NovelParser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovelRepository @Inject constructor(
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val parser: NovelParser
) {
    fun getAllNovels(): Flow<List<NovelEntity>> = novelDao.getAllNovels()
    fun getFavoriteNovels(): Flow<List<NovelEntity>> = novelDao.getFavoriteNovels()
    fun getChapters(novelId: Long): Flow<List<ChapterEntity>> = chapterDao.getChapters(novelId)
    fun getBookmarks(novelId: Long): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarks(novelId)

    suspend fun importNovel(url: String): Long? {
        val parsed = parser.parseNovelPage(url) ?: return null

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

        // Save chapter stubs (without content, download on demand)
        val chapterEntities = parsed.chapters.map { ch ->
            ChapterEntity(
                novelId = novelId,
                chapterNumber = ch.number,
                title = ch.title,
                content = "",
                sourceUrl = ch.url,
                isDownloaded = false
            )
        }
        chapterDao.insertChapters(chapterEntities)

        return novelId
    }

    suspend fun downloadChapter(chapter: ChapterEntity): ChapterEntity {
        if (chapter.isDownloaded && chapter.content.isNotBlank()) return chapter

        val content = parser.parseChapterContent(chapter.sourceUrl)

        // Do not mark as downloaded if content is blank — parser failed or returned error string
        if (content.isBlank()) return chapter

        val updated = chapter.copy(
            content = content,
            isDownloaded = true,
            wordCount = content.split("\\s+".toRegex()).size
        )
        chapterDao.insertChapter(updated)

        // Track how many chapters have been downloaded on the parent novel
        novelDao.incrementDownloadedChapters(chapter.novelId)

        return updated
    }

    suspend fun getChapterById(id: Long): ChapterEntity? = chapterDao.getChapterById(id)

    suspend fun getReadingProgress(novelId: Long): ReadingProgressEntity? =
        bookmarkDao.getReadingProgress(novelId)

    suspend fun saveReadingProgress(novelId: Long, chapterId: Long, chapterNumber: Float, scrollOffset: Int) {
        bookmarkDao.upsertReadingProgress(
            ReadingProgressEntity(
                novelId = novelId,
                lastChapterId = chapterId,
                lastChapterNumber = chapterNumber,
                scrollOffset = scrollOffset
            )
        )
    }

    suspend fun addBookmark(
        novelId: Long,
        chapterId: Long,
        chapterNumber: Float,
        text: String,
        note: String = ""
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

    suspend fun toggleFavorite(novelId: Long, isFavorite: Boolean) {
        novelDao.setFavorite(novelId, isFavorite)
    }

    suspend fun deleteNovel(novel: NovelEntity) {
        novelDao.deleteNovel(novel)
    }
}

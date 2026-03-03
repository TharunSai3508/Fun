package com.unistream.webnovel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unistream.webnovel.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NovelLibraryUiState(
    val novels: List<NovelEntity> = emptyList(),
    val favoriteNovels: List<NovelEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ReaderUiState(
    val novel: NovelEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val currentChapter: ChapterEntity? = null,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val readingProgress: ReadingProgressEntity? = null,
    val readerSettings: ReaderSettings = ReaderSettings(),
    val isLoading: Boolean = false,
    val isDownloadingChapter: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NovelViewModel @Inject constructor(
    private val repository: NovelRepository
) : ViewModel() {

    private val _libraryState = MutableStateFlow(NovelLibraryUiState(isLoading = true))
    val libraryState: StateFlow<NovelLibraryUiState> = _libraryState.asStateFlow()

    private val _readerState = MutableStateFlow(ReaderUiState())
    val readerState: StateFlow<ReaderUiState> = _readerState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            repository.getAllNovels().collect { novels ->
                _libraryState.update { it.copy(novels = novels, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.getFavoriteNovels().collect { favorites ->
                _libraryState.update { it.copy(favoriteNovels = favorites) }
            }
        }
    }

    fun importNovel(url: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                val novelId = repository.importNovel(url)
                if (novelId != null) {
                    _importState.value = ImportState.Success(novelId)
                } else {
                    _importState.value = ImportState.Error("Could not parse the novel from this URL. Try a different source.")
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    // ── Reader Functions ─────────────────────────────────────────────────
    fun loadNovelForReading(novelId: Long, chapterId: Long) {
        viewModelScope.launch {
            _readerState.update { it.copy(isLoading = true) }

            // Load chapters
            repository.getChapters(novelId).collect { chapters ->
                _readerState.update { it.copy(chapters = chapters) }
            }
        }

        viewModelScope.launch {
            // Load bookmarks
            repository.getBookmarks(novelId).collect { bookmarks ->
                _readerState.update { it.copy(bookmarks = bookmarks) }
            }
        }

        viewModelScope.launch {
            val progress = repository.getReadingProgress(novelId)
            _readerState.update { it.copy(readingProgress = progress, isLoading = false) }

            // Load the specific chapter (or last read)
            val targetChapterId = if (chapterId > 0) chapterId else progress?.lastChapterId ?: 0L
            if (targetChapterId > 0) {
                loadChapter(targetChapterId)
            } else {
                // Load first chapter
                val firstChapter = _readerState.value.chapters.firstOrNull()
                if (firstChapter != null) loadChapter(firstChapter.id)
            }
        }
    }

    fun loadChapter(chapterId: Long) {
        viewModelScope.launch {
            _readerState.update { it.copy(isDownloadingChapter = true) }
            val chapter = repository.getChapterById(chapterId)
            if (chapter != null) {
                val downloadedChapter = if (!chapter.isDownloaded) {
                    repository.downloadChapter(chapter)
                } else chapter
                _readerState.update { it.copy(currentChapter = downloadedChapter, isDownloadingChapter = false) }
            } else {
                _readerState.update { it.copy(isDownloadingChapter = false) }
            }
        }
    }

    fun navigateToNextChapter() {
        val current = _readerState.value.currentChapter ?: return
        val chapters = _readerState.value.chapters
        val currentIndex = chapters.indexOfFirst { it.id == current.id }
        val nextChapter = chapters.getOrNull(currentIndex + 1) ?: return
        loadChapter(nextChapter.id)
    }

    fun navigateToPreviousChapter() {
        val current = _readerState.value.currentChapter ?: return
        val chapters = _readerState.value.chapters
        val currentIndex = chapters.indexOfFirst { it.id == current.id }
        val prevChapter = chapters.getOrNull(currentIndex - 1) ?: return
        loadChapter(prevChapter.id)
    }

    fun saveProgress(novelId: Long, scrollOffset: Int) {
        val chapter = _readerState.value.currentChapter ?: return
        viewModelScope.launch {
            repository.saveReadingProgress(novelId, chapter.id, chapter.chapterNumber, scrollOffset)
        }
    }

    fun addBookmark(novelId: Long, selectedText: String, note: String = "") {
        val chapter = _readerState.value.currentChapter ?: return
        viewModelScope.launch {
            repository.addBookmark(novelId, chapter.id, chapter.chapterNumber, selectedText, note)
        }
    }

    fun updateReaderSettings(settings: ReaderSettings) {
        _readerState.update { it.copy(readerSettings = settings) }
    }

    fun toggleFavorite(novel: NovelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(novel.id, !novel.isFavorite)
        }
    }

    fun deleteNovel(novel: NovelEntity) {
        viewModelScope.launch {
            repository.deleteNovel(novel)
        }
    }
}

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val novelId: Long) : ImportState()
    data class Error(val message: String) : ImportState()
}

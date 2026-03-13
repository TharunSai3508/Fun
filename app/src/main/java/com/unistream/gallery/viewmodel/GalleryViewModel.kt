package com.unistream.gallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unistream.core.network.ResolvedMedia
import com.unistream.gallery.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UrlDownloadState {
    object Idle : UrlDownloadState()
    /** Fetching media list from the URL (resolve step). */
    object Resolving : UrlDownloadState()
    /** Saving the chosen media item to gallery storage. */
    object Downloading : UrlDownloadState()
    object Success : UrlDownloadState()
    data class Error(val message: String) : UrlDownloadState()
}

data class GalleryUiState(
    val allMedia: List<MediaItem> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val selectedMedia: Set<Long> = emptySet(),
    val currentFilter: MediaFilter = MediaFilter.ALL,
    val currentSort: SortOrder = SortOrder.DATE_DESC,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val errorMessage: String? = null,
    val urlDownloadState: UrlDownloadState = UrlDownloadState.Idle,
    val showUrlDownloadDialog: Boolean = false,
    /** Media options extracted from the URL — shown after resolve completes. */
    val resolvedMediaOptions: List<ResolvedMedia> = emptyList()
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState(isLoading = true))
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _hiddenMedia = MutableStateFlow<List<HiddenMediaEntity>>(emptyList())
    val hiddenMedia: StateFlow<List<HiddenMediaEntity>> = _hiddenMedia.asStateFlow()

    init {
        loadMedia()
        loadHiddenMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val media = repository.getAllMedia(
                    _uiState.value.currentFilter,
                    _uiState.value.currentSort
                )
                val albums = repository.getAlbums()
                _uiState.update {
                    it.copy(
                        allMedia = media,
                        filteredMedia = applySearch(media, it.searchQuery),
                        albums = albums,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message)
                }
            }
        }
    }

    private fun loadHiddenMedia() {
        viewModelScope.launch {
            repository.getHiddenMedia().collect { hidden ->
                _hiddenMedia.value = hidden
            }
        }
    }

    fun setFilter(filter: MediaFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
        loadMedia()
    }

    fun setSort(sort: SortOrder) {
        _uiState.update { it.copy(currentSort = sort) }
        loadMedia()
    }

    fun search(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredMedia = applySearch(state.allMedia, query)
            )
        }
    }

    private fun applySearch(media: List<MediaItem>, query: String): List<MediaItem> {
        if (query.isBlank()) return media
        return media.filter { item ->
            item.displayName.contains(query, ignoreCase = true) ||
            item.bucketName.contains(query, ignoreCase = true)
        }
    }

    fun toggleSelection(mediaId: Long) {
        _uiState.update { state ->
            val newSelected = state.selectedMedia.toMutableSet()
            if (newSelected.contains(mediaId)) newSelected.remove(mediaId)
            else newSelected.add(mediaId)
            state.copy(
                selectedMedia = newSelected,
                isSelectionMode = newSelected.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.filteredMedia.map { it.id }.toSet()
            state.copy(selectedMedia = allIds, isSelectionMode = allIds.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedMedia = emptySet(), isSelectionMode = false) }
    }

    fun toggleFavorite(mediaId: Long) {
        _uiState.update { state ->
            val newFavorites = state.favorites.toMutableSet()
            if (newFavorites.contains(mediaId)) newFavorites.remove(mediaId)
            else newFavorites.add(mediaId)
            state.copy(favorites = newFavorites)
        }
    }

    fun hideSelectedMedia() {
        viewModelScope.launch {
            val toHide = _uiState.value.selectedMedia.mapNotNull { id ->
                _uiState.value.allMedia.find { it.id == id }
            }
            // In production: copy file to encrypted storage, then delete from MediaStore
            toHide.forEach { media ->
                val hiddenPath = "hidden/${media.displayName}"
                repository.hideMedia(media, hiddenPath)
            }
            clearSelection()
            loadMedia()
        }
    }

    fun showUrlDownloadDialog() {
        _uiState.update { it.copy(showUrlDownloadDialog = true, resolvedMediaOptions = emptyList(), urlDownloadState = UrlDownloadState.Idle) }
    }

    fun dismissUrlDownloadDialog() {
        _uiState.update {
            it.copy(showUrlDownloadDialog = false, urlDownloadState = UrlDownloadState.Idle, resolvedMediaOptions = emptyList())
        }
    }

    /**
     * Step 1 — resolve [url] to a list of media options.
     * Results are stored in [GalleryUiState.resolvedMediaOptions].
     * If only one option is found and it is a direct link, skip the picker and download immediately.
     */
    fun resolveUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(urlDownloadState = UrlDownloadState.Resolving, resolvedMediaOptions = emptyList()) }
            val options = repository.resolveMediaFromUrl(url)
            when {
                options.isEmpty() -> _uiState.update {
                    it.copy(urlDownloadState = UrlDownloadState.Error("No media found at this URL. Try pasting the direct file link."))
                }
                options.size == 1 -> {
                    // Skip picker — download the single found item automatically
                    _uiState.update { it.copy(resolvedMediaOptions = options) }
                    downloadResolvedMedia(options.first())
                }
                else -> _uiState.update {
                    it.copy(resolvedMediaOptions = options, urlDownloadState = UrlDownloadState.Idle)
                }
            }
        }
    }

    /**
     * Step 2 — download a specific [ResolvedMedia] item that was returned by [resolveUrl].
     */
    fun downloadResolvedMedia(media: ResolvedMedia) {
        viewModelScope.launch {
            _uiState.update { it.copy(urlDownloadState = UrlDownloadState.Downloading) }
            val success = repository.downloadResolvedMedia(media)
            _uiState.update {
                it.copy(
                    urlDownloadState = if (success) UrlDownloadState.Success
                    else UrlDownloadState.Error("Download failed. Check your storage permissions.")
                )
            }
            if (success) {
                loadMedia()
            }
        }
    }

    /** Legacy convenience for direct URL downloads (no resolve step). */
    fun downloadUrlToGallery(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(urlDownloadState = UrlDownloadState.Downloading) }
            val success = repository.downloadMediaFromUrl(url)
            _uiState.update {
                it.copy(
                    urlDownloadState = if (success) UrlDownloadState.Success
                    else UrlDownloadState.Error("Download failed. Check the URL and try again.")
                )
            }
            if (success) loadMedia()
        }
    }

    fun resetUrlDownloadState() {
        _uiState.update { it.copy(urlDownloadState = UrlDownloadState.Idle, resolvedMediaOptions = emptyList()) }
    }
}

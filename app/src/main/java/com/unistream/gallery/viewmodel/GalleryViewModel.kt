package com.unistream.gallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unistream.gallery.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val isImporting: Boolean = false,
    val importSuccess: Boolean = false
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
            toHide.forEach { media ->
                try {
                    repository.hideMedia(media, "")
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Failed to hide: ${e.message}") }
                }
            }
            clearSelection()
            loadMedia()
        }
    }

    fun unhideMedia(entity: HiddenMediaEntity) {
        viewModelScope.launch {
            try {
                val success = repository.unhideMedia(entity)
                if (success) {
                    loadMedia()
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to restore media") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun deleteHiddenMedia(entity: HiddenMediaEntity) {
        viewModelScope.launch {
            try {
                repository.deleteHiddenMediaPermanently(entity)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun importFromUrl(url: String) = viewModelScope.launch {
        _uiState.update { it.copy(isImporting = true, errorMessage = null, importSuccess = false) }
        try {
            repository.importImageFromUrl(url)
            _uiState.update { it.copy(isImporting = false, importSuccess = true) }
            loadMedia()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isImporting = false, errorMessage = "Import failed: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearImportSuccess() {
        _uiState.update { it.copy(importSuccess = false) }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.filteredMedia.map { media -> media.id }.toSet()
            state.copy(
                selectedMedia = allIds,
                isSelectionMode = true
            )
        }
    }
}

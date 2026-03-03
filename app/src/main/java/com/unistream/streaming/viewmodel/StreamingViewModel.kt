package com.unistream.streaming.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unistream.streaming.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StreamingUiState(
    val localVideos: List<VideoSource> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val continueWatching: List<WatchHistoryEntity> = emptyList(),
    val watchHistory: List<WatchHistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val showUrlDialog: Boolean = false,
    val urlInput: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class StreamingViewModel @Inject constructor(
    private val repository: StreamingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamingUiState(isLoading = true))
    val uiState: StateFlow<StreamingUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            // Load local videos
            val localVideos = repository.getLocalVideos()
            _uiState.update { it.copy(localVideos = localVideos, isLoading = false) }
        }

        // Observe playlists
        viewModelScope.launch {
            repository.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }

        // Observe continue watching
        viewModelScope.launch {
            repository.getContinueWatching().collect { continueWatching ->
                _uiState.update { it.copy(continueWatching = continueWatching) }
            }
        }

        // Observe history
        viewModelScope.launch {
            repository.getWatchHistory().collect { history ->
                _uiState.update { it.copy(watchHistory = history) }
            }
        }
    }

    fun setUrlInput(url: String) {
        _uiState.update { it.copy(urlInput = url) }
    }

    fun showUrlDialog(show: Boolean) {
        _uiState.update { it.copy(showUrlDialog = show) }
    }

    fun prepareUrlSource(url: String): VideoSource? {
        if (url.isBlank()) return null
        val processedUrl = if (url.contains("drive.google.com")) {
            repository.convertDriveUrl(url)
        } else url
        val type = repository.detectSourceType(processedUrl)
        return VideoSource(
            type = type,
            uri = processedUrl,
            title = url.substringAfterLast("/").ifBlank { "Stream" }
        )
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun addToPlaylist(playlistId: Long, source: VideoSource) {
        viewModelScope.launch {
            repository.addToPlaylist(playlistId, source)
        }
    }

    fun recordWatch(source: VideoSource) {
        viewModelScope.launch {
            repository.recordWatch(source)
        }
    }

    fun updateProgress(uri: String, progressMs: Long) {
        viewModelScope.launch {
            repository.updateProgress(uri, progressMs)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

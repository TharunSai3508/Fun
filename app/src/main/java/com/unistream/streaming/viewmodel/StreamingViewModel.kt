package com.unistream.streaming.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unistream.streaming.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StreamingUiState(
    val localVideos: List<VideoSource> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val continueWatching: List<WatchHistoryEntity> = emptyList(),
    val watchHistory: List<WatchHistoryEntity> = emptyList(),
    val hiddenVideos: List<HiddenVideoEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val showUrlDialog: Boolean = false,
    val urlInput: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
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

    // ---------------------------------------------------------
    // INITIAL LOAD
    // ---------------------------------------------------------

    private fun loadContent() {

        viewModelScope.launch {

            val localVideos = repository.getLocalVideos()

            _uiState.update {
                it.copy(
                    localVideos = localVideos,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            repository.getAllPlaylists().collect {
                _uiState.update { s -> s.copy(playlists = it) }
            }
        }

        viewModelScope.launch {
            repository.getContinueWatching().collect {
                _uiState.update { s -> s.copy(continueWatching = it) }
            }
        }

        viewModelScope.launch {
            repository.getWatchHistory().collect {
                _uiState.update { s -> s.copy(watchHistory = it) }
            }
        }

        viewModelScope.launch {
            repository.getHiddenVideos().collect {
                _uiState.update { s -> s.copy(hiddenVideos = it) }
            }
        }
    }

    // ---------------------------------------------------------
    // URL INPUT
    // ---------------------------------------------------------

    fun setUrlInput(url: String) {
        _uiState.update { it.copy(urlInput = url) }
    }

    fun showUrlDialog(show: Boolean) {
        _uiState.update { it.copy(showUrlDialog = show) }
    }

    // ---------------------------------------------------------
    // PREPARE STREAM SOURCE
    // ---------------------------------------------------------

    suspend fun prepareUrlSource(url: String): VideoSource? {

        if (url.isBlank()) return null

        return try {

            // Normalize URL first
            val normalized = repository.playFromUrl(url)

            // Try resolving embedded video
            val resolved = repository.resolvePlayableUrl(normalized) ?: normalized

            val type = repository.detectSourceType(resolved)

            VideoSource(
                type = type,
                uri = resolved,
                title = resolved.substringAfterLast("/").ifBlank { "Stream" }
            )

        } catch (e: Exception) {

            _uiState.update {
                it.copy(errorMessage = e.message ?: "Invalid stream URL")
            }

            null
        }
    }

    // ---------------------------------------------------------
    // DOWNLOAD VIDEO
    // ---------------------------------------------------------

    fun downloadVideo(url: String) {

        viewModelScope.launch(Dispatchers.IO) {

            _uiState.update { it.copy(isDownloading = true) }

            val uri = repository.downloadFromUrl(url)

            _uiState.update {

                it.copy(
                    isDownloading = false,
                    errorMessage = if (uri == null)
                        "Download failed"
                    else
                        null
                )
            }
        }
    }

    // ---------------------------------------------------------
    // PLAYLIST
    // ---------------------------------------------------------

    fun createPlaylist(name: String) {

        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun addToPlaylist(
        playlistId: Long,
        source: VideoSource
    ) {

        viewModelScope.launch {
            repository.addToPlaylist(playlistId, source)
        }
    }

    // ---------------------------------------------------------
    // WATCH HISTORY
    // ---------------------------------------------------------

    fun recordWatch(source: VideoSource) {

        viewModelScope.launch {
            repository.recordWatch(source)
        }
    }

    fun updateProgress(
        uri: String,
        progressMs: Long
    ) {

        viewModelScope.launch {
            repository.updateProgress(uri, progressMs)
        }
    }

    // ---------------------------------------------------------
    // ERROR HANDLING
    // ---------------------------------------------------------

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ---------------------------------------------------------
    // HIDDEN VIDEOS
    // ---------------------------------------------------------

    fun hideVideo(source: VideoSource) {
        viewModelScope.launch {
            try {
                repository.hideVideo(source)
                _uiState.update { it.copy(successMessage = "Video hidden") }
                // Reload local videos
                val videos = repository.getLocalVideos()
                _uiState.update { it.copy(localVideos = videos) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to hide: ${e.message}") }
            }
        }
    }

    fun unhideVideo(entity: HiddenVideoEntity) {
        viewModelScope.launch {
            try {
                val success = repository.unhideVideo(entity)
                if (success) {
                    _uiState.update { it.copy(successMessage = "Video restored") }
                    val videos = repository.getLocalVideos()
                    _uiState.update { it.copy(localVideos = videos) }
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to restore video") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun deleteHiddenVideo(entity: HiddenVideoEntity) {
        viewModelScope.launch {
            try {
                repository.deleteHiddenVideoPermanently(entity)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error: ${e.message}") }
            }
        }
    }
}
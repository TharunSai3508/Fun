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
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
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
        _uiState.update { it.copy(errorMessage = null) }
    }
}
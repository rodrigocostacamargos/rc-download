package com.rcdownload.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcdownload.data.api.models.VideoMetadata
import com.rcdownload.data.extractor.StreamData
import com.rcdownload.data.repository.VideoRepository
import com.rcdownload.utils.YouTubeUrlValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados possíveis da tela principal. */
sealed class UiState {
    object Idle : UiState()
    object LoadingMetadata : UiState()
    data class MetadataLoaded(val metadata: VideoMetadata) : UiState()
    object PreparingStream : UiState()
    data class DownloadEnqueued(val streamData: StreamData) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(
    private val repository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Valida a URL e busca metadados via NewPipe Extractor. */
    fun fetchMetadata(url: String) {
        if (YouTubeUrlValidator.extractVideoId(url) == null) {
            Log.w(TAG, "fetchMetadata: URL inválida — $url")
            _uiState.value = UiState.Error("URL inválida. Use um link válido do YouTube.")
            return
        }

        Log.d(TAG, "fetchMetadata: url=$url")
        viewModelScope.launch {
            _uiState.value = UiState.LoadingMetadata
            repository.fetchVideoMetadata(url)
                .onSuccess {
                    Log.i(TAG, "fetchMetadata: sucesso — ${it.title}")
                    _uiState.value = UiState.MetadataLoaded(it)
                }
                .onFailure {
                    Log.e(TAG, "fetchMetadata: falha", it)
                    _uiState.value = UiState.Error(it.message ?: "Erro ao buscar metadados.")
                }
        }
    }

    /** Extrai a URL de stream via NewPipe e enfileira o download no DownloadManager. */
    fun startDownload(format: String, metadata: VideoMetadata) {
        Log.d(TAG, "startDownload: format=$format")
        viewModelScope.launch {
            _uiState.value = UiState.PreparingStream
            repository.getStreamData(format)
                .onSuccess { streamData ->
                    Log.i(TAG, "startDownload: stream pronta — ${streamData.fileName}")
                    repository.saveToHistory(metadata, format)
                    _uiState.value = UiState.DownloadEnqueued(streamData)
                }
                .onFailure {
                    Log.e(TAG, "startDownload: falha", it)
                    _uiState.value = UiState.Error(it.message ?: "Erro ao preparar download.")
                }
        }
    }

    /** Retorna ao estado inicial. */
    fun reset() {
        _uiState.value = UiState.Idle
    }

    companion object {
        private const val TAG = "RCDownload"
    }
}

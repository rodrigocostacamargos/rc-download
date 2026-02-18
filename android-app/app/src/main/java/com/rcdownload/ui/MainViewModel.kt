package com.rcdownload.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import java.io.File
import androidx.lifecycle.viewModelScope
import com.rcdownload.data.api.models.VideoMetadata
import com.rcdownload.data.repository.VideoRepository
import com.rcdownload.utils.YouTubeUrlValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados possíveis da tela principal — cada tela reage a exatamente um estado. */
sealed class UiState {
    object Idle : UiState()
    object LoadingMetadata : UiState()
    data class MetadataLoaded(val metadata: VideoMetadata) : UiState()
    object Downloading : UiState()
    data class DownloadProgress(val progress: Int) : UiState()
    data class DownloadComplete(val jobId: String, val fileName: String) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(
    private val repository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    /** Valida a URL, extrai o ID e busca metadados na YouTube API. */
    fun fetchMetadata(url: String) {
        val videoId = YouTubeUrlValidator.extractVideoId(url)
        if (videoId == null) {
            Log.w(TAG, "fetchMetadata: URL inválida — $url")
            _uiState.value = UiState.Error("URL inválida. Use um link válido do YouTube.")
            return
        }

        Log.d(TAG, "fetchMetadata: url=$url videoId=$videoId")
        viewModelScope.launch {
            _uiState.value = UiState.LoadingMetadata
            repository.fetchVideoMetadata(videoId, url)
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

    /** Inicia o download via serviço local e começa o polling de progresso. */
    fun startDownload(url: String, format: String, metadata: VideoMetadata) {
        Log.d(TAG, "startDownload: url=$url format=$format")
        viewModelScope.launch {
            _uiState.value = UiState.Downloading
            repository.startDownload(url, format)
                .onSuccess { jobId ->
                    Log.i(TAG, "startDownload: jobId=$jobId — iniciando polling")
                    pollStatus(jobId, metadata, format)
                }
                .onFailure {
                    Log.e(TAG, "startDownload: falha ao contatar serviço local", it)
                    _uiState.value = UiState.Error(
                        "Erro ao iniciar download: ${it.message}"
                    )
                }
        }
    }

    /** Consulta o status a cada 1,5s até completar ou falhar. */
    private fun pollStatus(jobId: String, metadata: VideoMetadata, format: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                repository.pollDownloadStatus(jobId)
                    .onSuccess { status ->
                        when (status.status) {
                            "pending", "downloading" ->
                                _uiState.value = UiState.DownloadProgress(status.progress)

                            "completed" -> {
                                val fileName = status.filePath
                                    ?.let { File(it).name }
                                    ?: "download"
                                repository.saveToHistory(metadata, format, status.filePath)
                                _uiState.value = UiState.DownloadComplete(jobId, fileName)
                                return@launch
                            }

                            "error" -> {
                                _uiState.value = UiState.Error(
                                    status.error ?: "Erro durante o download."
                                )
                                return@launch
                            }
                        }
                    }
                    .onFailure {
                        Log.e(TAG, "pollStatus: falha ao verificar status jobId=$jobId", it)
                        _uiState.value = UiState.Error("Erro ao verificar status: ${it.message}")
                        return@launch
                    }

                delay(1_500)
            }
        }
    }

    companion object {
        private const val TAG = "RCDownload"
    }

    /** Retorna ao estado inicial e cancela qualquer polling ativo. */
    fun reset() {
        pollingJob?.cancel()
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

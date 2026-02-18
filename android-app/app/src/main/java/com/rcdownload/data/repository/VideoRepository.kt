package com.rcdownload.data.repository

import android.util.Log
import com.rcdownload.data.api.LocalDownloadService
import com.rcdownload.data.api.YouTubeApiService
import com.rcdownload.data.api.models.DownloadRequest
import com.rcdownload.data.api.models.DownloadStatus
import com.rcdownload.data.api.models.VideoMetadata
import com.rcdownload.data.db.DownloadHistoryDao
import com.rcdownload.data.db.DownloadHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Único ponto de acesso a dados da UI.
 * Orquestra YouTube API, serviço local de download e banco Room.
 */
private const val TAG = "RCDownload"

class VideoRepository(
    private val youTubeApiService: YouTubeApiService,
    private val localDownloadService: LocalDownloadService,
    private val downloadHistoryDao: DownloadHistoryDao,
    private val apiKey: String
) {

    /**
     * Consulta a YouTube Data API v3 e retorna os metadados do vídeo.
     * Falha com mensagem descritiva em caso de vídeo não encontrado ou erro de rede.
     */
    suspend fun fetchVideoMetadata(videoId: String, originalUrl: String): Result<VideoMetadata> =
        runCatching {
            Log.d(TAG, "fetchVideoMetadata: videoId=$videoId")
            val response = youTubeApiService.getVideoMetadata(
                videoId = videoId,
                apiKey  = apiKey
            )
            val item = response.items?.firstOrNull()
                ?: error("Vídeo não encontrado. Verifique se a URL é pública e válida.")

            VideoMetadata(
                videoId      = item.id,
                title        = item.snippet.title,
                channelTitle = item.snippet.channelTitle,
                originalUrl  = originalUrl,
                license      = item.status.license,
                thumbnailUrl = item.snippet.thumbnails?.medium?.url
            ).also { Log.i(TAG, "fetchVideoMetadata: sucesso — title=${it.title}") }
        }.onFailure { Log.e(TAG, "fetchVideoMetadata: falha", it) }

    /**
     * Envia a requisição de download ao serviço local (Flask + yt-dlp).
     * Retorna o job_id para polling de progresso.
     */
    suspend fun startDownload(url: String, format: String): Result<String> =
        runCatching {
            Log.d(TAG, "startDownload: url=$url format=$format")
            val response = localDownloadService.startDownload(
                DownloadRequest(url = url, format = format)
            )
            response.jobId.also { Log.i(TAG, "startDownload: jobId=$it") }
        }.onFailure { Log.e(TAG, "startDownload: falha", it) }

    /** Consulta o status atual de um job de download. */
    suspend fun pollDownloadStatus(jobId: String): Result<DownloadStatus> =
        runCatching {
            localDownloadService.getDownloadStatus(jobId)
                .also { Log.d(TAG, "pollDownloadStatus: jobId=$jobId status=${it.status} progress=${it.progress}") }
        }.onFailure { Log.e(TAG, "pollDownloadStatus: falha jobId=$jobId", it) }

    /** Verifica se o serviço local está acessível antes de tentar download. */
    suspend fun isLocalServiceAvailable(): Boolean =
        runCatching { localDownloadService.health() }
            .onSuccess { Log.i(TAG, "isLocalServiceAvailable: OK") }
            .onFailure { Log.e(TAG, "isLocalServiceAvailable: falha", it) }
            .isSuccess

    /** Persiste o registro do download no banco local para histórico e atribuição. */
    suspend fun saveToHistory(
        metadata: VideoMetadata,
        format: String,
        filePath: String? = null
    ) {
        downloadHistoryDao.insert(
            DownloadHistoryEntity(
                videoId      = metadata.videoId,
                title        = metadata.title,
                channelTitle = metadata.channelTitle,
                originalUrl  = metadata.originalUrl,
                license      = metadata.license,
                format       = format,
                filePath     = filePath
            )
        )
    }

    /** Flow que emite a lista de downloads ao vivo — atualiza automaticamente. */
    fun getDownloadHistory(): Flow<List<DownloadHistoryEntity>> =
        downloadHistoryDao.getAllHistory()
}

package com.rcdownload.data.repository

import android.util.Log
import com.rcdownload.data.api.models.VideoMetadata
import com.rcdownload.data.db.DownloadHistoryDao
import com.rcdownload.data.db.DownloadHistoryEntity
import com.rcdownload.data.extractor.StreamData
import com.rcdownload.data.extractor.StreamExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfo

private const val TAG = "RCDownload"

/**
 * Único ponto de acesso a dados da UI.
 * Usa NewPipe Extractor para metadados e URLs de stream — sem servidor externo.
 */
class VideoRepository(
    private val downloadHistoryDao: DownloadHistoryDao
) {
    // StreamInfo cacheado entre fetchVideoMetadata e getStreamData
    private var cachedStreamInfo: StreamInfo? = null

    /**
     * Extrai metadados do vídeo via NewPipe (sem API key).
     * A StreamInfo é cacheada para reutilização em getStreamData.
     */
    suspend fun fetchVideoMetadata(url: String): Result<VideoMetadata> =
        withContext(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "fetchVideoMetadata: url=$url")
                val streamInfo = StreamExtractor.getStreamInfo(url)
                cachedStreamInfo = streamInfo

                VideoMetadata(
                    videoId      = streamInfo.id,
                    title        = streamInfo.name,
                    channelTitle = streamInfo.uploaderName,
                    originalUrl  = url,
                    license      = streamInfo.licence ?: "youtube",
                    thumbnailUrl = streamInfo.thumbnails.maxByOrNull { it.height }?.url
                ).also { Log.i(TAG, "fetchVideoMetadata: sucesso — title=${it.title}") }
            }.onFailure { Log.e(TAG, "fetchVideoMetadata: falha", it) }
        }

    /**
     * Retorna a URL de stream para o formato solicitado usando a StreamInfo em cache.
     * Deve ser chamado após fetchVideoMetadata.
     */
    suspend fun getStreamData(format: String): Result<StreamData> =
        withContext(Dispatchers.IO) {
            runCatching {
                val streamInfo = cachedStreamInfo
                    ?: error("Metadados não carregados. Verifique a URL primeiro.")

                Log.d(TAG, "getStreamData: format=$format")
                val streamData = when (format) {
                    "audio" -> StreamExtractor.getBestAudioStream(streamInfo)
                        ?: error("Nenhuma stream de áudio M4A disponível para este vídeo.")
                    else    -> StreamExtractor.getBestVideoStream(streamInfo)
                        ?: error("Nenhuma stream de vídeo MP4 disponível para este vídeo.")
                }
                Log.i(TAG, "getStreamData: fileName=${streamData.fileName}")
                streamData
            }.onFailure { Log.e(TAG, "getStreamData: falha", it) }
        }

    /** Persiste o registro do download no banco local para histórico. */
    suspend fun saveToHistory(metadata: VideoMetadata, format: String, filePath: String? = null) {
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

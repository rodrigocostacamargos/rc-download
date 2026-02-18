package com.rcdownload.data.api

import com.rcdownload.data.api.models.DownloadRequest
import com.rcdownload.data.api.models.DownloadResponse
import com.rcdownload.data.api.models.DownloadStatus
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Interface Retrofit para o serviço local Flask + yt-dlp. */
interface LocalDownloadService {

    /**
     * Inicia um download em background no serviço local.
     * Retorna imediatamente com um job_id para acompanhamento.
     */
    @POST("download")
    suspend fun startDownload(@Body request: DownloadRequest): DownloadResponse

    /**
     * Consulta o progresso de um download em andamento.
     * Chame periodicamente (polling) até status == "completed" ou "error".
     */
    @GET("status/{jobId}")
    suspend fun getDownloadStatus(@Path("jobId") jobId: String): DownloadStatus

    /** Health check — verifica se o serviço local está ativo antes de tentar baixar. */
    @GET("health")
    suspend fun health(): Map<String, String>
}

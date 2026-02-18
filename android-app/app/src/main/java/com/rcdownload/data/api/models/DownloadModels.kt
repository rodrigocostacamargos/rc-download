package com.rcdownload.data.api.models

import com.google.gson.annotations.SerializedName

/** Enviado ao serviço local para iniciar um download. */
data class DownloadRequest(
    @SerializedName("url")    val url: String,
    @SerializedName("format") val format: String   // "video" ou "audio"
)

/** Retornado imediatamente ao iniciar o download — contém o job_id para polling. */
data class DownloadResponse(
    @SerializedName("job_id")  val jobId: String,
    @SerializedName("status")  val status: String,
    @SerializedName("message") val message: String? = null
)

/** Status de um job em andamento — consultado por polling. */
data class DownloadStatus(
    @SerializedName("job_id")    val jobId: String,
    @SerializedName("status")    val status: String,    // "pending" | "downloading" | "completed" | "error"
    @SerializedName("progress")  val progress: Int = 0, // 0-100
    @SerializedName("file_path") val filePath: String? = null,
    @SerializedName("error")     val error: String? = null
)

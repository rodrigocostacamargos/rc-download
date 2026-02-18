package com.rcdownload.data.api

import com.rcdownload.data.api.models.YouTubeApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** Interface Retrofit para a YouTube Data API v3. */
interface YouTubeApiService {

    /**
     * Busca metadados de um vídeo.
     * @param videoId  ID de 11 caracteres do vídeo.
     * @param part     Partes a retornar — precisamos de snippet (título/canal) e status (licença).
     * @param apiKey   Chave da API — injetada via BuildConfig, nunca hardcoded.
     */
    @GET("videos")
    suspend fun getVideoMetadata(
        @Query("id")   videoId: String,
        @Query("part") part: String = "snippet,status",
        @Query("key")  apiKey: String
    ): YouTubeApiResponse
}

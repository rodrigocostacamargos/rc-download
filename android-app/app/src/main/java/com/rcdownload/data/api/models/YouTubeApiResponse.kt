package com.rcdownload.data.api.models

import com.google.gson.annotations.SerializedName

/** Resposta raiz da YouTube Data API v3 para o endpoint /videos. */
data class YouTubeApiResponse(
    @SerializedName("items") val items: List<YouTubeVideoItem>?
)

data class YouTubeVideoItem(
    @SerializedName("id")      val id: String,
    @SerializedName("snippet") val snippet: VideoSnippet,
    @SerializedName("status")  val status: VideoStatus
)

data class VideoSnippet(
    @SerializedName("title")        val title: String,
    @SerializedName("channelTitle") val channelTitle: String,
    @SerializedName("thumbnails")   val thumbnails: ThumbnailData?
)

/** O campo "license" retorna "youtube" ou "creativeCommon". */
data class VideoStatus(
    @SerializedName("license") val license: String
)

data class ThumbnailData(
    @SerializedName("medium") val medium: Thumbnail?,
    @SerializedName("high")   val high: Thumbnail?
)

data class Thumbnail(
    @SerializedName("url") val url: String
)

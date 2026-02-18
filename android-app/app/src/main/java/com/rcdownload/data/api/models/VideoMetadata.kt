package com.rcdownload.data.api.models

/**
 * Representa os metadados de um vídeo do YouTube após consulta à Data API v3.
 */
data class VideoMetadata(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val originalUrl: String,
    val license: String,           // "youtube" ou "creativeCommon"
    val thumbnailUrl: String? = null
) {
    /** True quando a licença retornada pela API for Creative Commons. */
    val isCreativeCommons: Boolean
        get() = license == "creativeCommon"

    /** Nome amigável da licença para exibição na UI. */
    val licenseDisplayName: String
        get() = when {
            license.contains("Creative Commons", ignoreCase = true) -> "Creative Commons"
            license.contains("youtube", ignoreCase = true)          -> "Licença padrão do YouTube"
            license == "creativeCommon"                              -> "Creative Commons (CC BY)"
            else                                                     -> license
        }
}

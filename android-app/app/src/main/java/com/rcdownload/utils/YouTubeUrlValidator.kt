package com.rcdownload.utils

/**
 * Valida URLs do YouTube e extrai o ID do vídeo.
 * Suporta os formatos mais comuns: watch, youtu.be, shorts e embed.
 */
object YouTubeUrlValidator {

    private val PATTERNS = listOf(
        // https://www.youtube.com/watch?v=XXXXXXXXXXX  (com ou sem parâmetros extras)
        Regex("""^https?://(?:www\.)?youtube\.com/watch\?(?:[^&]*&)*v=([a-zA-Z0-9_-]{11})"""),
        // https://youtu.be/XXXXXXXXXXX
        Regex("""^https?://youtu\.be/([a-zA-Z0-9_-]{11})"""),
        // https://www.youtube.com/shorts/XXXXXXXXXXX
        Regex("""^https?://(?:www\.)?youtube\.com/shorts/([a-zA-Z0-9_-]{11})"""),
        // https://www.youtube.com/embed/XXXXXXXXXXX
        Regex("""^https?://(?:www\.)?youtube\.com/embed/([a-zA-Z0-9_-]{11})"""),
    )

    /** Retorna true se a URL pertencer ao YouTube e tiver um ID de vídeo válido. */
    fun isValid(url: String): Boolean = extractVideoId(url) != null

    /**
     * Extrai o ID de 11 caracteres do vídeo a partir da URL.
     * Retorna null se a URL for inválida ou não reconhecida.
     */
    fun extractVideoId(url: String): String? {
        val trimmed = url.trim()
        for (pattern in PATTERNS) {
            val match = pattern.find(trimmed)
            if (match != null) return match.groupValues[1]
        }
        return null
    }
}

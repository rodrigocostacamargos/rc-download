package com.rcdownload.data.extractor

import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * Wrapper em torno do NewPipe Extractor.
 * Todas as funções fazem I/O de rede — chamar sempre em Dispatchers.IO.
 */
object StreamExtractor {

    /** Extrai todas as informações do vídeo (metadados + streams disponíveis). */
    fun getStreamInfo(url: String): StreamInfo =
        StreamInfo.getInfo(NewPipe.getService(0), url)

    /**
     * Retorna a melhor stream progressiva MP4 (vídeo + áudio já mesclados).
     * Streams progressivas existem até 720p — suficiente para mobile.
     */
    fun getBestVideoStream(streamInfo: StreamInfo): StreamData? {
        val stream = streamInfo.videoStreams
            .filter { it.format == MediaFormat.MPEG_4 }
            .maxByOrNull { parseResolution(it.resolution) }
            ?: return null

        return StreamData(
            url      = stream.content,
            fileName = "${sanitize(streamInfo.name)}.mp4",
            mimeType = "video/mp4"
        )
    }

    /**
     * Retorna a melhor stream de áudio M4A (AAC).
     * Reproduzido nativamente pelo Android sem conversão.
     */
    fun getBestAudioStream(streamInfo: StreamInfo): StreamData? {
        val stream = streamInfo.audioStreams
            .filter { it.format == MediaFormat.M4A }
            .maxByOrNull { it.averageBitrate }
            ?: return null

        return StreamData(
            url      = stream.content,
            fileName = "${sanitize(streamInfo.name)}.m4a",
            mimeType = "audio/mp4"
        )
    }

    private fun parseResolution(resolution: String): Int =
        resolution.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

    /** Remove caracteres inválidos em nomes de arquivo. */
    private fun sanitize(name: String): String =
        name.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()
}

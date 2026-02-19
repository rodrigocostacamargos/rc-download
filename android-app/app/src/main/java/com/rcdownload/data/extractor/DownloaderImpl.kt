package com.rcdownload.data.extractor

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request as OkRequest
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

/**
 * Implementação do Downloader do NewPipe Extractor usando OkHttp.
 * User-Agent de browser é obrigatório — sem ele o YouTube retorna 400.
 * Baseado na implementação oficial do app NewPipe.
 */
class DownloaderImpl(builder: OkHttpClient.Builder) : Downloader() {

    private val client: OkHttpClient = builder
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url       = request.url()
        val headers   = request.headers()
        val data      = request.dataToSend()

        val body = data?.toRequestBody(null)

        val requestBuilder = OkRequest.Builder()
            .method(httpMethod, body)
            .url(url)
            .addHeader("User-Agent", USER_AGENT)   // obrigatório para o YouTube aceitar

        headers.forEach { (name, values) ->
            requestBuilder.removeHeader(name)
            values.forEach { value -> requestBuilder.addHeader(name, value) }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            throw ReCaptchaException("429 Too Many Requests", url)
        }

        val responseBody = response.body?.string()
        val latestUrl    = response.request.url.toString()

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBody,
            latestUrl
        )
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}

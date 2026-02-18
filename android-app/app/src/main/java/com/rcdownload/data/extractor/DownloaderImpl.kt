package com.rcdownload.data.extractor

import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/**
 * Implementação do Downloader do NewPipe Extractor usando OkHttp.
 * Necessária para que o NewPipe faça requisições HTTP.
 */
class DownloaderImpl(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url       = request.url()
        val headers   = request.headers()
        val body      = request.dataToSend()

        val requestBuilder = OkRequest.Builder().url(url)
        headers.forEach { (key, values) ->
            values.forEach { value -> requestBuilder.addHeader(key, value) }
        }

        val okBody = when {
            httpMethod == "GET" || httpMethod == "HEAD" -> null
            body != null -> body.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
            else         -> ByteArray(0).toRequestBody(null)
        }
        requestBuilder.method(httpMethod, okBody)

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            throw ReCaptchaException("429 Too Many Requests", url)
        }

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body?.string(),
            response.request.url.toString()
        )
    }
}

package com.rcdownload.data.extractor

/** URL e metadados de uma stream pronta para download via DownloadManager. */
data class StreamData(
    val url: String,
    val fileName: String,
    val mimeType: String
)

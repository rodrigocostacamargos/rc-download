package com.rcdownload.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Registro de um download realizado, incluindo metadados para atribuição. */
@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val originalUrl: String,
    val license: String,                           // Preserva o tipo de licença original
    val format: String,                            // "video" ou "audio"
    val downloadedAt: Long = System.currentTimeMillis(),
    val filePath: String? = null
)

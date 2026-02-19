package com.rcdownload.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rcdownload.data.db.AppDatabase
import com.rcdownload.data.extractor.DownloaderImpl
import com.rcdownload.data.repository.VideoRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.schabi.newpipe.extractor.NewPipe

/**
 * Constrói o ViewModel com todas as dependências necessárias.
 * Inicializa o NewPipe Extractor com o downloader OkHttp.
 */
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.i(TAG, "Inicializando NewPipe Extractor")
        NewPipe.init(DownloaderImpl(buildOkHttpClientBuilder()))

        val repository = VideoRepository(
            downloadHistoryDao = AppDatabase.getInstance(context).downloadHistoryDao()
        )

        return MainViewModel(repository) as T
    }

    private fun buildOkHttpClientBuilder(): OkHttpClient.Builder {
        val logging = HttpLoggingInterceptor { msg -> Log.d(TAG, msg) }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
    }

    companion object {
        private const val TAG = "RCDownload"
    }
}

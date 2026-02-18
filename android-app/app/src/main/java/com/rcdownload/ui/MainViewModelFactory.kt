package com.rcdownload.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rcdownload.BuildConfig
import com.rcdownload.data.api.LocalDownloadService
import com.rcdownload.data.api.YouTubeApiService
import com.rcdownload.data.db.AppDatabase
import com.rcdownload.data.repository.VideoRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Constrói o ViewModel com todas as dependências necessárias.
 * Em projetos maiores, substituir por Hilt/Dagger.
 */
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.i(TAG, "Criando ViewModel — LOCAL_SERVICE_URL=${BuildConfig.LOCAL_SERVICE_URL}")
        val client = buildOkHttpClient()

        val youTubeApi = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApiService::class.java)

        val localApi = Retrofit.Builder()
            .baseUrl(BuildConfig.LOCAL_SERVICE_URL)   // lido de local.properties
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LocalDownloadService::class.java)

        val repository = VideoRepository(
            youTubeApiService   = youTubeApi,
            localDownloadService = localApi,
            downloadHistoryDao  = AppDatabase.getInstance(context).downloadHistoryDao(),
            apiKey              = BuildConfig.YOUTUBE_API_KEY  // lido de local.properties
        )

        return MainViewModel(repository) as T
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { msg -> Log.d(TAG, msg) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    companion object {
        private const val TAG = "RCDownload"
    }
}

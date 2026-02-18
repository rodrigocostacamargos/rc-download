package com.rcdownload

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.rcdownload.data.api.models.VideoMetadata
import com.rcdownload.databinding.ActivityMainBinding
import com.rcdownload.ui.MainViewModel
import com.rcdownload.ui.MainViewModelFactory
import com.rcdownload.ui.UiState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(this) }

    private var currentMetadata: VideoMetadata? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnVerify.setOnClickListener {
            val url = binding.urlInput.text?.toString()?.trim().orEmpty()
            if (url.isEmpty()) {
                binding.urlInputLayout.error = "Cole um link do YouTube"
                return@setOnClickListener
            }
            binding.urlInputLayout.error = null
            viewModel.fetchMetadata(url)
        }

        binding.btnDownloadVideo.setOnClickListener {
            currentMetadata?.let { viewModel.startDownload("video", it) }
        }

        binding.btnDownloadAudio.setOnClickListener {
            currentMetadata?.let { viewModel.startDownload("audio", it) }
        }

        binding.btnReset.setOnClickListener { viewModel.reset() }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Idle            -> showIdle()
                    is UiState.LoadingMetadata -> showLoading("Buscando metadados...")
                    is UiState.MetadataLoaded  -> showMetadata(state.metadata)
                    is UiState.PreparingStream -> showLoading("Preparando download...")
                    is UiState.DownloadEnqueued -> {
                        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                        val req = DownloadManager.Request(Uri.parse(state.streamData.url))
                            .setTitle(state.streamData.fileName)
                            .setDescription("RC Download")
                            .setMimeType(state.streamData.mimeType)
                            .setNotificationVisibility(
                                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            )
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                state.streamData.fileName
                            )
                        dm.enqueue(req)
                        showDownloadEnqueued(state.streamData.fileName)
                    }
                    is UiState.Error           -> showError(state.message)
                }
            }
        }
    }

    // ── Helpers de UI ────────────────────────────────────────────────────────

    private fun showIdle() {
        currentMetadata = null
        binding.metadataCard.visibility    = View.GONE
        binding.downloadButtons.visibility = View.GONE
        binding.progressBar.visibility     = View.GONE
        binding.tvProgress.visibility      = View.GONE
        binding.tvStatus.visibility        = View.GONE
        binding.btnVerify.isEnabled        = true
        binding.btnReset.visibility        = View.GONE
    }

    private fun showLoading(message: String) {
        binding.progressBar.isIndeterminate = true
        binding.progressBar.visibility      = View.VISIBLE
        binding.tvProgress.text             = message
        binding.tvProgress.visibility       = View.VISIBLE
        binding.tvStatus.visibility         = View.GONE
        binding.btnVerify.isEnabled         = false
        binding.downloadButtons.visibility  = View.GONE
    }

    private fun showMetadata(metadata: VideoMetadata) {
        currentMetadata = metadata
        binding.tvTitle.text   = metadata.title
        binding.tvChannel.text = "Canal: ${metadata.channelTitle}"
        binding.tvLicense.text = "Licença: ${metadata.licenseDisplayName}"
        binding.metadataCard.visibility    = View.VISIBLE
        binding.downloadButtons.visibility = View.VISIBLE
        binding.progressBar.visibility     = View.GONE
        binding.tvProgress.visibility      = View.GONE
        binding.tvStatus.visibility        = View.GONE
        binding.btnVerify.isEnabled        = true
        binding.btnReset.visibility        = View.VISIBLE
    }

    private fun showDownloadEnqueued(fileName: String) {
        binding.progressBar.visibility     = View.GONE
        binding.tvProgress.visibility      = View.GONE
        binding.tvStatus.text              = "Download enfileirado!"
        binding.tvStatus.visibility        = View.VISIBLE
        binding.downloadButtons.visibility = View.VISIBLE
        Snackbar.make(
            binding.root,
            "Salvando em Downloads: $fileName",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showError(message: String) {
        binding.progressBar.visibility     = View.GONE
        binding.tvProgress.visibility      = View.GONE
        binding.tvStatus.text              = message
        binding.tvStatus.visibility        = View.VISIBLE
        binding.downloadButtons.visibility = if (currentMetadata != null) View.VISIBLE else View.GONE
        binding.btnVerify.isEnabled        = true
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}

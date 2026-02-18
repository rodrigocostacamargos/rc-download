package com.rcdownload.ui

import app.cash.turbine.test
import com.rcdownload.data.api.models.DownloadStatus
import com.rcdownload.data.api.models.VideoMetadata
import com.rcdownload.data.repository.VideoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private val repository: VideoRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val validUrl    = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val testVideoId = "dQw4w9WgXcQ"

    private val fakeMetadata = VideoMetadata(
        videoId      = testVideoId,
        title        = "Never Gonna Give You Up",
        channelTitle = "Rick Astley",
        originalUrl  = validUrl,
        license      = "youtube"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Estado inicial ───────────────────────────────────────────────────────

    @Test
    fun `estado inicial e Idle`() {
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    // ── fetchMetadata ────────────────────────────────────────────────────────

    @Test
    fun `fetchMetadata com URL invalida emite Error imediatamente`() = runTest {
        viewModel.uiState.test {
            skipItems(1) // Idle inicial

            viewModel.fetchMetadata("nao-e-youtube.com/video")

            val error = awaitItem()
            assertTrue(error is UiState.Error)
            assertTrue((error as UiState.Error).message.contains("inválida"))
        }
    }

    @Test
    fun `fetchMetadata com URL valida emite LoadingMetadata depois MetadataLoaded`() = runTest {
        coEvery { repository.fetchVideoMetadata(testVideoId, validUrl) } returns
            Result.success(fakeMetadata)

        viewModel.uiState.test {
            skipItems(1) // Idle

            viewModel.fetchMetadata(validUrl)

            assertEquals(UiState.LoadingMetadata, awaitItem())

            val loaded = awaitItem()
            assertTrue(loaded is UiState.MetadataLoaded)
            assertEquals(fakeMetadata, (loaded as UiState.MetadataLoaded).metadata)
        }
    }

    @Test
    fun `fetchMetadata com erro de API emite Error apos LoadingMetadata`() = runTest {
        coEvery { repository.fetchVideoMetadata(any(), any()) } returns
            Result.failure(Exception("Quota exceeded"))

        viewModel.uiState.test {
            skipItems(1) // Idle

            viewModel.fetchMetadata(validUrl)

            skipItems(1) // LoadingMetadata
            val error = awaitItem()
            assertTrue(error is UiState.Error)
            assertEquals("Quota exceeded", (error as UiState.Error).message)
        }
    }

    @Test
    fun `fetchMetadata com erro sem mensagem exibe fallback`() = runTest {
        coEvery { repository.fetchVideoMetadata(any(), any()) } returns
            Result.failure(Exception())

        viewModel.uiState.test {
            skipItems(1)
            viewModel.fetchMetadata(validUrl)
            skipItems(1)
            val error = awaitItem()
            // Não deve lançar NPE — fallback deve estar presente
            assertTrue((error as UiState.Error).message.isNotBlank())
        }
    }

    // ── startDownload ────────────────────────────────────────────────────────

    @Test
    fun `startDownload emite Downloading depois DownloadComplete quando job conclui`() = runTest {
        coEvery { repository.startDownload(validUrl, "video") } returns Result.success("job-001")
        coEvery { repository.pollDownloadStatus("job-001") } returns Result.success(
            DownloadStatus(jobId = "job-001", status = "completed", progress = 100, filePath = "/dl/video.mp4")
        )

        viewModel.uiState.test {
            skipItems(1) // Idle

            viewModel.startDownload(validUrl, "video", fakeMetadata)

            assertEquals(UiState.Downloading, awaitItem())

            val complete = awaitItem()
            assertTrue(complete is UiState.DownloadComplete)
            assertEquals("/dl/video.mp4", (complete as UiState.DownloadComplete).filePath)
        }
    }

    @Test
    fun `startDownload emite DownloadProgress durante download`() = runTest {
        coEvery { repository.startDownload(validUrl, "video") } returns Result.success("job-002")
        // Primeira chamada: downloading em 50%; segunda: completed
        coEvery { repository.pollDownloadStatus("job-002") } returnsMany listOf(
            Result.success(DownloadStatus("job-002", "downloading", 50)),
            Result.success(DownloadStatus("job-002", "completed", 100, "/dl/video.mp4"))
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.startDownload(validUrl, "video", fakeMetadata)

            skipItems(1) // Downloading
            val progress = awaitItem()
            assertTrue(progress is UiState.DownloadProgress)
            assertEquals(50, (progress as UiState.DownloadProgress).progress)

            val complete = awaitItem()
            assertTrue(complete is UiState.DownloadComplete)
        }
    }

    @Test
    fun `startDownload emite Error quando servico local esta offline`() = runTest {
        coEvery { repository.startDownload(any(), any()) } returns
            Result.failure(Exception("Connection refused"))

        viewModel.uiState.test {
            skipItems(1)
            viewModel.startDownload(validUrl, "video", fakeMetadata)
            skipItems(1) // Downloading

            val error = awaitItem()
            assertTrue(error is UiState.Error)
            assertTrue((error as UiState.Error).message.contains("serviço local"))
        }
    }

    @Test
    fun `startDownload emite Error quando job falha no servidor`() = runTest {
        coEvery { repository.startDownload(any(), any()) } returns Result.success("job-err")
        coEvery { repository.pollDownloadStatus("job-err") } returns Result.success(
            DownloadStatus("job-err", "error", 0, error = "Video unavailable")
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.startDownload(validUrl, "video", fakeMetadata)
            skipItems(1) // Downloading

            val error = awaitItem()
            assertTrue(error is UiState.Error)
            assertEquals("Video unavailable", (error as UiState.Error).message)
        }
    }

    @Test
    fun `startDownload salva historico quando download e concluido`() = runTest {
        coEvery { repository.startDownload(any(), any()) } returns Result.success("job-hist")
        coEvery { repository.pollDownloadStatus("job-hist") } returns Result.success(
            DownloadStatus("job-hist", "completed", 100, "/dl/audio.mp3")
        )

        viewModel.startDownload(validUrl, "audio", fakeMetadata)
        advanceUntilIdle()

        coVerify { repository.saveToHistory(fakeMetadata, "audio", "/dl/audio.mp3") }
    }

    // ── reset ────────────────────────────────────────────────────────────────

    @Test
    fun `reset retorna para Idle a partir de MetadataLoaded`() = runTest {
        coEvery { repository.fetchVideoMetadata(any(), any()) } returns
            Result.success(fakeMetadata)

        viewModel.fetchMetadata(validUrl)
        advanceUntilIdle()

        assertEquals(UiState.MetadataLoaded::class, viewModel.uiState.value::class)

        viewModel.reset()
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `reset retorna para Idle a partir de Error`() = runTest {
        viewModel.fetchMetadata("url-invalida")
        // Erro emitido sincronicamente para URL inválida
        assertTrue(viewModel.uiState.value is UiState.Error)

        viewModel.reset()
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }
}

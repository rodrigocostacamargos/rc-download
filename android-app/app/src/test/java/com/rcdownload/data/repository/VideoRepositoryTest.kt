package com.rcdownload.data.repository

import com.rcdownload.data.api.LocalDownloadService
import com.rcdownload.data.api.YouTubeApiService
import com.rcdownload.data.api.models.DownloadModels
import com.rcdownload.data.api.models.DownloadRequest
import com.rcdownload.data.api.models.DownloadResponse
import com.rcdownload.data.api.models.DownloadStatus
import com.rcdownload.data.api.models.Thumbnail
import com.rcdownload.data.api.models.ThumbnailData
import com.rcdownload.data.api.models.VideoMetadata
import com.rcdownload.data.api.models.VideoSnippet
import com.rcdownload.data.api.models.VideoStatus
import com.rcdownload.data.api.models.YouTubeApiResponse
import com.rcdownload.data.api.models.YouTubeVideoItem
import com.rcdownload.data.db.DownloadHistoryDao
import com.rcdownload.data.db.DownloadHistoryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VideoRepositoryTest {

    private lateinit var repository: VideoRepository
    private val youTubeApiService: YouTubeApiService = mockk()
    private val localDownloadService: LocalDownloadService = mockk()
    private val downloadHistoryDao: DownloadHistoryDao = mockk(relaxed = true)
    private val apiKey = "test_api_key_12345"

    private val testUrl    = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    private val testVideoId = "dQw4w9WgXcQ"

    @Before
    fun setUp() {
        repository = VideoRepository(
            youTubeApiService    = youTubeApiService,
            localDownloadService = localDownloadService,
            downloadHistoryDao   = downloadHistoryDao,
            apiKey               = apiKey
        )
    }

    // ── fetchVideoMetadata ───────────────────────────────────────────────────

    @Test
    fun `fetchVideoMetadata retorna sucesso com video valido`() = runTest {
        coEvery { youTubeApiService.getVideoMetadata(any(), any(), any()) } returns
            fakeApiResponse(videoId = testVideoId, license = "youtube")

        val result = repository.fetchVideoMetadata(testVideoId, testUrl)

        assertTrue(result.isSuccess)
        with(result.getOrThrow()) {
            assertEquals(testVideoId, videoId)
            assertEquals("Título de Teste", title)
            assertEquals("Canal Teste", channelTitle)
            assertEquals("youtube", license)
            assertFalse(isCreativeCommons)
        }
    }

    @Test
    fun `fetchVideoMetadata retorna sucesso com licenca creative commons`() = runTest {
        coEvery { youTubeApiService.getVideoMetadata(any(), any(), any()) } returns
            fakeApiResponse(videoId = testVideoId, license = "creativeCommon")

        val result = repository.fetchVideoMetadata(testVideoId, testUrl)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isCreativeCommons)
    }

    @Test
    fun `fetchVideoMetadata falha quando lista de items e vazia`() = runTest {
        coEvery { youTubeApiService.getVideoMetadata(any(), any(), any()) } returns
            YouTubeApiResponse(items = emptyList())

        val result = repository.fetchVideoMetadata(testVideoId, testUrl)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("não encontrado") == true)
    }

    @Test
    fun `fetchVideoMetadata falha quando items e null`() = runTest {
        coEvery { youTubeApiService.getVideoMetadata(any(), any(), any()) } returns
            YouTubeApiResponse(items = null)

        val result = repository.fetchVideoMetadata(testVideoId, testUrl)

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchVideoMetadata falha com erro de rede`() = runTest {
        coEvery { youTubeApiService.getVideoMetadata(any(), any(), any()) } throws
            Exception("Network error")

        val result = repository.fetchVideoMetadata(testVideoId, testUrl)

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fetchVideoMetadata repassa a originalUrl corretamente`() = runTest {
        coEvery { youTubeApiService.getVideoMetadata(any(), any(), any()) } returns
            fakeApiResponse(testVideoId, "youtube")

        val result = repository.fetchVideoMetadata(testVideoId, testUrl)

        assertEquals(testUrl, result.getOrThrow().originalUrl)
    }

    // ── startDownload ────────────────────────────────────────────────────────

    @Test
    fun `startDownload retorna jobId em caso de sucesso`() = runTest {
        coEvery { localDownloadService.startDownload(any()) } returns
            DownloadResponse(jobId = "job-abc-123", status = "pending")

        val result = repository.startDownload(testUrl, "video")

        assertTrue(result.isSuccess)
        assertEquals("job-abc-123", result.getOrThrow())
    }

    @Test
    fun `startDownload falha quando servico local esta offline`() = runTest {
        coEvery { localDownloadService.startDownload(any()) } throws
            Exception("Connection refused")

        val result = repository.startDownload(testUrl, "video")

        assertTrue(result.isFailure)
    }

    @Test
    fun `startDownload envia o formato correto audio`() = runTest {
        coEvery { localDownloadService.startDownload(any()) } returns
            DownloadResponse(jobId = "job-001", status = "pending")

        repository.startDownload(testUrl, "audio")

        coVerify {
            localDownloadService.startDownload(match { it.format == "audio" })
        }
    }

    // ── pollDownloadStatus ───────────────────────────────────────────────────

    @Test
    fun `pollDownloadStatus retorna status completed`() = runTest {
        val expected = DownloadStatus(
            jobId    = "job-001",
            status   = "completed",
            progress = 100,
            filePath = "/downloads/video.mp4"
        )
        coEvery { localDownloadService.getDownloadStatus("job-001") } returns expected

        val result = repository.pollDownloadStatus("job-001")

        assertTrue(result.isSuccess)
        assertEquals("completed", result.getOrThrow().status)
        assertEquals(100, result.getOrThrow().progress)
        assertEquals("/downloads/video.mp4", result.getOrThrow().filePath)
    }

    @Test
    fun `pollDownloadStatus retorna status downloading com progresso`() = runTest {
        coEvery { localDownloadService.getDownloadStatus("job-002") } returns
            DownloadStatus(jobId = "job-002", status = "downloading", progress = 45)

        val result = repository.pollDownloadStatus("job-002")

        assertTrue(result.isSuccess)
        assertEquals(45, result.getOrThrow().progress)
    }

    @Test
    fun `pollDownloadStatus retorna status error com mensagem`() = runTest {
        coEvery { localDownloadService.getDownloadStatus("job-003") } returns
            DownloadStatus(jobId = "job-003", status = "error", error = "Video unavailable")

        val result = repository.pollDownloadStatus("job-003")

        assertTrue(result.isSuccess)
        assertEquals("error", result.getOrThrow().status)
        assertEquals("Video unavailable", result.getOrThrow().error)
    }

    @Test
    fun `pollDownloadStatus falha quando servico retorna excecao`() = runTest {
        coEvery { localDownloadService.getDownloadStatus(any()) } throws Exception("Timeout")

        val result = repository.pollDownloadStatus("job-404")

        assertTrue(result.isFailure)
    }

    // ── saveToHistory ────────────────────────────────────────────────────────

    @Test
    fun `saveToHistory chama dao insert com dados corretos`() = runTest {
        val metadata = VideoMetadata(
            videoId      = testVideoId,
            title        = "Título de Teste",
            channelTitle = "Canal Teste",
            originalUrl  = testUrl,
            license      = "youtube"
        )

        repository.saveToHistory(metadata, "video", "/downloads/video.mp4")

        coVerify {
            downloadHistoryDao.insert(match { entity ->
                entity.videoId      == testVideoId &&
                entity.title        == "Título de Teste" &&
                entity.channelTitle == "Canal Teste" &&
                entity.originalUrl  == testUrl &&
                entity.license      == "youtube" &&
                entity.format       == "video" &&
                entity.filePath     == "/downloads/video.mp4"
            })
        }
    }

    @Test
    fun `saveToHistory aceita filePath nulo`() = runTest {
        val metadata = VideoMetadata(testVideoId, "t", "c", testUrl, "youtube")

        repository.saveToHistory(metadata, "audio", null)

        coVerify {
            downloadHistoryDao.insert(match { it.filePath == null })
        }
    }

    // ── isLocalServiceAvailable ──────────────────────────────────────────────

    @Test
    fun `isLocalServiceAvailable retorna true quando servico responde`() = runTest {
        coEvery { localDownloadService.health() } returns mapOf("status" to "ok")

        assertTrue(repository.isLocalServiceAvailable())
    }

    @Test
    fun `isLocalServiceAvailable retorna false quando servico lanca excecao`() = runTest {
        coEvery { localDownloadService.health() } throws Exception("Connection refused")

        assertFalse(repository.isLocalServiceAvailable())
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun fakeApiResponse(videoId: String, license: String) = YouTubeApiResponse(
        items = listOf(
            YouTubeVideoItem(
                id = videoId,
                snippet = VideoSnippet(
                    title        = "Título de Teste",
                    channelTitle = "Canal Teste",
                    thumbnails   = ThumbnailData(
                        medium = Thumbnail("https://i.ytimg.com/vi/$videoId/mqdefault.jpg"),
                        high   = null
                    )
                ),
                status = VideoStatus(license = license)
            )
        )
    )
}

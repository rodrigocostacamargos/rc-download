package com.rcdownload.data.api.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMetadataTest {

    private fun metadata(license: String) = VideoMetadata(
        videoId      = "abc123defgh",
        title        = "Título de Teste",
        channelTitle = "Canal Teste",
        originalUrl  = "https://youtube.com/watch?v=abc123defgh",
        license      = license
    )

    // ── isCreativeCommons ────────────────────────────────────────────────────

    @Test
    fun `isCreativeCommons e true para licenca creativeCommon`() {
        assertTrue(metadata("creativeCommon").isCreativeCommons)
    }

    @Test
    fun `isCreativeCommons e false para licenca youtube`() {
        assertFalse(metadata("youtube").isCreativeCommons)
    }

    @Test
    fun `isCreativeCommons e false para licenca desconhecida`() {
        assertFalse(metadata("unknown").isCreativeCommons)
    }

    // ── licenseDisplayName ───────────────────────────────────────────────────

    @Test
    fun `displayName para creativeCommon e correto`() {
        assertEquals("Creative Commons (CC BY)", metadata("creativeCommon").licenseDisplayName)
    }

    @Test
    fun `displayName para youtube e correto`() {
        assertEquals("Licença padrão do YouTube", metadata("youtube").licenseDisplayName)
    }

    @Test
    fun `displayName retorna valor bruto para licenca desconhecida`() {
        assertEquals("outraLicenca", metadata("outraLicenca").licenseDisplayName)
    }

    // ── thumbnailUrl ─────────────────────────────────────────────────────────

    @Test
    fun `thumbnailUrl e null por padrao`() {
        val m = VideoMetadata("id", "t", "c", "url", "youtube")
        assertEquals(null, m.thumbnailUrl)
    }

    @Test
    fun `thumbnailUrl e preservado quando fornecido`() {
        val url = "https://i.ytimg.com/vi/abc/mqdefault.jpg"
        val m   = VideoMetadata("id", "t", "c", "url", "youtube", thumbnailUrl = url)
        assertEquals(url, m.thumbnailUrl)
    }
}

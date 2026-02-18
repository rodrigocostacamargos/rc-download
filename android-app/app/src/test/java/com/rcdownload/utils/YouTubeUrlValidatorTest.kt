package com.rcdownload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeUrlValidatorTest {

    // ── isValid ──────────────────────────────────────────────────────────────

    @Test
    fun `URL watch padrao e valida`() {
        assertTrue(YouTubeUrlValidator.isValid("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `URL watch sem www e valida`() {
        assertTrue(YouTubeUrlValidator.isValid("https://youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `URL youtu be curta e valida`() {
        assertTrue(YouTubeUrlValidator.isValid("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `URL shorts e valida`() {
        assertTrue(YouTubeUrlValidator.isValid("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    }

    @Test
    fun `URL embed e valida`() {
        assertTrue(YouTubeUrlValidator.isValid("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    }

    @Test
    fun `URL http sem s e valida`() {
        assertTrue(YouTubeUrlValidator.isValid("http://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `URL com parametros extras e valida`() {
        assertTrue(YouTubeUrlValidator.isValid("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s&list=PLxxx"))
    }

    @Test
    fun `URL vimeo e invalida`() {
        assertFalse(YouTubeUrlValidator.isValid("https://vimeo.com/123456789"))
    }

    @Test
    fun `string vazia e invalida`() {
        assertFalse(YouTubeUrlValidator.isValid(""))
    }

    @Test
    fun `URL raiz do youtube sem video e invalida`() {
        assertFalse(YouTubeUrlValidator.isValid("https://www.youtube.com/"))
    }

    @Test
    fun `URL de canal do youtube e invalida`() {
        assertFalse(YouTubeUrlValidator.isValid("https://www.youtube.com/@MrBeast"))
    }

    @Test
    fun `texto aleatorio e invalido`() {
        assertFalse(YouTubeUrlValidator.isValid("nao sou uma url"))
    }

    // ── extractVideoId ───────────────────────────────────────────────────────

    @Test
    fun `extrai ID de URL watch padrao`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlValidator.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun `extrai ID de URL youtu be`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlValidator.extractVideoId("https://youtu.be/dQw4w9WgXcQ")
        )
    }

    @Test
    fun `extrai ID de URL shorts`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlValidator.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ")
        )
    }

    @Test
    fun `extrai ID com parametros extras na URL`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlValidator.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s&feature=share")
        )
    }

    @Test
    fun `retorna null para URL invalida`() {
        assertNull(YouTubeUrlValidator.extractVideoId("https://vimeo.com/123"))
    }

    @Test
    fun `retorna null para string vazia`() {
        assertNull(YouTubeUrlValidator.extractVideoId(""))
    }

    @Test
    fun `ignora espacos em branco na URL`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlValidator.extractVideoId("  https://youtu.be/dQw4w9WgXcQ  ")
        )
    }
}

package com.example.data.util

import com.example.data.local.FtsSearchEngine
import com.example.data.local.ItemEntity
import com.example.data.local.MatchStrength
import com.example.data.local.NoteEntity
import com.example.data.local.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCoreUnitTest {

    @Test
    fun urlNormalizer_stripsTrackingParamsAndDefaultPorts_preservesVideoId() {
        val parsed = UrlNormalizer.normalize(
            "HTTPS://www.YouTube.com:443/watch?v=dQw4w9WgXcQ&utm_source=share&si=abc123"
        )
        assertNotNull(parsed)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", parsed!!.canonicalUrl)
        assertEquals("youtube.com", parsed.domain)
        assertEquals("video", parsed.inferredContentType)
        assertEquals("youtube", parsed.inferredSourceType)
    }

    @Test
    fun urlNormalizer_rejectsUnsafeSchemesAndContentUris() {
        assertNull(UrlNormalizer.normalize("javascript:alert(1)"))
        assertNull(UrlNormalizer.normalize("file:///sdcard/secret.txt"))
        assertNull(UrlNormalizer.normalize("content://com.android.providers.downloads/document/1"))
        assertFalse(UrlNormalizer.isSafeRemoteUrl("javascript:void(0)"))
        assertTrue(UrlNormalizer.isContentUri("content://media/external/images/media/42"))
    }

    @Test
    fun urlNormalizer_classifiesGithubRepoTitle() {
        val parsed = UrlNormalizer.normalize("https://github.com/android/compose-samples/")
        assertNotNull(parsed)
        assertEquals("github", parsed!!.inferredContentType)
        assertEquals("android/compose-samples", parsed.suggestedTitle)
        assertEquals("https://github.com/android/compose-samples", parsed.canonicalUrl)
    }

    @Test
    fun unicodeNormalization_preservesMixedBengaliEnglish() {
        val mixed = UrlNormalizer.normalizeUnicode("বাংলা Android টিউটোরিয়াল Compose")
        assertTrue(mixed.contains("বাংলা"))
        assertTrue(mixed.contains("Compose"))
    }
}

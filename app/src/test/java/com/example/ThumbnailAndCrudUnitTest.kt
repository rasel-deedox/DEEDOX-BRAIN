package com.example

import com.example.data.network.MetadataEnricher
import com.example.data.util.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailAndCrudUnitTest {

    @Test
    fun `resolveSmartThumbnailUrl extracts YouTube and GitHub and favicon fallbacks`() {
        assertEquals(
            "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            UrlNormalizer.resolveSmartThumbnailUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share")
        )
        assertEquals(
            "https://i.ytimg.com/vi/abc123XYZ/hqdefault.jpg",
            UrlNormalizer.resolveSmartThumbnailUrl("https://youtu.be/abc123XYZ")
        )
        assertEquals(
            "https://opengraph.githubassets.com/1/kotlin/kotlinx.coroutines",
            UrlNormalizer.resolveSmartThumbnailUrl("https://github.com/kotlin/kotlinx.coroutines")
        )

        val faviconFallback = UrlNormalizer.resolveSmartThumbnailUrl("https://developer.android.com/compose")
        assertEquals("https://www.google.com/s2/favicons?domain=developer.android.com&sz=128", faviconFallback)
        assertTrue(UrlNormalizer.isFaviconFallbackUrl(faviconFallback))
        assertFalse(UrlNormalizer.isFaviconFallbackUrl("https://opengraph.githubassets.com/1/kotlin/kotlinx.coroutines"))
    }

    @Test
    fun `resolveRelativeUrl resolves relative and protocol-relative urls safely`() {
        assertEquals(
            "https://example.com/assets/og-banner.png",
            UrlNormalizer.resolveRelativeUrl("https://example.com/blog/post-1", "/assets/og-banner.png")
        )
        assertEquals(
            "https://cdn.example.com/hero.jpg",
            UrlNormalizer.resolveRelativeUrl("https://example.com/blog", "//cdn.example.com/hero.jpg")
        )
        assertNull(
            UrlNormalizer.resolveRelativeUrl("https://example.com", "javascript:alert(1)")
        )
    }

    @Test
    fun `parseHtmlMetadata extracts og image twitter image and apple touch icon with entity decoding`() {
        val html = """
            <html>
              <head>
                <title>Fallback Title</title>
                <meta property="og:title" content="Iconly &amp; Liquid Glass" />
                <meta name="description" content="Curated &quot;tactile&quot; UI" />
                <meta content="/media/cover.png" property="og:image" />
              </head>
            </html>
        """.trimIndent()

        val parsed = MetadataEnricher.parseHtmlMetadata(html, "https://design.example.com/articles/glass")
        assertEquals("Iconly & Liquid Glass", parsed.title)
        assertEquals("Curated \"tactile\" UI", parsed.description)
        assertEquals("https://design.example.com/media/cover.png", parsed.thumbnailUrl)

        val twitterOnlyHtml = """
            <html>
              <head>
                <title>Twitter Card Article</title>
                <meta name="twitter:image" content="https://cdn.site.dev/card.webp" />
              </head>
            </html>
        """.trimIndent()
        val parsedTwitter = MetadataEnricher.parseHtmlMetadata(twitterOnlyHtml, "https://site.dev/post")
        assertEquals("Twitter Card Article", parsedTwitter.title)
        assertEquals("https://cdn.site.dev/card.webp", parsedTwitter.thumbnailUrl)

        val iconFallbackHtml = """
            <html>
              <head>
                <title>Simple Page</title>
                <link rel="apple-touch-icon" href="/icons/apple-touch.png" />
              </head>
            </html>
        """.trimIndent()
        val parsedIcon = MetadataEnricher.parseHtmlMetadata(iconFallbackHtml, "https://simple.org/docs")
        assertEquals("https://simple.org/icons/apple-touch.png", parsedIcon.thumbnailUrl)
    }

    @Test
    fun `instagram embed html extracts video thumbnail author caption and ignores static icons`() {
        val embedHtml = """
            <html>
              <head>
                <meta property="og:title" content="Instagram" />
                <meta property="og:image" content="https://static.cdninstagram.com/rsrc.php/v3/yI/r/VsNE-OHk_8a.png" />
              </head>
              <body>
                <span class="UsernameText">design.daily</span>
                <img class="EmbeddedMediaImage" src="https://scontent-iad3-1.cdninstagram.com/v/t51.2885-15/reel_cover_999.jpg?stp=dst-jpg" />
                <div class="Caption">
                  <a class="CaptionUsername">design.daily</a>
                  Motion choreography tutorial for Android Compose &amp; SwiftUI &#x2014; save this workflow!
                </div>
              </body>
            </html>
        """.trimIndent()

        val parsed = MetadataEnricher.parseInstagramEmbedHtml(embedHtml, "https://www.instagram.com/reel/DAbc123XYZ/embed/captioned/")
        requireNotNull(parsed)
        assertEquals("https://scontent-iad3-1.cdninstagram.com/v/t51.2885-15/reel_cover_999.jpg?stp=dst-jpg", parsed.thumbnailUrl)
        assertTrue(parsed.title?.contains("@design.daily") == true)
        assertTrue(parsed.title?.contains("Motion choreography tutorial") == true)
        assertTrue(parsed.description?.contains("Motion choreography tutorial for Android Compose & SwiftUI") == true)
    }

    @Test
    fun `parseOEmbedJson extracts YouTube and TikTok video title description and thumbnail`() {
        val tiktokJson = """
            {
              "version": "1.0",
              "type": "video",
              "title": "Build a clean liquid glass navbar in 30 seconds",
              "author_name": "StudioUI",
              "provider_name": "TikTok",
              "thumbnail_url": "https://p16-sign.tiktokcdn.com/obj/tos-maliva/frame_cover.webp"
            }
        """.trimIndent()

        val parsed = MetadataEnricher.parseOEmbedJson(tiktokJson, "https://www.tiktok.com/@studioui/video/732100000")
        requireNotNull(parsed)
        assertEquals("Build a clean liquid glass navbar in 30 seconds", parsed.title)
        assertEquals("By StudioUI on TikTok", parsed.description)
        assertEquals("https://p16-sign.tiktokcdn.com/obj/tos-maliva/frame_cover.webp", parsed.thumbnailUrl)
    }

    @Test
    fun `parseHtmlMetadata skips generic icon og image and uses jsonld thumbnailUrl plus youtube shortDescription`() {
        val html = """
            <html>
              <head>
                <title>YouTube</title>
                <meta property="og:image" content="https://www.youtube.com/img/desktop/yt_1200.png" />
                <script type="application/ld+json">
                  {
                    "@type": "VideoObject",
                    "name": "Jetpack Compose Animation Masterclass",
                    "thumbnailUrl": ["https://i.ytimg.com/vi/xyz987/maxresdefault.jpg"]
                  }
                </script>
              </head>
              <body>
                <script>var ytInitialPlayerResponse = {"videoDetails":{"shortDescription":"Learn spring physics, shared element transitions, and graphicsLayer optimization."}};</script>
              </body>
            </html>
        """.trimIndent()

        val parsed = MetadataEnricher.parseHtmlMetadata(html, "https://www.youtube.com/watch?v=xyz987")
        assertEquals("Jetpack Compose Animation Masterclass", parsed.title)
        assertEquals("Learn spring physics, shared element transitions, and graphicsLayer optimization.", parsed.description)
        assertEquals("https://i.ytimg.com/vi/xyz987/maxresdefault.jpg", parsed.thumbnailUrl)
    }

    @Test
    fun `urlNormalizer classifies reels tiktok and identifies placeholder titles and platform icons`() {
        val igReel = UrlNormalizer.normalize("https://www.instagram.com/reel/C8xK2m_A1b/?igsh=MzRlODBiNWFlZA==")
        requireNotNull(igReel)
        assertEquals("video", igReel.inferredContentType)
        assertEquals("instagram", igReel.inferredSourceType)
        assertEquals("https://www.instagram.com/reel/C8xK2m_A1b", igReel.canonicalUrl)
        assertEquals("reel" to "C8xK2m_A1b", UrlNormalizer.extractInstagramShortcode(igReel.originalUrl))

        assertTrue(UrlNormalizer.isFaviconFallbackUrl("https://static.cdninstagram.com/rsrc.php/v3/yI/r/VsNE-OHk_8a.png"))
        assertTrue(UrlNormalizer.looksLikePlaceholderTitle("Instagram", igReel.originalUrl, igReel.domain))
        assertTrue(UrlNormalizer.looksLikePlaceholderTitle(igReel.suggestedTitle, igReel.originalUrl, igReel.domain))
        assertFalse(UrlNormalizer.looksLikePlaceholderTitle("Cinematic Color Grading Breakdown", igReel.originalUrl, igReel.domain))
    }
}

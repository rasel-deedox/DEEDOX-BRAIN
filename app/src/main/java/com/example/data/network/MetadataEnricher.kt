package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.util.UrlNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class EnrichedMetadata(
    val title: String?,
    val description: String?,
    val thumbnailUrl: String?,
    val isLiveReachable: Boolean
)

class MetadataEnricher(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun fetchMetadata(url: String): Result<EnrichedMetadata> = withContext(Dispatchers.IO) {
        if (!UrlNormalizer.isSafeRemoteUrl(url)) {
            return@withContext Result.failure(IllegalArgumentException("Unsupported URL scheme"))
        }
        if (!isOnline()) {
            return@withContext Result.failure(IllegalStateException("Device is offline"))
        }

        val smartFallback = UrlNormalizer.resolveSmartThumbnailUrl(url)
        val host = runCatching { URI(url).host?.lowercase(Locale.US)?.removePrefix("www.") }.getOrNull().orEmpty()

        try {
            val result = when {
                host.contains("youtube.com") || host == "youtu.be" -> fetchYouTubeMetadata(url, smartFallback)
                host.contains("instagram.com") || host == "instagr.am" -> fetchInstagramMetadata(url, smartFallback)
                host.contains("tiktok.com") -> fetchTikTokMetadata(url, smartFallback)
                host.contains("vimeo.com") -> fetchVimeoMetadata(url, smartFallback)
                host == "x.com" || host.endsWith(".x.com") || host.contains("twitter.com") -> fetchTwitterMetadata(url, smartFallback)
                host.contains("reddit.com") || host == "redd.it" -> fetchRedditMetadata(url, smartFallback)
                else -> fetchGenericWebMetadata(url, smartFallback)
            }
            Result.success(result)
        } catch (e: Exception) {
            if (smartFallback != null) {
                Result.success(
                    EnrichedMetadata(
                        title = null,
                        description = null,
                        thumbnailUrl = smartFallback,
                        isLiveReachable = false
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    private fun fetchYouTubeMetadata(url: String, smartFallback: String?): EnrichedMetadata {
        val encoded = URLEncoder.encode(url, Charsets.UTF_8.name())
        val oEmbed = fetchOEmbed("https://www.youtube.com/oembed?url=$encoded&format=json", url)

        val videoId = UrlNormalizer.extractYouTubeVideoId(url)
        val directThumb = videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
        val pageMeta = fetchHtmlWithUa(
            url = url,
            userAgent = DESKTOP_CHROME_UA,
            extraHeaders = mapOf(
                "Accept-Language" to "en-US,en;q=0.9",
                "Cookie" to "CONSENT=YES+cb.20210328-17-p0.en+FX+999"
            ),
            forceFullRead = true
        )

        val rawTitle = firstNonBlank(oEmbed?.title, pageMeta?.title)
        val rawDesc = firstNonBlank(pageMeta?.description, oEmbed?.description)
        val rawThumb = chooseBestThumbnail(oEmbed?.thumbnailUrl, pageMeta?.thumbnailUrl, directThumb, smartFallback)

        return EnrichedMetadata(
            title = rawTitle?.take(400),
            description = rawDesc?.take(2000),
            thumbnailUrl = rawThumb?.take(2000),
            isLiveReachable = oEmbed != null || pageMeta != null
        )
    }

    private fun fetchInstagramMetadata(url: String, smartFallback: String?): EnrichedMetadata {
        // Resolve share links (e.g. /share/reel/... or instagr.am) to canonical reel/post URL when possible
        val resolvedUrl = resolveRedirectUrl(url, FACEBOOK_CRAWLER_UA) ?: url
        val shortcodePair = UrlNormalizer.extractInstagramShortcode(resolvedUrl)
            ?: UrlNormalizer.extractInstagramShortcode(url)

        // 1. Try link-unfurler User-Agents which Meta edge serves with full og:title, og:description, og:image
        var bestMeta: EnrichedMetadata? = null
        val unfurlerAgents = listOf(FACEBOOK_CRAWLER_UA, WHATSAPP_CRAWLER_UA, TWITTER_BOT_UA)
        for (ua in unfurlerAgents) {
            val candidate = fetchHtmlWithUa(
                url = resolvedUrl,
                userAgent = ua,
                extraHeaders = mapOf("Accept-Language" to "en-US,en;q=0.9"),
                forceFullRead = true
            )
            if (candidate != null) {
                bestMeta = mergeMetadata(bestMeta, candidate)
                if (hasNonIconThumbnail(bestMeta?.thumbnailUrl) &&
                    !bestMeta?.title.isNullOrBlank() &&
                    !bestMeta?.description.isNullOrBlank()
                ) {
                    break
                }
            }
        }

        // 2. Query Instagram's public /embed/captioned/ endpoint when shortcode is known and anything is missing
        if (shortcodePair != null &&
            (!hasNonIconThumbnail(bestMeta?.thumbnailUrl) ||
                bestMeta?.title.isNullOrBlank() ||
                bestMeta?.description.isNullOrBlank())
        ) {
            val (kind, code) = shortcodePair
            val embedEndpoints = listOf(
                "https://www.instagram.com/$kind/$code/embed/captioned/",
                "https://www.instagram.com/p/$code/embed/captioned/",
                "https://www.instagram.com/$kind/$code/embed/"
            ).distinct()
            for (embedUrl in embedEndpoints) {
                val embedMeta = fetchInstagramEmbed(embedUrl)
                if (embedMeta != null) {
                    bestMeta = mergeMetadata(bestMeta, embedMeta)
                    if (hasNonIconThumbnail(bestMeta?.thumbnailUrl) && !bestMeta?.title.isNullOrBlank()) {
                        break
                    }
                }
            }
        }

        // 3. Direct media endpoint redirect check (/p/<code>/media/?size=l) if video/post image still missing
        var verifiedDirectMedia: String? = null
        if (shortcodePair != null && !hasNonIconThumbnail(bestMeta?.thumbnailUrl)) {
            verifiedDirectMedia = probeRedirectedImageUrl("https://www.instagram.com/p/${shortcodePair.second}/media/?size=l")
        }

        // 4. Optional Noembed aggregator fallback if title/thumbnail still absent
        var noembedMeta: EnrichedMetadata? = null
        if (bestMeta?.title.isNullOrBlank() || (!hasNonIconThumbnail(bestMeta?.thumbnailUrl) && verifiedDirectMedia == null)) {
            val encoded = URLEncoder.encode(resolvedUrl, Charsets.UTF_8.name())
            noembedMeta = fetchOEmbed("https://noembed.com/embed?url=$encoded", resolvedUrl)
        }

        val merged = mergeMetadata(bestMeta, noembedMeta)
        val bestThumb = chooseBestThumbnail(
            merged?.thumbnailUrl,
            verifiedDirectMedia,
            smartFallback
        )
        return EnrichedMetadata(
            title = merged?.title?.take(400),
            description = merged?.description?.take(2000),
            thumbnailUrl = bestThumb?.take(2000),
            isLiveReachable = merged?.isLiveReachable == true || verifiedDirectMedia != null
        )
    }

    private fun fetchTikTokMetadata(url: String, smartFallback: String?): EnrichedMetadata {
        val resolvedUrl = resolveRedirectUrl(url, DESKTOP_CHROME_UA) ?: url
        val encoded = URLEncoder.encode(resolvedUrl, Charsets.UTF_8.name())
        val oEmbed = fetchOEmbed("https://www.tiktok.com/oembed?url=$encoded", resolvedUrl)

        var htmlMeta: EnrichedMetadata? = null
        if (!hasNonIconThumbnail(oEmbed?.thumbnailUrl) || oEmbed?.description.isNullOrBlank()) {
            htmlMeta = fetchHtmlWithUa(resolvedUrl, FACEBOOK_CRAWLER_UA, forceFullRead = true)
                ?: fetchHtmlWithUa(resolvedUrl, WHATSAPP_CRAWLER_UA, forceFullRead = true)
        }

        val merged = mergeMetadata(oEmbed, htmlMeta)
        return EnrichedMetadata(
            title = merged?.title?.take(400),
            description = merged?.description?.take(2000),
            thumbnailUrl = chooseBestThumbnail(merged?.thumbnailUrl, smartFallback)?.take(2000),
            isLiveReachable = merged?.isLiveReachable == true
        )
    }

    private fun fetchVimeoMetadata(url: String, smartFallback: String?): EnrichedMetadata {
        val encoded = URLEncoder.encode(url, Charsets.UTF_8.name())
        val oEmbed = fetchOEmbed("https://vimeo.com/api/oembed.json?url=$encoded", url)
        val htmlMeta = if (oEmbed == null || !hasNonIconThumbnail(oEmbed.thumbnailUrl) || oEmbed.description.isNullOrBlank()) {
            fetchHtmlWithUa(url, DESKTOP_CHROME_UA)
        } else {
            null
        }
        val merged = mergeMetadata(oEmbed, htmlMeta)
        return EnrichedMetadata(
            title = merged?.title?.take(400),
            description = merged?.description?.take(2000),
            thumbnailUrl = chooseBestThumbnail(merged?.thumbnailUrl, smartFallback)?.take(2000),
            isLiveReachable = merged?.isLiveReachable == true
        )
    }

    private fun fetchTwitterMetadata(url: String, smartFallback: String?): EnrichedMetadata {
        val encoded = URLEncoder.encode(url, Charsets.UTF_8.name())
        val oEmbed = fetchOEmbed("https://publish.twitter.com/oembed?url=$encoded&omit_script=true", url)
        var htmlMeta: EnrichedMetadata? = null
        for (ua in listOf(TWITTER_BOT_UA, FACEBOOK_CRAWLER_UA, WHATSAPP_CRAWLER_UA)) {
            htmlMeta = fetchHtmlWithUa(url, ua, forceFullRead = true)
            if (htmlMeta != null && hasNonIconThumbnail(htmlMeta.thumbnailUrl)) break
        }
        val merged = mergeMetadata(htmlMeta, oEmbed)
        return EnrichedMetadata(
            title = merged?.title?.take(400),
            description = merged?.description?.take(2000),
            thumbnailUrl = chooseBestThumbnail(merged?.thumbnailUrl, smartFallback)?.take(2000),
            isLiveReachable = merged?.isLiveReachable == true
        )
    }

    private fun fetchRedditMetadata(url: String, smartFallback: String?): EnrichedMetadata {
        val encoded = URLEncoder.encode(url, Charsets.UTF_8.name())
        val oEmbed = fetchOEmbed("https://www.reddit.com/oembed?url=$encoded", url)
        val htmlMeta = fetchHtmlWithUa(url, FACEBOOK_CRAWLER_UA, forceFullRead = true)
            ?: fetchHtmlWithUa(url, DESKTOP_CHROME_UA)
        val merged = mergeMetadata(htmlMeta, oEmbed)
        return EnrichedMetadata(
            title = merged?.title?.take(400),
            description = merged?.description?.take(2000),
            thumbnailUrl = chooseBestThumbnail(merged?.thumbnailUrl, smartFallback)?.take(2000),
            isLiveReachable = merged?.isLiveReachable == true
        )
    }

    private fun fetchGenericWebMetadata(url: String, smartFallback: String?): EnrichedMetadata {
        val mobileMeta = fetchHtmlWithUa(url, MOBILE_CHROME_UA)
        if (mobileMeta != null &&
            hasNonIconThumbnail(mobileMeta.thumbnailUrl) &&
            !mobileMeta.title.isNullOrBlank() &&
            !mobileMeta.description.isNullOrBlank()
        ) {
            return mobileMeta
        }

        var best = mobileMeta
        if (best == null || !hasNonIconThumbnail(best.thumbnailUrl) || best.title.isNullOrBlank()) {
            val crawlerMeta = fetchHtmlWithUa(url, FACEBOOK_CRAWLER_UA, forceFullRead = true)
                ?: fetchHtmlWithUa(url, DESKTOP_CHROME_UA, forceFullRead = true)
            best = mergeMetadata(best, crawlerMeta)
        }

        if (best == null || !hasNonIconThumbnail(best.thumbnailUrl) || best.title.isNullOrBlank()) {
            val encoded = URLEncoder.encode(url, Charsets.UTF_8.name())
            val noembed = fetchOEmbed("https://noembed.com/embed?url=$encoded", url)
            best = mergeMetadata(best, noembed)
        }

        return best ?: EnrichedMetadata(
            title = null,
            description = null,
            thumbnailUrl = smartFallback,
            isLiveReachable = false
        )
    }

    private fun fetchHtmlWithUa(
        url: String,
        userAgent: String,
        extraHeaders: Map<String, String> = emptyMap(),
        forceFullRead: Boolean = false
    ): EnrichedMetadata? {
        return try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
            extraHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val resolvedBaseUrl = response.request.url.toString()
                if (resolvedBaseUrl.contains("/accounts/login", ignoreCase = true)) {
                    return null
                }
                if (!response.isSuccessful) {
                    return null
                }
                val contentType = response.header("Content-Type").orEmpty()
                if (contentType.startsWith("image/", ignoreCase = true)) {
                    return EnrichedMetadata(
                        title = null,
                        description = null,
                        thumbnailUrl = resolvedBaseUrl,
                        isLiveReachable = true
                    )
                }
                if (!contentType.contains("text/html", ignoreCase = true) &&
                    !contentType.contains("application/xhtml", ignoreCase = true)
                ) {
                    return null
                }
                val bodyStream = response.body?.byteStream() ?: return null
                val html = readUpToBytes(bodyStream, maxBytes = 512 * 1024, forceReadPastHead = forceFullRead)
                val parsed = parseHtmlMetadata(html = html, baseUrl = resolvedBaseUrl)
                parsed.copy(isLiveReachable = true)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchInstagramEmbed(embedUrl: String): EnrichedMetadata? {
        return try {
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", DESKTOP_CHROME_UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://www.instagram.com/")
                .build()

            client.newCall(request).execute().use { response ->
                val resolvedBase = response.request.url.toString()
                if (!response.isSuccessful || resolvedBase.contains("/accounts/login", ignoreCase = true)) {
                    return null
                }
                val bodyStream = response.body?.byteStream() ?: return null
                val html = readUpToBytes(bodyStream, maxBytes = 512 * 1024, forceReadPastHead = true)
                parseInstagramEmbedHtml(html, resolvedBase)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchOEmbed(endpointUrl: String, sourceUrl: String): EnrichedMetadata? {
        return try {
            val request = Request.Builder()
                .url(endpointUrl)
                .header("User-Agent", DESKTOP_CHROME_UA)
                .header("Accept", "application/json,text/plain,*/*")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val rawJson = response.body?.string()?.takeIf { it.isNotBlank() } ?: return null
                parseOEmbedJson(rawJson, sourceUrl)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveRedirectUrl(url: String, userAgent: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", userAgent)
                .build()
            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (!finalUrl.contains("/accounts/login", ignoreCase = true) && UrlNormalizer.isSafeRemoteUrl(finalUrl)) {
                    finalUrl
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun probeRedirectedImageUrl(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", DESKTOP_CHROME_UA)
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .build()
            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val contentType = response.header("Content-Type").orEmpty()
                if (response.isSuccessful &&
                    contentType.startsWith("image/", ignoreCase = true) &&
                    !finalUrl.contains("/accounts/login", ignoreCase = true) &&
                    !UrlNormalizer.isFaviconFallbackUrl(finalUrl)
                ) {
                    finalUrl
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readUpToBytes(
        stream: java.io.InputStream,
        maxBytes: Int,
        forceReadPastHead: Boolean = false
    ): String {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0
        while (total < maxBytes) {
            val remaining = maxBytes - total
            val n = stream.read(chunk, 0, minOf(chunk.size, remaining))
            if (n <= 0) break
            out.write(chunk, 0, n)
            total += n
            if (!forceReadPastHead) {
                val partial = out.toString(Charsets.UTF_8.name())
                val headCloseIdx = partial.indexOf("</head>", ignoreCase = true)
                if (headCloseIdx != -1) {
                    // Stop early only when <head> already holds non-generic title, description & real preview image
                    val headSlice = partial.substring(0, headCloseIdx)
                    val quickMeta = parseHtmlMetadata(headSlice, "https://placeholder.local")
                    if (!quickMeta.title.isNullOrBlank() &&
                        !quickMeta.description.isNullOrBlank() &&
                        hasNonIconThumbnail(quickMeta.thumbnailUrl)
                    ) {
                        return headSlice
                    }
                }
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    companion object {
        private const val MOBILE_CHROME_UA =
            "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        private const val DESKTOP_CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val FACEBOOK_CRAWLER_UA =
            "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"
        private const val WHATSAPP_CRAWLER_UA =
            "WhatsApp/2.24.19.86 A"
        private const val TWITTER_BOT_UA =
            "Twitterbot/1.0"

        fun hasNonIconThumbnail(url: String?): Boolean {
            return !url.isNullOrBlank() && !UrlNormalizer.isFaviconFallbackUrl(url)
        }

        private fun chooseBestThumbnail(vararg candidates: String?): String? {
            val nonBlank = candidates.mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            return nonBlank.firstOrNull { !UrlNormalizer.isFaviconFallbackUrl(it) }
                ?: nonBlank.firstOrNull()
        }

        private fun firstNonBlank(vararg values: String?): String? {
            return values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotBlank) }
        }

        private fun mergeMetadata(primary: EnrichedMetadata?, secondary: EnrichedMetadata?): EnrichedMetadata? {
            if (primary == null) return secondary
            if (secondary == null) return primary
            val mergedTitle = firstNonBlank(
                primary.title?.takeUnless { UrlNormalizer.isGenericPlatformTitle(it) },
                secondary.title?.takeUnless { UrlNormalizer.isGenericPlatformTitle(it) },
                primary.title,
                secondary.title
            )
            val mergedDesc = firstNonBlank(primary.description, secondary.description)
            val mergedThumb = chooseBestThumbnail(primary.thumbnailUrl, secondary.thumbnailUrl)
            return EnrichedMetadata(
                title = mergedTitle,
                description = mergedDesc,
                thumbnailUrl = mergedThumb,
                isLiveReachable = primary.isLiveReachable || secondary.isLiveReachable
            )
        }

        fun parseOEmbedJson(rawJson: String, sourceUrl: String): EnrichedMetadata? {
            if (rawJson.isBlank()) return null
            val titleRaw = extractJsonStringField(rawJson, "title")
            val authorRaw = extractJsonStringField(rawJson, "author_name")
            val providerRaw = extractJsonStringField(rawJson, "provider_name")
            val descRaw = extractJsonStringField(rawJson, "description")
            val htmlField = extractJsonStringField(rawJson, "html")
            val thumbRaw = extractJsonStringField(rawJson, "thumbnail_url")
                ?.let { UrlNormalizer.resolveRelativeUrl(sourceUrl, it) }

            val strippedHtmlQuote = htmlField
                ?.let { Regex("""<p[^>]*>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(it)?.groupValues?.getOrNull(1) }
                ?.let { stripHtmlTags(it) }
                ?.takeIf { it.isNotBlank() }

            val cleanTitle = when {
                titleRaw != null && !UrlNormalizer.isGenericPlatformTitle(titleRaw) ->
                    decodeHtmlEntities(titleRaw)
                strippedHtmlQuote != null && authorRaw != null ->
                    "$authorRaw: ${strippedHtmlQuote.take(90)}"
                authorRaw != null && providerRaw != null ->
                    "$authorRaw on $providerRaw"
                else -> titleRaw
            }

            val cleanDesc = firstNonBlank(
                descRaw?.let(::decodeHtmlEntities),
                strippedHtmlQuote,
                if (authorRaw != null && providerRaw != null && cleanTitle != null && !cleanTitle.contains(authorRaw, ignoreCase = true)) {
                    "By $authorRaw on $providerRaw"
                } else null
            )

            return if (cleanTitle == null && cleanDesc == null && thumbRaw == null) {
                null
            } else {
                EnrichedMetadata(
                    title = cleanTitle?.take(400),
                    description = cleanDesc?.take(2000),
                    thumbnailUrl = thumbRaw?.take(2000),
                    isLiveReachable = true
                )
            }
        }

        fun parseInstagramEmbedHtml(html: String, baseUrl: String): EnrichedMetadata? {
            if (html.isBlank()) return null

            // 1. Extract main media thumbnail (EmbeddedMediaImage or JSON thumbnail_url/display_url)
            val jsonThumbRegexes = listOf(
                Regex(""""thumbnail_url"\s*:\s*"([^"]+)""""),
                Regex(""""display_url"\s*:\s*"([^"]+)""""),
                Regex(""""image_url"\s*:\s*"([^"]+)"""")
            )
            val jsonThumbCandidates = jsonThumbRegexes.mapNotNull { rx ->
                rx.find(html)?.groupValues?.getOrNull(1)?.let(::unescapeJsonString)
            }

            val imgTagCandidates = listOfNotNull(
                extractImgSrcByClass(html, "EmbeddedMediaImage"),
                extractMetaContent(html, "property", "og:image:secure_url"),
                extractMetaContent(html, "property", "og:image"),
                extractAnyCdnMediaImg(html)
            )

            val resolvedThumb = (jsonThumbCandidates + imgTagCandidates)
                .mapNotNull { UrlNormalizer.resolveRelativeUrl(baseUrl, it) }
                .firstOrNull { !UrlNormalizer.isFaviconFallbackUrl(it) }

            // 2. Extract Instagram username / handle
            val username = firstNonBlank(
                Regex("""class=["'][^"']*UsernameText[^"']*["'][^>]*>([^<]+)<""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.getOrNull(1)?.let(::decodeHtmlEntities),
                Regex(""""username"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)?.let(::unescapeJsonString)
            )?.removePrefix("@")?.trim()

            // 3. Extract Instagram caption
            val captionHtml = Regex(
                """<div[^>]*class=["'][^"']*\bCaption\b[^"']*["'][^>]*>(.*?)</div>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ).find(html)?.groupValues?.getOrNull(1)
            val cleanedDivCaption = captionHtml?.let(::stripHtmlTags)
                ?.replace(Regex("""^(?:@?${Regex.escape(username.orEmpty())}\s*)""", RegexOption.IGNORE_CASE), "")
                ?.trim()
                ?.takeIf { it.isNotBlank() && !isGenericSocialBoilerplate(it) }

            val jsonCaption = Regex(""""caption"\s*:\s*"([^"]+)"""")
                .find(html)?.groupValues?.getOrNull(1)
                ?.let(::unescapeJsonString)
                ?.let(::decodeHtmlEntities)
                ?.trim()
                ?.takeIf { it.isNotBlank() && !isGenericSocialBoilerplate(it) }

            val ogTitle = extractMetaContent(html, "property", "og:title")
                ?.takeUnless { UrlNormalizer.isGenericPlatformTitle(it) }
            val ogDesc = extractMetaContent(html, "property", "og:description")
                ?.takeUnless { isGenericSocialBoilerplate(it) }

            val captionText = firstNonBlank(cleanedDivCaption, jsonCaption, ogDesc)

            val finalTitle = when {
                !captionText.isNullOrBlank() && !username.isNullOrBlank() -> {
                    val snippet = captionText.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty().take(90)
                    "@$username: $snippet".trimEnd(':', ' ')
                }
                !captionText.isNullOrBlank() ->
                    captionText.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(110)
                !ogTitle.isNullOrBlank() -> ogTitle
                !username.isNullOrBlank() -> "@$username on Instagram"
                else -> null
            }

            val finalDesc = firstNonBlank(
                captionText,
                if (!username.isNullOrBlank()) "Instagram post by @$username" else null
            )

            if (finalTitle == null && finalDesc == null && resolvedThumb == null) return null
            return EnrichedMetadata(
                title = finalTitle?.take(400),
                description = finalDesc?.take(2000),
                thumbnailUrl = resolvedThumb?.take(2000),
                isLiveReachable = true
            )
        }

        fun parseHtmlMetadata(html: String, baseUrl: String): EnrichedMetadata {
            val jsonLd = extractJsonLdMetadata(html, baseUrl)
            val youtubeShortDesc = extractYouTubeShortDescription(html)

            val rawTitleCandidates = listOfNotNull(
                extractMetaContent(html, "property", "og:title"),
                extractMetaContent(html, "name", "og:title"),
                extractMetaContent(html, "name", "twitter:title"),
                extractMetaContent(html, "property", "twitter:title"),
                extractMetaContent(html, "name", "title"),
                jsonLd.title,
                extractTitleTag(html)
            )
            val bestTitle = rawTitleCandidates.firstOrNull { !UrlNormalizer.isGenericPlatformTitle(it) }

            val rawDescCandidates = listOfNotNull(
                youtubeShortDesc,
                extractMetaContent(html, "property", "og:description"),
                extractMetaContent(html, "name", "og:description"),
                extractMetaContent(html, "name", "twitter:description"),
                extractMetaContent(html, "property", "twitter:description"),
                extractMetaContent(html, "name", "description"),
                jsonLd.description
            )
            val bestDesc = rawDescCandidates.firstOrNull { !isGenericSocialBoilerplate(it) }
                ?: rawDescCandidates.firstOrNull()

            // Primary video/article thumbnail candidates (skip favicons & platform logo shells)
            val primaryImageCandidates = listOfNotNull(
                extractMetaContent(html, "property", "og:image:secure_url"),
                extractMetaContent(html, "property", "og:image"),
                extractMetaContent(html, "property", "og:image:url"),
                extractMetaContent(html, "name", "og:image"),
                extractMetaContent(html, "name", "twitter:image"),
                extractMetaContent(html, "property", "twitter:image"),
                extractMetaContent(html, "name", "twitter:image:src"),
                extractMetaContent(html, "itemprop", "thumbnailUrl"),
                extractMetaContent(html, "itemprop", "image"),
                extractLinkHref(html, "image_src"),
                jsonLd.thumbnailUrl,
                extractVideoPoster(html)
            )

            val primaryThumb = primaryImageCandidates.firstNotNullOfOrNull { candidate ->
                UrlNormalizer.resolveRelativeUrl(baseUrl, candidate)
                    ?.takeUnless { UrlNormalizer.isFaviconFallbackUrl(it) }
            }

            // Icon fallback candidates only if NO genuine preview thumbnail exists
            val iconImageCandidates = listOfNotNull(
                extractLinkHref(html, "apple-touch-icon"),
                extractLinkHref(html, "icon"),
                extractLinkHref(html, "shortcut icon")
            )
            val fallbackIconThumb = iconImageCandidates.firstNotNullOfOrNull { candidate ->
                UrlNormalizer.resolveRelativeUrl(baseUrl, candidate)
            }

            val resolvedThumb = primaryThumb
                ?: fallbackIconThumb
                ?: UrlNormalizer.resolveSmartThumbnailUrl(baseUrl)

            return EnrichedMetadata(
                title = bestTitle?.trim()?.take(400),
                description = bestDesc?.trim()?.take(2000),
                thumbnailUrl = resolvedThumb?.trim()?.take(2000),
                isLiveReachable = true
            )
        }

        private data class JsonLdFields(
            val title: String? = null,
            val description: String? = null,
            val thumbnailUrl: String? = null
        )

        private fun extractJsonLdMetadata(html: String, baseUrl: String): JsonLdFields {
            val scriptRegex = Regex(
                """<script[^>]*type=["']application/ld\+json["'][^>]*>(.*?)</script>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            var foundTitle: String? = null
            var foundDesc: String? = null
            var foundThumb: String? = null

            for (match in scriptRegex.findAll(html)) {
                val jsonText = match.groupValues.getOrNull(1)?.trim() ?: continue
                if (jsonText.isBlank()) continue
                foundTitle = foundTitle ?: firstNonBlank(
                    extractJsonStringField(jsonText, "headline"),
                    extractJsonStringField(jsonText, "name")
                )?.let(::decodeHtmlEntities)
                foundDesc = foundDesc ?: extractJsonStringField(jsonText, "description")
                    ?.let(::decodeHtmlEntities)
                foundThumb = foundThumb ?: extractJsonLdThumbUrl(jsonText, baseUrl)
            }
            return JsonLdFields(foundTitle, foundDesc, foundThumb)
        }

        private fun extractJsonLdThumbUrl(jsonText: String, baseUrl: String): String? {
            for (key in listOf("thumbnailUrl", "thumbnail", "image")) {
                val strVal = extractJsonStringField(jsonText, key)
                if (strVal != null) {
                    val resolved = UrlNormalizer.resolveRelativeUrl(baseUrl, strVal)
                    if (resolved != null && !UrlNormalizer.isFaviconFallbackUrl(resolved)) return resolved
                }
                val arrVal = extractJsonFirstArrayStringField(jsonText, key)
                if (arrVal != null) {
                    val resolved = UrlNormalizer.resolveRelativeUrl(baseUrl, arrVal)
                    if (resolved != null && !UrlNormalizer.isFaviconFallbackUrl(resolved)) return resolved
                }
                val objBlockMatch = Regex(
                    """"${Regex.escape(key)}"\s*:\s*\{([^}]*)\}""",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ).find(jsonText)
                if (objBlockMatch != null) {
                    val inner = objBlockMatch.groupValues[1]
                    val innerUrl = firstNonBlank(
                        extractJsonStringField(inner, "url"),
                        extractJsonStringField(inner, "contentUrl")
                    )
                    val resolved = UrlNormalizer.resolveRelativeUrl(baseUrl, innerUrl)
                    if (resolved != null && !UrlNormalizer.isFaviconFallbackUrl(resolved)) return resolved
                }
            }
            return null
        }

        private fun extractJsonStringField(jsonText: String, key: String): String? {
            val regex = Regex(""""${Regex.escape(key)}"\s*:\s*"((?:\\.|[^"\\]){1,6000})"""")
            val raw = regex.find(jsonText)?.groupValues?.getOrNull(1) ?: return null
            return unescapeJsonString(raw).trim().takeIf { it.isNotBlank() }
        }

        private fun extractJsonFirstArrayStringField(jsonText: String, key: String): String? {
            val regex = Regex(
                """"${Regex.escape(key)}"\s*:\s*\[\s*"((?:\\.|[^"\\]){1,2000})"""",
                RegexOption.DOT_MATCHES_ALL
            )
            val raw = regex.find(jsonText)?.groupValues?.getOrNull(1) ?: return null
            return unescapeJsonString(raw).trim().takeIf { it.isNotBlank() }
        }

        private fun extractYouTubeShortDescription(html: String): String? {
            if (!html.contains("shortDescription")) return null
            val match = Regex(""""shortDescription"\s*:\s*"((?:\\.|[^"\\]){1,4000})"""").find(html)
                ?: return null
            return unescapeJsonString(match.groupValues[1])
                .replace(Regex("""\s+"""), " ")
                .trim()
                .takeIf { it.isNotBlank() }
        }

        private fun extractVideoPoster(html: String): String? {
            val match = Regex(
                """<video[^>]*\bposter\s*=\s*["']([^"']+)["']""",
                RegexOption.IGNORE_CASE
            ).find(html)
            return match?.groupValues?.getOrNull(1)?.let(::decodeHtmlEntities)?.trim()?.takeIf { it.isNotBlank() }
        }

        private fun extractImgSrcByClass(html: String, classToken: String): String? {
            val p1 = Regex(
                """<img[^>]*\bclass\s*=\s*["'][^"']*\b${Regex.escape(classToken)}\b[^"']*["'][^>]*\bsrc\s*=\s*["']([^"']+)["']""",
                RegexOption.IGNORE_CASE
            )
            val p2 = Regex(
                """<img[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*\bclass\s*=\s*["'][^"']*\b${Regex.escape(classToken)}\b[^"']*["']""",
                RegexOption.IGNORE_CASE
            )
            val raw = p1.find(html)?.groupValues?.getOrNull(1)
                ?: p2.find(html)?.groupValues?.getOrNull(1)
            return raw?.let(::decodeHtmlEntities)?.trim()?.takeIf { it.isNotBlank() }
        }

        private fun extractAnyCdnMediaImg(html: String): String? {
            val imgRx = Regex("""<img[^>]*\bsrc\s*=\s*["']([^"']*scontent[^"']+)["']""", RegexOption.IGNORE_CASE)
            return imgRx.findAll(html)
                .mapNotNull { it.groupValues.getOrNull(1)?.let(::decodeHtmlEntities)?.trim() }
                .firstOrNull { candidate ->
                    val lower = candidate.lowercase(Locale.US)
                    !lower.contains("s150x150") && !lower.contains("s100x100") && !UrlNormalizer.isFaviconFallbackUrl(lower)
                }
        }

        private fun extractTitleTag(html: String): String? {
            val match = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)
            return match?.groupValues?.getOrNull(1)?.let { decodeHtmlEntities(it).trim() }?.takeIf { it.isNotBlank() }
        }

        private fun extractMetaContent(html: String, attr: String, attrValue: String): String? {
            val pattern1 = Regex(
                """<meta[^>]*\b$attr\s*=\s*["']${Regex.escape(attrValue)}["'][^>]*\bcontent\s*=\s*["']([^"']+)["']""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val pattern2 = Regex(
                """<meta[^>]*\bcontent\s*=\s*["']([^"']+)["'][^>]*\b$attr\s*=\s*["']${Regex.escape(attrValue)}["']""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val raw = pattern1.find(html)?.groupValues?.getOrNull(1)
                ?: pattern2.find(html)?.groupValues?.getOrNull(1)
            return raw?.let { decodeHtmlEntities(it).trim() }?.takeIf { it.isNotBlank() }
        }

        private fun extractLinkHref(html: String, relValue: String): String? {
            val pattern1 = Regex(
                """<link[^>]*\brel\s*=\s*["'][^"']*\b${Regex.escape(relValue)}\b[^"']*["'][^>]*\bhref\s*=\s*["']([^"']+)["']""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val pattern2 = Regex(
                """<link[^>]*\bhref\s*=\s*["']([^"']+)["'][^>]*\brel\s*=\s*["'][^"']*\b${Regex.escape(relValue)}\b[^"']*["']""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val raw = pattern1.find(html)?.groupValues?.getOrNull(1)
                ?: pattern2.find(html)?.groupValues?.getOrNull(1)
            return raw?.let { decodeHtmlEntities(it).trim() }?.takeIf { it.isNotBlank() }
        }

        private fun isGenericSocialBoilerplate(text: String): Boolean {
            val lower = text.trim().lowercase(Locale.US)
            return lower.startsWith("create an account or log in to instagram") ||
                lower.startsWith("welcome back to instagram") ||
                lower.startsWith("see posts, photos and more on facebook") ||
                lower.startsWith("log into facebook to start sharing") ||
                lower == "watch the latest videos on tiktok" ||
                lower == "enable javascript and cookies to continue"
        }

        private fun stripHtmlTags(rawHtml: String): String {
            return decodeHtmlEntities(
                rawHtml
                    .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("""<[^>]+>"""), " ")
            ).trim()
        }

        private fun unescapeJsonString(raw: String): String {
            val sb = StringBuilder(raw.length)
            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                if (c == '\\' && i + 1 < raw.length) {
                    val next = raw[i + 1]
                    when (next) {
                        '\\' -> { sb.append('\\'); i += 2 }
                        '/' -> { sb.append('/'); i += 2 }
                        '"' -> { sb.append('"'); i += 2 }
                        'n' -> { sb.append(' '); i += 2 }
                        'r' -> { i += 2 }
                        't' -> { sb.append(' '); i += 2 }
                        'u' -> {
                            if (i + 5 < raw.length) {
                                val hex = raw.substring(i + 2, i + 6)
                                val cp = hex.toIntOrNull(16)
                                if (cp != null) {
                                    sb.append(cp.toChar())
                                    i += 6
                                    continue
                                }
                            }
                            sb.append(next); i += 2
                        }
                        else -> { sb.append(next); i += 2 }
                    }
                } else {
                    sb.append(c)
                    i++
                }
            }
            return sb.toString()
        }

        private fun decodeHtmlEntities(input: String): String {
            var decoded = input
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&nbsp;", " ")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .replace("&rsquo;", "'")
                .replace("&lsquo;", "'")
                .replace("&rdquo;", "\"")
                .replace("&ldquo;", "\"")
                .replace("&hellip;", "...")
            decoded = Regex("""&#x([0-9a-fA-F]+);""").replace(decoded) { m ->
                val cp = m.groupValues[1].toIntOrNull(16) ?: return@replace m.value
                runCatching { String(Character.toChars(cp)) }.getOrDefault(m.value)
            }
            decoded = Regex("""&#(\d+);""").replace(decoded) { m ->
                val cp = m.groupValues[1].toIntOrNull() ?: return@replace m.value
                runCatching { String(Character.toChars(cp)) }.getOrDefault(m.value)
            }
            return decoded.replace(Regex("""\s+"""), " ")
        }
    }
}

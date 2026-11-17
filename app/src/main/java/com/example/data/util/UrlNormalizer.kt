package com.example.data.util

import java.net.URI
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

data class ParsedUrlResult(
    val originalUrl: String,
    val canonicalUrl: String,
    val domain: String,
    val inferredContentType: String,
    val inferredSourceType: String,
    val suggestedTitle: String
)

object UrlNormalizer {
    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_source_platform", "utm_creative_format",
        "fbclid", "gclid", "dclid", "msclkid", "mc_cid", "mc_eid",
        "igshid", "igsh", "si", "feature", "ref_src", "ref_url", "spm",
        "is_from_webapp", "sender_device"
    )

    private val BLOCKED_SCHEMES = setOf("javascript", "data", "vbscript", "file")
    private val URL_REGEX = Regex("""https?://[^\s<>"'\]\)]+""", RegexOption.IGNORE_CASE)

    fun extractFirstWebUrl(text: String): String? {
        return URL_REGEX.find(text)?.value?.trimEnd('.', ',', ';', ')', ']')
    }

    fun isSafeRemoteUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return false
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex <= 0) return false
        val scheme = trimmed.substring(0, colonIndex).lowercase(Locale.US)
        if (scheme in BLOCKED_SCHEMES || scheme == "content") return false
        return scheme == "http" || scheme == "https"
    }

    fun isContentUri(raw: String): Boolean {
        return raw.trim().lowercase(Locale.US).startsWith("content://")
    }

    fun normalize(rawUrl: String): ParsedUrlResult? {
        val trimmed = rawUrl.trim()
        if (!isSafeRemoteUrl(trimmed)) return null

        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return null
        }

        val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
        if (scheme != "http" && scheme != "https") return null
        val rawHost = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null

        val portPart = when {
            uri.port == -1 -> ""
            scheme == "http" && uri.port == 80 -> ""
            scheme == "https" && uri.port == 443 -> ""
            else -> ":${uri.port}"
        }

        val cleanPath = (uri.rawPath ?: "").let { path ->
            if (path.length > 1 && path.endsWith("/")) path.dropLast(1) else path
        }

        val filteredQuery = uri.rawQuery
            ?.split("&")
            ?.filter { param ->
                if (param.isBlank()) return@filter false
                val key = param.substringBefore("=").lowercase(Locale.US)
                key !in TRACKING_PARAMS && !key.startsWith("utm_")
            }
            ?.sorted()
            ?.joinToString("&")
            ?.takeIf { it.isNotBlank() }

        val canonicalHost = uri.host.lowercase(Locale.US)
        val canonical = buildString {
            append(scheme).append("://").append(canonicalHost).append(portPart)
            append(if (cleanPath.isEmpty()) "" else cleanPath)
            if (filteredQuery != null) append("?").append(filteredQuery)
        }

        val (contentType, sourceType) = classifyUrl(rawHost, cleanPath)
        val suggestedTitle = inferTitleFromUrl(rawHost, cleanPath)

        return ParsedUrlResult(
            originalUrl = trimmed,
            canonicalUrl = canonical,
            domain = rawHost,
            inferredContentType = contentType,
            inferredSourceType = sourceType,
            suggestedTitle = suggestedTitle
        )
    }

    private fun classifyUrl(host: String, path: String): Pair<String, String> {
        val lowerPath = path.lowercase(Locale.US)
        return when {
            host.contains("youtube.com") || host == "youtu.be" -> "video" to "youtube"
            host.contains("instagram.com") || host == "instagr.am" -> {
                if (lowerPath.contains("/reel") || lowerPath.contains("/tv/")) {
                    "video" to "instagram"
                } else {
                    "article" to "instagram"
                }
            }
            host.contains("tiktok.com") -> "video" to "tiktok"
            host.contains("vimeo.com") -> "video" to "vimeo"
            host.contains("twitch.tv") -> "video" to "twitch"
            host.contains("dailymotion.com") || host == "dai.ly" -> "video" to "dailymotion"
            host == "fb.watch" || ((host.contains("facebook.com") || host == "fb.com") &&
                (lowerPath.contains("/reel") || lowerPath.contains("/watch") || lowerPath.contains("/videos"))) ->
                "video" to "facebook"
            host.contains("facebook.com") || host == "fb.com" -> "article" to "facebook"
            host == "x.com" || host.endsWith(".x.com") || host.contains("twitter.com") -> "article" to "x"
            host.contains("reddit.com") || host == "redd.it" -> "article" to "reddit"
            host.contains("github.com") -> "github" to "github"
            lowerPath.endsWith(".pdf") -> "document" to "web"
            lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
                lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".webp") -> "image" to "web"
            else -> "article" to "web"
        }
    }

    private fun inferTitleFromUrl(host: String, path: String): String {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (host.contains("github.com") && segments.size >= 2) {
            return "${segments[0]}/${segments[1]}"
        }
        val last = segments.lastOrNull()
        if (!last.isNullOrBlank()) {
            return last.replace('-', ' ').replace('_', ' ')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        return host
    }

    fun normalizeUnicode(input: String): String {
        return Normalizer.normalize(input.trim(), Normalizer.Form.NFC)
    }

    fun normalizeTagName(tag: String): String {
        return normalizeUnicode(tag)
            .lowercase(Locale.getDefault())
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun resolveRelativeUrl(baseUrl: String, candidate: String?): String? {
        val raw = candidate?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (raw.startsWith("data:", ignoreCase = true) ||
            raw.startsWith("javascript:", ignoreCase = true) ||
            raw.startsWith("vbscript:", ignoreCase = true)
        ) {
            return null
        }
        return try {
            val resolved = URI(baseUrl).resolve(raw).toString()
            resolved.takeIf { isSafeRemoteUrl(it) }
        } catch (_: Exception) {
            raw.takeIf { isSafeRemoteUrl(it) }
        }
    }

    fun resolveSmartThumbnailUrl(rawUrl: String, domainHint: String? = null): String? {
        val trimmed = rawUrl.trim()
        if (!isSafeRemoteUrl(trimmed)) return null
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return null
        }
        val host = (domainHint ?: uri.host ?: "").lowercase(Locale.US).removePrefix("www.")
        if (host.isBlank()) return null
        val path = uri.rawPath.orEmpty()

        // 1. Direct image URL
        val lowerPath = path.lowercase(Locale.US)
        if (lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
            lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".webp") ||
            lowerPath.endsWith(".gif")
        ) {
            return trimmed
        }

        // 2. YouTube high-quality video thumbnail
        extractYouTubeVideoId(uri, host)?.let { videoId ->
            return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }

        // 3. GitHub repository social Open Graph card
        if (host == "github.com") {
            val segments = path.split('/').filter { it.isNotBlank() }
            val reserved = setOf(
                "features", "explore", "topics", "collections", "trending",
                "events", "sponsors", "settings", "notifications", "login",
                "signup", "pricing", "enterprise", "marketplace", "search"
            )
            if (segments.size >= 2 && segments[0].lowercase(Locale.US) !in reserved) {
                return "https://opengraph.githubassets.com/1/${segments[0]}/${segments[1]}"
            }
        }

        // 4. Deterministic crisp favicon fallback for all other web domains
        return "https://www.google.com/s2/favicons?domain=$host&sz=128"
    }

    fun isFaviconFallbackUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase(Locale.US)
        return lower.contains("google.com/s2/favicons") ||
            lower.endsWith(".ico") ||
            lower.contains("favicon.") ||
            isGenericPlatformIconUrl(lower)
    }

    fun isGenericPlatformIconUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase(Locale.US)
        return lower.contains("/rsrc.php/") ||
            lower.contains("static.cdninstagram.com") ||
            lower.contains("instagram.com/static/images/") ||
            lower.contains("/images/ico/") ||
            lower.contains("apple-touch-icon") ||
            lower.contains("youtube.com/img/desktop/yt_") ||
            lower.contains("abs.twimg.com/r/img/") ||
            lower.contains("abs.twimg.com/responsive-web/client-web/icon") ||
            lower.contains("lf16-tiktok-web.ttwstatic.com") && lower.contains("logo") ||
            lower.contains("default_profile")
    }

    fun looksLikePlaceholderTitle(title: String, rawUrl: String?, domain: String?): Boolean {
        val clean = title.trim()
        if (clean.isBlank()) return true
        val lower = clean.lowercase(Locale.US)
        if (isGenericPlatformTitle(lower)) return true
        val cleanDomain = domain?.lowercase(Locale.US)?.removePrefix("www.")
        if (!cleanDomain.isNullOrBlank() && lower == cleanDomain) return true
        if (!rawUrl.isNullOrBlank()) {
            val inferred = normalize(rawUrl)?.suggestedTitle?.trim()
            if (!inferred.isNullOrBlank() && clean.equals(inferred, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun isGenericPlatformTitle(rawTitle: String?): Boolean {
        val lower = rawTitle?.trim()?.lowercase(Locale.US).orEmpty()
        if (lower.isBlank()) return true
        return lower in setOf(
            "instagram",
            "login • instagram",
            "log in • instagram",
            "instagram photo",
            "instagram reel",
            "tiktok",
            "tiktok - make your day",
            "youtube",
            "facebook",
            "log in or sign up to view",
            "x",
            "twitter",
            "reddit",
            "just a moment...",
            "attention required! | cloudflare",
            "access denied"
        )
    }

    fun extractInstagramShortcode(rawUrl: String): Pair<String, String>? {
        val uri = try {
            URI(rawUrl.trim())
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        if (!host.contains("instagram.com") && host != "instagr.am") return null
        val segments = uri.rawPath.orEmpty().split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null
        return when (val kind = segments[0].lowercase(Locale.US)) {
            "reel", "reels" -> "reel" to segments[1]
            "p" -> "p" to segments[1]
            "tv" -> "tv" to segments[1]
            else -> null
        }
    }

    fun extractYouTubeVideoId(rawUrl: String): String? {
        val uri = try {
            URI(rawUrl.trim())
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        return extractYouTubeVideoId(uri, host)
    }

    private fun extractYouTubeVideoId(uri: URI, host: String): String? {
        if (host == "youtu.be") {
            return uri.rawPath?.trim('/')?.substringBefore('/')?.takeIf { it.isNotBlank() }
        }
        if (host.contains("youtube.com")) {
            val path = uri.rawPath.orEmpty()
            val query = uri.rawQuery.orEmpty()
            query.split('&').forEach { param ->
                if (param.startsWith("v=")) {
                    return param.removePrefix("v=").takeIf { it.isNotBlank() }
                }
            }
            val segments = path.split('/').filter { it.isNotBlank() }
            if (segments.size >= 2 && segments[0] in setOf("shorts", "live", "embed", "v")) {
                return segments[1].takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    fun idempotencyKey(payload: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }
}

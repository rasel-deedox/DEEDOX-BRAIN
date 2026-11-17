package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Locale

data class ExtractedVideoPreview(
    val thumbnailFileUri: String?,
    val durationMs: Long,
    val durationLabel: String
)

object VideoThumbnailGenerator {

    private const val TARGET_FRAME_TIME_US = 1_000_000L
    private const val MAX_THUMB_DIMENSION = 720

    suspend fun extractAndCachePreview(
        context: Context,
        videoUri: Uri,
        cacheKey: String
    ): ExtractedVideoPreview? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L

            val seekUs = if (durationMs in 1L..999L) (durationMs * 500L) else TARGET_FRAME_TIME_US
            val rawFrame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    seekUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    MAX_THUMB_DIMENSION,
                    MAX_THUMB_DIMENSION
                ) ?: retriever.getScaledFrameAtTime(
                    0L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    MAX_THUMB_DIMENSION,
                    MAX_THUMB_DIMENSION
                )
            } else {
                retriever.getFrameAtTime(seekUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }

            val savedThumbUri = rawFrame?.let { bitmap ->
                val scaled = scaleBitmapIfNeeded(bitmap)
                val thumbDir = File(context.cacheDir, "video_thumbs").apply { mkdirs() }
                val safeKey = cacheKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64).ifBlank { "vid" }
                val outFile = File(thumbDir, "vthumb_${safeKey}.jpg")
                outFile.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 86, out)
                }
                if (scaled !== bitmap) scaled.recycle()
                bitmap.recycle()
                Uri.fromFile(outFile).toString()
            }

            ExtractedVideoPreview(
                thumbnailFileUri = savedThumbUri,
                durationMs = durationMs,
                durationLabel = formatDurationLabel(durationMs)
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun scaleBitmapIfNeeded(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= MAX_THUMB_DIMENSION && h <= MAX_THUMB_DIMENSION) return source
        val scale = MAX_THUMB_DIMENSION.toFloat() / maxOf(w, h).toFloat()
        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    fun formatDurationLabel(durationMs: Long): String {
        if (durationMs <= 0L) return ""
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun mergeDurationMetadata(existingJson: String, durationMs: Long): String {
        if (durationMs <= 0L) return existingJson
        return try {
            val obj = if (existingJson.isBlank()) JSONObject() else JSONObject(existingJson)
            obj.put("durationMs", durationMs)
            obj.put("durationLabel", formatDurationLabel(durationMs))
            obj.toString()
        } catch (_: Exception) {
            JSONObject().apply {
                put("durationMs", durationMs)
                put("durationLabel", formatDurationLabel(durationMs))
            }.toString()
        }
    }

    fun extractDurationLabel(metadataJson: String): String? {
        if (metadataJson.isBlank() || metadataJson == "{}") return null
        return try {
            val obj = JSONObject(metadataJson)
            val explicit = obj.optString("durationLabel", "").takeIf { it.isNotBlank() }
            if (explicit != null) return explicit
            val ms = obj.optLong("durationMs", 0L)
            if (ms > 0L) formatDurationLabel(ms) else null
        } catch (_: Exception) {
            null
        }
    }
}

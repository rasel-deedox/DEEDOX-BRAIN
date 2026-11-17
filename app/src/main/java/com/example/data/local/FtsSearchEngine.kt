package com.example.data.local

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.util.UrlNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class MatchStrength(val label: String) {
    EXACT("Exact Match"),
    STRONG("Strong Match"),
    RELATED("Related Match")
}

data class LocalSearchHit(
    val itemId: String,
    val score: Double,
    val matchStrength: MatchStrength,
    val matchReason: String,
    val snippet: String
)

class FtsSearchEngine(private val database: AppDatabase) {
    @Volatile
    private var ftsInitialized = false
    @Volatile
    private var ftsAvailable = false
    @Volatile
    private var useFts5 = false

    private fun checkExistingFtsTable(db: SupportSQLiteDatabase): Boolean? {
        return try {
            db.query(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'items_fts'"
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val sql = cursor.getString(0).orEmpty()
                    sql.contains("fts5", ignoreCase = true)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun detectCompileOptions(db: SupportSQLiteDatabase): Pair<Boolean, Boolean> {
        var supportsFts5 = false
        var supportsFts4 = false
        try {
            db.query("PRAGMA compile_options").use { cursor ->
                while (cursor.moveToNext()) {
                    val opt = cursor.getString(0).orEmpty().uppercase()
                    if (opt == "ENABLE_FTS5" || opt.contains("FTS5")) supportsFts5 = true
                    if (opt == "ENABLE_FTS4" || opt == "ENABLE_FTS3" || opt.contains("FTS4")) supportsFts4 = true
                }
            }
        } catch (_: Exception) {
            // Fall through to safe FTS4 default on Android SQLite
        }
        return supportsFts5 to supportsFts4
    }

    private fun ensureFtsTable(db: SupportSQLiteDatabase) {
        if (ftsInitialized) return
        synchronized(this) {
            if (ftsInitialized) return

            // If table already exists from an earlier session, preserve its engine type without extra DDL.
            val existingIsFts5 = checkExistingFtsTable(db)
            if (existingIsFts5 != null) {
                useFts5 = existingIsFts5
                ftsAvailable = true
                ftsInitialized = true
                return
            }

            val (supportsFts5, supportsFts4) = detectCompileOptions(db)

            if (supportsFts5) {
                try {
                    db.execSQL(
                        """
                        CREATE VIRTUAL TABLE IF NOT EXISTS items_fts USING fts5(
                            item_id UNINDEXED,
                            title,
                            description,
                            domain,
                            tags_text,
                            notes_text,
                            filename,
                            extracted_text,
                            tokenize='unicode61'
                        )
                        """.trimIndent()
                    )
                    useFts5 = true
                    ftsAvailable = true
                    ftsInitialized = true
                    return
                } catch (e: Exception) {
                    Log.w("FtsSearchEngine", "FTS5 creation failed, using FTS4: ${e.message}")
                }
            }

            // Android's built-in SQLite ships FTS3/FTS4 with unicode61 enabled (minSdk 24+), without FTS5.
            if (supportsFts4 || !supportsFts5) {
                try {
                    db.execSQL(
                        """
                        CREATE VIRTUAL TABLE IF NOT EXISTS items_fts USING fts4(
                            item_id,
                            title,
                            description,
                            domain,
                            tags_text,
                            notes_text,
                            filename,
                            extracted_text,
                            tokenize=unicode61
                        )
                        """.trimIndent()
                    )
                    useFts5 = false
                    ftsAvailable = true
                    ftsInitialized = true
                    return
                } catch (e2: Exception) {
                    Log.w("FtsSearchEngine", "FTS4 unavailable, falling back to deterministic scan: ${e2.message}")
                }
            }

            useFts5 = false
            ftsAvailable = false
            ftsInitialized = true
        }
    }

    suspend fun indexItem(itemId: String) = withContext(Dispatchers.IO) {
        val dao = database.libraryDao()
        val item = dao.getItemById(itemId)
        val db = database.openHelper.writableDatabase
        ensureFtsTable(db)
        if (!ftsAvailable) return@withContext

        db.execSQL("DELETE FROM items_fts WHERE item_id = ?", arrayOf(itemId))
        if (item == null || item.deletedAt != null) return@withContext

        val tags = dao.getTagsForItem(itemId).joinToString(" ") { it.name }
        val notes = dao.getNotesForItem(itemId).joinToString(" ") { it.body }
        val file = dao.getFileSourceForItem(itemId)?.displayName.orEmpty()
        val snapshot = dao.getSnapshotForItem(itemId)?.extractedText.orEmpty()

        db.execSQL(
            """
            INSERT INTO items_fts(item_id, title, description, domain, tags_text, notes_text, filename, extracted_text)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                item.id,
                UrlNormalizer.normalizeUnicode(item.title),
                UrlNormalizer.normalizeUnicode(item.description),
                UrlNormalizer.normalizeUnicode(item.domain.orEmpty()),
                UrlNormalizer.normalizeUnicode(tags),
                UrlNormalizer.normalizeUnicode(notes),
                UrlNormalizer.normalizeUnicode(file),
                UrlNormalizer.normalizeUnicode(snapshot)
            )
        )
    }

    suspend fun rebuildIndex() = withContext(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase
        ensureFtsTable(db)
        if (!ftsAvailable) return@withContext
        db.execSQL("DELETE FROM items_fts")
        val items = database.libraryDao().getAllActiveItems()
        items.forEach { item -> indexItem(item.id) }
    }

    suspend fun searchLocal(
        rawQuery: String,
        items: List<ItemEntity>,
        tagsByItem: Map<String, List<TagEntity>>,
        notesByItem: Map<String, List<NoteEntity>>,
        fileSourcesByItem: Map<String, FileSourceEntity>
    ): List<LocalSearchHit> = withContext(Dispatchers.IO) {
        val normalizedQuery = UrlNormalizer.normalizeUnicode(rawQuery)
        if (normalizedQuery.isBlank()) return@withContext emptyList()

        val db = database.openHelper.writableDatabase
        ensureFtsTable(db)

        val ftsCandidateIds = mutableSetOf<String>()
        // Extract strictly Unicode letters/digits/underscores and lowercase them so
        // tokens never collide with FTS boolean keywords (AND/OR/NOT/NEAR) or FTS4 quote/punctuation syntax.
        val ftsTerms = Regex("""[\p{L}\p{N}_]+""")
            .findAll(normalizedQuery)
            .map { it.value.lowercase() }
            .distinct()
            .toList()

        if (ftsAvailable && ftsTerms.isNotEmpty()) {
            val matchExpr = ftsTerms.joinToString(" OR ") { "$it*" }
            try {
                db.query(
                    "SELECT item_id FROM items_fts WHERE items_fts MATCH ? LIMIT 250",
                    arrayOf(matchExpr)
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        ftsCandidateIds.add(cursor.getString(0))
                    }
                }
            } catch (_: Exception) {
                // Deterministic substring scan below covers partial Unicode / Bengali infix tokens
            }
        }

        val lowerQuery = normalizedQuery.lowercase()
        val queryTokens = lowerQuery.split(Regex("""\s+""")).filter { it.isNotBlank() }

        val hits = mutableListOf<LocalSearchHit>()
        for (item in items) {
            if (item.deletedAt != null) continue
            val titleNorm = UrlNormalizer.normalizeUnicode(item.title).lowercase()
            val descNorm = UrlNormalizer.normalizeUnicode(item.description).lowercase()
            val urlNorm = (item.canonicalUrl ?: item.originalUrl).orEmpty().lowercase()
            val domainNorm = item.domain.orEmpty().lowercase()
            val tagNames = tagsByItem[item.id].orEmpty().map { UrlNormalizer.normalizeUnicode(it.name).lowercase() }
            val noteBodies = notesByItem[item.id].orEmpty().map { UrlNormalizer.normalizeUnicode(it.body).lowercase() }
            val fileName = fileSourcesByItem[item.id]?.displayName?.let {
                UrlNormalizer.normalizeUnicode(it).lowercase()
            }.orEmpty()

            var score = 0.0
            val reasons = mutableListOf<String>()

            // PRD 9.2 deterministic ranking:
            // 1. Exact URL / Title matches first
            if (urlNorm == lowerQuery || titleNorm == lowerQuery) {
                score += 100.0
                reasons.add("Exact title/URL match")
            } else if (titleNorm.contains(lowerQuery) || urlNorm.contains(lowerQuery)) {
                score += 65.0
                reasons.add("Title or URL contains query")
            }

            // 2. Filename / title token overlap
            val titleFileMatches = queryTokens.count { token ->
                titleNorm.contains(token) || fileName.contains(token)
            }
            if (titleFileMatches > 0) {
                score += titleFileMatches * 22.0
                if (fileName.isNotEmpty() && queryTokens.any { fileName.contains(it) }) {
                    reasons.add("Filename match")
                } else if (reasons.isEmpty()) {
                    reasons.add("Title token overlap")
                }
            }

            // 3. Tags
            val tagMatches = queryTokens.count { token -> tagNames.any { it.contains(token) } }
            if (tagMatches > 0) {
                score += tagMatches * 18.0
                reasons.add("Tag match")
            }

            // 4. Domain
            if (queryTokens.any { domainNorm.contains(it) }) {
                score += 14.0
                reasons.add("Domain match")
            }

            // 5. Description and notes
            val noteMatches = queryTokens.count { token -> noteBodies.any { it.contains(token) } }
            if (noteMatches > 0) {
                score += noteMatches * 12.0
                reasons.add("Personal note match")
            }
            val descMatches = queryTokens.count { token -> descNorm.contains(token) }
            if (descMatches > 0) {
                score += descMatches * 8.0
                reasons.add("Description match")
            }

            if (item.id in ftsCandidateIds && score == 0.0) {
                score += 6.0
                reasons.add(if (useFts5) "FTS5 index match" else "FTS4 index match")
            }

            if (score > 0.0) {
                val strength = when {
                    score >= 80.0 -> MatchStrength.EXACT
                    score >= 30.0 -> MatchStrength.STRONG
                    else -> MatchStrength.RELATED
                }
                val snippet = noteBodies.firstOrNull { b -> queryTokens.any { b.contains(it) } }
                    ?.take(120)
                    ?: item.description.take(120).ifBlank { item.domain ?: item.contentType }

                hits.add(
                    LocalSearchHit(
                        itemId = item.id,
                        score = score + (item.updatedAt / 1_000_000_000_000.0), // recency tie-breaker
                        matchStrength = strength,
                        matchReason = reasons.distinct().joinToString(" • "),
                        snippet = snippet
                    )
                )
            }
        }

        hits.sortedByDescending { it.score }
    }
}

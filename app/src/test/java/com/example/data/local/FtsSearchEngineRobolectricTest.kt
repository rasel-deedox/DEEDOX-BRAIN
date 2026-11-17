package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FtsSearchEngineRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var engine: FtsSearchEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        engine = FtsSearchEngine(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun rebuildIndex_andSearchLocal_handlesMixedEnglishBengaliAndPunctuationWithoutSqlErrors() = runBlocking {
        val item1 = ItemEntity(
            id = "item-1",
            title = "Jetpack Compose Architecture Guide",
            description = "Modern state management and C++ interop notes",
            originalUrl = "https://github.com/android/compose-samples",
            canonicalUrl = "https://github.com/android/compose-samples",
            domain = "github.com",
            contentType = "github",
            sourceType = "github"
        )
        val item2 = ItemEntity(
            id = "item-2",
            title = "বাংলা অ্যান্ড্রয়েড টিউটোরিয়াল",
            description = "অফলাইন নোট ও রিসোর্স",
            domain = "docs.example.com",
            contentType = "note",
            sourceType = "manual"
        )

        val dao = database.libraryDao()
        dao.upsertItem(item1)
        dao.upsertItem(item2)
        dao.upsertSnapshot(
            ContentSnapshotEntity(
                itemId = item1.id,
                extractedText = "Full extracted snapshot about Navigation AND Coroutines",
                wordCount = 7
            )
        )

        engine.rebuildIndex()

        // Verify items_fts was created in sqlite_master and populated cleanly
        val db = database.openHelper.readableDatabase
        val ftsRowCount = db.query("SELECT COUNT(*) FROM items_fts").use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        assertEquals(2, ftsRowCount)

        // Query containing FTS reserved operators (AND, OR), punctuation, and extracted snapshot words
        val snapshotHits = engine.searchLocal(
            rawQuery = "Coroutines AND https://github.com",
            items = listOf(item1, item2),
            tagsByItem = emptyMap(),
            notesByItem = emptyMap(),
            fileSourcesByItem = emptyMap()
        )
        assertTrue(snapshotHits.any { it.itemId == item1.id })

        // Mixed Bengali query
        val bengaliHits = engine.searchLocal(
            rawQuery = "বাংলা টিউটোরিয়াল",
            items = listOf(item1, item2),
            tagsByItem = emptyMap(),
            notesByItem = emptyMap(),
            fileSourcesByItem = emptyMap()
        )
        assertEquals(1, bengaliHits.size)
        assertEquals(item2.id, bengaliHits.first().itemId)
    }
}

package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BlockerRuleEntity
import com.example.data.local.CollectionEntity
import com.example.data.local.CollectionItemCrossRef
import com.example.data.local.FocusSessionEntity
import com.example.data.local.FolderEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.ItemEntity
import com.example.data.local.ItemFolderCrossRef
import com.example.data.media.VideoThumbnailGenerator
import com.example.data.media.VoicePlaybackState
import com.example.data.repository.HabitGamificationEngine
import com.example.data.repository.WorkspaceBlock
import com.example.data.repository.WorkspaceRepository
import com.example.data.storage.LocalFileReferenceManager
import com.example.monk.LaunchableAppInfo
import com.example.monk.MonkBlockerEngine
import com.example.reminders.ReminderScheduler
import com.example.ui.components.EmergencyExitDialog
import com.example.ui.components.EmergencyExitValidator
import com.example.ui.components.FloatingIconlyBottomDock
import com.example.ui.components.LiquidDeleteConfirmationDialog
import com.example.ui.components.MainNavTab
import com.example.ui.components.MathChallengeEquation
import com.example.ui.components.VaultHeroCardStack
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.MonkRuleBuilderSheet
import com.example.ui.screens.NotionEditorSheet
import com.example.ui.theme.PersonalAiLibraryTheme
import com.example.ui.viewmodel.LibrarySnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h892dp-xhdpi")
class WorkspaceAndMonkFeaturesRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun migration1To2_preservesExistingV1DataAndCreatesWorkspaceTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_${UUID.randomUUID()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create a v1 SQLite database matching AppDatabase v1 tables and seed sample data
        val v1Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `items` (
                                `id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT, `title` TEXT NOT NULL,
                                `description` TEXT NOT NULL, `originalUrl` TEXT, `canonicalUrl` TEXT,
                                `domain` TEXT, `contentType` TEXT NOT NULL, `sourceType` TEXT NOT NULL,
                                `thumbnailUrl` TEXT, `isFavorite` INTEGER NOT NULL, `status` TEXT NOT NULL,
                                `isDeviceLocal` INTEGER NOT NULL, `localOnly` INTEGER NOT NULL,
                                `localVersion` INTEGER NOT NULL, `serverVersion` INTEGER NOT NULL,
                                `syncState` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL,
                                `metadataJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `folders` (
                                `id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT, `name` TEXT NOT NULL,
                                `parentFolderId` TEXT, `colorHex` TEXT NOT NULL, `iconKey` TEXT NOT NULL,
                                `sortOrder` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL,
                                `isArchived` INTEGER NOT NULL, `isInbox` INTEGER NOT NULL,
                                `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                                `deletedAt` INTEGER
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val v1Db = v1Helper.writableDatabase
        v1Db.execSQL(
            """
            INSERT INTO `items` VALUES (
                'item_v1_1', 'user1', 'Saved Kotlin Video', 'Local video clip',
                'content://sample/vid.mp4', 'content://sample/vid.mp4', NULL, 'video', 'local_file',
                NULL, 1, 'organized', 1, 1, 1, 0, 'local_only', 'k1', '{}', 1000, 1000, NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL(
            """
            INSERT INTO `folders` VALUES (
                'folder_v1_1', 'user1', 'Android Lectures', NULL, '#E53935', 'folder',
                0, 0, 0, 0, 1, 'local_only', 1000, 1000, NULL
            )
            """.trimIndent()
        )

        // Apply MIGRATION_1_2 directly on the v1 database
        AppDatabase.MIGRATION_1_2.migrate(v1Db)

        // Verify v1 rows survived intact
        v1Db.query("SELECT id, title FROM items WHERE id = 'item_v1_1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Saved Kotlin Video", c.getString(1))
        }
        v1Db.query("SELECT id, name FROM folders WHERE id = 'folder_v1_1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Android Lectures", c.getString(1))
        }

        // Verify all 7 new v2 tables exist
        val expectedTables = listOf(
            "workspace_notes",
            "workspace_tasks",
            "smart_reminders",
            "habits",
            "habit_completions",
            "blocker_rules",
            "focus_sessions"
        )
        expectedTables.forEach { table ->
            v1Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'").use { c ->
                assertTrue("Expected table $table after MIGRATION_1_2", c.moveToFirst())
            }
        }
        v1Db.close()
        v1Helper.close()
        dbFile.delete()
    }

    @Test
    fun videoThumbnailMetadata_formatsDurationAndMergesJsonCleanly() {
        assertEquals("", VideoThumbnailGenerator.formatDurationLabel(0L))
        assertEquals("04:25", VideoThumbnailGenerator.formatDurationLabel(265_000L))
        assertEquals("1:05:09", VideoThumbnailGenerator.formatDurationLabel(3_909_000L))

        val merged = VideoThumbnailGenerator.mergeDurationMetadata("""{"mimeType":"video/mp4"}""", 265_000L)
        assertEquals("04:25", VideoThumbnailGenerator.extractDurationLabel(merged))
        assertNull(VideoThumbnailGenerator.extractDurationLabel("{}"))
    }

    @Test
    fun localFileReferenceManager_resolvesTreeDisplayNameFallbackCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = LocalFileReferenceManager(context)
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownloads%2FLectures")
        assertEquals("Lectures", manager.resolveTreeDisplayName(uri))
    }

    @Test
    fun workspaceBlockCodec_andHabitStreakCalculation_workAccurately() {
        val blocks = listOf(
            WorkspaceBlock(id = "1", type = WorkspaceBlock.TYPE_H1, text = "Monk Protocol"),
            WorkspaceBlock(id = "2", type = WorkspaceBlock.TYPE_CHECKLIST, text = "Block Shorts", checked = true),
            WorkspaceBlock(id = "3", type = WorkspaceBlock.TYPE_ITEM_REF, linkedItemId = "item_9", linkedItemTitle = "Compose Book")
        )
        val json = WorkspaceBlock.encodeList(blocks)
        val decoded = WorkspaceBlock.parseList(json)
        assertEquals(3, decoded.size)
        assertEquals("Monk Protocol", decoded[0].text)
        assertTrue(decoded[1].checked)
        assertEquals("item_9", decoded[2].linkedItemId)

        val today = 20200L
        val streak = WorkspaceRepository.computeHabitStreak(
            completedDays = setOf(today, today - 1L, today - 2L, today - 4L),
            todayDay = today
        )
        assertEquals(3, streak)

        val yesterdayStreak = WorkspaceRepository.computeHabitStreak(
            completedDays = setOf(today - 1L, today - 2L),
            todayDay = today
        )
        assertEquals(2, yesterdayStreak)
    }

    @Test
    fun reminderScheduler_computesRecurrencesAccurately() {
        val now = 1_700_000_000_000L
        assertNull(
            ReminderScheduler.computeNextRecurrenceMillis(now, ReminderScheduler.RECURRENCE_ONCE, 0, now)
        )
        val every2h = ReminderScheduler.computeNextRecurrenceMillis(now, ReminderScheduler.RECURRENCE_EVERY_2_HOURS, 0, now)
        assertEquals(now + 2L * 3_600_000L, every2h)

        val daily = ReminderScheduler.computeNextRecurrenceMillis(now, ReminderScheduler.RECURRENCE_DAILY, 0, now)
        assertEquals(now + 24L * 3_600_000L, daily)

        val weekly = ReminderScheduler.computeNextRecurrenceMillis(now, ReminderScheduler.RECURRENCE_WEEKLY, 0, now)
        assertEquals(now + 7L * 24L * 3_600_000L, weekly)

        val customPast = ReminderScheduler.computeNextRecurrenceMillis(now - 90_000L, ReminderScheduler.RECURRENCE_CUSTOM_MINUTES, 1, now)
        assertNotNull(customPast)
        assertTrue(customPast!! > now)
    }

    @Test
    fun monkBlockerEngine_evaluatesInstantDelayScheduledOvernightAndFocusSessionRules() {
        val targetPkg = "com.instagram.android"
        val selfPkg = "com.aistudio.ailibrary.k7m9xp"
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 12, 23, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Safe system package never blocked
        val instantRule = BlockerRuleEntity(
            id = "r_inst",
            title = "No Reels",
            mode = MonkBlockerEngine.MODE_INSTANT,
            blockedPackagesJson = MonkBlockerEngine.encodePackages(listOf(targetPkg)),
            isEnabled = true
        )
        assertNull(MonkBlockerEngine.evaluatePackageBlock("com.android.settings", selfPkg, listOf(instantRule), null, now))
        assertNull(MonkBlockerEngine.evaluatePackageBlock(selfPkg, selfPkg, listOf(instantRule), null, now))
        assertEquals("No Reels", MonkBlockerEngine.evaluatePackageBlock(targetPkg, selfPkg, listOf(instantRule), null, now)?.ruleTitle)

        // Overnight scheduled window 22:00 (1320) -> 06:00 (360) active at 23:15
        val overnightRule = BlockerRuleEntity(
            id = "r_sched",
            title = "Night Lock",
            mode = MonkBlockerEngine.MODE_SCHEDULED,
            blockedPackagesJson = MonkBlockerEngine.encodePackages(listOf(targetPkg)),
            startMinuteOfDay = 22 * 60,
            endMinuteOfDay = 6 * 60,
            daysOfWeekCsv = "1,2,3,4,5,6,7",
            isEnabled = true
        )
        assertEquals(
            "Night Lock",
            MonkBlockerEngine.evaluatePackageBlock(targetPkg, selfPkg, listOf(overnightRule), null, now)?.ruleTitle
        )

        // Countdown delay before trigger vs after trigger + active focus session override
        val futureDelayRule = BlockerRuleEntity(
            id = "r_delay",
            title = "Delayed Lock",
            mode = MonkBlockerEngine.MODE_COUNTDOWN_DELAY,
            blockedPackagesJson = MonkBlockerEngine.encodePackages(listOf(targetPkg)),
            delayTriggerAtMillis = now + 60_000L,
            isEnabled = true
        )
        assertNull(MonkBlockerEngine.evaluatePackageBlock(targetPkg, selfPkg, listOf(futureDelayRule), null, now))
        assertEquals(
            MonkBlockerEngine.MODE_COUNTDOWN_DELAY,
            MonkBlockerEngine.evaluatePackageBlock(targetPkg, selfPkg, listOf(futureDelayRule), null, now + 65_000L)?.mode
        )

        val focusSession = FocusSessionEntity(
            id = "fs_1",
            label = "Deep Sprint",
            plannedMinutes = 25,
            startedAt = now - 60_000L
        )
        val focusMatch = MonkBlockerEngine.evaluatePackageBlock(targetPkg, selfPkg, listOf(futureDelayRule), focusSession, now)
        assertEquals("FOCUS_SESSION", focusMatch?.mode)
    }

    @Test
    fun emergencyExitValidator_and3StageComposeFlow_gatesUnlockUntilAllStagesPass() {
        assertFalse(EmergencyExitValidator.isStage1Valid("short", "too short", "nope"))
        assertTrue(
            EmergencyExitValidator.isStage1Valid(
                "Need urgent work passcode",
                "Interrupting my study block",
                "Yes this is genuinely time sensitive"
            )
        )
        assertFalse(EmergencyExitValidator.isStage2Valid("999", 240))
        assertTrue(EmergencyExitValidator.isStage2Valid("240", 240))
        assertFalse(EmergencyExitValidator.isStage3Valid("wrong phrase", 0, true))
        assertFalse(EmergencyExitValidator.isStage3Valid(EmergencyExitValidator.REQUIRED_PHRASE, 5, true))
        assertFalse(EmergencyExitValidator.isStage3Valid(EmergencyExitValidator.REQUIRED_PHRASE, 0, false))
        assertTrue(EmergencyExitValidator.isStage3Valid(EmergencyExitValidator.REQUIRED_PHRASE, 0, true))

        var unlocked = false
        composeTestRule.setContent {
            PersonalAiLibraryTheme {
                EmergencyExitDialog(
                    targetRuleTitle = "Night Lock",
                    onDismiss = {},
                    onUnlockConfirmed = { unlocked = true },
                    initialMathEquation = MathChallengeEquation(prompt = "(12 × 11) + 18 = ?", expectedAnswer = 150),
                    patienceDurationSeconds = 0
                )
            }
        }
        composeTestRule.waitForIdle()

        // Stage 1
        composeTestRule.onNodeWithTag("emergency_reflect_1").performScrollTo().performTextReplacement("Need one urgent security message now")
        composeTestRule.onNodeWithTag("emergency_reflect_2").performScrollTo().performTextReplacement("Pausing architecture sprint briefly")
        composeTestRule.onNodeWithTag("emergency_reflect_3").performScrollTo().performTextReplacement("Only for two minutes maximum")
        composeTestRule.onNodeWithTag("emergency_next_unlock_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Stage 2
        composeTestRule.onNodeWithTag("emergency_math_input").performScrollTo().performTextReplacement("150")
        composeTestRule.onNodeWithTag("emergency_next_unlock_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Stage 3
        composeTestRule.onNodeWithTag("emergency_phrase_input").performScrollTo().performTextReplacement(EmergencyExitValidator.REQUIRED_PHRASE)
        composeTestRule.onNodeWithTag("emergency_confirm_checkbox").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("emergency_next_unlock_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue("Expected EmergencyExitDialog unlock callback after all 3 stages passed", unlocked)
    }

    @Test
    fun foldersScreen_collectionDetailAndAddToTargetActionsWork() {
        val folder = FolderEntity(id = "f_study", name = "CS Lectures", isInbox = false)
        val collection = CollectionEntity(id = "col_finals", name = "Finals Prep", description = "Core videos & notes")
        val item = ItemEntity(
            id = "item_vid",
            title = "OS Kernels Lecture",
            contentType = "video",
            sourceType = "local_file",
            metadataJson = """{"durationLabel":"04:25"}"""
        )
        val snapshot = LibrarySnapshot(
            items = listOf(item),
            folders = listOf(folder),
            itemFolders = listOf(ItemFolderCrossRef(itemId = "item_vid", folderId = "f_study")),
            collections = listOf(collection),
            collectionItems = listOf(CollectionItemCrossRef(collectionId = "col_finals", itemId = "item_vid"))
        )

        var addedCollectionTarget: String? = null
        composeTestRule.setContent {
            PersonalAiLibraryTheme {
                FoldersScreen(
                    snapshot = snapshot,
                    vaultPin = null,
                    isVaultLocked = false,
                    onSetVaultPin = {},
                    onSetVaultLocked = {},
                    onCreateFolder = { _, _ -> },
                    onRenameFolder = { _, _ -> },
                    onDeleteFolder = { _, _ -> },
                    onAddStarterFolders = {},
                    onRenameTag = { _, _ -> },
                    onDeleteTag = {},
                    onCreateCollection = { _, _ -> },
                    onOpenItem = {},
                    onToggleFavorite = {},
                    onQuickAddToCollection = { addedCollectionTarget = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Open Collections tab and tap Finals Prep card
        composeTestRule.onNodeWithTag("folders_root_list").performScrollToKey("folders_segment")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("segment_collections").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("folders_root_list").performScrollToKey("col_finals")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("collection_card_col_finals").performClick()
        composeTestRule.waitForIdle()

        // Verify collection detail shows + Add to Collection and item duration pill
        composeTestRule.onNodeWithTag("collection_add_item_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("collection_detail_list").performScrollToKey("col_col_finals_item_vid")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("video_duration_badge_item_vid", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("collection_detail_list").performScrollToKey("collection_add_cta")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("collection_add_item_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("target_add_link_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("col_finals", addedCollectionTarget)
    }

    @Test
    fun migration2To3_preservesExistingHabitsAndAddsCustomReminderColumns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v2_v3_test_${UUID.randomUUID()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val v2Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `habits` (
                                `id` TEXT NOT NULL PRIMARY KEY,
                                `title` TEXT NOT NULL,
                                `subtitle` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `targetPerWeek` INTEGER NOT NULL,
                                `accentColorHex` TEXT NOT NULL,
                                `cloudSync` INTEGER NOT NULL,
                                `isArchived` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                `deletedAt` INTEGER
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val v2Db = v2Helper.writableDatabase
        v2Db.execSQL(
            """
            INSERT INTO `habits` VALUES (
                'h_v2_1', 'Deep Reading', '20 pages daily', 'focus', 7, '#E53935', 1, 0, 1000, 1000, NULL
            )
            """.trimIndent()
        )

        AppDatabase.MIGRATION_2_3.migrate(v2Db)

        v2Db.query(
            "SELECT title, reminderEnabled, reminderHour, reminderMinute, recurrenceType, intervalMinutes, ringtoneUri, ringtoneTitle FROM habits WHERE id = 'h_v2_1'"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Deep Reading", c.getString(0))
            assertEquals(0, c.getInt(1))
            assertEquals(9, c.getInt(2))
            assertEquals(0, c.getInt(3))
            assertEquals("DAILY", c.getString(4))
            assertEquals(1440, c.getInt(5))
            assertNull(c.getString(6))
            assertEquals("Default System Sound", c.getString(7))
        }

        v2Db.close()
        v2Helper.close()
        dbFile.delete()
    }

    @Test
    fun habitGamificationEngine_awardsCheckInXpAndStreakBonusesAndLevelsUp() {
        val today = 20300L
        val habit = HabitEntity(id = "h_read", title = "Deep Work")
        // 10 completions spanning today..today-9 => 10 * 25 = 250 XP + (10 / 3) * 50 = 150 bonus XP = 400 XP => Level 2 (150/250)
        val completions = (0L until 10L).map { offset ->
            HabitCompletionEntity(
                habitId = "h_read",
                epochDay = today - offset
            )
        }
        val summary = HabitGamificationEngine.compute(listOf(habit), completions, today)
        assertEquals(10, summary.bestStreak)
        assertEquals(1, summary.activeStreaksCount)
        assertEquals(10, summary.totalCompletions)
        assertEquals(150, summary.streakBonusXp)
        assertEquals(400, summary.totalXp)
        assertEquals(2, summary.level)
        assertEquals(150, summary.xpIntoLevel)
        assertEquals("Focus Apprentice", summary.rankTitle)
    }

    @Test
    fun workspaceBlockPageIndex_andNotionEditorSheet_addsNewPagesCleanly() {
        val blocks = listOf(
            WorkspaceBlock(id = "b1", type = WorkspaceBlock.TYPE_TEXT, text = "Intro", pageIndex = 0),
            WorkspaceBlock(id = "b2", type = WorkspaceBlock.TYPE_H2, text = "Second Page", pageIndex = 1)
        )
        val decoded = WorkspaceBlock.parseList(WorkspaceBlock.encodeList(blocks))
        assertEquals(0, decoded[0].pageIndex)
        assertEquals(1, decoded[1].pageIndex)

        composeTestRule.setContent {
            PersonalAiLibraryTheme {
                NotionEditorSheet(
                    existingNote = null,
                    availableItems = emptyList(),
                    playbackState = VoicePlaybackState(),
                    onTogglePlayback = {},
                    onSeekPlayback = { _, _ -> },
                    onAttachReminder = { _, _ -> },
                    onSave = { _, _, _, _, _, _, _, _, _ -> },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("notion_editor_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notion_add_new_page_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("notion_page_pill_2").assertIsDisplayed()
    }

    @Test
    fun liquidDeleteConfirmationDialog_guardsDestructiveActionUntilConfirmed() {
        var deleted = false
        val dialogVisible = androidx.compose.runtime.mutableStateOf(true)

        composeTestRule.setContent {
            PersonalAiLibraryTheme {
                if (dialogVisible.value) {
                    LiquidDeleteConfirmationDialog(
                        title = "Delete Item?",
                        message = "Confirm deletion",
                        onConfirm = {
                            deleted = true
                            dialogVisible.value = false
                        },
                        onDismiss = { dialogVisible.value = false }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("delete_confirmation_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_delete_button").performClick()
        composeTestRule.waitForIdle()
        assertFalse("Cancel must never trigger destructive callback", deleted)

        // Re-open and test confirm path
        dialogVisible.value = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_delete_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Confirm Delete must invoke destructive callback", deleted)
    }

    @Test
    fun monkRuleBuilderSheet_rendersMoreThan15AppsAndAppIconsWithoutTruncation() {
        val apps = (1..20).map { index ->
            LaunchableAppInfo(
                packageName = "com.vendor.app$index",
                label = "Focus App $index",
                iconUri = "file:///tmp/fake_icon_$index.png"
            )
        }
        composeTestRule.setContent {
            PersonalAiLibraryTheme {
                MonkRuleBuilderSheet(
                    launchableApps = apps,
                    onDismiss = {},
                    onSaveRule = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("20 of 20 apps").assertIsDisplayed()
        composeTestRule.onNodeWithTag("monk_rule_apps_lazy_list")
            .performScrollToKey("com.vendor.app20")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("monk_app_row_com.vendor.app20").assertIsDisplayed()
        composeTestRule.onNodeWithTag("monk_app_icon_com.vendor.app20", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun fiveSlotDockAndVaultHeroQuickAccessButtonsWorkCleanly() {
        assertEquals(4, MainNavTab.entries.size)

        var selectedTabResult: MainNavTab? = null
        var openedWorkspace = false
        var openedMonk = false

        composeTestRule.setContent {
            PersonalAiLibraryTheme {
                androidx.compose.foundation.layout.Column {
                    VaultHeroCardStack(
                        snapshot = LibrarySnapshot(),
                        hasPin = false,
                        isVaultLocked = false,
                        onPinVaultClick = {},
                        onVaultDetailsClick = {},
                        onToggleLockClick = {},
                        onOpenWorkspaceClick = { openedWorkspace = true },
                        onOpenMonkModeClick = { openedMonk = true }
                    )
                    FloatingIconlyBottomDock(
                        selectedTab = MainNavTab.HOME,
                        isAiGlowing = false,
                        onSelectTab = { selectedTabResult = it },
                        onAddClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("vault_open_workspace_button").performClick()
        composeTestRule.onNodeWithTag("vault_open_monk_button").performClick()
        composeTestRule.onNodeWithTag("nav_tab_folders").performClick()
        composeTestRule.waitForIdle()

        assertTrue(openedWorkspace)
        assertTrue(openedMonk)
        assertEquals(MainNavTab.FOLDERS, selectedTabResult)
    }
}

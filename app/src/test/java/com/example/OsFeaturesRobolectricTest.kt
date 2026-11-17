package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiLibraryService
import com.example.data.cloud.CloudOsSync
import com.example.data.local.AppDatabase
import com.example.data.local.DetoxSessionState
import com.example.data.local.FinanceSectorEntity
import com.example.data.local.FinanceTransactionEntity
import com.example.data.local.StartupPhaseItemEntity
import com.example.data.local.StartupProjectEntity
import com.example.data.local.StartupSprintTaskEntity
import com.example.data.local.TeamActivityEntity
import com.example.data.local.TeamEmailInviteEntity
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamNotificationEntity
import com.example.data.local.TeamSharedDocEntity
import com.example.data.local.TeamSharedTaskEntity
import com.example.data.local.TeamWorkspaceEntity
import com.example.data.local.WorkspaceNoteEntity
import com.example.data.media.VoicePlaybackState
import com.example.data.repository.BusinessCrmDeal
import com.example.data.repository.BusinessOkrItem
import com.example.data.repository.BusinessSopDoc
import com.example.data.repository.LedgerTimeFilter
import com.example.data.repository.NoteBacklinksParser
import com.example.data.repository.OsRepository
import com.example.data.repository.PollVotesCodec
import com.example.data.repository.TeamAnalyticsCalculator
import com.example.data.repository.WorkspaceBlock
import com.example.monk.MonkBlockerEngine
import com.example.ui.screens.HubSection
import com.example.ui.screens.LauncherMetrics
import com.example.ui.screens.LibraryHubScreen
import com.example.ui.screens.NotionEditorSheet
import com.example.ui.screens.StartupOsScreen
import com.example.ui.screens.TeamCollabScreen
import com.example.ui.screens.parseDollarsToExactCents
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.example.ui.theme.PersonalAiLibraryTheme
import com.example.ui.viewmodel.LibrarySnapshot
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h892dp-xhdpi")
class OsFeaturesRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun migration3To4_preservesV3DataAndCreatesAll10OsTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_3_4_${UUID.randomUUID()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val v3Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val v3Statements = listOf(
                            "CREATE TABLE IF NOT EXISTS `items` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `originalUrl` TEXT, `canonicalUrl` TEXT, `domain` TEXT, `contentType` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `thumbnailUrl` TEXT, `isFavorite` INTEGER NOT NULL, `status` TEXT NOT NULL, `isDeviceLocal` INTEGER NOT NULL, `localOnly` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `serverVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `metadataJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_items_userId` ON `items` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_canonicalUrl` ON `items` (`canonicalUrl`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_contentType` ON `items` (`contentType`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_updatedAt` ON `items` (`updatedAt`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_status` ON `items` (`status`)",
                            "CREATE TABLE IF NOT EXISTS `folders` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `parentFolderId` TEXT, `colorHex` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isInbox` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_folders_userId` ON `folders` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_folders_parentFolderId` ON `folders` (`parentFolderId`)",
                            "CREATE INDEX IF NOT EXISTS `index_folders_sortOrder` ON `folders` (`sortOrder`)",
                            "CREATE TABLE IF NOT EXISTS `item_folders` (`itemId` TEXT NOT NULL, `folderId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`itemId`, `folderId`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_item_folders_folderId` ON `item_folders` (`folderId`)",
                            "CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `origin` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_normalizedName` ON `tags` (`normalizedName`)",
                            "CREATE TABLE IF NOT EXISTS `item_tags` (`itemId` TEXT NOT NULL, `tagId` TEXT NOT NULL, PRIMARY KEY(`itemId`, `tagId`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_item_tags_tagId` ON `item_tags` (`tagId`)",
                            "CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `userId` TEXT, `body` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `isConflictCopy` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_notes_itemId` ON `notes` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `collections` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `collection_items` (`collectionId` TEXT NOT NULL, `itemId` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`collectionId`, `itemId`), FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_collection_items_itemId` ON `collection_items` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `file_sources` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `contentUri` TEXT NOT NULL, `displayName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `modifiedAt` INTEGER, `extension` TEXT NOT NULL, `persistedPermission` INTEGER NOT NULL, `availabilityStatus` TEXT NOT NULL, `lastCheckedAt` INTEGER NOT NULL, `optionalHash` TEXT, `indexingStatus` TEXT NOT NULL, `metadataJson` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_file_sources_itemId` ON `file_sources` (`itemId`)",
                            "CREATE INDEX IF NOT EXISTS `index_file_sources_contentUri` ON `file_sources` (`contentUri`)",
                            "CREATE TABLE IF NOT EXISTS `connected_sources` (`id` TEXT NOT NULL, `providerUri` TEXT NOT NULL, `displayLabel` TEXT NOT NULL, `grantedPermissionFlags` INTEGER NOT NULL, `lastScanTime` INTEGER, `scanStatus` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `itemCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `content_snapshots` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `extractedText` TEXT NOT NULL, `wordCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_content_snapshots_itemId` ON `content_snapshots` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `ai_jobs` (`id` TEXT NOT NULL, `jobType` TEXT NOT NULL, `status` TEXT NOT NULL, `errorCode` TEXT, `errorMessage` TEXT, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `organization_plans` (`id` TEXT NOT NULL, `userId` TEXT, `strategy` TEXT NOT NULL, `confidenceBand` TEXT NOT NULL, `reason` TEXT NOT NULL, `warningsJson` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, `appliedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `organization_operations` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `itemId` TEXT, `itemTitle` TEXT NOT NULL, `operationType` TEXT NOT NULL, `fromFolderId` TEXT, `fromFolderName` TEXT NOT NULL, `toFolderId` TEXT, `toFolderName` TEXT NOT NULL, `tagName` TEXT, `confidenceBand` TEXT NOT NULL, `reason` TEXT NOT NULL, `isSelected` INTEGER NOT NULL, `isApplied` INTEGER NOT NULL, `previousFolderIdsJson` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`planId`) REFERENCES `organization_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_organization_operations_planId` ON `organization_operations` (`planId`)",
                            "CREATE TABLE IF NOT EXISTS `sync_events` (`id` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `operation` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `syncState` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `lastErrorCode` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_sync_events_syncState` ON `sync_events` (`syncState`)",
                            "CREATE INDEX IF NOT EXISTS `index_sync_events_entityId` ON `sync_events` (`entityId`)",
                            "CREATE TABLE IF NOT EXISTS `audit_logs` (`id` TEXT NOT NULL, `actionType` TEXT NOT NULL, `planId` TEXT, `summary` TEXT NOT NULL, `beforeStateJson` TEXT NOT NULL, `afterStateJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `user_settings` (`id` TEXT NOT NULL, `onboardingCompleted` INTEGER NOT NULL, `guestModeConfirmed` INTEGER NOT NULL, `cloudSyncOptIn` INTEGER NOT NULL, `aiEnabled` INTEGER NOT NULL, `includeNotesInAi` INTEGER NOT NULL, `includeExtractedTextInAi` INTEGER NOT NULL, `autoMetadataEnrichment` INTEGER NOT NULL, `diagnosticsEnabled` INTEGER NOT NULL, `darkThemeOverride` INTEGER, `lastSyncCursor` INTEGER NOT NULL, `lastSyncedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `diagnostic_logs` (`id` TEXT NOT NULL, `operationId` TEXT NOT NULL, `errorCode` TEXT NOT NULL, `sanitizedMessage` TEXT NOT NULL, `appVersion` TEXT NOT NULL, `androidVersion` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `workspace_notes` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `blocksJson` TEXT NOT NULL, `tagsCsv` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `linkedItemId` TEXT, `linkedReminderId` TEXT, `audioUri` TEXT, `audioDurationMs` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_userId` ON `workspace_notes` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_cloudSync` ON `workspace_notes` (`cloudSync`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_updatedAt` ON `workspace_notes` (`updatedAt`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_isPinned` ON `workspace_notes` (`isPinned`)",
                            "CREATE TABLE IF NOT EXISTS `workspace_tasks` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `dueDateEpochDay` INTEGER NOT NULL, `priority` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `audioUri` TEXT, `audioDurationMs` INTEGER NOT NULL, `habitId` TEXT, `reminderId` TEXT, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_userId` ON `workspace_tasks` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_dueDateEpochDay` ON `workspace_tasks` (`dueDateEpochDay`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_isCompleted` ON `workspace_tasks` (`isCompleted`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_cloudSync` ON `workspace_tasks` (`cloudSync`)",
                            "CREATE TABLE IF NOT EXISTS `smart_reminders` (`id` TEXT NOT NULL, `userId` TEXT, `mode` TEXT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `priority` TEXT NOT NULL, `targetItemId` TEXT, `targetNoteId` TEXT, `blockerRuleId` TEXT, `triggerAtMillis` INTEGER NOT NULL, `recurrenceType` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, `ringtoneUri` TEXT, `ringtoneTitle` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `lastTriggeredAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_userId` ON `smart_reminders` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_triggerAtMillis` ON `smart_reminders` (`triggerAtMillis`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_isEnabled` ON `smart_reminders` (`isEnabled`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_targetItemId` ON `smart_reminders` (`targetItemId`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_targetNoteId` ON `smart_reminders` (`targetNoteId`)",
                            "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `category` TEXT NOT NULL, `targetPerWeek` INTEGER NOT NULL, `colorHex` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderHour` INTEGER NOT NULL, `reminderMinute` INTEGER NOT NULL, `recurrenceType` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, `ringtoneUri` TEXT, `ringtoneTitle` TEXT NOT NULL, `cloudSync` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_habits_userId` ON `habits` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_habits_isArchived` ON `habits` (`isArchived`)",
                            "CREATE INDEX IF NOT EXISTS `index_habits_cloudSync` ON `habits` (`cloudSync`)",
                            "CREATE TABLE IF NOT EXISTS `habit_completions` (`habitId` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `epochDay`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_habit_completions_epochDay` ON `habit_completions` (`epochDay`)",
                            "CREATE TABLE IF NOT EXISTS `blocker_rules` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `mode` TEXT NOT NULL, `blockedPackagesJson` TEXT NOT NULL, `startMinuteOfDay` INTEGER NOT NULL, `endMinuteOfDay` INTEGER NOT NULL, `daysOfWeekCsv` TEXT NOT NULL, `delayTriggerAtMillis` INTEGER, `delayDurationMinutes` INTEGER NOT NULL, `activeUntilMillis` INTEGER, `preReminderMinutes` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `label` TEXT NOT NULL, `plannedMinutes` INTEGER NOT NULL, `actualMinutes` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `completed` INTEGER NOT NULL, `emergencyUnlocked` INTEGER NOT NULL, `blockedCount` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_focus_sessions_startedAt` ON `focus_sessions` (`startedAt`)"
                        )
                        v3Statements.forEach { db.execSQL(it) }
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val rawDb = v3Helper.writableDatabase
        rawDb.execSQL(
            """
            INSERT INTO `habits` (
                `id`, `userId`, `title`, `subtitle`, `category`, `targetPerWeek`, `colorHex`,
                `reminderEnabled`, `reminderHour`, `reminderMinute`, `recurrenceType`, `intervalMinutes`,
                `ringtoneUri`, `ringtoneTitle`, `cloudSync`, `isArchived`, `createdAt`, `updatedAt`, `deletedAt`
            ) VALUES (
                'h_v3', NULL, 'Deep Work', 'Daily 90m block', 'Focus', 5, '#E53935',
                1, 8, 30, 'DAILY', 1440, NULL, 'Default System Sound', 1, 0, 1000, 1000, NULL
            )
            """.trimIndent()
        )
        rawDb.close()
        v3Helper.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                val habits = migrated.workspaceDao().getAllHabits()
                assertEquals(1, habits.size)
                assertEquals("Deep Work", habits.first().title)

                // Exercise newly added OsDao tables
                val dao = migrated.osDao()
                dao.upsertFinanceSector(
                    FinanceSectorEntity(id = "sec_test", name = "Software", isCustom = true)
                )
                assertEquals(1, dao.getAllFinanceSectors().size)
            }
        } finally {
            migrated.close()
            dbFile.delete()
        }
    }

    @Test
    fun noteBacklinksParser_extractsResolvesAndFindsBidirectionalLinks() {
        val target = WorkspaceNoteEntity(
            id = "n_target",
            title = "Architecture Spec",
            blocksJson = WorkspaceBlock.encodeList(listOf(WorkspaceBlock(text = "Core blueprint")))
        )
        val source = WorkspaceNoteEntity(
            id = "n_source",
            title = "Sprint Kickoff",
            blocksJson = WorkspaceBlock.encodeList(
                listOf(WorkspaceBlock(text = "Review [[Architecture Spec]] and [[Missing Note]]"))
            )
        )
        val notes = listOf(target, source)

        val extracted = NoteBacklinksParser.extractLinkedTitlesFromBlocksJson(source.blocksJson)
        assertEquals(listOf("Architecture Spec", "Missing Note"), extracted)

        val resolved = NoteBacklinksParser.resolveLinks(extracted, notes, excludeNoteId = source.id)
        assertEquals(target.id, resolved[0].targetNote?.id)
        assertNull(resolved[1].targetNote)

        val backlinks = NoteBacklinksParser.findBacklinksForNote(target, notes)
        assertEquals(listOf(source.id), backlinks.map { it.id })
    }

    @Test
    fun digitalDetox_blocksNonWhitelistedApps_allowsWhitelistAndLauncher() {
        val now = 1_700_000_000_000L
        val detox = DetoxSessionState(
            isActive = true,
            durationHours = 6,
            startedAtMillis = now - 60_000L,
            endsAtMillis = now + 3_600_000L,
            greyscaleUi = true,
            suppressNotifications = true,
            whitelistedPackagesCsv = "com.whatsapp"
        )

        val blocked = MonkBlockerEngine.evaluatePackageBlock(
            packageName = "com.instagram.android",
            selfPackageName = "com.aistudio.ailibrary.k7m9xp",
            rules = emptyList(),
            activeFocusSession = null,
            nowMillis = now,
            detoxState = detox,
            homeLauncherPackages = setOf("com.google.android.apps.nexuslauncher")
        )
        assertNotNull(blocked)
        assertEquals(MonkBlockerEngine.MODE_DIGITAL_DETOX, blocked?.mode)

        val whitelisted = MonkBlockerEngine.evaluatePackageBlock(
            packageName = "com.whatsapp",
            selfPackageName = "com.aistudio.ailibrary.k7m9xp",
            rules = emptyList(),
            activeFocusSession = null,
            nowMillis = now,
            detoxState = detox,
            homeLauncherPackages = setOf("com.google.android.apps.nexuslauncher")
        )
        assertNull(whitelisted)

        val launcher = MonkBlockerEngine.evaluatePackageBlock(
            packageName = "com.google.android.apps.nexuslauncher",
            selfPackageName = "com.aistudio.ailibrary.k7m9xp",
            rules = emptyList(),
            activeFocusSession = null,
            nowMillis = now,
            detoxState = detox,
            homeLauncherPackages = setOf("com.google.android.apps.nexuslauncher")
        )
        assertNull(launcher)
    }

    @Test
    fun startupOsRepository_autoSeeds6Phases_andMovesKanbanTasks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.ailibrary.k7m9xp")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake")
                    .build()
            )
        } else FirebaseApp.getInstance()
        val cloudSync = CloudOsSync(FirebaseFirestore.getInstance(app, "demo-db"))
        val repo = OsRepository(context, db, cloudSync, AiLibraryService())

        try {
            runBlocking {
                val created = repo.createStartupProject(
                    name = "FounderOS",
                    tagline = "Execution OS",
                    targetPersona = "Builders",
                    techStack = "Kotlin"
                )
                val phases = db.osDao().getPhaseItemsForStartup(created.id)
                assertEquals((1..6).toSet(), phases.map { it.phaseNumber }.toSet())
                assertTrue(phases.size >= 12)

                val task = repo.saveSprintTask(
                    startupId = created.id,
                    title = "Launch V1",
                    description = "Submit APK",
                    columnStatus = "BACKLOG",
                    priority = "HIGH",
                    tag = "Release"
                )!!
                repo.moveSprintTaskColumn(task, "LIVE")
                val updatedTasks = db.osDao().getSprintTasksForStartup(created.id)
                assertEquals("LIVE", updatedTasks.find { it.id == task.id }?.columnStatus)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun moneyLedgerMath_exactCentsAndDateSectorFilters_areAccurate() {
        assertEquals(12345L, parseDollarsToExactCents("123.45"))
        assertEquals(1999L, parseDollarsToExactCents("$19.99"))
        assertEquals(0L, parseDollarsToExactCents("-50"))

        val sectors = listOf(
            FinanceSectorEntity(id = "s_inc", name = "Revenue", typeAffinity = "INCOME"),
            FinanceSectorEntity(id = "s_exp", name = "Cloud", typeAffinity = "EXPENSE")
        )
        val now = 1_700_000_500_000L
        val txs = listOf(
            FinanceTransactionEntity(
                id = "t1",
                txType = "INCOME",
                amountCents = 100_000L,
                sectorId = "s_inc",
                sectorName = "Revenue",
                occurredAtMillis = now
            ),
            FinanceTransactionEntity(
                id = "t2",
                txType = "EXPENSE",
                amountCents = 25_000L,
                sectorId = "s_exp",
                sectorName = "Cloud",
                occurredAtMillis = now - 60_000L
            ),
            FinanceTransactionEntity(
                id = "t3",
                txType = "EXPENSE",
                amountCents = 10_000L,
                sectorId = "s_exp",
                sectorName = "Cloud",
                occurredAtMillis = now - 40L * 86_400_000L
            )
        )

        val todayOnly = OsRepository.filterTransactions(
            transactions = txs,
            filter = LedgerTimeFilter.TODAY,
            selectedSectorId = null,
            customStartMillis = null,
            customEndMillis = null,
            nowMillis = now
        )
        assertEquals(listOf("t1", "t2"), todayOnly.map { it.id })

        val summary = OsRepository.computeLedgerSummary(todayOnly, sectors)
        assertEquals(100_000L, summary.totalIncomeCents)
        assertEquals(25_000L, summary.totalExpenseCents)
        assertEquals(75_000L, summary.netBalanceCents)
        assertEquals(75, summary.savingsRatePercent)
        assertEquals("$750.00", OsRepository.formatCurrencyCents(summary.netBalanceCents))
    }

    @Test
    fun allPagesHubLauncher_shows5OsTiles_andTriggersCallbacks() {
        var startupOpened = false
        var moneyOpened = false
        var teamOpened = false

        composeTestRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.LIGHT, accentPreset = LiquidAccentPreset.CRIMSON_BRONZE) {
                LibraryHubScreen(
                    snapshot = LibrarySnapshot(),
                    initialSection = HubSection.ALL_PAGES,
                    themeMode = AppThemeMode.LIGHT,
                    accentPreset = LiquidAccentPreset.CRIMSON_BRONZE,
                    vaultPin = null,
                    isSignedIn = false,
                    userDisplayName = "Tester",
                    userEmail = null,
                    isOnline = true,
                    syncBadgeState = com.example.data.cloud.SyncBadgeState.LOCAL_ONLY,
                    isSyncing = false,
                    syncMessage = null,
                    isAiBusy = false,
                    onSelectThemeMode = {},
                    onSelectAccentPreset = {},
                    onUpdateProfileName = {},
                    onSecretAdminTrigger = {},
                    onOpenItem = {},
                    onToggleFavorite = {},
                    onOrganizeInboxClick = {},
                    onOpenAiAssistant = {},
                    onPickLocalFileClick = {},
                    onPickPhotoVideoClick = {},
                    onConnectFolderTreeClick = {},
                    onRefreshFileHealthClick = {},
                    onGoogleSignInClick = {},
                    onSignOutClick = {},
                    onSyncNowClick = {},
                    onUpdateSettings = {},
                    onExportBackupClick = {},
                    onImportBackupClick = {},
                    onClearDiagnosticsClick = {},
                    launcherMetrics = LauncherMetrics(
                        monkSummary = "Detox Locked",
                        isDetoxActive = true,
                        workspaceSummary = "LV 3 • 4 Notes",
                        startupSummary = "2 Ventures",
                        moneySummary = "$1,250.00 Net",
                        teamSummary = "1 Team"
                    ),
                    onOpenStartupOs = { startupOpened = true },
                    onOpenMoneyLedger = { moneyOpened = true },
                    onOpenTeamCollab = { teamOpened = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("hub_tab_all_pages").assertIsDisplayed()
        composeTestRule.onNodeWithTag("launcher_tile_startup_os").performClick()
        composeTestRule.onNodeWithTag("launcher_tile_money_ledger").performClick()
        composeTestRule.onNodeWithTag("launcher_tile_team_collab").performClick()

        assertTrue(startupOpened)
        assertTrue(moneyOpened)
        assertTrue(teamOpened)
    }

    @Test
    fun notionEditorSheet_displaysExactTimestampsAndBacklinksCard() {
        val target = WorkspaceNoteEntity(
            id = "note_a",
            title = "Roadmap",
            blocksJson = WorkspaceBlock.encodeList(listOf(WorkspaceBlock(text = "See [[Sprint Plan]]"))),
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_600_000L
        )
        val backlinker = WorkspaceNoteEntity(
            id = "note_b",
            title = "Sprint Plan",
            blocksJson = WorkspaceBlock.encodeList(listOf(WorkspaceBlock(text = "Depends on [[Roadmap]]"))),
            createdAt = 1_700_000_100_000L,
            updatedAt = 1_700_000_200_000L
        )

        composeTestRule.setContent {
            PersonalAiLibraryTheme {
                NotionEditorSheet(
                    existingNote = target,
                    availableItems = emptyList(),
                    allNotes = listOf(target, backlinker),
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
        composeTestRule.onNodeWithTag("notion_timestamps_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("block_tool_link_note").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Backlinks (1)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun migration4To5_preservesTeamRowsAndAddsUpgradedColumns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_4_5_${UUID.randomUUID()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val v4Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val v3HelperCallback = listOf(
                            "CREATE TABLE IF NOT EXISTS `items` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `originalUrl` TEXT, `canonicalUrl` TEXT, `domain` TEXT, `contentType` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `thumbnailUrl` TEXT, `isFavorite` INTEGER NOT NULL, `status` TEXT NOT NULL, `isDeviceLocal` INTEGER NOT NULL, `localOnly` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `serverVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `metadataJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_items_userId` ON `items` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_canonicalUrl` ON `items` (`canonicalUrl`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_contentType` ON `items` (`contentType`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_updatedAt` ON `items` (`updatedAt`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_status` ON `items` (`status`)",
                            "CREATE TABLE IF NOT EXISTS `folders` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `parentFolderId` TEXT, `colorHex` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isInbox` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_folders_userId` ON `folders` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_folders_parentFolderId` ON `folders` (`parentFolderId`)",
                            "CREATE INDEX IF NOT EXISTS `index_folders_sortOrder` ON `folders` (`sortOrder`)",
                            "CREATE TABLE IF NOT EXISTS `item_folders` (`itemId` TEXT NOT NULL, `folderId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`itemId`, `folderId`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_item_folders_folderId` ON `item_folders` (`folderId`)",
                            "CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `origin` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_normalizedName` ON `tags` (`normalizedName`)",
                            "CREATE TABLE IF NOT EXISTS `item_tags` (`itemId` TEXT NOT NULL, `tagId` TEXT NOT NULL, PRIMARY KEY(`itemId`, `tagId`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_item_tags_tagId` ON `item_tags` (`tagId`)",
                            "CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `userId` TEXT, `body` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `isConflictCopy` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_notes_itemId` ON `notes` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `collections` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `collection_items` (`collectionId` TEXT NOT NULL, `itemId` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`collectionId`, `itemId`), FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_collection_items_itemId` ON `collection_items` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `file_sources` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `contentUri` TEXT NOT NULL, `displayName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `modifiedAt` INTEGER, `extension` TEXT NOT NULL, `persistedPermission` INTEGER NOT NULL, `availabilityStatus` TEXT NOT NULL, `lastCheckedAt` INTEGER NOT NULL, `optionalHash` TEXT, `indexingStatus` TEXT NOT NULL, `metadataJson` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_file_sources_itemId` ON `file_sources` (`itemId`)",
                            "CREATE INDEX IF NOT EXISTS `index_file_sources_contentUri` ON `file_sources` (`contentUri`)",
                            "CREATE TABLE IF NOT EXISTS `connected_sources` (`id` TEXT NOT NULL, `providerUri` TEXT NOT NULL, `displayLabel` TEXT NOT NULL, `grantedPermissionFlags` INTEGER NOT NULL, `lastScanTime` INTEGER, `scanStatus` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `itemCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `content_snapshots` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `extractedText` TEXT NOT NULL, `wordCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_content_snapshots_itemId` ON `content_snapshots` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `ai_jobs` (`id` TEXT NOT NULL, `jobType` TEXT NOT NULL, `status` TEXT NOT NULL, `errorCode` TEXT, `errorMessage` TEXT, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `organization_plans` (`id` TEXT NOT NULL, `userId` TEXT, `strategy` TEXT NOT NULL, `confidenceBand` TEXT NOT NULL, `reason` TEXT NOT NULL, `warningsJson` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, `appliedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `organization_operations` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `itemId` TEXT, `itemTitle` TEXT NOT NULL, `operationType` TEXT NOT NULL, `fromFolderId` TEXT, `fromFolderName` TEXT NOT NULL, `toFolderId` TEXT, `toFolderName` TEXT NOT NULL, `tagName` TEXT, `confidenceBand` TEXT NOT NULL, `reason` TEXT NOT NULL, `isSelected` INTEGER NOT NULL, `isApplied` INTEGER NOT NULL, `previousFolderIdsJson` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`planId`) REFERENCES `organization_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_organization_operations_planId` ON `organization_operations` (`planId`)",
                            "CREATE TABLE IF NOT EXISTS `sync_events` (`id` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `operation` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `syncState` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `lastErrorCode` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_sync_events_syncState` ON `sync_events` (`syncState`)",
                            "CREATE INDEX IF NOT EXISTS `index_sync_events_entityId` ON `sync_events` (`entityId`)",
                            "CREATE TABLE IF NOT EXISTS `audit_logs` (`id` TEXT NOT NULL, `actionType` TEXT NOT NULL, `planId` TEXT, `summary` TEXT NOT NULL, `beforeStateJson` TEXT NOT NULL, `afterStateJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `user_settings` (`id` TEXT NOT NULL, `onboardingCompleted` INTEGER NOT NULL, `guestModeConfirmed` INTEGER NOT NULL, `cloudSyncOptIn` INTEGER NOT NULL, `aiEnabled` INTEGER NOT NULL, `includeNotesInAi` INTEGER NOT NULL, `includeExtractedTextInAi` INTEGER NOT NULL, `autoMetadataEnrichment` INTEGER NOT NULL, `diagnosticsEnabled` INTEGER NOT NULL, `darkThemeOverride` INTEGER, `lastSyncCursor` INTEGER NOT NULL, `lastSyncedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `diagnostic_logs` (`id` TEXT NOT NULL, `operationId` TEXT NOT NULL, `errorCode` TEXT NOT NULL, `sanitizedMessage` TEXT NOT NULL, `appVersion` TEXT NOT NULL, `androidVersion` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `workspace_notes` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `blocksJson` TEXT NOT NULL, `tagsCsv` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `linkedItemId` TEXT, `linkedReminderId` TEXT, `audioUri` TEXT, `audioDurationMs` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_userId` ON `workspace_notes` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_cloudSync` ON `workspace_notes` (`cloudSync`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_updatedAt` ON `workspace_notes` (`updatedAt`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_isPinned` ON `workspace_notes` (`isPinned`)",
                            "CREATE TABLE IF NOT EXISTS `workspace_tasks` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `dueDateEpochDay` INTEGER NOT NULL, `priority` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `audioUri` TEXT, `audioDurationMs` INTEGER NOT NULL, `habitId` TEXT, `reminderId` TEXT, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_userId` ON `workspace_tasks` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_dueDateEpochDay` ON `workspace_tasks` (`dueDateEpochDay`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_isCompleted` ON `workspace_tasks` (`isCompleted`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_cloudSync` ON `workspace_tasks` (`cloudSync`)",
                            "CREATE TABLE IF NOT EXISTS `smart_reminders` (`id` TEXT NOT NULL, `userId` TEXT, `mode` TEXT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `priority` TEXT NOT NULL, `targetItemId` TEXT, `targetNoteId` TEXT, `blockerRuleId` TEXT, `triggerAtMillis` INTEGER NOT NULL, `recurrenceType` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, `ringtoneUri` TEXT, `ringtoneTitle` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `lastTriggeredAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_userId` ON `smart_reminders` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_triggerAtMillis` ON `smart_reminders` (`triggerAtMillis`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_isEnabled` ON `smart_reminders` (`isEnabled`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_targetItemId` ON `smart_reminders` (`targetItemId`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_targetNoteId` ON `smart_reminders` (`targetNoteId`)",
                            "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `category` TEXT NOT NULL, `targetPerWeek` INTEGER NOT NULL, `colorHex` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderHour` INTEGER NOT NULL, `reminderMinute` INTEGER NOT NULL, `recurrenceType` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, `ringtoneUri` TEXT, `ringtoneTitle` TEXT NOT NULL, `cloudSync` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_habits_userId` ON `habits` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_habits_isArchived` ON `habits` (`isArchived`)",
                            "CREATE INDEX IF NOT EXISTS `index_habits_cloudSync` ON `habits` (`cloudSync`)",
                            "CREATE TABLE IF NOT EXISTS `habit_completions` (`habitId` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `epochDay`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_habit_completions_epochDay` ON `habit_completions` (`epochDay`)",
                            "CREATE TABLE IF NOT EXISTS `blocker_rules` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `mode` TEXT NOT NULL, `blockedPackagesJson` TEXT NOT NULL, `startMinuteOfDay` INTEGER NOT NULL, `endMinuteOfDay` INTEGER NOT NULL, `daysOfWeekCsv` TEXT NOT NULL, `delayTriggerAtMillis` INTEGER, `delayDurationMinutes` INTEGER NOT NULL, `activeUntilMillis` INTEGER, `preReminderMinutes` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `label` TEXT NOT NULL, `plannedMinutes` INTEGER NOT NULL, `actualMinutes` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `completed` INTEGER NOT NULL, `emergencyUnlocked` INTEGER NOT NULL, `blockedCount` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_focus_sessions_startedAt` ON `focus_sessions` (`startedAt`)"
                        )
                        v3HelperCallback.forEach { db.execSQL(it) }
                        AppDatabase.MIGRATION_3_4.migrate(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val rawDb = v4Helper.writableDatabase
        rawDb.execSQL(
            """
            INSERT INTO `team_members` (`teamId`, `uid`, `displayName`, `email`, `role`, `isOnline`, `lastActiveAtMillis`)
            VALUES ('team_v4', 'uid_alice', 'Alice', 'alice@test.com', 'OWNER', 1, 5000)
            """.trimIndent()
        )
        rawDb.close()
        v4Helper.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                migrated.osDao().upsertTeamMember(
                    TeamMemberEntity(
                        teamId = "team_v4",
                        uid = "uid_bob",
                        displayName = "Bob",
                        roleTag = "UI/UX Designer",
                        customStatus = "In Focus"
                    )
                )
                assertTrue(migrated.isOpen)
            }
        } finally {
            migrated.close()
            dbFile.delete()
        }
    }

    @Test
    fun teamAnalyticsCalculator_computesOverallMemberAndWeeklyVelocity() {
        val members = listOf(
            TeamMemberEntity(teamId = "t1", uid = "u1", displayName = "Alice", role = "OWNER", roleTag = "Team Lead"),
            TeamMemberEntity(teamId = "t1", uid = "u2", displayName = "Bob", role = "MEMBER", roleTag = "Lead Developer")
        )
        val tasks = listOf(
            TeamSharedTaskEntity(
                id = "t_1",
                teamId = "t1",
                title = "API Auth",
                dayOfWeek = "Sat",
                assignedToUidsCsv = "u1,u2",
                assignedToNamesCsv = "Alice,Bob",
                status = "done",
                isCompleted = true
            ),
            TeamSharedTaskEntity(
                id = "t_2",
                teamId = "t1",
                title = "Compose Charts",
                dayOfWeek = "Mon",
                assignedToUidsCsv = "u2",
                assignedToNamesCsv = "Bob",
                status = "in_progress",
                isCompleted = false
            ),
            TeamSharedTaskEntity(
                id = "t_3",
                teamId = "t1",
                title = "Play Review",
                dayOfWeek = "Fri",
                assignedToUidsCsv = "u1",
                assignedToNamesCsv = "Alice",
                status = "todo",
                isCompleted = false
            ),
            TeamSharedTaskEntity(
                id = "t_4",
                teamId = "t1",
                title = "QA Smoke",
                dayOfWeek = "Sat",
                assignedToUidsCsv = "u2",
                assignedToNamesCsv = "Bob",
                status = "done",
                isCompleted = true
            )
        )

        val summary = TeamAnalyticsCalculator.compute(members, tasks)
        assertEquals(4, summary.totalTasks)
        assertEquals(2, summary.doneTasks)
        assertEquals(1, summary.inProgressTasks)
        assertEquals(1, summary.todoTasks)
        assertEquals(50, summary.completionPercent)

        val bobStat = summary.memberStats.first { it.uid == "u2" }
        assertEquals(3, bobStat.assignedCount)
        assertEquals(2, bobStat.completedCount)
        assertEquals(1, bobStat.inProgressCount)

        val satVelocity = summary.weeklyVelocity.first { it.dayOfWeek == "Sat" }
        assertEquals(2, satVelocity.totalCount)
        assertEquals(2, satVelocity.doneCount)
        assertEquals(1f, satVelocity.completionRatio)
    }

    @Test
    fun teamCollabScreen_filtersByDayAndMember_rendersAnalyticsAndNotices() {
        val team = TeamWorkspaceEntity(
            id = "team_1",
            ownerId = "u_alice",
            name = "Core Dev Team",
            description = "Ship Liquid Glass v5",
            inviteCode = "DEEDOX-849",
            maxMembers = 5,
            memberUidsCsv = "u_alice,u_bob"
        )
        val members = listOf(
            TeamMemberEntity(
                teamId = "team_1",
                uid = "u_alice",
                displayName = "Alice",
                email = "alice@deedox.io",
                role = "OWNER",
                roleTag = "Team Lead",
                customStatus = "Available"
            ),
            TeamMemberEntity(
                teamId = "team_1",
                uid = "u_bob",
                displayName = "Bob",
                email = "bob@deedox.io",
                role = "MEMBER",
                roleTag = "Lead Developer",
                customStatus = "Working on API"
            )
        )
        val tasks = listOf(
            TeamSharedTaskEntity(
                id = "task_mon",
                teamId = "team_1",
                title = "Monday API Freeze",
                dayOfWeek = "Mon",
                startTime = "10:00 AM",
                endTime = "12:30 PM",
                assignedToUidsCsv = "u_bob",
                assignedToNamesCsv = "Bob",
                status = "in_progress",
                priority = "HIGH"
            ),
            TeamSharedTaskEntity(
                id = "task_wed",
                teamId = "team_1",
                title = "Wednesday UI QA",
                dayOfWeek = "Wed",
                startTime = "02:00 PM",
                endTime = "04:00 PM",
                assignedToUidsCsv = "u_alice",
                assignedToNamesCsv = "Alice",
                status = "done",
                isCompleted = true,
                priority = "MEDIUM"
            )
        )
        val notes = listOf(
            TeamSharedDocEntity(
                id = "note_1",
                teamId = "team_1",
                title = "Architecture Spec",
                content = "Checklist ready",
                blocksJson = WorkspaceBlock.encodeList(listOf(WorkspaceBlock(type = WorkspaceBlock.TYPE_CHECKLIST, text = "Deploy rules", checked = true))),
                tagsCsv = "backend",
                authorUid = "u_alice",
                authorName = "Alice",
                lastEditedBy = "Bob"
            )
        )
        val notices = listOf(
            TeamNotificationEntity(
                id = "not_1",
                teamId = "team_1",
                senderUid = "u_alice",
                senderName = "Alice",
                title = "Sprint Sync",
                message = "@Bob check API freeze",
                taggedUidsCsv = "u_bob",
                taggedNamesCsv = "Bob"
            )
        )

        composeTestRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.LIGHT, accentPreset = LiquidAccentPreset.CRIMSON_BRONZE) {
                TeamCollabScreen(
                    isSignedIn = true,
                    currentUserUid = "u_alice",
                    teams = listOf(team),
                    activeTeamId = team.id,
                    members = members,
                    tasks = tasks,
                    docs = notes,
                    notifications = notices,
                    latestAlertBanner = null,
                    invitePreview = null,
                    isBusy = false,
                    teamError = null,
                    onGoogleSignInClick = {},
                    onSelectTeam = {},
                    onCreateTeam = { _, _, _, _ -> },
                    onLookupInviteCode = {},
                    onJoinTeamByCode = { _, _ -> },
                    onExpandTeamLimit = {},
                    onUpdateMemberProfile = { _, _, _, _, _ -> },
                    onSaveScheduleTask = { _, _, _, _, _, _, _, _, _, _, _ -> },
                    onCycleTaskStatus = {},
                    onSetTaskStatus = { _, _ -> },
                    onDeleteSharedTask = {},
                    onSaveSharedNote = { _, _, _, _, _, _, _ -> },
                    onDeleteSharedDoc = {},
                    onSendTeamAlert = { _, _, _, _ -> },
                    onDismissAlertBanner = {},
                    onClearTeamError = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_selector_pill").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monday API Freeze").assertIsDisplayed()

        // Filter by Wed day tab -> Monday task hides, Wednesday task stays
        composeTestRule.onNodeWithTag("day_filter_Wed").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Wednesday UI QA").assertIsDisplayed()

        // Switch to Analytics tab -> verify completion gauge & member performance card
        composeTestRule.onNodeWithTag("team_tab_analytics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_analytics_gauge_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
        composeTestRule.onNodeWithTag("team_member_performance_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("team_weekly_velocity_card").performScrollTo().assertIsDisplayed()

        // Switch to Notes tab -> verify co-edited Notion note
        composeTestRule.onNodeWithTag("team_tab_notes").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Architecture Spec").assertIsDisplayed()

        // Switch to Notices tab -> verify tagged notice
        composeTestRule.onNodeWithTag("team_tab_notices").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sprint Sync").assertIsDisplayed()
    }

    @Test
    fun migration5To6_addsBusinessModulesTeamActivitiesAndEmailInvites() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_5_6_${UUID.randomUUID()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val v5Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val v3Tables = listOf(
                            "CREATE TABLE IF NOT EXISTS `items` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `originalUrl` TEXT, `canonicalUrl` TEXT, `domain` TEXT, `contentType` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `thumbnailUrl` TEXT, `isFavorite` INTEGER NOT NULL, `status` TEXT NOT NULL, `isDeviceLocal` INTEGER NOT NULL, `localOnly` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `serverVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `metadataJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_items_userId` ON `items` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_canonicalUrl` ON `items` (`canonicalUrl`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_contentType` ON `items` (`contentType`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_updatedAt` ON `items` (`updatedAt`)",
                            "CREATE INDEX IF NOT EXISTS `index_items_status` ON `items` (`status`)",
                            "CREATE TABLE IF NOT EXISTS `folders` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `parentFolderId` TEXT, `colorHex` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isInbox` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_folders_userId` ON `folders` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_folders_parentFolderId` ON `folders` (`parentFolderId`)",
                            "CREATE INDEX IF NOT EXISTS `index_folders_sortOrder` ON `folders` (`sortOrder`)",
                            "CREATE TABLE IF NOT EXISTS `item_folders` (`itemId` TEXT NOT NULL, `folderId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`itemId`, `folderId`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_item_folders_folderId` ON `item_folders` (`folderId`)",
                            "CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `origin` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_normalizedName` ON `tags` (`normalizedName`)",
                            "CREATE TABLE IF NOT EXISTS `item_tags` (`itemId` TEXT NOT NULL, `tagId` TEXT NOT NULL, PRIMARY KEY(`itemId`, `tagId`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_item_tags_tagId` ON `item_tags` (`tagId`)",
                            "CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `userId` TEXT, `body` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `isConflictCopy` INTEGER NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_notes_itemId` ON `notes` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `collections` (`id` TEXT NOT NULL, `userId` TEXT, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `localVersion` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `collection_items` (`collectionId` TEXT NOT NULL, `itemId` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`collectionId`, `itemId`), FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_collection_items_itemId` ON `collection_items` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `file_sources` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `contentUri` TEXT NOT NULL, `displayName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `modifiedAt` INTEGER, `extension` TEXT NOT NULL, `persistedPermission` INTEGER NOT NULL, `availabilityStatus` TEXT NOT NULL, `lastCheckedAt` INTEGER NOT NULL, `optionalHash` TEXT, `indexingStatus` TEXT NOT NULL, `metadataJson` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_file_sources_itemId` ON `file_sources` (`itemId`)",
                            "CREATE INDEX IF NOT EXISTS `index_file_sources_contentUri` ON `file_sources` (`contentUri`)",
                            "CREATE TABLE IF NOT EXISTS `connected_sources` (`id` TEXT NOT NULL, `providerUri` TEXT NOT NULL, `displayLabel` TEXT NOT NULL, `grantedPermissionFlags` INTEGER NOT NULL, `lastScanTime` INTEGER, `scanStatus` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `itemCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `content_snapshots` (`id` TEXT NOT NULL, `itemId` TEXT NOT NULL, `extractedText` TEXT NOT NULL, `wordCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_content_snapshots_itemId` ON `content_snapshots` (`itemId`)",
                            "CREATE TABLE IF NOT EXISTS `ai_jobs` (`id` TEXT NOT NULL, `jobType` TEXT NOT NULL, `status` TEXT NOT NULL, `errorCode` TEXT, `errorMessage` TEXT, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `organization_plans` (`id` TEXT NOT NULL, `userId` TEXT, `strategy` TEXT NOT NULL, `confidenceBand` TEXT NOT NULL, `reason` TEXT NOT NULL, `warningsJson` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, `appliedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `organization_operations` (`id` TEXT NOT NULL, `planId` TEXT NOT NULL, `itemId` TEXT, `itemTitle` TEXT NOT NULL, `operationType` TEXT NOT NULL, `fromFolderId` TEXT, `fromFolderName` TEXT NOT NULL, `toFolderId` TEXT, `toFolderName` TEXT NOT NULL, `tagName` TEXT, `confidenceBand` TEXT NOT NULL, `reason` TEXT NOT NULL, `isSelected` INTEGER NOT NULL, `isApplied` INTEGER NOT NULL, `previousFolderIdsJson` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`planId`) REFERENCES `organization_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_organization_operations_planId` ON `organization_operations` (`planId`)",
                            "CREATE TABLE IF NOT EXISTS `sync_events` (`id` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `operation` TEXT NOT NULL, `idempotencyKey` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `syncState` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `lastErrorCode` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_sync_events_syncState` ON `sync_events` (`syncState`)",
                            "CREATE INDEX IF NOT EXISTS `index_sync_events_entityId` ON `sync_events` (`entityId`)",
                            "CREATE TABLE IF NOT EXISTS `audit_logs` (`id` TEXT NOT NULL, `actionType` TEXT NOT NULL, `planId` TEXT, `summary` TEXT NOT NULL, `beforeStateJson` TEXT NOT NULL, `afterStateJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `user_settings` (`id` TEXT NOT NULL, `onboardingCompleted` INTEGER NOT NULL, `guestModeConfirmed` INTEGER NOT NULL, `cloudSyncOptIn` INTEGER NOT NULL, `aiEnabled` INTEGER NOT NULL, `includeNotesInAi` INTEGER NOT NULL, `includeExtractedTextInAi` INTEGER NOT NULL, `autoMetadataEnrichment` INTEGER NOT NULL, `diagnosticsEnabled` INTEGER NOT NULL, `darkThemeOverride` INTEGER, `lastSyncCursor` INTEGER NOT NULL, `lastSyncedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `diagnostic_logs` (`id` TEXT NOT NULL, `operationId` TEXT NOT NULL, `errorCode` TEXT NOT NULL, `sanitizedMessage` TEXT NOT NULL, `appVersion` TEXT NOT NULL, `androidVersion` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `workspace_notes` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `blocksJson` TEXT NOT NULL, `tagsCsv` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `linkedItemId` TEXT, `linkedReminderId` TEXT, `audioUri` TEXT, `audioDurationMs` INTEGER NOT NULL, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_userId` ON `workspace_notes` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_cloudSync` ON `workspace_notes` (`cloudSync`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_updatedAt` ON `workspace_notes` (`updatedAt`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_notes_isPinned` ON `workspace_notes` (`isPinned`)",
                            "CREATE TABLE IF NOT EXISTS `workspace_tasks` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `dueDateEpochDay` INTEGER NOT NULL, `priority` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `audioUri` TEXT, `audioDurationMs` INTEGER NOT NULL, `habitId` TEXT, `reminderId` TEXT, `syncState` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_userId` ON `workspace_tasks` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_dueDateEpochDay` ON `workspace_tasks` (`dueDateEpochDay`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_isCompleted` ON `workspace_tasks` (`isCompleted`)",
                            "CREATE INDEX IF NOT EXISTS `index_workspace_tasks_cloudSync` ON `workspace_tasks` (`cloudSync`)",
                            "CREATE TABLE IF NOT EXISTS `smart_reminders` (`id` TEXT NOT NULL, `userId` TEXT, `mode` TEXT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `priority` TEXT NOT NULL, `targetItemId` TEXT, `targetNoteId` TEXT, `blockerRuleId` TEXT, `triggerAtMillis` INTEGER NOT NULL, `recurrenceType` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, `ringtoneUri` TEXT, `ringtoneTitle` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `cloudSync` INTEGER NOT NULL, `lastTriggeredAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_userId` ON `smart_reminders` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_triggerAtMillis` ON `smart_reminders` (`triggerAtMillis`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_isEnabled` ON `smart_reminders` (`isEnabled`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_targetItemId` ON `smart_reminders` (`targetItemId`)",
                            "CREATE INDEX IF NOT EXISTS `index_smart_reminders_targetNoteId` ON `smart_reminders` (`targetNoteId`)",
                            "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `userId` TEXT, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `category` TEXT NOT NULL, `targetPerWeek` INTEGER NOT NULL, `colorHex` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderHour` INTEGER NOT NULL, `reminderMinute` INTEGER NOT NULL, `recurrenceType` TEXT NOT NULL, `intervalMinutes` INTEGER NOT NULL, `ringtoneUri` TEXT, `ringtoneTitle` TEXT NOT NULL, `cloudSync` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_habits_userId` ON `habits` (`userId`)",
                            "CREATE INDEX IF NOT EXISTS `index_habits_isArchived` ON `habits` (`isArchived`)",
                            "CREATE INDEX IF NOT EXISTS `index_habits_cloudSync` ON `habits` (`cloudSync`)",
                            "CREATE TABLE IF NOT EXISTS `habit_completions` (`habitId` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `epochDay`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            "CREATE INDEX IF NOT EXISTS `index_habit_completions_epochDay` ON `habit_completions` (`epochDay`)",
                            "CREATE TABLE IF NOT EXISTS `blocker_rules` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `mode` TEXT NOT NULL, `blockedPackagesJson` TEXT NOT NULL, `startMinuteOfDay` INTEGER NOT NULL, `endMinuteOfDay` INTEGER NOT NULL, `daysOfWeekCsv` TEXT NOT NULL, `delayTriggerAtMillis` INTEGER, `delayDurationMinutes` INTEGER NOT NULL, `activeUntilMillis` INTEGER, `preReminderMinutes` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `label` TEXT NOT NULL, `plannedMinutes` INTEGER NOT NULL, `actualMinutes` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `completed` INTEGER NOT NULL, `emergencyUnlocked` INTEGER NOT NULL, `blockedCount` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                            "CREATE INDEX IF NOT EXISTS `index_focus_sessions_startedAt` ON `focus_sessions` (`startedAt`)"
                        )
                        v3Tables.forEach { db.execSQL(it) }
                        AppDatabase.MIGRATION_3_4.migrate(db)
                        AppDatabase.MIGRATION_4_5.migrate(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val rawDb = v5Helper.writableDatabase
        rawDb.execSQL(
            """
            INSERT INTO `startup_projects` (`id`, `userId`, `name`, `tagline`, `stage`, `targetPersona`, `techStack`, `aiPlanMarkdown`, `cloudSync`, `createdAt`, `updatedAt`)
            VALUES ('biz_v5', 'u1', 'V5 Venture', 'Preserved across v6', 'MVP', 'Founders', 'Kotlin', '', 1, 1000, 1000)
            """.trimIndent()
        )
        rawDb.close()
        v5Helper.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()

        try {
            runBlocking {
                val preserved = migrated.osDao().getStartupProjectById("biz_v5")
                assertNotNull(preserved)
                assertEquals("STARTUP", preserved?.projectType)
                migrated.osDao().upsertTeamActivities(
                    listOf(
                        TeamActivityEntity(
                            id = "act_v6",
                            teamId = "team_v6",
                            actorUid = "u1",
                            actorName = "Alice",
                            actionType = "TEAM",
                            summary = "Created workspace",
                            targetId = "team_v6"
                        )
                    )
                )
                migrated.osDao().upsertTeamEmailInvites(
                    listOf(
                        TeamEmailInviteEntity(
                            id = "inv_v6",
                            teamId = "team_v6",
                            teamName = "V6 Team",
                            inviteCode = "DEEDOX-600",
                            invitedEmail = "bob@deedox.io",
                            invitedRole = "ADMIN",
                            roleTag = "Lead",
                            invitedByUid = "u1",
                            invitedByName = "Alice",
                            status = "PENDING"
                        )
                    )
                )
                assertTrue(migrated.isOpen)
            }
        } finally {
            migrated.close()
            dbFile.delete()
        }
    }

    @Test
    fun businessModelsAndPollCodec_encodeDecodeAndVoteSafely() {
        val deals = listOf(
            BusinessCrmDeal(clientName = "Acme Corp", contactEmail = "ceo@acme.io", dealValueCents = 450_000L, stage = BusinessCrmDeal.STAGE_NEGOTIATION),
            BusinessCrmDeal(clientName = "Beta Labs", dealValueCents = 125_000L, stage = BusinessCrmDeal.STAGE_ACTIVE_CLIENT)
        )
        val parsedDeals = BusinessCrmDeal.parseList(BusinessCrmDeal.encodeList(deals))
        assertEquals(2, parsedDeals.size)
        assertEquals(450_000L, parsedDeals[0].dealValueCents)
        assertEquals(BusinessCrmDeal.STAGE_ACTIVE_CLIENT, parsedDeals[1].stage)

        val okrs = listOf(BusinessOkrItem(objective = "Grow ARR", keyResult = "$10k MRR", currentValue = 5000.0, targetValue = 10000.0, unitLabel = "USD"))
        val parsedOkrs = BusinessOkrItem.parseList(BusinessOkrItem.encodeList(okrs))
        assertEquals(0.5f, parsedOkrs.first().progressRatio)

        val (sopTitle, sopContent) = BusinessSopDoc.defaultTemplateContent(BusinessSopDoc.TYPE_PITCH_DECK, "Deedox")
        assertTrue(sopTitle.contains("Deedox"))
        assertTrue(sopContent.contains("Problem"))

        val optsJson = PollVotesCodec.encodeOptions(listOf("Ship Friday", "Ship Monday", ""))
        assertEquals(listOf("Ship Friday", "Ship Monday"), PollVotesCodec.parseOptions(optsJson))
        val v1 = PollVotesCodec.vote("{}", "u1", 0)
        val v2 = PollVotesCodec.vote(v1, "u2", 1)
        val v3 = PollVotesCodec.vote(v2, "u1", 1)
        val parsedVotes = PollVotesCodec.parseVotes(v3)
        assertEquals(1, parsedVotes["u1"])
        assertEquals(1, parsedVotes["u2"])
    }

    @Test
    fun teamAnalyticsCalculator_handlesAllFiveKanbanStages() {
        val members = listOf(TeamMemberEntity(teamId = "t1", uid = "u1", displayName = "Alice", role = "OWNER"))
        val tasks = listOf(
            TeamSharedTaskEntity(id = "s1", teamId = "t1", title = "Idea", status = "backlog", assignedToUidsCsv = "u1"),
            TeamSharedTaskEntity(id = "s2", teamId = "t1", title = "Spec", status = "todo", assignedToUidsCsv = "u1"),
            TeamSharedTaskEntity(id = "s3", teamId = "t1", title = "Build", status = "in_progress", assignedToUidsCsv = "u1"),
            TeamSharedTaskEntity(id = "s4", teamId = "t1", title = "QA", status = "review", assignedToUidsCsv = "u1"),
            TeamSharedTaskEntity(id = "s5", teamId = "t1", title = "Ship", status = "done", isCompleted = true, assignedToUidsCsv = "u1")
        )
        val summary = TeamAnalyticsCalculator.compute(members, tasks)
        assertEquals(5, summary.totalTasks)
        assertEquals(1, summary.backlogTasks)
        assertEquals(1, summary.reviewTasks)
        assertEquals(1, summary.doneTasks)
        assertEquals(20, summary.completionPercent)
    }

    @Test
    fun teamCollabScreen_kanbanTableActivityTabsAndPollVoting() {
        val team = TeamWorkspaceEntity(
            id = "team_ent",
            ownerId = "u_alice",
            name = "Enterprise Squad",
            description = "Unified OS",
            inviteCode = "DEEDOX-999",
            maxMembers = 10,
            memberUidsCsv = "u_alice,u_bob"
        )
        val members = listOf(
            TeamMemberEntity(teamId = "team_ent", uid = "u_alice", displayName = "Alice", role = "OWNER", roleTag = "Founder"),
            TeamMemberEntity(teamId = "team_ent", uid = "u_bob", displayName = "Bob", role = "MEMBER", roleTag = "Engineer")
        )
        val tasks = listOf(
            TeamSharedTaskEntity(
                id = "t_rev",
                teamId = "team_ent",
                title = "Audit Billing API",
                dayOfWeek = "Thu",
                status = "review",
                priority = "HIGH",
                assignedToUidsCsv = "u_bob",
                assignedToNamesCsv = "Bob",
                sourceBusinessId = "biz_1"
            )
        )
        val poll = TeamNotificationEntity(
            id = "poll_1",
            teamId = "team_ent",
            senderUid = "u_alice",
            senderName = "Alice",
            title = "Release Window?",
            message = "Vote on ship time",
            noticeType = "poll",
            pollOptionsJson = PollVotesCodec.encodeOptions(listOf("Morning", "Evening")),
            pollVotesJson = "{}"
        )
        val activities = listOf(
            TeamActivityEntity(
                id = "act_1",
                teamId = "team_ent",
                actorUid = "u_bob",
                actorName = "Bob",
                actionType = "BUSINESS_SYNC",
                summary = "Synced Venture roadmap to team"
            )
        )
        var votedOption = -1

        composeTestRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.LIGHT, accentPreset = LiquidAccentPreset.CRIMSON_BRONZE) {
                TeamCollabScreen(
                    isSignedIn = true,
                    currentUserUid = "u_alice",
                    teams = listOf(team),
                    activeTeamId = team.id,
                    members = members,
                    tasks = tasks,
                    docs = emptyList(),
                    notifications = listOf(poll),
                    latestAlertBanner = null,
                    invitePreview = null,
                    isBusy = false,
                    teamError = null,
                    activities = activities,
                    onGoogleSignInClick = {},
                    onSelectTeam = {},
                    onCreateTeam = { _, _, _, _ -> },
                    onLookupInviteCode = {},
                    onJoinTeamByCode = { _, _ -> },
                    onExpandTeamLimit = {},
                    onUpdateMemberProfile = { _, _, _, _, _ -> },
                    onSaveScheduleTask = { _, _, _, _, _, _, _, _, _, _, _ -> },
                    onCycleTaskStatus = {},
                    onSetTaskStatus = { _, _ -> },
                    onDeleteSharedTask = {},
                    onSaveSharedNote = { _, _, _, _, _, _, _ -> },
                    onDeleteSharedDoc = {},
                    onSendTeamAlert = { _, _, _, _ -> },
                    onDismissAlertBanner = {},
                    onClearTeamError = {},
                    onVotePoll = { _, _, idx -> votedOption = idx },
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasTestTag("team_task_view_kanban"))
        composeTestRule.onNodeWithTag("team_task_view_kanban").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasTestTag("team_kanban_stage_review"))
        composeTestRule.onNodeWithTag("team_kanban_stage_review").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasText("Audit Billing API"))
        composeTestRule.onNodeWithText("Audit Billing API").assertIsDisplayed()

        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasTestTag("team_task_view_table"))
        composeTestRule.onNodeWithTag("team_task_view_table").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasText("Audit Billing API"))
        composeTestRule.onNodeWithText("Audit Billing API").assertIsDisplayed()

        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasTestTag("team_tab_activity"))
        composeTestRule.onNodeWithTag("team_tab_activity").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasText("Synced Venture roadmap to team"))
        composeTestRule.onNodeWithText("Synced Venture roadmap to team").assertIsDisplayed()

        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasTestTag("team_tab_notices"))
        composeTestRule.onNodeWithTag("team_tab_notices").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("team_collab_screen_list").performScrollToNode(hasTestTag("poll_option_poll_1_0"))
        composeTestRule.onNodeWithTag("poll_option_poll_1_0").performClick()
        assertEquals(0, votedOption)
    }

    @Test
    fun startupOsScreen_rendersFinancialSnapshotCrmOkrsSopsAndShareTeamDialog() {
        val project = StartupProjectEntity(
            id = "biz_1",
            name = "Deedox Cloud",
            tagline = "AI OS for builders",
            stage = "BETA",
            projectType = "AGENCY",
            crmDealsJson = BusinessCrmDeal.encodeList(
                listOf(BusinessCrmDeal(id = "d1", clientName = "Northstar Labs", dealValueCents = 300_000L, stage = BusinessCrmDeal.STAGE_ACTIVE_CLIENT))
            ),
            okrsJson = BusinessOkrItem.encodeList(
                listOf(BusinessOkrItem(id = "o1", objective = "Q4 Revenue", keyResult = "Close 5 retainers", currentValue = 3.0, targetValue = 5.0, unitLabel = "deals"))
            ),
            sopDocsJson = BusinessSopDoc.encodeList(
                listOf(BusinessSopDoc(id = "s1", title = "Client Onboarding Playbook", templateType = BusinessSopDoc.TYPE_SOP, content = "Step 1: Kickoff"))
            )
        )
        val txs = listOf(
            FinanceTransactionEntity(id = "tx1", txType = "INCOME", amountCents = 500_000L, sectorId = "sec_1", sectorName = "Client Payment", paymentMethod = "BANK")
        )
        val teams = listOf(
            TeamWorkspaceEntity(id = "team_1", ownerId = "u1", name = "Core Agency", inviteCode = "AGY-101")
        )

        composeTestRule.setContent {
            PersonalAiLibraryTheme(themeMode = AppThemeMode.NIGHT, accentPreset = LiquidAccentPreset.CRIMSON_BRONZE) {
                StartupOsScreen(
                    projects = listOf(project),
                    phaseItems = listOf(
                        StartupPhaseItemEntity(id = "p1", startupId = "biz_1", phaseNumber = 1, phaseTitle = "1. Ideation", title = "Validate ICP")
                    ),
                    sprintTasks = listOf(
                        StartupSprintTaskEntity(id = "sp1", startupId = "biz_1", title = "Launch Portal", columnStatus = "IN_DEV")
                    ),
                    isGeneratingBlueprint = false,
                    aiError = null,
                    financeTransactions = txs,
                    teams = teams,
                    onCreateProject = { _, _, _, _, _, _ -> },
                    onUpdateStage = { _, _ -> },
                    onTogglePhaseItem = {},
                    onAddCustomPhaseItem = { _, _, _, _, _ -> },
                    onDeletePhaseItem = {},
                    onSaveSprintTask = { _, _, _, _, _, _, _ -> },
                    onMoveSprintTaskColumn = { _, _ -> },
                    onDeleteSprintTask = {},
                    onDeleteProject = {},
                    onGenerateAiBlueprint = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("startup_os_screen_list").performScrollToNode(hasTestTag("startup_financial_snapshot_card"))
        composeTestRule.onNodeWithTag("startup_financial_snapshot_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("startup_os_screen_list").performScrollToNode(hasTestTag("startup_tab_crm"))
        composeTestRule.onNodeWithTag("startup_tab_crm").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("startup_os_screen_list").performScrollToNode(hasText("Northstar Labs"))
        composeTestRule.onNodeWithText("Northstar Labs").assertIsDisplayed()

        composeTestRule.onNodeWithTag("startup_os_screen_list").performScrollToNode(hasTestTag("startup_tab_okrs"))
        composeTestRule.onNodeWithTag("startup_tab_okrs").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("startup_os_screen_list").performScrollToNode(hasText("Q4 Revenue"))
        composeTestRule.onNodeWithText("Q4 Revenue").assertIsDisplayed()

        composeTestRule.onNodeWithTag("startup_os_screen_list").performScrollToNode(hasTestTag("startup_tab_sops"))
        composeTestRule.onNodeWithTag("startup_tab_sops").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("startup_os_screen_list").performScrollToNode(hasText("Client Onboarding Playbook"))
        composeTestRule.onNodeWithText("Client Onboarding Playbook").assertIsDisplayed()

        composeTestRule.onNodeWithTag("startup_os_screen_list").performScrollToNode(hasTestTag("startup_share_team_button"))
        composeTestRule.onNodeWithTag("startup_share_team_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Share Workflow with Team").assertIsDisplayed()
    }
}

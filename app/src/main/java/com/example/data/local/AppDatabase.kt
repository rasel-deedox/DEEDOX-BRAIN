package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ItemEntity::class,
        FolderEntity::class,
        ItemFolderCrossRef::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        NoteEntity::class,
        CollectionEntity::class,
        CollectionItemCrossRef::class,
        FileSourceEntity::class,
        ConnectedSourceEntity::class,
        ContentSnapshotEntity::class,
        AiJobEntity::class,
        OrganizationPlanEntity::class,
        OrganizationOperationEntity::class,
        SyncEventEntity::class,
        AuditLogEntity::class,
        UserSettingsEntity::class,
        DiagnosticLogEntity::class,
        WorkspaceNoteEntity::class,
        WorkspaceTaskEntity::class,
        SmartReminderEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        BlockerRuleEntity::class,
        FocusSessionEntity::class,
        StartupProjectEntity::class,
        StartupPhaseItemEntity::class,
        StartupSprintTaskEntity::class,
        FinanceSectorEntity::class,
        FinanceTransactionEntity::class,
        TeamWorkspaceEntity::class,
        TeamMemberEntity::class,
        TeamSharedTaskEntity::class,
        TeamSharedDocEntity::class,
        TeamNotificationEntity::class,
        TeamActivityEntity::class,
        TeamEmailInviteEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun osDao(): OsDao

    companion object {
        const val INBOX_FOLDER_ID = "system_inbox"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workspace_notes` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT,
                        `title` TEXT NOT NULL,
                        `blocksJson` TEXT NOT NULL,
                        `tagsCsv` TEXT NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `cloudSync` INTEGER NOT NULL,
                        `linkedItemId` TEXT,
                        `linkedReminderId` TEXT,
                        `audioUri` TEXT,
                        `audioDurationMs` INTEGER NOT NULL,
                        `syncState` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_notes_userId` ON `workspace_notes` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_notes_cloudSync` ON `workspace_notes` (`cloudSync`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_notes_updatedAt` ON `workspace_notes` (`updatedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_notes_isPinned` ON `workspace_notes` (`isPinned`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workspace_tasks` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `dueDateEpochDay` INTEGER NOT NULL,
                        `priority` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `cloudSync` INTEGER NOT NULL,
                        `audioUri` TEXT,
                        `audioDurationMs` INTEGER NOT NULL,
                        `habitId` TEXT,
                        `reminderId` TEXT,
                        `syncState` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_tasks_userId` ON `workspace_tasks` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_tasks_dueDateEpochDay` ON `workspace_tasks` (`dueDateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_tasks_isCompleted` ON `workspace_tasks` (`isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_tasks_cloudSync` ON `workspace_tasks` (`cloudSync`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `smart_reminders` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT,
                        `mode` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `targetItemId` TEXT,
                        `targetNoteId` TEXT,
                        `blockerRuleId` TEXT,
                        `triggerAtMillis` INTEGER NOT NULL,
                        `recurrenceType` TEXT NOT NULL,
                        `intervalMinutes` INTEGER NOT NULL,
                        `ringtoneUri` TEXT,
                        `ringtoneTitle` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `cloudSync` INTEGER NOT NULL,
                        `lastTriggeredAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_smart_reminders_userId` ON `smart_reminders` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_smart_reminders_triggerAtMillis` ON `smart_reminders` (`triggerAtMillis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_smart_reminders_isEnabled` ON `smart_reminders` (`isEnabled`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_smart_reminders_targetItemId` ON `smart_reminders` (`targetItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_smart_reminders_targetNoteId` ON `smart_reminders` (`targetNoteId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `targetPerWeek` INTEGER NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `cloudSync` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_userId` ON `habits` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_isArchived` ON `habits` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_cloudSync` ON `habits` (`cloudSync`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_completions` (
                        `habitId` TEXT NOT NULL,
                        `epochDay` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `count` INTEGER NOT NULL,
                        PRIMARY KEY(`habitId`, `epochDay`),
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_epochDay` ON `habit_completions` (`epochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blocker_rules` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `blockedPackagesJson` TEXT NOT NULL,
                        `startMinuteOfDay` INTEGER NOT NULL,
                        `endMinuteOfDay` INTEGER NOT NULL,
                        `daysOfWeekCsv` TEXT NOT NULL,
                        `delayTriggerAtMillis` INTEGER,
                        `delayDurationMinutes` INTEGER NOT NULL,
                        `activeUntilMillis` INTEGER,
                        `preReminderMinutes` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `id` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `plannedMinutes` INTEGER NOT NULL,
                        `actualMinutes` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER,
                        `completed` INTEGER NOT NULL,
                        `emergencyUnlocked` INTEGER NOT NULL,
                        `blockedCount` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_startedAt` ON `focus_sessions` (`startedAt`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderHour` INTEGER NOT NULL DEFAULT 9")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderMinute` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `recurrenceType` TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `intervalMinutes` INTEGER NOT NULL DEFAULT 1440")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `ringtoneUri` TEXT")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `ringtoneTitle` TEXT NOT NULL DEFAULT 'Default System Sound'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `startup_projects` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT,
                        `name` TEXT NOT NULL,
                        `tagline` TEXT NOT NULL,
                        `stage` TEXT NOT NULL,
                        `targetPersona` TEXT NOT NULL,
                        `techStack` TEXT NOT NULL,
                        `aiPlanMarkdown` TEXT NOT NULL,
                        `cloudSync` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_startup_projects_userId` ON `startup_projects` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_startup_projects_updatedAt` ON `startup_projects` (`updatedAt`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `startup_phase_items` (
                        `id` TEXT NOT NULL,
                        `startupId` TEXT NOT NULL,
                        `phaseNumber` INTEGER NOT NULL,
                        `phaseTitle` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `resourceUrl` TEXT,
                        `isCompleted` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`startupId`) REFERENCES `startup_projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_startup_phase_items_startupId` ON `startup_phase_items` (`startupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_startup_phase_items_phaseNumber` ON `startup_phase_items` (`phaseNumber`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `startup_sprint_tasks` (
                        `id` TEXT NOT NULL,
                        `startupId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `columnStatus` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`startupId`) REFERENCES `startup_projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_startup_sprint_tasks_startupId` ON `startup_sprint_tasks` (`startupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_startup_sprint_tasks_columnStatus` ON `startup_sprint_tasks` (`columnStatus`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `finance_sectors` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT,
                        `name` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `typeAffinity` TEXT NOT NULL,
                        `isCustom` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_sectors_userId` ON `finance_sectors` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_sectors_typeAffinity` ON `finance_sectors` (`typeAffinity`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `finance_transactions` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT,
                        `txType` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `sectorId` TEXT NOT NULL,
                        `sectorName` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `occurredAtMillis` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_userId` ON `finance_transactions` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_sectorId` ON `finance_transactions` (`sectorId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_occurredAtMillis` ON `finance_transactions` (`occurredAtMillis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transactions_txType` ON `finance_transactions` (`txType`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_workspaces` (
                        `id` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `inviteCode` TEXT NOT NULL,
                        `maxMembers` INTEGER NOT NULL,
                        `memberUidsCsv` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_workspaces_ownerId` ON `team_workspaces` (`ownerId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_members` (
                        `teamId` TEXT NOT NULL,
                        `uid` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `isOnline` INTEGER NOT NULL,
                        `lastActiveAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`teamId`, `uid`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_members_teamId` ON `team_members` (`teamId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_shared_tasks` (
                        `id` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `assigneeUid` TEXT NOT NULL,
                        `assigneeName` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `dueAtMillis` INTEGER,
                        `createdByUid` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_shared_tasks_teamId` ON `team_shared_tasks` (`teamId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_shared_tasks_isCompleted` ON `team_shared_tasks` (`isCompleted`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_shared_docs` (
                        `id` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `resourceUrl` TEXT,
                        `authorUid` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_shared_docs_teamId` ON `team_shared_docs` (`teamId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_notifications` (
                        `id` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `senderUid` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_notifications_teamId` ON `team_notifications` (`teamId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_notifications_createdAtMillis` ON `team_notifications` (`createdAtMillis`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `team_members` ADD COLUMN `photoUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `team_members` ADD COLUMN `roleTag` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `team_members` ADD COLUMN `customStatus` TEXT NOT NULL DEFAULT 'Available'")
                db.execSQL("ALTER TABLE `team_members` ADD COLUMN `joinedAtMillis` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `dayOfWeek` TEXT NOT NULL DEFAULT 'Mon'")
                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `dateLabel` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `startTime` TEXT NOT NULL DEFAULT '10:00 AM'")
                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `endTime` TEXT NOT NULL DEFAULT '12:00 PM'")
                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `assignedToUidsCsv` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `assignedToNamesCsv` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'todo'")

                db.execSQL("ALTER TABLE `team_shared_docs` ADD COLUMN `blocksJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `team_shared_docs` ADD COLUMN `tagsCsv` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `team_shared_docs` ADD COLUMN `lastEditedBy` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `team_notifications` ADD COLUMN `taggedUidsCsv` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `team_notifications` ADD COLUMN `taggedNamesCsv` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `startup_projects` ADD COLUMN `projectType` TEXT NOT NULL DEFAULT 'STARTUP'")
                db.execSQL("ALTER TABLE `startup_projects` ADD COLUMN `crmDealsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `startup_projects` ADD COLUMN `okrsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `startup_projects` ADD COLUMN `sopDocsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `startup_projects` ADD COLUMN `sharedTeamId` TEXT")

                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `deadlineMillis` INTEGER")
                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `linkedDriveUrl` TEXT")
                db.execSQL("ALTER TABLE `team_shared_tasks` ADD COLUMN `sourceBusinessId` TEXT")

                db.execSQL("ALTER TABLE `team_shared_docs` ADD COLUMN `parentId` TEXT")
                db.execSQL("ALTER TABLE `team_shared_docs` ADD COLUMN `icon` TEXT NOT NULL DEFAULT '📄'")
                db.execSQL("ALTER TABLE `team_shared_docs` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `team_notifications` ADD COLUMN `noticeType` TEXT NOT NULL DEFAULT 'notice'")
                db.execSQL("ALTER TABLE `team_notifications` ADD COLUMN `pollOptionsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `team_notifications` ADD COLUMN `pollVotesJson` TEXT NOT NULL DEFAULT '{}'")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_activities` (
                        `id` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `actorUid` TEXT NOT NULL,
                        `actorName` TEXT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_activities_teamId` ON `team_activities` (`teamId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_activities_createdAtMillis` ON `team_activities` (`createdAtMillis`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_email_invites` (
                        `id` TEXT NOT NULL,
                        `teamId` TEXT NOT NULL,
                        `teamName` TEXT NOT NULL,
                        `inviteCode` TEXT NOT NULL,
                        `invitedEmail` TEXT NOT NULL,
                        `invitedRole` TEXT NOT NULL,
                        `roleTag` TEXT NOT NULL,
                        `invitedByUid` TEXT NOT NULL,
                        `invitedByName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_email_invites_teamId` ON `team_email_invites` (`teamId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_team_email_invites_invitedEmail` ON `team_email_invites` (`invitedEmail`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_ai_library.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

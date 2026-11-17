package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Immutable
@Entity(
    tableName = "workspace_notes",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["cloudSync"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isPinned"])
    ]
)
data class WorkspaceNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val title: String,
    val blocksJson: String = "[]",
    val tagsCsv: String = "",
    val isPinned: Boolean = false,
    val cloudSync: Boolean = true,
    val linkedItemId: String? = null,
    val linkedReminderId: String? = null,
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
    val syncState: String = "sync_pending",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "workspace_tasks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["dueDateEpochDay"]),
        Index(value = ["isCompleted"]),
        Index(value = ["cloudSync"])
    ]
)
data class WorkspaceTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val title: String,
    val notes: String = "",
    val dueDateEpochDay: Long = System.currentTimeMillis() / 86_400_000L,
    val priority: String = "MEDIUM",
    val isCompleted: Boolean = false,
    val cloudSync: Boolean = true,
    val audioUri: String? = null,
    val audioDurationMs: Long = 0L,
    val habitId: String? = null,
    val reminderId: String? = null,
    val syncState: String = "sync_pending",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "smart_reminders",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["triggerAtMillis"]),
        Index(value = ["isEnabled"]),
        Index(value = ["targetItemId"]),
        Index(value = ["targetNoteId"])
    ]
)
data class SmartReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val mode: String = "PERSONAL",
    val title: String,
    val notes: String = "",
    val priority: String = "MEDIUM",
    val targetItemId: String? = null,
    val targetNoteId: String? = null,
    val blockerRuleId: String? = null,
    val triggerAtMillis: Long,
    val recurrenceType: String = "ONCE",
    val intervalMinutes: Int = 0,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Default Alert",
    val isEnabled: Boolean = true,
    val cloudSync: Boolean = true,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["isArchived"]),
        Index(value = ["cloudSync"])
    ]
)
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val title: String,
    val subtitle: String = "",
    val category: String = "Focus",
    val targetPerWeek: Int = 7,
    val colorHex: String = "#E53935",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val recurrenceType: String = "DAILY",
    val intervalMinutes: Int = 1440,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Default System Sound",
    val cloudSync: Boolean = true,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "epochDay"],
    indices = [Index(value = ["epochDay"])],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitCompletionEntity(
    val habitId: String,
    val epochDay: Long,
    val completedAt: Long = System.currentTimeMillis(),
    val count: Int = 1
)

@Immutable
@Entity(tableName = "blocker_rules")
data class BlockerRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val mode: String = "SCHEDULED",
    val blockedPackagesJson: String = "[]",
    val startMinuteOfDay: Int = 540,
    val endMinuteOfDay: Int = 1020,
    val daysOfWeekCsv: String = "1,2,3,4,5,6,7",
    val delayTriggerAtMillis: Long? = null,
    val delayDurationMinutes: Int = 15,
    val activeUntilMillis: Long? = null,
    val preReminderMinutes: Int = 5,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["startedAt"])]
)
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String = "Monk Mode Session",
    val plannedMinutes: Int = 25,
    val actualMinutes: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val completed: Boolean = false,
    val emergencyUnlocked: Boolean = false,
    val blockedCount: Int = 0
)

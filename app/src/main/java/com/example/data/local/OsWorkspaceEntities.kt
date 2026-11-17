package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Immutable
@Entity(
    tableName = "startup_projects",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ]
)
data class StartupProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val name: String,
    val tagline: String = "",
    val stage: String = "IDEATION",
    val targetPersona: String = "",
    val techStack: String = "",
    val aiPlanMarkdown: String = "",
    val cloudSync: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val projectType: String = "STARTUP",
    val crmDealsJson: String = "[]",
    val okrsJson: String = "[]",
    val sopDocsJson: String = "[]",
    val sharedTeamId: String? = null
)

@Immutable
@Entity(
    tableName = "startup_phase_items",
    indices = [
        Index(value = ["startupId"]),
        Index(value = ["phaseNumber"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = StartupProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["startupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StartupPhaseItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startupId: String,
    val phaseNumber: Int,
    val phaseTitle: String,
    val title: String,
    val notes: String = "",
    val resourceUrl: String? = null,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "startup_sprint_tasks",
    indices = [
        Index(value = ["startupId"]),
        Index(value = ["columnStatus"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = StartupProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["startupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StartupSprintTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startupId: String,
    val title: String,
    val description: String = "",
    val columnStatus: String = "BACKLOG",
    val priority: String = "MEDIUM",
    val tag: String = "Core",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "finance_sectors",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["typeAffinity"])
    ]
)
data class FinanceSectorEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val name: String,
    val iconKey: String = "wallet",
    val colorHex: String = "#E53935",
    val typeAffinity: String = "BOTH",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "finance_transactions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["sectorId"]),
        Index(value = ["occurredAtMillis"]),
        Index(value = ["txType"])
    ]
)
data class FinanceTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val txType: String = "EXPENSE",
    val amountCents: Long,
    val sectorId: String,
    val sectorName: String,
    val paymentMethod: String = "BANK",
    val notes: String = "",
    val occurredAtMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "team_workspaces",
    indices = [Index(value = ["ownerId"])]
)
data class TeamWorkspaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val name: String,
    val description: String = "",
    val inviteCode: String,
    val maxMembers: Int = 5,
    val memberUidsCsv: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "team_members",
    primaryKeys = ["teamId", "uid"],
    indices = [Index(value = ["teamId"])]
)
data class TeamMemberEntity(
    val teamId: String,
    val uid: String,
    val displayName: String,
    val email: String = "",
    val photoUrl: String = "",
    val role: String = "MEMBER",
    val roleTag: String = "",
    val customStatus: String = "Available",
    val isOnline: Boolean = true,
    val lastActiveAtMillis: Long = System.currentTimeMillis(),
    val joinedAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "team_shared_tasks",
    indices = [Index(value = ["teamId"]), Index(value = ["isCompleted"])]
)
data class TeamSharedTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val teamId: String,
    val title: String,
    val description: String = "",
    val dayOfWeek: String = "Mon",
    val dateLabel: String = "",
    val startTime: String = "10:00 AM",
    val endTime: String = "12:00 PM",
    val assigneeUid: String = "",
    val assigneeName: String = "",
    val assignedToUidsCsv: String = "",
    val assignedToNamesCsv: String = "",
    val status: String = "todo",
    val priority: String = "MEDIUM",
    val isCompleted: Boolean = false,
    val dueAtMillis: Long? = null,
    val createdByUid: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deadlineMillis: Long? = null,
    val linkedDriveUrl: String? = null,
    val sourceBusinessId: String? = null
)

@Immutable
@Entity(
    tableName = "team_shared_docs",
    indices = [Index(value = ["teamId"])]
)
data class TeamSharedDocEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val teamId: String,
    val title: String,
    val content: String = "",
    val blocksJson: String = "[]",
    val tagsCsv: String = "",
    val resourceUrl: String? = null,
    val authorUid: String = "",
    val authorName: String = "",
    val lastEditedBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val parentId: String? = null,
    val icon: String = "📄",
    val isPinned: Boolean = false
)

@Immutable
@Entity(
    tableName = "team_notifications",
    indices = [Index(value = ["teamId"]), Index(value = ["createdAtMillis"])]
)
data class TeamNotificationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val teamId: String,
    val senderUid: String,
    val senderName: String,
    val title: String,
    val message: String,
    val taggedUidsCsv: String = "",
    val taggedNamesCsv: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val noticeType: String = "notice",
    val pollOptionsJson: String = "[]",
    val pollVotesJson: String = "{}"
)

@Immutable
@Entity(
    tableName = "team_activities",
    indices = [Index(value = ["teamId"]), Index(value = ["createdAtMillis"])]
)
data class TeamActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val teamId: String,
    val actorUid: String,
    val actorName: String,
    val actionType: String = "TEAM",
    val summary: String,
    val targetId: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "team_email_invites",
    indices = [Index(value = ["teamId"]), Index(value = ["invitedEmail"])]
)
data class TeamEmailInviteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val teamId: String,
    val teamName: String,
    val inviteCode: String,
    val invitedEmail: String,
    val invitedRole: String = "MEMBER",
    val roleTag: String = "Member",
    val invitedByUid: String = "",
    val invitedByName: String = "",
    val status: String = "PENDING",
    val createdAtMillis: Long = System.currentTimeMillis()
)

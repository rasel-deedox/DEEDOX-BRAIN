package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Immutable
@Entity(
    tableName = "items",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["canonicalUrl"]),
        Index(value = ["contentType"]),
        Index(value = ["updatedAt"]),
        Index(value = ["status"])
    ]
)
data class ItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val title: String,
    val description: String = "",
    val originalUrl: String? = null,
    val canonicalUrl: String? = null,
    val domain: String? = null,
    val contentType: String = "link",
    val sourceType: String = "manual",
    val thumbnailUrl: String? = null,
    val isFavorite: Boolean = false,
    val status: String = "unsorted",
    val isDeviceLocal: Boolean = false,
    val localOnly: Boolean = false,
    val localVersion: Int = 1,
    val serverVersion: Int = 0,
    val syncState: String = "local_only",
    val idempotencyKey: String = "",
    val metadataJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["parentFolderId"]),
        Index(value = ["sortOrder"])
    ]
)
data class FolderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val name: String,
    val parentFolderId: String? = null,
    val colorHex: String = "#E53935",
    val iconKey: String = "folder",
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isInbox: Boolean = false,
    val localVersion: Int = 1,
    val syncState: String = "local_only",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "item_folders",
    primaryKeys = ["itemId", "folderId"],
    indices = [Index(value = ["folderId"])],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ItemFolderCrossRef(
    val itemId: String,
    val folderId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "tags",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class TagEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val name: String,
    val normalizedName: String,
    val origin: String = "user",
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "item_tags",
    primaryKeys = ["itemId", "tagId"],
    indices = [Index(value = ["tagId"])],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ItemTagCrossRef(
    val itemId: String,
    val tagId: String
)

@Immutable
@Entity(
    tableName = "notes",
    indices = [Index(value = ["itemId"])],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val userId: String? = null,
    val body: String,
    val isPinned: Boolean = false,
    val isConflictCopy: Boolean = false,
    val localVersion: Int = 1,
    val syncState: String = "local_only",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val name: String,
    val description: String = "",
    val localVersion: Int = 1,
    val syncState: String = "local_only",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "itemId"],
    indices = [Index(value = ["itemId"])],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CollectionItemCrossRef(
    val collectionId: String,
    val itemId: String,
    val sortOrder: Int = 0
)

@Immutable
@Entity(
    tableName = "file_sources",
    indices = [Index(value = ["itemId"]), Index(value = ["contentUri"])],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FileSourceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val deviceId: String,
    val contentUri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val modifiedAt: Long? = null,
    val extension: String = "",
    val persistedPermission: Boolean = false,
    val availabilityStatus: String = "NOT_YET_CHECKED",
    val lastCheckedAt: Long = System.currentTimeMillis(),
    val optionalHash: String? = null,
    val indexingStatus: String = "indexed",
    val metadataJson: String = "{}"
)

@Immutable
@Entity(tableName = "connected_sources")
data class ConnectedSourceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val providerUri: String,
    val displayLabel: String,
    val grantedPermissionFlags: Int = 0,
    val lastScanTime: Long? = null,
    val scanStatus: String = "ready",
    val deviceId: String,
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "content_snapshots",
    indices = [Index(value = ["itemId"])],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ContentSnapshotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val extractedText: String,
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "ai_jobs")
data class AiJobEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val jobType: String,
    val status: String,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Immutable
@Entity(tableName = "organization_plans")
data class OrganizationPlanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val strategy: String = "Smart Inbox Organization",
    val confidenceBand: String = "MEDIUM",
    val reason: String = "",
    val warningsJson: String = "[]",
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86_400_000L,
    val appliedAt: Long? = null
)

@Immutable
@Entity(
    tableName = "organization_operations",
    indices = [Index(value = ["planId"])],
    foreignKeys = [
        ForeignKey(
            entity = OrganizationPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrganizationOperationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val itemId: String? = null,
    val itemTitle: String = "",
    val operationType: String,
    val fromFolderId: String? = null,
    val fromFolderName: String = "Inbox",
    val toFolderId: String? = null,
    val toFolderName: String = "",
    val tagName: String? = null,
    val confidenceBand: String = "HIGH",
    val reason: String = "",
    val isSelected: Boolean = true,
    val isApplied: Boolean = false,
    val previousFolderIdsJson: String = "[]"
)

@Entity(
    tableName = "sync_events",
    indices = [Index(value = ["syncState"]), Index(value = ["entityId"])]
)
data class SyncEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val operation: String,
    val idempotencyKey: String,
    val payloadJson: String = "{}",
    val syncState: String = "PENDING",
    val retryCount: Int = 0,
    val lastErrorCode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val actionType: String,
    val planId: String? = null,
    val summary: String,
    val beforeStateJson: String = "{}",
    val afterStateJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: String = "default",
    val onboardingCompleted: Boolean = false,
    val guestModeConfirmed: Boolean = false,
    val cloudSyncOptIn: Boolean = false,
    val aiEnabled: Boolean = true,
    val includeNotesInAi: Boolean = false,
    val includeExtractedTextInAi: Boolean = false,
    val autoMetadataEnrichment: Boolean = true,
    val diagnosticsEnabled: Boolean = true,
    val darkThemeOverride: Boolean? = null,
    val lastSyncCursor: Long = 0L,
    val lastSyncedAt: Long? = null
)

@Immutable
@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val operationId: String,
    val errorCode: String,
    val sanitizedMessage: String,
    val appVersion: String,
    val androidVersion: Int,
    val createdAt: Long = System.currentTimeMillis()
)

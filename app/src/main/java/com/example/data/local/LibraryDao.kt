package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    // --- Items ---
    @Query("SELECT * FROM items WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getAllActiveItems(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): ItemEntity?

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    fun observeItemById(itemId: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE canonicalUrl = :canonicalUrl AND deletedAt IS NULL LIMIT 1")
    suspend fun findByCanonicalUrl(canonicalUrl: String): ItemEntity?

    @Query("SELECT * FROM items WHERE idempotencyKey = :key AND deletedAt IS NULL LIMIT 1")
    suspend fun findByIdempotencyKey(key: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity)

    @Query("UPDATE items SET isFavorite = :isFavorite, updatedAt = :updatedAt, localVersion = localVersion + 1 WHERE id = :itemId")
    suspend fun updateFavorite(itemId: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE items SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = 'PENDING' WHERE id = :itemId")
    suspend fun softDeleteItem(itemId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE items SET status = :status, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun updateItemStatus(itemId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    // --- Folders ---
    @Query("SELECT * FROM folders WHERE deletedAt IS NULL ORDER BY isInbox DESC, sortOrder ASC, name ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE deletedAt IS NULL ORDER BY isInbox DESC, sortOrder ASC, name ASC")
    suspend fun getAllActiveFolders(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE LOWER(name) = LOWER(:name) AND deletedAt IS NULL LIMIT 1")
    suspend fun findFolderByName(name: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("UPDATE folders SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :folderId")
    suspend fun softDeleteFolder(folderId: String, deletedAt: Long = System.currentTimeMillis())

    // --- Item <-> Folder CrossRefs ---
    @Query("SELECT * FROM item_folders")
    fun observeAllItemFolders(): Flow<List<ItemFolderCrossRef>>

    @Query("SELECT folderId FROM item_folders WHERE itemId = :itemId")
    suspend fun getFolderIdsForItem(itemId: String): List<String>

    @Query("SELECT itemId FROM item_folders WHERE folderId = :folderId")
    suspend fun getItemIdsForFolder(folderId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemFolder(crossRef: ItemFolderCrossRef)

    @Query("DELETE FROM item_folders WHERE itemId = :itemId AND folderId = :folderId")
    suspend fun deleteItemFolder(itemId: String, folderId: String)

    @Query("DELETE FROM item_folders WHERE itemId = :itemId")
    suspend fun clearFoldersForItem(itemId: String)

    @Query("DELETE FROM item_folders WHERE folderId = :folderId")
    suspend fun clearItemsFromFolder(folderId: String)

    // --- Tags ---
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE normalizedName = :normalized LIMIT 1")
    suspend fun findTagByNormalizedName(normalized: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: String)

    @Query("SELECT * FROM item_tags")
    fun observeAllItemTags(): Flow<List<ItemTagCrossRef>>

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN item_tags it ON t.id = it.tagId
        WHERE it.itemId = :itemId
        ORDER BY t.name ASC
        """
    )
    suspend fun getTagsForItem(itemId: String): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTag(crossRef: ItemTagCrossRef)

    @Query("DELETE FROM item_tags WHERE itemId = :itemId AND tagId = :tagId")
    suspend fun deleteItemTag(itemId: String, tagId: String)

    @Query("UPDATE item_tags SET tagId = :targetTagId WHERE tagId = :sourceTagId")
    suspend fun repointItemTags(sourceTagId: String, targetTagId: String)

    // --- Notes ---
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE itemId = :itemId AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotesForItem(itemId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE itemId = :itemId AND deletedAt IS NULL")
    suspend fun getNotesForItem(itemId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL")
    suspend fun getAllActiveNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Query("UPDATE notes SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: String, deletedAt: Long = System.currentTimeMillis())

    // --- Collections ---
    @Query("SELECT * FROM collections WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE deletedAt IS NULL")
    suspend fun getAllActiveCollections(): List<CollectionEntity>

    @Query("SELECT * FROM collection_items ORDER BY sortOrder ASC")
    fun observeAllCollectionItems(): Flow<List<CollectionItemCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollection(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionItem(crossRef: CollectionItemCrossRef)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND itemId = :itemId")
    suspend fun deleteCollectionItem(collectionId: String, itemId: String)

    @Query("UPDATE collections SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :collectionId")
    suspend fun softDeleteCollection(collectionId: String, deletedAt: Long = System.currentTimeMillis())

    // --- File Sources & Connected Sources ---
    @Query("SELECT * FROM file_sources")
    fun observeAllFileSources(): Flow<List<FileSourceEntity>>

    @Query("SELECT * FROM file_sources WHERE itemId = :itemId LIMIT 1")
    suspend fun getFileSourceForItem(itemId: String): FileSourceEntity?

    @Query("SELECT * FROM file_sources")
    suspend fun getAllFileSources(): List<FileSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFileSource(source: FileSourceEntity)

    @Query("SELECT * FROM connected_sources ORDER BY createdAt DESC")
    fun observeConnectedSources(): Flow<List<ConnectedSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnectedSource(source: ConnectedSourceEntity)

    @Query("DELETE FROM connected_sources WHERE id = :id")
    suspend fun deleteConnectedSource(id: String)

    // --- Snapshots ---
    @Query("SELECT * FROM content_snapshots WHERE itemId = :itemId LIMIT 1")
    suspend fun getSnapshotForItem(itemId: String): ContentSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: ContentSnapshotEntity)

    // --- AI Jobs & Organization Plans ---
    @Query("SELECT * FROM ai_jobs ORDER BY createdAt DESC LIMIT 50")
    fun observeAiJobs(): Flow<List<AiJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiJob(job: AiJobEntity)

    @Query("SELECT * FROM organization_plans ORDER BY createdAt DESC")
    fun observeOrganizationPlans(): Flow<List<OrganizationPlanEntity>>

    @Query("SELECT * FROM organization_plans WHERE id = :planId LIMIT 1")
    suspend fun getPlanById(planId: String): OrganizationPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: OrganizationPlanEntity)

    @Query("SELECT * FROM organization_operations WHERE planId = :planId")
    fun observeOperationsForPlan(planId: String): Flow<List<OrganizationOperationEntity>>

    @Query("SELECT * FROM organization_operations WHERE planId = :planId")
    suspend fun getOperationsForPlan(planId: String): List<OrganizationOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOperations(ops: List<OrganizationOperationEntity>)

    @Query("UPDATE organization_operations SET isSelected = :selected WHERE id = :operationId")
    suspend fun updateOperationSelection(operationId: String, selected: Boolean)

    // --- Sync Queue & Audit & Settings & Diagnostics ---
    @Query("SELECT * FROM sync_events WHERE syncState != 'SYNCED' ORDER BY createdAt ASC")
    fun observePendingSyncEvents(): Flow<List<SyncEventEntity>>

    @Query("SELECT * FROM sync_events WHERE syncState != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getPendingSyncEvents(): List<SyncEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncEvent(event: SyncEventEntity)

    @Query("DELETE FROM sync_events WHERE syncState = 'SYNCED'")
    suspend fun pruneSyncedEvents()

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 100")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM user_settings WHERE id = 'default' LIMIT 1")
    fun observeUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 'default' LIMIT 1")
    suspend fun getUserSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserSettings(settings: UserSettingsEntity)

    @Query("SELECT * FROM diagnostic_logs ORDER BY createdAt DESC LIMIT 100")
    fun observeDiagnosticLogs(): Flow<List<DiagnosticLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosticLog(log: DiagnosticLogEntity)

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearDiagnosticLogs()

    // --- Transactional Operations ---
    @Transaction
    suspend fun moveItemToFolderReplacing(itemId: String, targetFolderId: String) {
        clearFoldersForItem(itemId)
        insertItemFolder(ItemFolderCrossRef(itemId = itemId, folderId = targetFolderId))
        updateItemStatus(itemId = itemId, status = "organized")
    }

    @Transaction
    suspend fun deleteFolderWithStrategy(
        folderId: String,
        moveTargetFolderId: String?
    ) {
        val itemIds = getItemIdsForFolder(folderId)
        clearItemsFromFolder(folderId)
        if (moveTargetFolderId != null) {
            itemIds.forEach { itemId ->
                insertItemFolder(ItemFolderCrossRef(itemId = itemId, folderId = moveTargetFolderId))
            }
        }
        softDeleteFolder(folderId)
    }
}

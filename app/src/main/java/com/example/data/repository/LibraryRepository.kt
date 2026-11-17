package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import com.example.data.ai.AiLibraryService
import com.example.data.ai.AiOrganizationDraft
import com.example.data.ai.AiSearchResponse
import com.example.data.ai.AiTagSummarySuggestion
import com.example.data.backup.BackupExporter
import com.example.data.backup.LibraryBackupPayload
import com.example.data.cloud.CloudAdminVaultConfig
import com.example.data.cloud.CloudDeviceSession
import com.example.data.cloud.CloudDeviceTransfer
import com.example.data.cloud.CloudGlobalAiConfig
import com.example.data.cloud.CloudReview
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.SupabaseAuthAndSyncClient
import com.example.data.cloud.SupabaseSignUpOutcome
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.AdminPasscodeCrypto
import com.example.data.local.AdminVaultConfig
import com.example.data.local.DetoxRuntimeStore
import com.example.data.local.DetoxSessionState
import com.example.data.local.LibraryReview
import com.example.data.local.OnboardingProfile
import com.example.data.local.SavedAccountRecord
import com.example.data.local.SupabaseSession
import com.example.data.local.UserPreferencesStore
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.example.data.local.AiJobEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.CollectionEntity
import com.example.data.local.CollectionItemCrossRef
import com.example.data.local.ConnectedSourceEntity
import com.example.data.local.ContentSnapshotEntity
import com.example.data.local.DiagnosticLogEntity
import com.example.data.local.FileSourceEntity
import com.example.data.local.FolderEntity
import com.example.data.local.FtsSearchEngine
import com.example.data.local.ItemEntity
import com.example.data.local.ItemFolderCrossRef
import com.example.data.local.ItemTagCrossRef
import com.example.data.local.LocalSearchHit
import com.example.data.local.NoteEntity
import com.example.data.local.OrganizationOperationEntity
import com.example.data.local.OrganizationPlanEntity
import com.example.data.local.SyncEventEntity
import com.example.data.local.TagEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.media.VideoThumbnailGenerator
import com.example.data.network.MetadataEnricher
import com.example.data.storage.FileHealthStatus
import com.example.data.storage.LocalFileReferenceManager
import com.example.data.util.UrlNormalizer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface CaptureOutcome {
    data class Saved(val item: ItemEntity) : CaptureOutcome
    data class DuplicateDetected(
        val existingItem: ItemEntity,
        val normalizedUrl: String,
        val suggestedTitle: String
    ) : CaptureOutcome
    data class Error(val message: String) : CaptureOutcome
}

class LibraryRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val ftsSearchEngine: FtsSearchEngine,
    private val metadataEnricher: MetadataEnricher,
    private val fileManager: LocalFileReferenceManager,
    private val cloudSync: CloudSyncRepository,
    private val aiService: AiLibraryService,
    private val preferencesStore: UserPreferencesStore = UserPreferencesStore(context.applicationContext),
    private val supabaseClient: SupabaseAuthAndSyncClient = SupabaseAuthAndSyncClient()
) {
    private val dao = database.libraryDao()

    val themeMode: Flow<AppThemeMode> = preferencesStore.themeMode
    val accentPreset: Flow<LiquidAccentPreset> = preferencesStore.accentPreset
    val customDisplayName: Flow<String?> = preferencesStore.customDisplayName
    val customEmail: Flow<String?> = preferencesStore.customEmail
    val profileAvatar: Flow<String> = preferencesStore.profileAvatar
    val vaultPin: Flow<String?> = preferencesStore.vaultPin
    val isVaultLocked: Flow<Boolean> = preferencesStore.isVaultLocked
    val savedAccounts: Flow<List<SavedAccountRecord>> = preferencesStore.savedAccounts
    val localReviews: Flow<List<LibraryReview>> = preferencesStore.localReviews
    val adminVaultConfig: Flow<AdminVaultConfig> = preferencesStore.adminVaultConfig
    val supabaseSession: Flow<SupabaseSession?> = preferencesStore.supabaseSession
    val onboardingProfile: Flow<OnboardingProfile> = preferencesStore.onboardingProfile
    val detoxSessionState: Flow<DetoxSessionState> = preferencesStore.detoxSessionState
    val recentFinanceIconKeys: Flow<List<String>> = preferencesStore.recentFinanceIconKeys
    val financeTxIconOverrides: Flow<Map<String, String>> = preferencesStore.financeTxIconOverrides

    val allItems: Flow<List<ItemEntity>> = dao.observeAllItems()
    val allFolders: Flow<List<FolderEntity>> = dao.observeAllFolders()
    val allItemFolders: Flow<List<ItemFolderCrossRef>> = dao.observeAllItemFolders()
    val allTags: Flow<List<TagEntity>> = dao.observeAllTags()
    val allItemTags: Flow<List<ItemTagCrossRef>> = dao.observeAllItemTags()
    val allNotes: Flow<List<NoteEntity>> = dao.observeAllNotes()
    val allCollections: Flow<List<CollectionEntity>> = dao.observeAllCollections()
    val allCollectionItems: Flow<List<CollectionItemCrossRef>> = dao.observeAllCollectionItems()
    val allFileSources: Flow<List<FileSourceEntity>> = dao.observeAllFileSources()
    val connectedSources: Flow<List<ConnectedSourceEntity>> = dao.observeConnectedSources()
    val organizationPlans: Flow<List<OrganizationPlanEntity>> = dao.observeOrganizationPlans()
    val pendingSyncEvents: Flow<List<SyncEventEntity>> = dao.observePendingSyncEvents()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.observeAuditLogs()
    val userSettings: Flow<UserSettingsEntity?> = dao.observeUserSettings()
    val diagnosticLogs: Flow<List<DiagnosticLogEntity>> = dao.observeDiagnosticLogs()
    val aiJobs: Flow<List<AiJobEntity>> = dao.observeAiJobs()

    fun observeOperationsForPlan(planId: String): Flow<List<OrganizationOperationEntity>> =
        dao.observeOperationsForPlan(planId)

    fun observeNotesForItem(itemId: String): Flow<List<NoteEntity>> =
        dao.observeNotesForItem(itemId)

    fun observeItem(itemId: String): Flow<ItemEntity?> = dao.observeItemById(itemId)

    fun isOnline(): Boolean = metadataEnricher.isOnline()

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        val existingInbox = dao.getFolderById(AppDatabase.INBOX_FOLDER_ID)
        if (existingInbox == null) {
            dao.upsertFolder(
                FolderEntity(
                    id = AppDatabase.INBOX_FOLDER_ID,
                    name = "Inbox",
                    colorHex = "#E53935",
                    iconKey = "inbox",
                    sortOrder = -1,
                    isInbox = true
                )
            )
        }
        val settings = dao.getUserSettings()
        if (settings == null) {
            dao.upsertUserSettings(UserSettingsEntity())
        }
        val adminCfg = preferencesStore.getCurrentAdminVaultConfig()
        syncRuntimeAiFromVault(adminCfg)
        // Backfill deterministic smart thumbnails for URLs and local video previews
        dao.getAllActiveItems().forEach { item ->
            val targetUrl = item.originalUrl ?: item.canonicalUrl
            if (!targetUrl.isNullOrBlank() && item.thumbnailUrl.isNullOrBlank()) {
                val smart = UrlNormalizer.resolveSmartThumbnailUrl(targetUrl, item.domain)
                if (!smart.isNullOrBlank()) {
                    dao.upsertItem(item.copy(thumbnailUrl = smart))
                }
            } else if (item.isDeviceLocal && item.contentType == "video" &&
                (item.thumbnailUrl.isNullOrBlank() || VideoThumbnailGenerator.extractDurationLabel(item.metadataJson) == null)
            ) {
                val srcUri = dao.getFileSourceForItem(item.id)?.contentUri
                if (!srcUri.isNullOrBlank()) {
                    val preview = VideoThumbnailGenerator.extractAndCachePreview(
                        context = context,
                        videoUri = Uri.parse(srcUri),
                        cacheKey = item.id
                    )
                    if (preview != null) {
                        val nextThumb = item.thumbnailUrl?.takeIf { it.isNotBlank() } ?: preview.thumbnailFileUri
                        val nextMeta = VideoThumbnailGenerator.mergeDurationMetadata(item.metadataJson, preview.durationMs)
                        if (nextThumb != item.thumbnailUrl || nextMeta != item.metadataJson) {
                            dao.upsertItem(item.copy(thumbnailUrl = nextThumb, metadataJson = nextMeta))
                        }
                    }
                }
            }
        }
        ftsSearchEngine.rebuildIndex()
    }

    suspend fun completeOnboarding(guestMode: Boolean, cloudSyncOptIn: Boolean) {
        val current = dao.getUserSettings() ?: UserSettingsEntity()
        dao.upsertUserSettings(
            current.copy(
                onboardingCompleted = true,
                guestModeConfirmed = guestMode,
                cloudSyncOptIn = cloudSyncOptIn
            )
        )
    }

    suspend fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        val current = dao.getUserSettings() ?: UserSettingsEntity()
        dao.upsertUserSettings(transform(current))
    }

    suspend fun saveOnboardingProfile(
        role: String,
        useCase: String,
        workflow: String,
        saveHabit: String = "",
        wizardCompleted: Boolean = true
    ) = withContext(Dispatchers.IO) {
        preferencesStore.saveOnboardingProfile(
            OnboardingProfile(
                role = role.trim(),
                primaryUseCase = useCase.trim(),
                saveHabit = saveHabit.trim(),
                workflowPreset = workflow.trim().ifBlank { "starter_folders" },
                wizardCompleted = wizardCompleted
            )
        )
        if (currentAuthUid() != null && isOnline()) {
            val user = Firebase.auth.currentUser
            cloudSync.upsertUserProfile(
                displayName = user?.displayName ?: "Library User",
                cloudSyncEnabled = true
            )
        }
    }

    suspend fun createStarterFolders(names: List<String>) {
        val palette = listOf("#E53935", "#B84B2A", "#603A36", "#EDEAE4", "#D93829", "#1C1B1A")
        val existing = dao.getAllActiveFolders()
        names.forEachIndexed { idx, name ->
            if (existing.none { it.name.equals(name, ignoreCase = true) }) {
                createFolder(
                    name = name,
                    parentFolderId = null,
                    colorHex = palette[idx % palette.size],
                    iconKey = "folder"
                )
            }
        }
    }

    suspend fun seedDemoLibraryData() = withContext(Dispatchers.IO) {
        ensureInitialized()
        if (dao.getAllActiveItems().isNotEmpty()) return@withContext

        createStarterFolders(
            listOf(
                "AI & Technology",
                "GitHub Projects",
                "Design Inspiration",
                "Learning Resources",
                "Personal Notes"
            )
        )
        val folders = dao.getAllActiveFolders()
        fun folderIdFor(name: String): String =
            folders.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id ?: AppDatabase.INBOX_FOLDER_ID

        captureWebUrlOrText(
            rawInput = "https://github.com/kotlin/kotlinx.coroutines",
            customTitle = "Kotlinx Coroutines — Async & Flows",
            folderId = folderIdFor("GitHub Projects"),
            tagNames = listOf("kotlin", "coroutines", "android"),
            initialNote = "Reference library for structured concurrency, StateFlow & Dispatchers.",
            allowDuplicateSave = true
        )
        captureWebUrlOrText(
            rawInput = "https://github.com/android/nowinandroid",
            customTitle = "Now in Android — Official Architecture App",
            folderId = folderIdFor("GitHub Projects"),
            tagNames = listOf("android", "compose", "mvvm"),
            initialNote = "Clean Architecture + Material 3 adaptive layout blueprint.",
            allowDuplicateSave = true
        )
        captureWebUrlOrText(
            rawInput = "https://ai.google.dev/gemini-api/docs",
            customTitle = "Gemini API & Multimodal AI Docs",
            folderId = folderIdFor("AI & Technology"),
            tagNames = listOf("gemini", "ai", "prompts"),
            initialNote = "Structured JSON output, vision prompts, and AI workflow docs.",
            allowDuplicateSave = true
        )
        captureWebUrlOrText(
            rawInput = "https://developer.android.com/develop/ui/compose",
            customTitle = "Jetpack Compose & Liquid Material 3",
            folderId = folderIdFor("Design Inspiration"),
            tagNames = listOf("compose", "design", "ui"),
            initialNote = "Edge-to-edge WindowInsets, graphicsLayer, and spring animations.",
            allowDuplicateSave = true
        )
        captureWebUrlOrText(
            rawInput = "Offline-first Room + FTS5 full-text search delivers instant local recall across titles, URLs, notes & files with optional cloud sync.",
            customTitle = "Offline-First AI Vault Blueprint",
            folderId = folderIdFor("Personal Notes"),
            tagNames = listOf("notes", "vault", "search"),
            initialNote = "Tap any resource card to inspect metadata, tags, folders & reviews.",
            allowDuplicateSave = true
        )
        ftsSearchEngine.rebuildIndex()
    }

    suspend fun previewUrlMetadata(url: String): Result<com.example.data.network.EnrichedMetadata> {
        return metadataEnricher.fetchMetadata(url)
    }

    suspend fun captureWebUrlOrText(
        rawInput: String,
        customTitle: String? = null,
        customDescription: String? = null,
        folderId: String = AppDatabase.INBOX_FOLDER_ID,
        tagNames: List<String> = emptyList(),
        initialNote: String? = null,
        preloadedThumbnailUrl: String? = null,
        allowDuplicateSave: Boolean = false,
        collectionId: String? = null
    ): CaptureOutcome = withContext(Dispatchers.IO) {
        val trimmed = UrlNormalizer.normalizeUnicode(rawInput)
        if (trimmed.isEmpty()) {
            return@withContext CaptureOutcome.Error("Please enter a valid URL or note text.")
        }

        val extractedUrl = UrlNormalizer.extractFirstWebUrl(trimmed)
        if (extractedUrl == null) {
            // Check for blocked schemes like javascript: or file:
            val lower = trimmed.lowercase()
            if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("vbscript:")) {
                return@withContext CaptureOutcome.Error("Unsupported or unsafe URL scheme rejected.")
            }
            if (UrlNormalizer.isContentUri(trimmed)) {
                return@withContext CaptureOutcome.Error("Use 'Add Local File' to attach content:// references.")
            }
            // Save as plain text note item
            val title = customTitle?.takeIf { it.isNotBlank() }
                ?: trimmed.lineSequence().firstOrNull()?.take(80)?.ifBlank { "Quick Note" }
                ?: "Quick Note"
            val idKey = UrlNormalizer.idempotencyKey("note:$title:$trimmed")
            val item = ItemEntity(
                id = UUID.randomUUID().toString(),
                userId = currentAuthUid(),
                title = title,
                description = customDescription?.takeIf { it.isNotBlank() }?.take(4000) ?: trimmed.take(4000),
                contentType = "note",
                sourceType = "note",
                status = if (folderId == AppDatabase.INBOX_FOLDER_ID) "unsorted" else "organized",
                idempotencyKey = idKey,
                syncState = syncStateForNewItem()
            )
            persistNewItem(item, folderId, tagNames, initialNote ?: trimmed, collectionId)
            return@withContext CaptureOutcome.Saved(item)
        }

        val parsed = UrlNormalizer.normalize(extractedUrl)
            ?: return@withContext CaptureOutcome.Error("Malformed URL. Please check the web address.")

        if (!allowDuplicateSave) {
            val existing = dao.findByCanonicalUrl(parsed.canonicalUrl)
            if (existing != null) {
                return@withContext CaptureOutcome.DuplicateDetected(
                    existingItem = existing,
                    normalizedUrl = parsed.canonicalUrl,
                    suggestedTitle = customTitle?.takeIf { it.isNotBlank() } ?: parsed.suggestedTitle
                )
            }
        }

        val title = customTitle?.takeIf { it.isNotBlank() } ?: parsed.suggestedTitle
        val initialThumb = preloadedThumbnailUrl
            ?.takeIf { it.isNotBlank() && UrlNormalizer.isSafeRemoteUrl(it) && !UrlNormalizer.isFaviconFallbackUrl(it) }
            ?: UrlNormalizer.resolveSmartThumbnailUrl(parsed.originalUrl, parsed.domain)
        val initialDesc = customDescription?.takeIf { it.isNotBlank() }?.take(2000)
            ?: if (trimmed != extractedUrl) trimmed.take(1000) else ""
        val idKey = UrlNormalizer.idempotencyKey("url:${parsed.canonicalUrl}:${System.currentTimeMillis() / 5000}")
        val item = ItemEntity(
            id = UUID.randomUUID().toString(),
            userId = currentAuthUid(),
            title = title,
            description = initialDesc,
            originalUrl = parsed.originalUrl,
            canonicalUrl = parsed.canonicalUrl,
            domain = parsed.domain,
            contentType = parsed.inferredContentType,
            sourceType = parsed.inferredSourceType,
            thumbnailUrl = initialThumb,
            status = if (folderId == AppDatabase.INBOX_FOLDER_ID) "unsorted" else "organized",
            idempotencyKey = idKey,
            syncState = syncStateForNewItem()
        )
        persistNewItem(item, folderId, tagNames, initialNote, collectionId)
        CaptureOutcome.Saved(item)
    }

    suspend fun enrichItemMetadata(itemId: String, overwriteTitle: Boolean = false): Result<ItemEntity> = withContext(Dispatchers.IO) {
        val item = dao.getItemById(itemId)
            ?: return@withContext Result.failure(IllegalStateException("Item not found"))
        val url = item.originalUrl ?: item.canonicalUrl
            ?: return@withContext Result.failure(IllegalStateException("No URL to enrich"))

        val smartThumb = UrlNormalizer.resolveSmartThumbnailUrl(url, item.domain)
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (!settings.autoMetadataEnrichment && !overwriteTitle) {
            return@withContext Result.success(item)
        }

        val result = metadataEnricher.fetchMetadata(url)
        result.fold(
            onSuccess = { enriched ->
                val enrichedThumb = enriched.thumbnailUrl?.takeIf { it.isNotBlank() }
                val bestThumbnail = when {
                    enrichedThumb != null && !UrlNormalizer.isFaviconFallbackUrl(enrichedThumb) -> enrichedThumb
                    !item.thumbnailUrl.isNullOrBlank() && !UrlNormalizer.isFaviconFallbackUrl(item.thumbnailUrl) -> item.thumbnailUrl
                    enrichedThumb != null -> enrichedThumb
                    else -> item.thumbnailUrl?.takeIf { it.isNotBlank() } ?: smartThumb
                }

                val scrapedTitle = enriched.title?.takeIf {
                    it.isNotBlank() && !UrlNormalizer.isGenericPlatformTitle(it)
                }
                val nextTitle = if (scrapedTitle != null &&
                    (overwriteTitle || UrlNormalizer.looksLikePlaceholderTitle(item.title, url, item.domain))
                ) {
                    scrapedTitle
                } else {
                    item.title
                }

                val scrapedDesc = enriched.description?.takeIf { it.isNotBlank() }
                val nextDescription = if (scrapedDesc != null && (overwriteTitle || item.description.isBlank())) {
                    scrapedDesc
                } else {
                    item.description
                }

                val updated = item.copy(
                    title = nextTitle,
                    description = nextDescription,
                    thumbnailUrl = bestThumbnail,
                    localVersion = item.localVersion + 1,
                    updatedAt = System.currentTimeMillis()
                )
                dao.upsertItem(updated)
                ftsSearchEngine.indexItem(updated.id)
                enqueueSyncEvent("item", updated.id, "UPSERT")
                pushItemToConfiguredClouds(updated)
                Result.success(updated)
            },
            onFailure = { error ->
                if (item.thumbnailUrl.isNullOrBlank() && !smartThumb.isNullOrBlank()) {
                    val fallbackUpdated = item.copy(thumbnailUrl = smartThumb)
                    dao.upsertItem(fallbackUpdated)
                }
                recordDiagnostic("ENRICH_META", "META_ERR", error.message ?: "Metadata fetch failed")
                Result.failure(error)
            }
        )
    }

    suspend fun refreshMissingThumbnails(limit: Int = 8) = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext
        val candidates = dao.getAllActiveItems().filter { item ->
            val url = item.originalUrl ?: item.canonicalUrl
            !url.isNullOrBlank() && (
                item.thumbnailUrl.isNullOrBlank() ||
                    UrlNormalizer.isFaviconFallbackUrl(item.thumbnailUrl) ||
                    item.description.isBlank() ||
                    UrlNormalizer.looksLikePlaceholderTitle(item.title, url, item.domain)
                )
        }.take(limit)
        for (item in candidates) {
            enrichItemMetadata(item.id)
        }
    }

    suspend fun captureLocalFileUri(
        uri: Uri,
        folderId: String = AppDatabase.INBOX_FOLDER_ID,
        tagNames: List<String> = emptyList(),
        note: String? = null,
        collectionId: String? = null
    ): CaptureOutcome = withContext(Dispatchers.IO) {
        fileManager.tryTakePersistablePermission(uri, isTree = false)
        val inspected = fileManager.inspectFileUri(uri)
        val contentType = when {
            inspected.mimeType.startsWith("image/") -> "image"
            inspected.mimeType.startsWith("video/") -> "video"
            inspected.mimeType.contains("pdf") || inspected.mimeType.startsWith("text/") -> "document"
            else -> "local_file"
        }
        val sourceType = if (inspected.persistedPermission) "saf_file" else "photo_picker"
        val itemId = UUID.randomUUID().toString()
        val videoPreview = if (contentType == "video") {
            VideoThumbnailGenerator.extractAndCachePreview(
                context = context,
                videoUri = uri,
                cacheKey = itemId
            )
        } else {
            null
        }
        val durationSuffix = videoPreview?.durationLabel?.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
        val item = ItemEntity(
            id = itemId,
            userId = currentAuthUid(),
            title = inspected.displayName,
            description = "${inspected.mimeType} • ${formatBytes(inspected.sizeBytes)}$durationSuffix",
            contentType = contentType,
            sourceType = sourceType,
            thumbnailUrl = videoPreview?.thumbnailFileUri,
            metadataJson = if (videoPreview != null) {
                VideoThumbnailGenerator.mergeDurationMetadata("{}", videoPreview.durationMs)
            } else {
                "{}"
            },
            status = if (folderId == AppDatabase.INBOX_FOLDER_ID) "unsorted" else "organized",
            isDeviceLocal = true,
            localOnly = false,
            idempotencyKey = UrlNormalizer.idempotencyKey("file:${inspected.uriString}"),
            syncState = syncStateForNewItem()
        )
        persistNewItem(item, folderId, tagNames, note, collectionId)

        dao.upsertFileSource(
            FileSourceEntity(
                itemId = item.id,
                deviceId = fileManager.getDeviceId(),
                contentUri = inspected.uriString,
                displayName = inspected.displayName,
                mimeType = inspected.mimeType,
                sizeBytes = inspected.sizeBytes,
                modifiedAt = inspected.modifiedAt,
                extension = inspected.extension,
                persistedPermission = inspected.persistedPermission,
                availabilityStatus = inspected.healthStatus.code,
                lastCheckedAt = System.currentTimeMillis()
            )
        )

        if (!inspected.extractedPreviewText.isNullOrBlank()) {
            dao.upsertSnapshot(
                ContentSnapshotEntity(
                    itemId = item.id,
                    extractedText = inspected.extractedPreviewText,
                    wordCount = inspected.extractedPreviewText.split(Regex("""\s+""")).size
                )
            )
        }
        ftsSearchEngine.indexItem(item.id)
        CaptureOutcome.Saved(item)
    }

    suspend fun connectFolderTree(treeUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        val persisted = fileManager.tryTakePersistablePermission(treeUri, isTree = true)
        val label = fileManager.resolveTreeDisplayName(treeUri)
        val existingFolder = dao.findFolderByName(label)
        val targetFolder = existingFolder ?: FolderEntity(
            userId = currentAuthUid(),
            name = UrlNormalizer.normalizeUnicode(label).take(120),
            colorHex = "#B84B2A",
            iconKey = "local_tree",
            syncState = syncStateForNewItem()
        ).also { created ->
            dao.upsertFolder(created)
            enqueueSyncEvent("folder", created.id, "UPSERT")
        }

        val scanned = fileManager.scanDocumentTree(treeUri, maxItems = 35)
        var added = 0
        for (file in scanned) {
            val outcome = captureLocalFileUri(
                uri = Uri.parse(file.uriString),
                folderId = targetFolder.id
            )
            if (outcome is CaptureOutcome.Saved) added++
        }
        dao.upsertConnectedSource(
            ConnectedSourceEntity(
                providerUri = treeUri.toString(),
                displayLabel = targetFolder.name,
                grantedPermissionFlags = if (persisted) 1 else 0,
                lastScanTime = System.currentTimeMillis(),
                scanStatus = if (persisted) "synced" else "temporary_grant",
                deviceId = fileManager.getDeviceId(),
                itemCount = scanned.size
            )
        )
        Result.success(added)
    }

    suspend fun refreshAllFileHealth(): Int = withContext(Dispatchers.IO) {
        val sources = dao.getAllFileSources()
        var issues = 0
        for (source in sources) {
            val status = fileManager.checkHealth(source.contentUri)
            if (status != FileHealthStatus.AVAILABLE) issues++
            dao.upsertFileSource(
                source.copy(
                    availabilityStatus = status.code,
                    lastCheckedAt = System.currentTimeMillis()
                )
            )
        }
        issues
    }

    suspend fun markPermissionRevokedForTest(fileSourceId: String) = withContext(Dispatchers.IO) {
        val sources = dao.getAllFileSources()
        sources.find { it.id == fileSourceId }?.let { src ->
            dao.upsertFileSource(
                src.copy(
                    persistedPermission = false,
                    availabilityStatus = FileHealthStatus.PERMISSION_NEEDED.code,
                    lastCheckedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun persistNewItem(
        item: ItemEntity,
        folderId: String,
        tagNames: List<String>,
        noteBody: String?,
        collectionId: String? = null
    ) {
        dao.upsertItem(item)
        dao.insertItemFolder(ItemFolderCrossRef(itemId = item.id, folderId = folderId))
        if (!collectionId.isNullOrBlank()) {
            dao.insertCollectionItem(CollectionItemCrossRef(collectionId = collectionId, itemId = item.id))
        }
        tagNames.forEach { raw -> addTagToItem(item.id, raw, origin = "user") }
        if (!noteBody.isNullOrBlank()) {
            dao.upsertNote(
                NoteEntity(
                    itemId = item.id,
                    userId = item.userId,
                    body = UrlNormalizer.normalizeUnicode(noteBody),
                    syncState = item.syncState
                )
            )
        }
        ftsSearchEngine.indexItem(item.id)
        enqueueSyncEvent("item", item.id, "UPSERT")
        pushItemToConfiguredClouds(item)
    }

    suspend fun updateItemDetails(
        itemId: String,
        title: String,
        rawUrl: String?,
        description: String
    ): Result<ItemEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getItemById(itemId)
            ?: return@withContext Result.failure(IllegalStateException("Item not found."))
        val cleanTitle = UrlNormalizer.normalizeUnicode(title).trim().take(500)
        if (cleanTitle.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Title cannot be empty."))
        }
        val cleanDesc = UrlNormalizer.normalizeUnicode(description).trim().take(4000)
        val trimmedUrl = rawUrl?.trim()?.takeIf { it.isNotBlank() }

        val updated = if (existing.isDeviceLocal) {
            existing.copy(
                title = cleanTitle,
                description = cleanDesc,
                localVersion = existing.localVersion + 1,
                updatedAt = System.currentTimeMillis(),
                syncState = syncStateForNewItem()
            )
        } else if (trimmedUrl != null) {
            val parsed = UrlNormalizer.normalize(trimmedUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Please enter a valid http/https URL."))
            val urlChanged = parsed.canonicalUrl != existing.canonicalUrl
            val nextThumb = if (urlChanged || existing.thumbnailUrl.isNullOrBlank()) {
                UrlNormalizer.resolveSmartThumbnailUrl(parsed.originalUrl, parsed.domain)
            } else {
                existing.thumbnailUrl
            }
            existing.copy(
                title = cleanTitle,
                description = cleanDesc,
                originalUrl = parsed.originalUrl,
                canonicalUrl = parsed.canonicalUrl,
                domain = parsed.domain,
                contentType = parsed.inferredContentType,
                sourceType = parsed.inferredSourceType,
                thumbnailUrl = nextThumb,
                localVersion = existing.localVersion + 1,
                updatedAt = System.currentTimeMillis(),
                syncState = syncStateForNewItem()
            )
        } else {
            existing.copy(
                title = cleanTitle,
                description = cleanDesc,
                localVersion = existing.localVersion + 1,
                updatedAt = System.currentTimeMillis(),
                syncState = syncStateForNewItem()
            )
        }

        dao.upsertItem(updated)
        ftsSearchEngine.indexItem(updated.id)
        enqueueSyncEvent("item", updated.id, "UPSERT")
        pushItemToConfiguredClouds(updated)
        Result.success(updated)
    }

    suspend fun toggleFavorite(itemId: String, currentlyFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(itemId, !currentlyFavorite)
        enqueueSyncEvent("item", itemId, "UPSERT")
        dao.getItemById(itemId)?.let { pushItemToConfiguredClouds(it) }
    }

    suspend fun softDeleteItem(itemId: String) = withContext(Dispatchers.IO) {
        val deletedAt = System.currentTimeMillis()
        dao.softDeleteItem(itemId, deletedAt)
        ftsSearchEngine.indexItem(itemId)
        enqueueSyncEvent("item", itemId, "DELETE")
        val deletedEntity = dao.getItemById(itemId)
        if (deletedEntity != null && !deletedEntity.localOnly) {
            val settings = dao.getUserSettings() ?: UserSettingsEntity()
            if (settings.cloudSyncOptIn && currentAuthUid() != null && isOnline()) {
                val folderIds = dao.getFolderIdsForItem(itemId)
                val tagNames = dao.getTagsForItem(itemId).map { it.name }
                cloudSync.pushItem(deletedEntity, folderIds, tagNames)
            }
            val adminCfg = preferencesStore.getCurrentAdminVaultConfig()
            val supaSession = preferencesStore.getCurrentSupabaseSession()
            if (supaSession != null && adminCfg.supabaseUrl.isNotBlank() && adminCfg.supabaseAnonKey.isNotBlank() && isOnline()) {
                supabaseClient.softDeleteItem(
                    supabaseUrl = adminCfg.supabaseUrl,
                    anonKey = adminCfg.supabaseAnonKey,
                    session = supaSession,
                    itemId = itemId,
                    deletedAtMillis = deletedAt
                )
            }
        }
    }

    suspend fun addItemToFolder(itemId: String, folderId: String) = withContext(Dispatchers.IO) {
        dao.insertItemFolder(ItemFolderCrossRef(itemId = itemId, folderId = folderId))
        if (folderId != AppDatabase.INBOX_FOLDER_ID) {
            dao.updateItemStatus(itemId, "organized")
        }
        enqueueSyncEvent("item", itemId, "UPSERT")
    }

    suspend fun removeItemFromFolder(itemId: String, folderId: String) = withContext(Dispatchers.IO) {
        dao.deleteItemFolder(itemId, folderId)
        enqueueSyncEvent("item", itemId, "UPSERT")
    }

    suspend fun createFolder(
        name: String,
        parentFolderId: String? = null,
        colorHex: String = "#E53935",
        iconKey: String = "folder"
    ): FolderEntity = withContext(Dispatchers.IO) {
        val folder = FolderEntity(
            userId = currentAuthUid(),
            name = UrlNormalizer.normalizeUnicode(name).take(120),
            parentFolderId = parentFolderId,
            colorHex = colorHex,
            iconKey = iconKey,
            syncState = syncStateForNewItem()
        )
        dao.upsertFolder(folder)
        enqueueSyncEvent("folder", folder.id, "UPSERT")
        folder
    }

    suspend fun renameFolder(folderId: String, newName: String) = withContext(Dispatchers.IO) {
        val folder = dao.getFolderById(folderId) ?: return@withContext
        if (folder.isInbox) return@withContext
        dao.upsertFolder(
            folder.copy(
                name = UrlNormalizer.normalizeUnicode(newName).take(120),
                localVersion = folder.localVersion + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
        enqueueSyncEvent("folder", folder.id, "UPSERT")
    }

    suspend fun deleteFolder(folderId: String, moveTargetFolderId: String?) = withContext(Dispatchers.IO) {
        if (folderId == AppDatabase.INBOX_FOLDER_ID) return@withContext
        dao.deleteFolderWithStrategy(folderId, moveTargetFolderId)
        enqueueSyncEvent("folder", folderId, "DELETE")
    }

    suspend fun addTagToItem(itemId: String, rawTag: String, origin: String = "user") = withContext(Dispatchers.IO) {
        val clean = UrlNormalizer.normalizeUnicode(rawTag).trim()
        if (clean.isBlank()) return@withContext
        val normalized = UrlNormalizer.normalizeTagName(clean)
        val existingTag = dao.findTagByNormalizedName(normalized)
        val tag = existingTag ?: TagEntity(
            userId = currentAuthUid(),
            name = clean,
            normalizedName = normalized,
            origin = origin
        ).also { dao.upsertTag(it) }
        dao.insertItemTag(ItemTagCrossRef(itemId = itemId, tagId = tag.id))
        ftsSearchEngine.indexItem(itemId)
        enqueueSyncEvent("item", itemId, "UPSERT")
    }

    suspend fun removeTagFromItem(itemId: String, tagId: String) = withContext(Dispatchers.IO) {
        dao.deleteItemTag(itemId, tagId)
        ftsSearchEngine.indexItem(itemId)
        enqueueSyncEvent("item", itemId, "UPSERT")
    }

    suspend fun renameTag(tagId: String, newName: String) = withContext(Dispatchers.IO) {
        val tags = dao.getAllTags()
        val tag = tags.find { it.id == tagId } ?: return@withContext
        val clean = UrlNormalizer.normalizeUnicode(newName).trim()
        if (clean.isBlank()) return@withContext
        val normalized = UrlNormalizer.normalizeTagName(clean)
        val conflict = dao.findTagByNormalizedName(normalized)
        if (conflict != null && conflict.id != tagId) {
            dao.repointItemTags(sourceTagId = tagId, targetTagId = conflict.id)
            dao.deleteTagById(tagId)
        } else {
            dao.upsertTag(tag.copy(name = clean, normalizedName = normalized))
        }
        ftsSearchEngine.rebuildIndex()
    }

    suspend fun deleteTag(tagId: String) = withContext(Dispatchers.IO) {
        dao.deleteTagById(tagId)
        ftsSearchEngine.rebuildIndex()
    }

    suspend fun saveNote(itemId: String, body: String, existingNoteId: String? = null) = withContext(Dispatchers.IO) {
        val cleanBody = UrlNormalizer.normalizeUnicode(body)
        if (cleanBody.isBlank()) return@withContext
        val existing = if (existingNoteId != null) {
            dao.getNotesForItem(itemId).find { it.id == existingNoteId }
        } else null

        val note = existing?.copy(
            body = cleanBody,
            localVersion = existing.localVersion + 1,
            updatedAt = System.currentTimeMillis(),
            syncState = syncStateForNewItem()
        ) ?: NoteEntity(
            itemId = itemId,
            userId = currentAuthUid(),
            body = cleanBody,
            syncState = syncStateForNewItem()
        )
        dao.upsertNote(note)
        ftsSearchEngine.indexItem(itemId)
        enqueueSyncEvent("note", note.id, "UPSERT")
    }

    suspend fun deleteNote(noteId: String, itemId: String) = withContext(Dispatchers.IO) {
        dao.softDeleteNote(noteId)
        ftsSearchEngine.indexItem(itemId)
        enqueueSyncEvent("note", noteId, "DELETE")
    }

    suspend fun createCollection(name: String, description: String = "", itemIds: List<String> = emptyList()) =
        withContext(Dispatchers.IO) {
            val col = CollectionEntity(
                userId = currentAuthUid(),
                name = UrlNormalizer.normalizeUnicode(name),
                description = UrlNormalizer.normalizeUnicode(description),
                syncState = syncStateForNewItem()
            )
            dao.upsertCollection(col)
            itemIds.forEachIndexed { idx, id ->
                dao.insertCollectionItem(CollectionItemCrossRef(col.id, id, idx))
            }
            enqueueSyncEvent("collection", col.id, "UPSERT")
        }

    suspend fun addItemToCollection(collectionId: String, itemId: String) = withContext(Dispatchers.IO) {
        dao.insertCollectionItem(CollectionItemCrossRef(collectionId = collectionId, itemId = itemId))
        enqueueSyncEvent("collection", collectionId, "UPSERT")
    }

    suspend fun removeItemFromCollection(collectionId: String, itemId: String) = withContext(Dispatchers.IO) {
        dao.deleteCollectionItem(collectionId, itemId)
        enqueueSyncEvent("collection", collectionId, "UPSERT")
    }

    suspend fun deleteCollection(collectionId: String) = withContext(Dispatchers.IO) {
        dao.softDeleteCollection(collectionId)
        enqueueSyncEvent("collection", collectionId, "DELETE")
    }

    // --- Search ---
    suspend fun searchLocal(query: String): List<LocalSearchHit> {
        val items = dao.getAllActiveItems()
        val tagsByItem = buildTagsByItemMap()
        val notesByItem = buildNotesByItemMap()
        val fileSourcesByItem = dao.getAllFileSources().associateBy { it.itemId }
        return ftsSearchEngine.searchLocal(query, items, tagsByItem, notesByItem, fileSourcesByItem)
    }

    suspend fun searchOnlineWithAi(query: String): Result<AiSearchResponse> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Device is offline. Local search results remain available."))
        }
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (!settings.aiEnabled) {
            return Result.failure(IllegalStateException("Online AI is disabled in Privacy & AI Settings."))
        }
        val localHits = searchLocal(query)
        val items = dao.getAllActiveItems()
        val candidates = if (localHits.isNotEmpty()) {
            val hitIds = localHits.take(30).map { it.itemId }.toSet()
            items.filter { it.id in hitIds }
        } else {
            items.take(30)
        }
        val tagsByItem = buildTagsByItemMap()
        val notesByItem = buildNotesByItemMap()
        return aiService.onlineSearchRerank(
            query = query,
            candidates = candidates,
            tagsByItem = tagsByItem,
            notesByItem = notesByItem,
            includeNotes = settings.includeNotesInAi
        )
    }

    suspend fun askAiAssistant(question: String): Result<String> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Internet connection required for AI Assistant."))
        }
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (!settings.aiEnabled) {
            return Result.failure(IllegalStateException("Online AI is disabled in Privacy & AI Settings."))
        }
        val hits = searchLocal(question)
        val all = dao.getAllActiveItems()
        val candidates = if (hits.isNotEmpty()) {
            val ids = hits.take(20).map { it.itemId }.toSet()
            all.filter { it.id in ids }
        } else {
            all.take(20)
        }
        return aiService.askAssistant(
            userQuestion = question,
            candidateItems = candidates,
            tagsByItem = buildTagsByItemMap(),
            notesByItem = buildNotesByItemMap(),
            includeNotes = settings.includeNotesInAi
        )
    }

    suspend fun suggestTagsAndSummaryForItem(itemId: String): Result<AiTagSummarySuggestion> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Internet required for AI suggestions."))
        }
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (!settings.aiEnabled) {
            return Result.failure(IllegalStateException("Online AI is disabled in Privacy & AI Settings."))
        }
        val item = dao.getItemById(itemId)
            ?: return Result.failure(IllegalStateException("Item not found"))
        val notes = dao.getNotesForItem(itemId)
        val folders = dao.getAllActiveFolders()
        return aiService.suggestTagsAndSummary(item, notes, folders, settings.includeNotesInAi)
    }

    // --- AI Organization Preview / Approval / Apply / Undo ---
    suspend fun generateOrganizationPreview(targetItems: List<ItemEntity>): Result<String> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Internet connection required to generate AI organization plan."))
        }
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (!settings.aiEnabled) {
            return Result.failure(IllegalStateException("Online AI is disabled in Privacy & AI Settings."))
        }
        val folders = dao.getAllActiveFolders()
        val jobId = UUID.randomUUID().toString()
        dao.upsertAiJob(AiJobEntity(id = jobId, jobType = "organization_plan", status = "RUNNING"))

        val draftResult = aiService.generateOrganizationPlan(
            unsortedItems = targetItems,
            existingFolders = folders,
            tagsByItem = buildTagsByItemMap(),
            notesByItem = buildNotesByItemMap(),
            includeNotes = settings.includeNotesInAi
        )

        return draftResult.fold(
            onSuccess = { draft ->
                val planId = createPlanRecord(draft, targetItems, folders)
                dao.upsertAiJob(
                    AiJobEntity(
                        id = jobId,
                        jobType = "organization_plan",
                        status = "COMPLETED",
                        completedAt = System.currentTimeMillis()
                    )
                )
                Result.success(planId)
            },
            onFailure = { error ->
                dao.upsertAiJob(
                    AiJobEntity(
                        id = jobId,
                        jobType = "organization_plan",
                        status = "FAILED",
                        errorCode = "AI_ORG_ERR",
                        errorMessage = error.message,
                        completedAt = System.currentTimeMillis()
                    )
                )
                recordDiagnostic("AI_ORG_PLAN", "AI_ORG_ERR", error.message ?: "Organization failed")
                Result.failure(error)
            }
        )
    }

    private suspend fun createPlanRecord(
        draft: AiOrganizationDraft,
        items: List<ItemEntity>,
        folders: List<FolderEntity>
    ): String {
        val planId = UUID.randomUUID().toString()
        val folderById = folders.associateBy { it.id }
        val itemById = items.associateBy { it.id }
        val overallBand = if (draft.operations.any { it.confidenceBand == "LOW" }) "MEDIUM" else "HIGH"

        dao.upsertPlan(
            OrganizationPlanEntity(
                id = planId,
                userId = currentAuthUid(),
                strategy = draft.strategy,
                confidenceBand = overallBand,
                reason = draft.summaryReason,
                warningsJson = JSONArray(draft.warnings).toString(),
                status = "PENDING"
            )
        )

        val operations = mutableListOf<OrganizationOperationEntity>()
        for (op in draft.operations) {
            val item = itemById[op.itemId] ?: continue
            val currentFolderIds = dao.getFolderIdsForItem(item.id)
            val currentFolderName = currentFolderIds
                .mapNotNull { folderById[it]?.name }
                .joinToString(", ")
                .ifBlank { "Unfiled" }
            val existingTarget = folders.find { it.name.equals(op.targetFolderName, ignoreCase = true) }

            operations.add(
                OrganizationOperationEntity(
                    planId = planId,
                    itemId = item.id,
                    itemTitle = item.title,
                    operationType = "MOVE_TO_FOLDER",
                    fromFolderId = currentFolderIds.firstOrNull(),
                    fromFolderName = currentFolderName,
                    toFolderId = existingTarget?.id,
                    toFolderName = op.targetFolderName,
                    tagName = op.suggestedTags.joinToString(", ").takeIf { it.isNotBlank() },
                    confidenceBand = op.confidenceBand,
                    reason = op.reason,
                    isSelected = op.confidenceBand != "LOW",
                    isApplied = false,
                    previousFolderIdsJson = JSONArray(currentFolderIds).toString()
                )
            )
        }
        dao.upsertOperations(operations)
        return planId
    }

    suspend fun togglePlanOperationSelection(operationId: String, selected: Boolean) {
        dao.updateOperationSelection(operationId, selected)
    }

    suspend fun rejectOrganizationPlan(planId: String) = withContext(Dispatchers.IO) {
        val plan = dao.getPlanById(planId) ?: return@withContext
        dao.upsertPlan(plan.copy(status = "REJECTED"))
        dao.insertAuditLog(
            AuditLogEntity(
                actionType = "ORG_PLAN_REJECTED",
                planId = planId,
                summary = "Rejected AI organization plan (${plan.strategy}). No library changes made."
            )
        )
    }

    suspend fun applyApprovedOrganizationPlan(planId: String): Result<Int> = withContext(Dispatchers.IO) {
        val plan = dao.getPlanById(planId)
            ?: return@withContext Result.failure(IllegalStateException("Plan not found"))
        if (plan.status == "APPROVED") {
            return@withContext Result.failure(IllegalStateException("Plan already applied"))
        }
        val ops = dao.getOperationsForPlan(planId).filter { it.isSelected }
        if (ops.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Select at least one operation to approve."))
        }

        val createdFolderIds = mutableListOf<String>()
        val updatedOps = mutableListOf<OrganizationOperationEntity>()
        val beforeSnapshots = JSONArray()
        val afterSnapshots = JSONArray()

        for (op in ops) {
            val itemId = op.itemId ?: continue
            val existingFolder = dao.findFolderByName(op.toFolderName)
            val targetFolder = existingFolder ?: createFolder(op.toFolderName).also {
                createdFolderIds.add(it.id)
            }

            beforeSnapshots.put(
                JSONObject().apply {
                    put("itemId", itemId)
                    put("previousFolderIds", op.previousFolderIdsJson)
                }
            )

            dao.moveItemToFolderReplacing(itemId, targetFolder.id)
            op.tagName?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.forEach { tag ->
                addTagToItem(itemId, tag, origin = "ai")
            }

            afterSnapshots.put(
                JSONObject().apply {
                    put("itemId", itemId)
                    put("newFolderId", targetFolder.id)
                    put("createdFolderIds", JSONArray(createdFolderIds))
                }
            )
            updatedOps.add(op.copy(toFolderId = targetFolder.id, isApplied = true))
            enqueueSyncEvent("item", itemId, "UPSERT")
        }

        dao.upsertOperations(updatedOps)
        dao.upsertPlan(
            plan.copy(
                status = "APPROVED",
                appliedAt = System.currentTimeMillis()
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                actionType = "ORG_PLAN_APPLIED",
                planId = planId,
                summary = "Applied ${updatedOps.size} AI organization operations (${createdFolderIds.size} new folders).",
                beforeStateJson = JSONObject().apply {
                    put("items", beforeSnapshots)
                    put("createdFolderIds", JSONArray(createdFolderIds))
                }.toString(),
                afterStateJson = afterSnapshots.toString()
            )
        )
        Result.success(updatedOps.size)
    }

    suspend fun undoOrganizationPlan(planId: String): Result<Int> = withContext(Dispatchers.IO) {
        val plan = dao.getPlanById(planId)
            ?: return@withContext Result.failure(IllegalStateException("Plan not found"))
        if (plan.status != "APPROVED") {
            return@withContext Result.failure(IllegalStateException("Only applied plans can be undone."))
        }
        val ops = dao.getOperationsForPlan(planId).filter { it.isApplied }
        var restoredCount = 0
        for (op in ops) {
            val itemId = op.itemId ?: continue
            dao.clearFoldersForItem(itemId)
            val prevArr = JSONArray(op.previousFolderIdsJson)
            for (i in 0 until prevArr.length()) {
                val prevId = prevArr.optString(i)
                if (prevId.isNotBlank()) {
                    dao.insertItemFolder(ItemFolderCrossRef(itemId = itemId, folderId = prevId))
                }
            }
            if (prevArr.length() == 0 || (prevArr.length() == 1 && prevArr.optString(0) == AppDatabase.INBOX_FOLDER_ID)) {
                dao.updateItemStatus(itemId, "unsorted")
            }
            restoredCount++
            enqueueSyncEvent("item", itemId, "UPSERT")
        }
        dao.upsertPlan(plan.copy(status = "UNDONE"))
        dao.insertAuditLog(
            AuditLogEntity(
                actionType = "ORG_PLAN_UNDONE",
                planId = planId,
                summary = "Rolled back folder assignments for $restoredCount items."
            )
        )
        Result.success(restoredCount)
    }

    // --- Cloud Sync (Opt-In, Auth-Gated) ---
    suspend fun syncNow(): Result<String> = withContext(Dispatchers.IO) {
        val user = Firebase.auth.currentUser
        val supaSession = preferencesStore.getCurrentSupabaseSession()
        val adminCfg = preferencesStore.getCurrentAdminVaultConfig()
        val canSupabaseSync = supaSession != null &&
            adminCfg.supabaseUrl.isNotBlank() &&
            adminCfg.supabaseAnonKey.isNotBlank()

        if (user == null && !canSupabaseSync) {
            return@withContext Result.failure(IllegalStateException("Sign in with Google or Supabase to enable cloud sync."))
        }
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (!settings.cloudSyncOptIn) {
            return@withContext Result.failure(IllegalStateException("Cloud sync is currently opted out in Privacy & Sync Settings."))
        }
        if (!isOnline()) {
            return@withContext Result.failure(IllegalStateException("Device is offline. Local changes are safely queued."))
        }

        try {
            var syncedSummaryCount = 0
            val tagsByItem = buildTagsByItemMap()

            if (user != null) {
                cloudSync.upsertUserProfile(user.displayName ?: "Library User", true).getOrThrow()

                // Push all folders
                for (folder in dao.getAllActiveFolders()) {
                    if (!folder.isInbox) {
                        cloudSync.pushFolder(folder).getOrThrow()
                    }
                }

                // Push all active items (metadata only — never local file binaries!)
                for (item in dao.getAllActiveItems()) {
                    if (item.localOnly) continue
                    val folderIds = dao.getFolderIdsForItem(item.id)
                    val tagNames = tagsByItem[item.id].orEmpty().map { it.name }
                    cloudSync.pushItem(item, folderIds, tagNames).getOrThrow()
                    dao.upsertItem(item.copy(userId = user.uid, syncState = "synced"))
                }

                // Push notes
                for (note in dao.getAllActiveNotes()) {
                    cloudSync.pushNote(note).getOrThrow()
                }

                // Pull remote items & handle safe note conflict resolution
                val remoteItems = cloudSync.fetchAllCloudItems().getOrThrow()
                syncedSummaryCount += remoteItems.size
                for (remote in remoteItems) {
                    val local = dao.getItemById(remote.id)
                    if (local == null && remote.deletedAtMillis == null) {
                        val imported = ItemEntity(
                            id = remote.id,
                            userId = user.uid,
                            title = remote.title,
                            description = remote.description.orEmpty(),
                            originalUrl = remote.originalUrl,
                            canonicalUrl = remote.canonicalUrl,
                            domain = remote.domain,
                            contentType = remote.contentType,
                            sourceType = remote.sourceType,
                            thumbnailUrl = remote.thumbnailUrl,
                            isFavorite = remote.isFavorite,
                            status = remote.status,
                            isDeviceLocal = remote.isDeviceLocal,
                            localVersion = remote.localVersion.toInt(),
                            syncState = "synced"
                        )
                        dao.upsertItem(imported)
                        remote.folderIds.forEach { fId ->
                            if (dao.getFolderById(fId) != null) {
                                dao.insertItemFolder(ItemFolderCrossRef(imported.id, fId))
                            }
                        }
                        remote.tagNames.forEach { tName -> addTagToItem(imported.id, tName) }
                        ftsSearchEngine.indexItem(imported.id)
                    }
                }

                // Pull remote notes safely without overwriting conflicting local edits
                val remoteNotes = cloudSync.fetchAllCloudNotes().getOrThrow()
                for (rNote in remoteNotes) {
                    val localNotes = dao.getNotesForItem(rNote.itemId)
                    val existing = localNotes.find { it.id == rNote.id }
                    if (existing != null && existing.body != rNote.body && existing.localVersion > rNote.localVersion) {
                        dao.upsertNote(
                            NoteEntity(
                                id = "${rNote.id}_conflict_${System.currentTimeMillis()}",
                                itemId = rNote.itemId,
                                userId = user.uid,
                                body = "[Synced Conflict Copy]\n${rNote.body}",
                                isPinned = rNote.isPinned,
                                isConflictCopy = true,
                                localVersion = rNote.localVersion.toInt(),
                                syncState = "synced"
                            )
                        )
                    } else if (existing == null && rNote.deletedAtMillis == null && dao.getItemById(rNote.itemId) != null) {
                        dao.upsertNote(
                            NoteEntity(
                                id = rNote.id,
                                itemId = rNote.itemId,
                                userId = user.uid,
                                body = rNote.body,
                                isPinned = rNote.isPinned,
                                localVersion = rNote.localVersion.toInt(),
                                syncState = "synced"
                            )
                        )
                    }
                }
            }

            if (canSupabaseSync && supaSession != null) {
                for (item in dao.getAllActiveItems()) {
                    if (item.localOnly) continue
                    val folderIds = dao.getFolderIdsForItem(item.id)
                    val tagNames = tagsByItem[item.id].orEmpty().map { it.name }
                    val res = supabaseClient.upsertItem(
                        supabaseUrl = adminCfg.supabaseUrl,
                        anonKey = adminCfg.supabaseAnonKey,
                        session = supaSession,
                        item = item,
                        folderIds = folderIds,
                        tagNames = tagNames
                    )
                    if (res.isSuccess) {
                        syncedSummaryCount++
                        if (user == null) {
                            dao.upsertItem(item.copy(syncState = "synced"))
                        }
                    }
                }
                for (note in dao.getAllActiveNotes()) {
                    supabaseClient.upsertNote(
                        supabaseUrl = adminCfg.supabaseUrl,
                        anonKey = adminCfg.supabaseAnonKey,
                        session = supaSession,
                        note = note
                    )
                }
            }

            dao.getPendingSyncEvents().forEach { ev ->
                dao.upsertSyncEvent(ev.copy(syncState = "SYNCED", updatedAt = System.currentTimeMillis()))
            }
            dao.pruneSyncedEvents()

            updateSettings {
                it.copy(
                    cloudSyncOptIn = true,
                    lastSyncedAt = System.currentTimeMillis()
                )
            }
            Result.success("Synced $syncedSummaryCount cloud records safely.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordDiagnostic("CLOUD_SYNC", "SYNC_ERR", e.message ?: "Cloud sync failed")
            Result.failure(e)
        }
    }

    // --- Backup Export / Import ---
    suspend fun exportBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        val payload = LibraryBackupPayload(
            items = dao.getAllActiveItems(),
            folders = dao.getAllActiveFolders(),
            itemFolders = dao.getAllActiveItems().flatMap { item ->
                dao.getFolderIdsForItem(item.id).map { fId -> ItemFolderCrossRef(item.id, fId) }
            },
            tags = dao.getAllTags(),
            itemTags = dao.getAllActiveItems().flatMap { item ->
                dao.getTagsForItem(item.id).map { tag -> ItemTagCrossRef(item.id, tag.id) }
            },
            notes = dao.getAllActiveNotes(),
            collections = dao.getAllActiveCollections(),
            collectionItems = emptyList()
        )
        BackupExporter.exportToUri(context, uri, payload)
    }

    suspend fun importBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        val result = BackupExporter.importFromUri(context, uri)
        result.map { payload ->
            payload.folders.forEach { dao.upsertFolder(it) }
            payload.items.forEach { dao.upsertItem(it) }
            payload.itemFolders.forEach { dao.insertItemFolder(it) }
            payload.tags.forEach { dao.upsertTag(it) }
            payload.itemTags.forEach { dao.insertItemTag(it) }
            payload.notes.forEach { dao.upsertNote(it) }
            payload.collections.forEach { dao.upsertCollection(it) }
            payload.collectionItems.forEach { dao.insertCollectionItem(it) }
            ftsSearchEngine.rebuildIndex()
            payload.items.size
        }
    }

    suspend fun clearDiagnostics() = dao.clearDiagnosticLogs()

    private suspend fun enqueueSyncEvent(entityType: String, entityId: String, operation: String) {
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (!settings.cloudSyncOptIn) return
        dao.upsertSyncEvent(
            SyncEventEntity(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                idempotencyKey = UrlNormalizer.idempotencyKey("$entityType:$entityId:$operation:${System.currentTimeMillis()}")
            )
        )
    }

    private suspend fun recordDiagnostic(operationId: String, errorCode: String, rawMessage: String) {
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (!settings.diagnosticsEnabled) return
        val redacted = rawMessage
            .replace(Regex("""AIza[0-9A-Za-z\-_]{20,}"""), "[REDACTED]")
            .replace(Regex("""Bearer\s+[0-9A-Za-z\.\-_]+"""), "[REDACTED]")
            .take(280)
        dao.insertDiagnosticLog(
            DiagnosticLogEntity(
                operationId = operationId,
                errorCode = errorCode,
                sanitizedMessage = redacted,
                appVersion = "1.0",
                androidVersion = Build.VERSION.SDK_INT
            )
        )
    }

    private suspend fun buildTagsByItemMap(): Map<String, List<TagEntity>> {
        val tagsById = dao.getAllTags().associateBy { it.id }
        val result = mutableMapOf<String, MutableList<TagEntity>>()
        for (item in dao.getAllActiveItems()) {
            val itemTags = dao.getTagsForItem(item.id)
            if (itemTags.isNotEmpty()) result[item.id] = itemTags.toMutableList()
        }
        return result
    }

    private suspend fun buildNotesByItemMap(): Map<String, List<NoteEntity>> {
        return dao.getAllActiveNotes().groupBy { it.itemId }
    }

    private suspend fun pushItemToConfiguredClouds(item: ItemEntity) {
        if (item.localOnly || !isOnline()) return
        val folderIds = dao.getFolderIdsForItem(item.id)
        val tagNames = dao.getTagsForItem(item.id).map { it.name }
        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        if (settings.cloudSyncOptIn && currentAuthUid() != null) {
            cloudSync.pushItem(item, folderIds, tagNames)
        }
        val adminCfg = preferencesStore.getCurrentAdminVaultConfig()
        val supaSession = preferencesStore.getCurrentSupabaseSession()
        if (supaSession != null && adminCfg.supabaseUrl.isNotBlank() && adminCfg.supabaseAnonKey.isNotBlank()) {
            supabaseClient.upsertItem(
                supabaseUrl = adminCfg.supabaseUrl,
                anonKey = adminCfg.supabaseAnonKey,
                session = supaSession,
                item = item,
                folderIds = folderIds,
                tagNames = tagNames
            )
        }
    }

    private fun currentAuthUid(): String? = try {
        Firebase.auth.currentUser?.uid
    } catch (_: Exception) {
        null
    }

    private fun syncStateForNewItem(): String {
        return if (currentAuthUid() != null) "sync_pending" else "local_only"
    }

    suspend fun signInWithSupabase(email: String, password: String): Result<SupabaseSession> {
        val cfg = preferencesStore.getCurrentAdminVaultConfig()
        val res = supabaseClient.signInWithPassword(
            supabaseUrl = cfg.supabaseUrl,
            anonKey = cfg.supabaseAnonKey,
            email = email,
            password = password
        )
        res.onSuccess { session ->
            preferencesStore.saveSupabaseSession(session)
            preferencesStore.setCustomDisplayName(session.displayName)
            preferencesStore.recordAccount("supabase:${session.userId}", session.displayName, session.email)
            completeOnboarding(guestMode = false, cloudSyncOptIn = true)
        }
        return res
    }

    suspend fun signUpWithSupabase(
        fullName: String,
        email: String,
        password: String
    ): Result<SupabaseSignUpOutcome> {
        val cfg = preferencesStore.getCurrentAdminVaultConfig()
        val res = supabaseClient.signUp(
            supabaseUrl = cfg.supabaseUrl,
            anonKey = cfg.supabaseAnonKey,
            fullName = fullName,
            email = email,
            password = password
        )
        res.onSuccess { outcome ->
            if (outcome is SupabaseSignUpOutcome.Authenticated) {
                val session = outcome.session
                preferencesStore.saveSupabaseSession(session)
                preferencesStore.setCustomDisplayName(session.displayName)
                preferencesStore.recordAccount("supabase:${session.userId}", session.displayName, session.email)
                completeOnboarding(guestMode = false, cloudSyncOptIn = true)
            }
        }
        return res
    }

    suspend fun signOutSupabase() {
        val cfg = preferencesStore.getCurrentAdminVaultConfig()
        val current = preferencesStore.getCurrentSupabaseSession()
        if (current != null && cfg.supabaseUrl.isNotBlank() && cfg.supabaseAnonKey.isNotBlank()) {
            supabaseClient.signOut(cfg.supabaseUrl, cfg.supabaseAnonKey, current.accessToken)
        }
        preferencesStore.clearSupabaseSession()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Unknown size"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun computeSyncBadgeState(
        isSignedIn: Boolean,
        cloudSyncOptIn: Boolean,
        pendingCount: Int,
        isSyncing: Boolean,
        hasSyncError: Boolean
    ): SyncBadgeState {
        return when {
            !isSignedIn && cloudSyncOptIn -> SyncBadgeState.SIGN_IN_REQUIRED
            !isSignedIn || !cloudSyncOptIn -> SyncBadgeState.LOCAL_ONLY
            hasSyncError -> SyncBadgeState.NEEDS_ATTENTION
            isSyncing -> SyncBadgeState.SYNCING
            pendingCount > 0 -> SyncBadgeState.SYNC_PENDING
            else -> SyncBadgeState.SYNCED
        }
    }

    // --- Theme, Multi-Device Sync & Reviews ---
    suspend fun getOrCreateDeviceId(): String = preferencesStore.getOrCreateDeviceId()

    suspend fun setThemeMode(mode: AppThemeMode) {
        preferencesStore.setThemeMode(mode)
        if (currentAuthUid() != null && isOnline()) {
            val settings = dao.getUserSettings()
            if (settings?.cloudSyncOptIn == true) {
                cloudSync.updateThemeMode(mode.code)
            }
        }
    }

    suspend fun applyRemoteThemeMode(rawCode: String?) {
        if (rawCode.isNullOrBlank()) return
        preferencesStore.setThemeMode(AppThemeMode.fromCode(rawCode))
    }

    suspend fun setAccentPreset(preset: LiquidAccentPreset) {
        preferencesStore.setAccentPreset(preset)
        if (currentAuthUid() != null && isOnline()) {
            val settings = dao.getUserSettings()
            if (settings?.cloudSyncOptIn == true) {
                cloudSync.updateAccentPreset(preset.code)
            }
        }
    }

    suspend fun applyRemoteAccentPreset(rawCode: String?) {
        if (rawCode.isNullOrBlank()) return
        preferencesStore.setAccentPreset(LiquidAccentPreset.fromCode(rawCode))
    }

    suspend fun updateUserProfileName(newName: String): Result<String> {
        val clean = UrlNormalizer.normalizeUnicode(newName).trim().take(120)
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Display name cannot be empty."))
        }
        preferencesStore.setCustomDisplayName(clean)
        val user = Firebase.auth.currentUser
        if (user != null) {
            try {
                user.updateProfile(
                    userProfileChangeRequest { displayName = clean }
                ).await()
            } catch (_: Exception) {
                // Keep local profile name even if Firebase Auth update fails offline
            }
            preferencesStore.recordAccount(user.uid, clean, user.email.orEmpty())
            if (isOnline()) {
                val syncEnabled = dao.getUserSettings()?.cloudSyncOptIn ?: true
                cloudSync.upsertUserProfile(
                    displayName = clean,
                    cloudSyncEnabled = syncEnabled,
                    accentPreset = null
                )
            }
        }
        return Result.success(clean)
    }

    suspend fun updateUserProfileDetails(
        newName: String,
        newEmail: String,
        avatarValue: String? = null,
        selectedImageUri: Uri? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanName = UrlNormalizer.normalizeUnicode(newName).trim().take(120)
        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Display name cannot be empty."))
        }
        val cleanEmail = newEmail.trim().take(160)
        preferencesStore.setCustomEmail(cleanEmail)

        var resolvedAvatar = avatarValue
        if (selectedImageUri != null) {
            resolvedAvatar = copyAvatarImageLocally(selectedImageUri) ?: selectedImageUri.toString()
        }
        if (!resolvedAvatar.isNullOrBlank()) {
            preferencesStore.setProfileAvatar(resolvedAvatar)
        }
        updateUserProfileName(cleanName)
    }

    private fun copyAvatarImageLocally(uri: Uri): String? {
        return try {
            val avatarDir = java.io.File(context.filesDir, "avatars").apply { mkdirs() }
            val dest = java.io.File(avatarDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
            avatarDir.listFiles()?.forEach { old -> if (old != dest) old.delete() }
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            Uri.fromFile(dest).toString()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setVaultPin(pin: String?) {
        preferencesStore.setVaultPin(pin)
    }

    suspend fun setVaultLocked(locked: Boolean) {
        preferencesStore.setVaultLocked(locked)
    }

    suspend fun recordSignedInAccount(uid: String, displayName: String, email: String) {
        preferencesStore.recordAccount(uid, displayName, email)
    }

    suspend fun registerDevicePresence(deviceName: String, status: String = "online") {
        if (currentAuthUid() == null || !isOnline()) return
        val deviceId = preferencesStore.getOrCreateDeviceId()
        cloudSync.registerOrHeartbeatDevice(
            deviceId = deviceId,
            deviceName = deviceName,
            platform = "android",
            status = status
        )
    }

    fun observeCloudDevices(userId: String): Flow<List<CloudDeviceSession>> =
        cloudSync.observeDevices(userId)

    fun observeCloudTransfers(userId: String): Flow<List<CloudDeviceTransfer>> =
        cloudSync.observeTransfers(userId)

    fun observeCloudReviews(userId: String): Flow<List<CloudReview>> =
        cloudSync.observeReviews(userId)

    fun observeRemoteThemeMode(userId: String): Flow<String?> =
        cloudSync.observeUserThemeMode(userId)

    fun observeRemoteAccentPreset(userId: String): Flow<String?> =
        cloudSync.observeUserAccentPreset(userId)

    fun observeRemoteAdminConfig(userId: String): Flow<CloudAdminVaultConfig?> =
        cloudSync.observeAdminConfig(userId)

    fun observeGlobalAiConfig(): Flow<CloudGlobalAiConfig?> =
        cloudSync.observeGlobalAiConfig()

    suspend fun fetchFreeOpenRouterModels(): Result<List<Pair<String, String>>> =
        aiService.fetchFreeOpenRouterModels()

    fun syncRuntimeAiFromVault(config: AdminVaultConfig) {
        val effective = AdminVaultConfig.resolveEffective(config)
        aiService.updateRuntimeConfig(
            provider = effective.aiProvider,
            openRouterApiKey = effective.openRouterApiKey,
            openRouterModel = effective.openRouterModel,
            providerManuallySelected = effective.providerManuallySelected
        )
    }

    suspend fun saveAdminVaultConfig(config: AdminVaultConfig): Result<Unit> {
        val current = preferencesStore.getCurrentAdminVaultConfig()
        val trimmedKey = config.openRouterApiKey.trim()
        val effectiveProvider = if (trimmedKey.isNotBlank() && !config.providerManuallySelected) {
            AdminVaultConfig.PROVIDER_OPENROUTER
        } else {
            config.aiProvider
        }
        val preserved = config.copy(
            aiProvider = effectiveProvider,
            openRouterApiKey = trimmedKey,
            openRouterModel = AdminVaultConfig.enforceFreeOpenRouterModel(config.openRouterModel),
            adminPasscodeHash = config.adminPasscodeHash ?: current.adminPasscodeHash,
            adminPasscodeSalt = config.adminPasscodeSalt ?: current.adminPasscodeSalt,
            lastSyncedAt = System.currentTimeMillis()
        )
        preferencesStore.saveAdminVaultConfig(preserved)
        syncRuntimeAiFromVault(preserved)

        val warnings = mutableListOf<String>()
        if (currentAuthUid() != null && isOnline()) {
            val pushRes = cloudSync.pushAdminConfig(
                aiProvider = preserved.aiProvider,
                openRouterApiKey = preserved.openRouterApiKey,
                openRouterModel = preserved.openRouterModel,
                supabaseUrl = preserved.supabaseUrl,
                supabaseAnonKey = preserved.supabaseAnonKey,
                customCredentialsJson = UserPreferencesStore.encodeCustomCredentials(preserved.customCredentials),
                adminPasscodeHash = preserved.adminPasscodeHash,
                adminPasscodeSalt = preserved.adminPasscodeSalt
            )
            pushRes.exceptionOrNull()?.let { warnings += "Firestore Vault: ${it.message}" }

            val globalRes = cloudSync.pushGlobalAiConfig(
                aiProvider = preserved.aiProvider,
                openRouterApiKey = preserved.openRouterApiKey,
                openRouterModel = preserved.openRouterModel,
                supabaseUrl = preserved.supabaseUrl,
                supabaseAnonKey = preserved.supabaseAnonKey
            )
            globalRes.exceptionOrNull()?.let { warnings += "Global App Broadcast: ${it.message}" }
        }

        val supaSession = preferencesStore.getCurrentSupabaseSession()
        if (supaSession != null && preserved.supabaseUrl.isNotBlank() && preserved.supabaseAnonKey.isNotBlank() && isOnline()) {
            val supaRes = supabaseClient.upsertAdminVaultConfig(
                supabaseUrl = preserved.supabaseUrl,
                anonKey = preserved.supabaseAnonKey,
                session = supaSession,
                config = preserved
            )
            supaRes.exceptionOrNull()?.let { warnings += "Supabase: ${it.message}" }
        }

        return if (warnings.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(warnings.joinToString(" • ")))
        }
    }

    suspend fun bootstrapRemoteAdminConfigIfNeeded() {
        if (currentAuthUid() == null || !isOnline()) return
        val local = preferencesStore.getCurrentAdminVaultConfig()
        val hasLocalData = local.hasAdminPasscode ||
            local.openRouterApiKey.isNotBlank() ||
            local.supabaseUrl.isNotBlank() ||
            local.customCredentials.isNotEmpty() ||
            local.aiProvider != AdminVaultConfig.PROVIDER_FIREBASE_AI
        if (!hasLocalData) return
        cloudSync.pushAdminConfig(
            aiProvider = local.aiProvider,
            openRouterApiKey = local.openRouterApiKey,
            openRouterModel = AdminVaultConfig.enforceFreeOpenRouterModel(local.openRouterModel),
            supabaseUrl = local.supabaseUrl,
            supabaseAnonKey = local.supabaseAnonKey,
            customCredentialsJson = UserPreferencesStore.encodeCustomCredentials(local.customCredentials),
            adminPasscodeHash = local.adminPasscodeHash,
            adminPasscodeSalt = local.adminPasscodeSalt
        )
    }

    suspend fun pullLatestCloudAdminConfig(): Result<AdminVaultConfig> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Device is offline. Cannot refresh cloud vault."))
        }
        var updatedAny = false
        if (currentAuthUid() != null) {
            val globalRes = cloudSync.fetchGlobalAiConfig()
            globalRes.onSuccess { globalRemote ->
                if (globalRemote != null) {
                    applyGlobalAiConfig(globalRemote)
                    updatedAny = true
                }
            }
            val fsRes = cloudSync.fetchAdminConfig()
            fsRes.onSuccess { remote ->
                if (remote != null) {
                    applyRemoteAdminVaultConfig(remote)
                    updatedAny = true
                } else {
                    bootstrapRemoteAdminConfigIfNeeded()
                }
            }.onFailure { err ->
                if (!updatedAny) return Result.failure(err)
            }
        }
        val current = preferencesStore.getCurrentAdminVaultConfig()
        val supaSession = preferencesStore.getCurrentSupabaseSession()
        if (!updatedAny && supaSession != null && current.supabaseUrl.isNotBlank() && current.supabaseAnonKey.isNotBlank()) {
            val supaRes = supabaseClient.fetchAdminVaultConfig(
                supabaseUrl = current.supabaseUrl,
                anonKey = current.supabaseAnonKey,
                session = supaSession
            )
            supaRes.onSuccess { remoteSupa ->
                if (remoteSupa != null) {
                    val merged = remoteSupa.copy(
                        openRouterModel = AdminVaultConfig.enforceFreeOpenRouterModel(remoteSupa.openRouterModel),
                        adminPasscodeHash = remoteSupa.adminPasscodeHash ?: current.adminPasscodeHash,
                        adminPasscodeSalt = remoteSupa.adminPasscodeSalt ?: current.adminPasscodeSalt
                    )
                    preferencesStore.saveAdminVaultConfig(merged)
                    syncRuntimeAiFromVault(merged)
                }
            }
        }
        return Result.success(preferencesStore.getCurrentAdminVaultConfig())
    }

    suspend fun applyGlobalAiConfig(remote: CloudGlobalAiConfig?) {
        if (remote == null) return
        val local = preferencesStore.getCurrentAdminVaultConfig()
        val remoteKey = remote.openRouterApiKey?.trim()?.takeIf { it.isNotBlank() }
        val nextKey = remoteKey ?: local.openRouterApiKey
        val nextModel = AdminVaultConfig.enforceFreeOpenRouterModel(
            remote.openRouterModel ?: local.openRouterModel
        )
        val remoteProvider = remote.aiProvider.takeIf {
            it in setOf(AdminVaultConfig.PROVIDER_FIREBASE_AI, AdminVaultConfig.PROVIDER_OPENROUTER)
        }
        val merged = local.copy(
            aiProvider = when {
                remoteProvider == AdminVaultConfig.PROVIDER_OPENROUTER -> AdminVaultConfig.PROVIDER_OPENROUTER
                remoteKey != null -> AdminVaultConfig.PROVIDER_OPENROUTER
                remoteProvider != null -> remoteProvider
                else -> local.aiProvider
            },
            openRouterApiKey = nextKey,
            openRouterModel = nextModel,
            supabaseUrl = remote.supabaseUrl?.trim()?.takeIf { it.isNotBlank() } ?: local.supabaseUrl,
            supabaseAnonKey = remote.supabaseAnonKey?.trim()?.takeIf { it.isNotBlank() } ?: local.supabaseAnonKey,
            lastSyncedAt = remote.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
        )
        preferencesStore.saveAdminVaultConfig(merged)
        syncRuntimeAiFromVault(merged)
    }

    suspend fun applyRemoteAdminVaultConfig(remote: CloudAdminVaultConfig?) {
        if (remote == null) return
        val local = preferencesStore.getCurrentAdminVaultConfig()
        val remoteTs = remote.updatedAt?.toDate()?.time ?: 0L
        val localTs = local.lastSyncedAt ?: 0L
        val shouldApplyAiFields = remoteTs >= localTs || local.openRouterApiKey.isBlank()
        val nextKey = if (shouldApplyAiFields) {
            remote.openRouterApiKey?.trim()?.takeIf { it.isNotBlank() } ?: local.openRouterApiKey
        } else {
            local.openRouterApiKey
        }
        val nextProvider = if (shouldApplyAiFields) {
            val rp = remote.aiProvider.takeIf {
                it in setOf(AdminVaultConfig.PROVIDER_FIREBASE_AI, AdminVaultConfig.PROVIDER_OPENROUTER)
            }
            if (!remote.openRouterApiKey.isNullOrBlank() && rp != AdminVaultConfig.PROVIDER_FIREBASE_AI) {
                AdminVaultConfig.PROVIDER_OPENROUTER
            } else {
                rp ?: local.aiProvider
            }
        } else {
            local.aiProvider
        }
        val merged = AdminVaultConfig(
            aiProvider = nextProvider,
            openRouterApiKey = nextKey,
            openRouterModel = AdminVaultConfig.enforceFreeOpenRouterModel(
                if (shouldApplyAiFields) {
                    remote.openRouterModel?.takeIf { it.isNotBlank() } ?: local.openRouterModel
                } else {
                    local.openRouterModel
                }
            ),
            supabaseUrl = remote.supabaseUrl?.takeIf { it.isNotBlank() } ?: local.supabaseUrl,
            supabaseAnonKey = remote.supabaseAnonKey?.takeIf { it.isNotBlank() } ?: local.supabaseAnonKey,
            customCredentials = remote.customCredentialsJson?.let {
                UserPreferencesStore.parseCustomCredentials(it)
            } ?: local.customCredentials,
            adminPasscodeHash = remote.adminPasscodeHash?.takeIf { it.isNotBlank() } ?: local.adminPasscodeHash,
            adminPasscodeSalt = remote.adminPasscodeSalt?.takeIf { it.isNotBlank() } ?: local.adminPasscodeSalt,
            lastSyncedAt = maxOf(remoteTs, localTs).takeIf { it > 0L } ?: System.currentTimeMillis(),
            providerManuallySelected = local.providerManuallySelected
        )
        preferencesStore.saveAdminVaultConfig(merged)
        syncRuntimeAiFromVault(merged)
    }

    suspend fun verifyOrCreateAdminPasscode(rawPasscode: String): Result<Boolean> {
        val trimmed = rawPasscode.trim()
        if (trimmed.length < 4) {
            return Result.failure(IllegalArgumentException("Passcode must be at least 4 characters."))
        }
        val current = preferencesStore.getCurrentAdminVaultConfig()
        return if (!current.hasAdminPasscode) {
            val salt = AdminPasscodeCrypto.generateSaltHex()
            val hash = AdminPasscodeCrypto.hashPasscode(trimmed, salt)
            val updated = current.copy(adminPasscodeHash = hash, adminPasscodeSalt = salt)
            saveAdminVaultConfig(updated)
            Result.success(true)
        } else if (AdminPasscodeCrypto.verify(trimmed, current.adminPasscodeHash, current.adminPasscodeSalt)) {
            Result.success(true)
        } else {
            Result.failure(IllegalArgumentException("Invalid admin passcode."))
        }
    }

    suspend fun changeAdminPasscode(newPasscode: String): Result<Unit> {
        val trimmed = newPasscode.trim()
        if (trimmed.length < 4) {
            return Result.failure(IllegalArgumentException("New passcode must be at least 4 characters."))
        }
        val current = preferencesStore.getCurrentAdminVaultConfig()
        val salt = AdminPasscodeCrypto.generateSaltHex()
        val hash = AdminPasscodeCrypto.hashPasscode(trimmed, salt)
        return saveAdminVaultConfig(
            current.copy(adminPasscodeHash = hash, adminPasscodeSalt = salt)
        )
    }

    suspend fun testAiConnection(
        previewProvider: String? = null,
        previewKey: String? = null,
        previewModel: String? = null
    ): Result<String> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Device is offline. Connect to internet to test AI connection."))
        }
        val stored = preferencesStore.getCurrentAdminVaultConfig()
        val effectiveKey = previewKey?.trim()?.takeIf { it.isNotBlank() } ?: stored.openRouterApiKey
        val effectiveProvider = previewProvider ?: stored.aiProvider
        val effectiveModel = previewModel?.trim()?.takeIf { it.isNotBlank() } ?: stored.openRouterModel
        aiService.updateRuntimeConfig(
            provider = effectiveProvider,
            openRouterApiKey = effectiveKey,
            openRouterModel = effectiveModel,
            providerManuallySelected = true
        )
        return try {
            aiService.testConnection()
        } finally {
            syncRuntimeAiFromVault(stored)
        }
    }

    suspend fun sendToDevice(
        title: String,
        payloadText: String,
        payloadType: String,
        fromDeviceName: String,
        targetDeviceId: String?
    ): Result<Unit> {
        if (currentAuthUid() == null) {
            return Result.failure(IllegalStateException("Sign in with Google to send across devices."))
        }
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Device is offline. Connect to internet to send to PC/Phone."))
        }
        return cloudSync.sendDeviceTransfer(
            transferId = UUID.randomUUID().toString(),
            title = title,
            payloadText = payloadText,
            payloadType = payloadType,
            fromDeviceName = fromDeviceName,
            targetDeviceId = targetDeviceId
        )
    }

    suspend fun acceptTransferToLibrary(transfer: CloudDeviceTransfer): Result<ItemEntity> {
        val raw = if (transfer.payloadType == "link") transfer.payloadText else "${transfer.title}\n${transfer.payloadText}"
        val outcome = captureWebUrlOrText(
            rawInput = raw,
            customTitle = transfer.title.takeIf { it.isNotBlank() },
            initialNote = "Received from ${transfer.fromDeviceName}"
        )
        return when (outcome) {
            is CaptureOutcome.Saved -> {
                cloudSync.markTransferReceived(transfer.id)
                Result.success(outcome.item)
            }
            is CaptureOutcome.DuplicateDetected -> {
                cloudSync.markTransferReceived(transfer.id)
                Result.success(outcome.existingItem)
            }
            is CaptureOutcome.Error -> Result.failure(IllegalStateException(outcome.message))
        }
    }

    suspend fun dismissTransfer(transferId: String) {
        cloudSync.markTransferReceived(transferId)
    }

    suspend fun submitReview(itemId: String?, authorName: String, rating: Int, body: String): Result<LibraryReview> {
        val cleanBody = UrlNormalizer.normalizeUnicode(body).trim()
        if (cleanBody.isBlank()) {
            return Result.failure(IllegalStateException("Please write a short review before submitting."))
        }
        val review = LibraryReview(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            authorName = authorName.ifBlank { "Library Member" },
            rating = rating.coerceIn(1, 5),
            body = cleanBody.take(1500),
            helpfulCount = 0,
            isHelpfulByMe = false,
            verified = currentAuthUid() != null
        )
        preferencesStore.saveOrUpdateReview(review)
        if (currentAuthUid() != null && isOnline()) {
            cloudSync.pushReview(
                reviewId = review.id,
                itemId = review.itemId,
                authorName = review.authorName,
                rating = review.rating,
                body = review.body,
                helpfulCount = review.helpfulCount
            )
        }
        return Result.success(review)
    }

    suspend fun toggleReviewHelpful(reviewId: String) {
        val updated = preferencesStore.toggleReviewHelpful(reviewId) ?: return
        if (currentAuthUid() != null && isOnline()) {
            cloudSync.pushReview(
                reviewId = updated.id,
                itemId = updated.itemId,
                authorName = updated.authorName,
                rating = updated.rating,
                body = updated.body,
                helpfulCount = updated.helpfulCount
            )
        }
    }

    suspend fun mergeCloudReviews(cloudReviews: List<CloudReview>) {
        val mapped = cloudReviews.map { cr ->
            LibraryReview(
                id = cr.id,
                itemId = cr.itemId,
                authorName = cr.authorName,
                rating = cr.rating,
                body = cr.body,
                helpfulCount = cr.helpfulCount,
                verified = true,
                createdAt = cr.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
        preferencesStore.mergeCloudReviews(mapped)
    }

    suspend fun saveDetoxSessionState(state: DetoxSessionState) {
        DetoxRuntimeStore.write(context, state)
        preferencesStore.saveDetoxSessionState(state)
    }

    suspend fun clearDetoxSessionState() {
        DetoxRuntimeStore.clear(context)
        preferencesStore.clearDetoxSessionState()
    }

    suspend fun recordRecentFinanceIcon(iconKey: String) {
        preferencesStore.recordRecentFinanceIcon(iconKey)
    }

    suspend fun setFinanceTransactionIcon(txId: String, iconKey: String) {
        preferencesStore.setFinanceTransactionIcon(txId, iconKey)
    }

    suspend fun removeFinanceTransactionIcon(txId: String) {
        preferencesStore.removeFinanceTransactionIcon(txId)
    }
}

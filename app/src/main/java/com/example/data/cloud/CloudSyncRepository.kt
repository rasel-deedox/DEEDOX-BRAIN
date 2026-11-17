package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AdminVaultConfig
import com.example.data.local.CollectionEntity
import com.example.data.local.FolderEntity
import com.example.data.local.ItemEntity
import com.example.data.local.NoteEntity
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

enum class SyncBadgeState(val label: String) {
    LOCAL_ONLY("Local Only"),
    SIGN_IN_REQUIRED("Sign-in Required"),
    SYNC_PENDING("Sync Pending"),
    SYNCING("Syncing"),
    SYNCED("Synced"),
    NEEDS_ATTENTION("Needs Attention")
}

@IgnoreExtraProperties
data class CloudLibraryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val originalUrl: String? = null,
    val canonicalUrl: String? = null,
    val domain: String? = null,
    val contentType: String = "link",
    val sourceType: String = "manual",
    val thumbnailUrl: String? = null,
    val isFavorite: Boolean = false,
    val status: String = "unsorted",
    val folderIds: List<String> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val isDeviceLocal: Boolean = false,
    val localVersion: Long = 1L,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudFolder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val parentFolderId: String? = null,
    val colorHex: String = "#E53935",
    val iconKey: String = "folder",
    val sortOrder: Long = 0L,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    val body: String = "",
    val isPinned: Boolean = false,
    val localVersion: Long = 1L,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudCollection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String? = null,
    val itemIds: List<String> = emptyList(),
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudDeviceSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val platform: String = "android",
    val status: String = "online",
    @ServerTimestamp val lastSeenAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudDeviceTransfer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val payloadText: String = "",
    val payloadType: String = "link",
    val fromDeviceName: String = "",
    val targetDeviceId: String? = null,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String? = null,
    val authorName: String = "",
    val rating: Int = 5,
    val body: String = "",
    val helpfulCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudAdminVaultConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val aiProvider: String = "firebase_ai",
    val openRouterApiKey: String? = null,
    val openRouterModel: String? = null,
    val supabaseUrl: String? = null,
    val supabaseAnonKey: String? = null,
    val customCredentialsJson: String? = null,
    val adminPasscodeHash: String? = null,
    val adminPasscodeSalt: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudGlobalAiConfig(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val aiProvider: String = "openrouter",
    val openRouterApiKey: String? = null,
    val openRouterModel: String? = null,
    val supabaseUrl: String? = null,
    val supabaseAnonKey: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudSyncRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCloudItems(userId: String): Flow<List<CloudLibraryItem>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Scoped userId mismatch" }
        val path = "users/$uid/items"
        db.collection("users").document(uid).collection("items")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CloudLibraryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val ex = fsError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun upsertUserProfile(
        displayName: String,
        cloudSyncEnabled: Boolean,
        themeMode: String? = null,
        accentPreset: String? = null
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            val validTheme = themeMode?.takeIf { it in setOf("LIGHT", "DIM", "NIGHT", "DARK") }
            val validAccent = accentPreset?.take(64)?.takeIf { it.isNotBlank() }
            if (existing.exists()) {
                val updateMap = mutableMapOf<String, Any>(
                    "displayName" to displayName.take(120).ifBlank { "Library User" },
                    "cloudSyncEnabled" to cloudSyncEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (validTheme != null) updateMap["themeMode"] = validTheme
                if (validAccent != null) updateMap["accentPreset"] = validAccent
                docRef.update(updateMap).await()
            } else {
                val createMap = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName.take(120).ifBlank { "Library User" },
                    "cloudSyncEnabled" to cloudSyncEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (validTheme != null) createMap["themeMode"] = validTheme
                if (validAccent != null) createMap["accentPreset"] = validAccent
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateThemeMode(themeMode: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val validTheme = if (themeMode in setOf("LIGHT", "DIM", "NIGHT", "DARK")) themeMode else "NIGHT"
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "themeMode" to validTheme,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val name = auth.currentUser?.displayName?.take(120).orEmpty().ifBlank { "Library User" }
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to name,
                        "cloudSyncEnabled" to true,
                        "themeMode" to validTheme,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAccentPreset(accentPreset: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val validAccent = accentPreset.take(64).ifBlank { "CRIMSON_BRONZE" }
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "accentPreset" to validAccent,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val name = auth.currentUser?.displayName?.take(120).orEmpty().ifBlank { "Library User" }
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to name,
                        "cloudSyncEnabled" to true,
                        "accentPreset" to validAccent,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserThemeMode(userId: String): Flow<String?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Scoped userId mismatch" }
        val docRef = db.collection("users").document(uid)
        docRef.snapshots()
            .map { snap -> snap.getString("themeMode") }
            .catch { error ->
                if (error is CancellationException) throw error
            }
            .collect { emit(it) }
    }

    fun observeUserAccentPreset(userId: String): Flow<String?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Scoped userId mismatch" }
        val docRef = db.collection("users").document(uid)
        docRef.snapshots()
            .map { snap -> snap.getString("accentPreset") }
            .catch { error ->
                if (error is CancellationException) throw error
            }
            .collect { emit(it) }
    }

    suspend fun pushAdminConfig(
        aiProvider: String,
        openRouterApiKey: String,
        openRouterModel: String,
        supabaseUrl: String,
        supabaseAnonKey: String,
        customCredentialsJson: String,
        adminPasscodeHash: String?,
        adminPasscodeSalt: String?
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("admin_vault").document("api_config")
        return try {
            val existing = docRef.get().await()
            val validProvider = if (aiProvider == "openrouter") "openrouter" else "firebase_ai"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "aiProvider" to validProvider,
                "openRouterApiKey" to openRouterApiKey.take(512),
                "openRouterModel" to AdminVaultConfig.enforceFreeOpenRouterModel(openRouterModel).take(160),
                "supabaseUrl" to supabaseUrl.take(512),
                "supabaseAnonKey" to supabaseAnonKey.take(1024),
                "customCredentialsJson" to customCredentialsJson.take(8000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            adminPasscodeHash?.takeIf { it.isNotBlank() }?.let { payload["adminPasscodeHash"] = it.take(128) }
            adminPasscodeSalt?.takeIf { it.isNotBlank() }?.let { payload["adminPasscodeSalt"] = it.take(64) }

            if (existing.exists()) {
                payload.remove("userId")
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAdminConfig(userId: String): Flow<CloudAdminVaultConfig?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Scoped userId mismatch" }
        val docRef = db.collection("users").document(uid).collection("admin_vault").document("api_config")
        docRef.snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    CloudAdminVaultConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val ex = fsError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.GET, docRef.path)
                }
            }
            .collect { emit(it) }
    }

    suspend fun fetchAdminConfig(): Result<CloudAdminVaultConfig?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("admin_vault").document("api_config")
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                Result.success(
                    snap.toObject(
                        CloudAdminVaultConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushGlobalAiConfig(
        aiProvider: String,
        openRouterApiKey: String,
        openRouterModel: String,
        supabaseUrl: String,
        supabaseAnonKey: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("app_config").document("ai_runtime")
        return try {
            val existing = docRef.get().await()
            val validProvider = if (aiProvider == "openrouter") "openrouter" else "firebase_ai"
            val payload = mutableMapOf<String, Any>(
                "aiProvider" to validProvider,
                "openRouterApiKey" to openRouterApiKey.take(512),
                "openRouterModel" to AdminVaultConfig.enforceFreeOpenRouterModel(openRouterModel).take(160),
                "supabaseUrl" to supabaseUrl.take(512),
                "supabaseAnonKey" to supabaseAnonKey.take(1024),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existing.exists()) {
                docRef.update(payload).await()
            } else {
                payload["adminUid"] = uid
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeGlobalAiConfig(): Flow<CloudGlobalAiConfig?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("app_config").document("ai_runtime")
        docRef.snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    CloudGlobalAiConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val ex = fsError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.GET, docRef.path)
                }
            }
            .collect { emit(it) }
    }

    suspend fun fetchGlobalAiConfig(): Result<CloudGlobalAiConfig?> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("app_config").document("ai_runtime")
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                Result.success(
                    snap.toObject(
                        CloudGlobalAiConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushItem(
        item: ItemEntity,
        folderIds: List<String>,
        tagNames: List<String>
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = sanitizeDocId(item.id)
        val docRef = db.collection("users").document(uid).collection("items").document(docId)
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to item.title.take(500).ifBlank { "Untitled" },
                "contentType" to validateContentType(item.contentType),
                "sourceType" to validateSourceType(item.sourceType),
                "isFavorite" to item.isFavorite,
                "status" to validateStatus(item.status),
                "folderIds" to folderIds.take(25),
                "tagNames" to tagNames.take(50),
                "isDeviceLocal" to item.isDeviceLocal,
                "localVersion" to item.localVersion.coerceAtLeast(1).toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            item.description.takeIf { it.isNotBlank() }?.let { payload["description"] = it.take(4000) }
            item.originalUrl?.takeIf { it.isNotBlank() }?.let { payload["originalUrl"] = it.take(2048) }
            item.canonicalUrl?.takeIf { it.isNotBlank() }?.let { payload["canonicalUrl"] = it.take(2048) }
            item.domain?.takeIf { it.isNotBlank() }?.let { payload["domain"] = it.take(255) }
            item.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { payload["thumbnailUrl"] = it.take(2048) }
            item.deletedAt?.let { payload["deletedAtMillis"] = it }

            if (existing.exists()) {
                payload.remove("userId")
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getItemById(itemId: String): Result<CloudLibraryItem?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("items").document(sanitizeDocId(itemId))
        return try {
            val snap = docRef.get().await()
            val obj = snap.toObject(
                CloudLibraryItem::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(obj)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchAllCloudItems(): Result<List<CloudLibraryItem>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val colRef = db.collection("users").document(uid).collection("items")
        return try {
            val snap = colRef.get().await()
            Result.success(
                snap.toObjects(
                    CloudLibraryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushFolder(folder: FolderEntity): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("folders").document(sanitizeDocId(folder.id))
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "name" to folder.name.take(120).ifBlank { "Folder" },
                "colorHex" to folder.colorHex.take(16).ifBlank { "#E53935" },
                "iconKey" to folder.iconKey.take(64).ifBlank { "folder" },
                "sortOrder" to folder.sortOrder.toLong(),
                "isFavorite" to folder.isFavorite,
                "isArchived" to folder.isArchived,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            folder.parentFolderId?.takeIf { it.isNotBlank() }?.let { payload["parentFolderId"] = sanitizeDocId(it) }
            folder.deletedAt?.let { payload["deletedAtMillis"] = it }

            if (existing.exists()) {
                payload.remove("userId")
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchAllCloudFolders(): Result<List<CloudFolder>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val colRef = db.collection("users").document(uid).collection("folders")
        return try {
            val snap = colRef.get().await()
            Result.success(
                snap.toObjects(
                    CloudFolder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushNote(note: NoteEntity): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("notes").document(sanitizeDocId(note.id))
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "itemId" to sanitizeDocId(note.itemId),
                "body" to note.body.take(20000),
                "isPinned" to note.isPinned,
                "localVersion" to note.localVersion.coerceAtLeast(1).toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            note.deletedAt?.let { payload["deletedAtMillis"] = it }

            if (existing.exists()) {
                payload.remove("userId")
                payload.remove("itemId")
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchAllCloudNotes(): Result<List<CloudNote>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val colRef = db.collection("users").document(uid).collection("notes")
        return try {
            val snap = colRef.get().await()
            Result.success(
                snap.toObjects(
                    CloudNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushCollection(collection: CollectionEntity, itemIds: List<String>): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("collections").document(sanitizeDocId(collection.id))
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "name" to collection.name.take(120).ifBlank { "Collection" },
                "itemIds" to itemIds.take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            collection.description.takeIf { it.isNotBlank() }?.let { payload["description"] = it.take(1000) }
            collection.deletedAt?.let { payload["deletedAtMillis"] = it }

            if (existing.exists()) {
                payload.remove("userId")
                docRef.update(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Real-Time Multi-Device Presence & Instant Send-to-Device Transfers ---
    suspend fun registerOrHeartbeatDevice(
        deviceId: String,
        deviceName: String,
        platform: String = "android",
        status: String = "online"
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("devices").document(sanitizeDocId(deviceId))
        return try {
            val existing = docRef.get().await()
            val validPlatform = if (platform in setOf("android", "web", "desktop")) platform else "android"
            val validStatus = if (status in setOf("online", "idle")) status else "online"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "deviceName" to deviceName.take(120).ifBlank { "Android Device" },
                "platform" to validPlatform,
                "status" to validStatus,
                "lastSeenAt" to FieldValue.serverTimestamp()
            )
            if (existing.exists()) {
                payload.remove("userId")
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeDevices(userId: String): Flow<List<CloudDeviceSession>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Scoped userId mismatch" }
        val path = "users/$uid/devices"
        db.collection("users").document(uid).collection("devices")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CloudDeviceSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val ex = fsError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.LIST, path)
                }
            }
            .collect { emit(it) }
    }

    suspend fun sendDeviceTransfer(
        transferId: String,
        title: String,
        payloadText: String,
        payloadType: String,
        fromDeviceName: String,
        targetDeviceId: String? = null
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("transfers").document(sanitizeDocId(transferId))
        return try {
            val validType = if (payloadType in setOf("link", "note", "file_ref")) payloadType else "note"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to title.take(300).ifBlank { "Shared Snippet" },
                "payloadText" to payloadText.take(10000).ifBlank { title.take(300) },
                "payloadType" to validType,
                "fromDeviceName" to fromDeviceName.take(120).ifBlank { "Android" },
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            targetDeviceId?.takeIf { it.isNotBlank() }?.let { payload["targetDeviceId"] = sanitizeDocId(it) }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markTransferReceived(transferId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("transfers").document(sanitizeDocId(transferId))
        return try {
            docRef.update(
                mapOf(
                    "status" to "received",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeTransfers(userId: String): Flow<List<CloudDeviceTransfer>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Scoped userId mismatch" }
        val path = "users/$uid/transfers"
        db.collection("users").document(uid).collection("transfers")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CloudDeviceTransfer::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val ex = fsError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.LIST, path)
                }
            }
            .collect { emit(it) }
    }

    // --- Real-Time Synced Reviews ---
    suspend fun pushReview(
        reviewId: String,
        itemId: String?,
        authorName: String,
        rating: Int,
        body: String,
        helpfulCount: Int
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("reviews").document(sanitizeDocId(reviewId))
        return try {
            val existing = docRef.get().await()
            val clampedRating = rating.coerceIn(1, 5)
            val clampedHelpful = helpfulCount.coerceAtLeast(0)
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "rating" to clampedRating,
                        "body" to body.take(2000).ifBlank { "Great resource" },
                        "helpfulCount" to clampedHelpful,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "authorName" to authorName.take(120).ifBlank { "Library Member" },
                    "rating" to clampedRating,
                    "body" to body.take(2000).ifBlank { "Great resource" },
                    "helpfulCount" to clampedHelpful,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                itemId?.takeIf { it.isNotBlank() }?.let { payload["itemId"] = sanitizeDocId(it) }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeReviews(userId: String): Flow<List<CloudReview>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Scoped userId mismatch" }
        val path = "users/$uid/reviews"
        db.collection("users").document(uid).collection("reviews")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CloudReview::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val ex = fsError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.LIST, path)
                }
            }
            .collect { emit(it) }
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        return cleaned.ifBlank { "doc_${System.currentTimeMillis()}" }
    }

    private fun validateContentType(raw: String): String {
        return if (raw in setOf("link", "video", "github", "article", "document", "image", "note", "local_file")) raw else "link"
    }

    private fun validateSourceType(raw: String): String {
        return if (raw in setOf("youtube", "github", "web", "saf_file", "photo_picker", "note", "share_intent", "manual")) raw else "manual"
    }

    private fun validateStatus(raw: String): String {
        return if (raw in setOf("unsorted", "processing", "needs_review", "failed_metadata", "duplicate_candidate", "organized", "archived")) raw else "unsorted"
    }
}

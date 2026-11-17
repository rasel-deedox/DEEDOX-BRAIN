package com.example.data.cloud

import android.content.Context
import com.example.R
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
import com.example.data.repository.HabitGamificationSummary
import com.example.data.repository.PollVotesCodec
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@IgnoreExtraProperties
data class CloudGamificationStats(
    @DocumentId val id: String = "stats",
    val userId: String = "",
    val totalXp: Int = 0,
    val level: Int = 1,
    val rankTitle: String = "Seedling",
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudStartupProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val tagline: String = "",
    val stage: String = "IDEATION",
    val projectType: String = "STARTUP",
    val targetPersona: String = "",
    val techStack: String = "",
    val aiPlanMarkdown: String = "",
    val phasesJson: String = "[]",
    val sprintTasksJson: String = "[]",
    val crmDealsJson: String = "[]",
    val okrsJson: String = "[]",
    val sopDocsJson: String = "[]",
    val sharedTeamId: String? = null,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudFinanceSector(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "wallet",
    val colorHex: String = "#E53935",
    val typeAffinity: String = "BOTH",
    val isCustom: Boolean = false,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudFinanceTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = "EXPENSE",
    val amountCents: Long = 0L,
    val sectorId: String = "",
    val sectorName: String = "",
    val paymentMethod: String = "BANK",
    val notes: String = "",
    val occurredAtMillis: Long = 0L,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudTeamInvitePreview(
    val inviteCode: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val ownerId: String = "",
    val maxMembers: Int = 5
)

class CloudOsSync(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required for Cloud Sync & Team Collaboration.")
    }

    private fun sanitizeId(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(128)
            .ifBlank { "doc_${System.currentTimeMillis()}" }
    }

    // =========================================================================
    // 1. Habit Gamification Cloud Sync
    // =========================================================================

    suspend fun pushGamificationStats(summary: HabitGamificationSummary): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("gamification").document("stats")
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "totalXp" to summary.totalXp.coerceAtLeast(0),
                "level" to summary.level.coerceAtLeast(1),
                "rankTitle" to summary.rankTitle.take(64).ifBlank { "Seedling" },
                "bestStreak" to summary.bestStreak.coerceAtLeast(0),
                "totalCompletions" to summary.totalCompletions.coerceAtLeast(0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
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

    suspend fun fetchGamificationStats(): Result<CloudGamificationStats?> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("gamification").document("stats")
        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) return Result.success(null)
            Result.success(
                CloudGamificationStats(
                    id = doc.id,
                    userId = doc.getString("userId").orEmpty(),
                    totalXp = (doc.getLong("totalXp") ?: 0L).toInt(),
                    level = (doc.getLong("level") ?: 1L).toInt().coerceAtLeast(1),
                    rankTitle = doc.getString("rankTitle") ?: "Seedling",
                    bestStreak = (doc.getLong("bestStreak") ?: 0L).toInt(),
                    totalCompletions = (doc.getLong("totalCompletions") ?: 0L).toInt(),
                    createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                    updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. Startup & Business OS Cloud Sync
    // =========================================================================

    suspend fun pushStartupProject(
        project: StartupProjectEntity,
        phases: List<StartupPhaseItemEntity>,
        sprintTasks: List<StartupSprintTaskEntity>
    ): Result<Unit> {
        if (!project.cloudSync) return Result.success(Unit)
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("startups").document(sanitizeId(project.id))
        return try {
            val existing = docRef.get().await()
            val validStage = if (project.stage in VALID_STARTUP_STAGES) project.stage else "IDEATION"
            val validType = if (project.projectType in VALID_PROJECT_TYPES) project.projectType else "STARTUP"
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "name" to project.name.take(160).ifBlank { "Untitled Startup" },
                "tagline" to project.tagline.take(300),
                "stage" to validStage,
                "projectType" to validType,
                "targetPersona" to project.targetPersona.take(1000),
                "techStack" to project.techStack.take(1000),
                "aiPlanMarkdown" to project.aiPlanMarkdown.take(50000),
                "phasesJson" to encodePhasesJson(phases).take(50000),
                "sprintTasksJson" to encodeSprintTasksJson(sprintTasks).take(50000),
                "crmDealsJson" to project.crmDealsJson.take(50000).ifBlank { "[]" },
                "okrsJson" to project.okrsJson.take(50000).ifBlank { "[]" },
                "sopDocsJson" to project.sopDocsJson.take(50000).ifBlank { "[]" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            project.sharedTeamId?.trim()?.takeIf { it.isNotBlank() }?.let {
                payload["sharedTeamId"] = it.take(128)
            }
            project.deletedAt?.let { payload["deletedAtMillis"] = it }

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

    suspend fun fetchStartupProjects(): Result<List<CloudStartupProject>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val colRef = db.collection("users").document(uid).collection("startups")
        return try {
            val snap = colRef.get().await()
            val list = snap.documents.map { doc ->
                CloudStartupProject(
                    id = doc.id,
                    userId = doc.getString("userId").orEmpty(),
                    name = doc.getString("name").orEmpty(),
                    tagline = doc.getString("tagline").orEmpty(),
                    stage = doc.getString("stage") ?: "IDEATION",
                    projectType = doc.getString("projectType") ?: "STARTUP",
                    targetPersona = doc.getString("targetPersona").orEmpty(),
                    techStack = doc.getString("techStack").orEmpty(),
                    aiPlanMarkdown = doc.getString("aiPlanMarkdown").orEmpty(),
                    phasesJson = doc.getString("phasesJson") ?: "[]",
                    sprintTasksJson = doc.getString("sprintTasksJson") ?: "[]",
                    crmDealsJson = doc.getString("crmDealsJson") ?: "[]",
                    okrsJson = doc.getString("okrsJson") ?: "[]",
                    sopDocsJson = doc.getString("sopDocsJson") ?: "[]",
                    sharedTeamId = doc.getString("sharedTeamId"),
                    deletedAtMillis = doc.getLong("deletedAtMillis"),
                    createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                    updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. Money Management & Custom Sector Ledger Sync
    // =========================================================================

    suspend fun pushFinanceSector(sector: FinanceSectorEntity): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("finance_sectors").document(sanitizeId(sector.id))
        return try {
            val existing = docRef.get().await()
            val validAffinity = if (sector.typeAffinity in VALID_TYPE_AFFINITIES) sector.typeAffinity else "BOTH"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "name" to sector.name.take(80).ifBlank { "General" },
                "iconKey" to sector.iconKey.take(48).ifBlank { "wallet" },
                "colorHex" to sector.colorHex.take(16).ifBlank { "#E53935" },
                "typeAffinity" to validAffinity,
                "isCustom" to sector.isCustom,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            sector.deletedAt?.let { payload["deletedAtMillis"] = it }

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

    suspend fun fetchFinanceSectors(): Result<List<CloudFinanceSector>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val colRef = db.collection("users").document(uid).collection("finance_sectors")
        return try {
            val snap = colRef.get().await()
            val list = snap.documents.map { doc ->
                CloudFinanceSector(
                    id = doc.id,
                    userId = doc.getString("userId").orEmpty(),
                    name = doc.getString("name").orEmpty(),
                    iconKey = doc.getString("iconKey") ?: "wallet",
                    colorHex = doc.getString("colorHex") ?: "#E53935",
                    typeAffinity = doc.getString("typeAffinity") ?: "BOTH",
                    isCustom = doc.getBoolean("isCustom") ?: false,
                    deletedAtMillis = doc.getLong("deletedAtMillis"),
                    createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                    updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushFinanceTransaction(tx: FinanceTransactionEntity): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("finance_transactions").document(sanitizeId(tx.id))
        return try {
            val existing = docRef.get().await()
            val validType = if (tx.txType in VALID_TX_TYPES) tx.txType else "EXPENSE"
            val validMethod = if (tx.paymentMethod in VALID_PAYMENT_METHODS) tx.paymentMethod else "BANK"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "txType" to validType,
                "amountCents" to tx.amountCents.coerceAtLeast(0L),
                "sectorId" to sanitizeId(tx.sectorId),
                "sectorName" to tx.sectorName.take(80).ifBlank { "General" },
                "paymentMethod" to validMethod,
                "notes" to tx.notes.take(2000),
                "occurredAtMillis" to tx.occurredAtMillis.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            tx.deletedAt?.let { payload["deletedAtMillis"] = it }

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

    suspend fun fetchFinanceTransactions(): Result<List<CloudFinanceTransaction>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val colRef = db.collection("users").document(uid).collection("finance_transactions")
        return try {
            val snap = colRef.get().await()
            val list = snap.documents.map { doc ->
                CloudFinanceTransaction(
                    id = doc.id,
                    userId = doc.getString("userId").orEmpty(),
                    txType = doc.getString("txType") ?: "EXPENSE",
                    amountCents = doc.getLong("amountCents") ?: 0L,
                    sectorId = doc.getString("sectorId").orEmpty(),
                    sectorName = doc.getString("sectorName").orEmpty(),
                    paymentMethod = doc.getString("paymentMethod") ?: "BANK",
                    notes = doc.getString("notes").orEmpty(),
                    occurredAtMillis = doc.getLong("occurredAtMillis") ?: 0L,
                    deletedAtMillis = doc.getLong("deletedAtMillis"),
                    createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                    updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 4. Real-Time Team & Group Collaboration
    // =========================================================================

    suspend fun createTeam(
        teamId: String,
        name: String,
        description: String,
        inviteCode: String,
        maxMembers: Int = 5,
        ownerName: String,
        ownerEmail: String,
        ownerPhotoUrl: String = ""
    ): Result<TeamWorkspaceEntity> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanTeamId = sanitizeId(teamId)
        val cleanCode = inviteCode.trim().uppercase().replace(Regex("[^A-Z0-9\\-]"), "").take(32)
            .ifBlank { "DEEDOX-${(100..999).random()}" }
        val cleanName = name.trim().take(120).ifBlank { "New Team" }
        val cleanDesc = description.trim().take(600)
        val clampedMax = maxMembers.coerceIn(2, 100)

        val teamRef = db.collection("teams").document(cleanTeamId)
        val inviteRef = db.collection("team_invites").document(cleanCode)
        val memberRef = teamRef.collection("members").document(uid)

        return try {
            teamRef.set(
                mapOf(
                    "ownerId" to uid,
                    "name" to cleanName,
                    "description" to cleanDesc,
                    "inviteCode" to cleanCode,
                    "maxMembers" to clampedMax,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            inviteRef.set(
                mapOf(
                    "teamId" to cleanTeamId,
                    "teamName" to cleanName,
                    "ownerId" to uid,
                    "maxMembers" to clampedMax,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val now = System.currentTimeMillis()
            memberRef.set(
                mapOf(
                    "uid" to uid,
                    "displayName" to ownerName.trim().take(120).ifBlank { "Team Owner" },
                    "email" to ownerEmail.trim().take(200),
                    "photoUrl" to ownerPhotoUrl.trim().take(1024),
                    "role" to "OWNER",
                    "roleTag" to "Team Lead",
                    "customStatus" to "Available",
                    "isOnline" to true,
                    "lastActiveAtMillis" to now,
                    "joinedAtMillis" to now,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                TeamWorkspaceEntity(
                    id = cleanTeamId,
                    ownerId = uid,
                    name = cleanName,
                    description = cleanDesc,
                    inviteCode = cleanCode,
                    maxMembers = clampedMax,
                    memberUidsCsv = uid,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, teamRef.path)
            Result.failure(e)
        }
    }

    suspend fun lookupTeamByInviteCode(inviteCode: String): Result<CloudTeamInvitePreview> {
        try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanCode = inviteCode.trim().uppercase().replace(Regex("[^A-Z0-9\\-]"), "")
        if (cleanCode.length < 4) {
            return Result.failure(IllegalArgumentException("Enter a valid invite code (at least 4 characters)."))
        }
        val inviteRef = db.collection("team_invites").document(cleanCode)
        return try {
            val doc = inviteRef.get().await()
            if (!doc.exists()) {
                return Result.failure(IllegalStateException("No team found for invite code \"$cleanCode\"."))
            }
            Result.success(
                CloudTeamInvitePreview(
                    inviteCode = doc.id,
                    teamId = doc.getString("teamId").orEmpty(),
                    teamName = doc.getString("teamName").orEmpty(),
                    ownerId = doc.getString("ownerId").orEmpty(),
                    maxMembers = (doc.getLong("maxMembers") ?: 5L).toInt()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, inviteRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinTeamByInviteCode(
        inviteCode: String,
        memberName: String,
        memberEmail: String,
        memberPhotoUrl: String = ""
    ): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val preview = lookupTeamByInviteCode(inviteCode).getOrElse { return Result.failure(it) }
        val teamId = preview.teamId.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Invite is missing target team ID."))

        val teamRef = db.collection("teams").document(teamId)
        val memberRef = teamRef.collection("members").document(uid)
        return try {
            // Self-append strictly modifies ONLY ['memberUids', 'updatedAt'] per zero-trust rule
            teamRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val now = System.currentTimeMillis()
            val cleanName = memberName.trim().take(120).ifBlank { "Member" }
            val cleanEmail = memberEmail.trim().take(200)
            val cleanPhoto = memberPhotoUrl.trim().take(1024)
            val existingMember = memberRef.get().await()
            if (existingMember.exists()) {
                memberRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "email" to cleanEmail,
                        "photoUrl" to cleanPhoto,
                        "isOnline" to true,
                        "lastActiveAtMillis" to now,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                memberRef.set(
                    mapOf(
                        "uid" to uid,
                        "displayName" to cleanName,
                        "email" to cleanEmail,
                        "photoUrl" to cleanPhoto,
                        "role" to if (preview.ownerId == uid) "OWNER" else "MEMBER",
                        "roleTag" to if (preview.ownerId == uid) "Team Lead" else "Member",
                        "customStatus" to "Available",
                        "isOnline" to true,
                        "lastActiveAtMillis" to now,
                        "joinedAtMillis" to now,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(teamId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, teamRef.path)
            Result.failure(
                IllegalStateException(
                    if (e.localizedMessage?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                        "Unable to join team — the group may be at its member limit (${preview.maxMembers}). Ask the owner to expand team size."
                    } else {
                        e.localizedMessage ?: "Failed to join team."
                    }
                )
            )
        }
    }

    suspend fun expandTeamMaxMembers(
        teamId: String,
        inviteCode: String,
        newMaxMembers: Int
    ): Result<Unit> {
        try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val clamped = newMaxMembers.coerceIn(2, 100)
        val cleanTeamId = sanitizeId(teamId)
        val cleanCode = inviteCode.trim().uppercase()
        val teamRef = db.collection("teams").document(cleanTeamId)
        return try {
            teamRef.update(
                mapOf(
                    "maxMembers" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (cleanCode.length >= 4) {
                db.collection("team_invites").document(cleanCode).update(
                    mapOf(
                        "maxMembers" to clamped,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, teamRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateMemberPresence(
        teamId: String,
        displayName: String,
        email: String,
        isOnline: Boolean,
        photoUrl: String = ""
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val memberRef = db.collection("teams").document(sanitizeId(teamId))
            .collection("members").document(uid)
        return try {
            val now = System.currentTimeMillis()
            val cleanPhoto = photoUrl.trim().take(1024)
            val existing = memberRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName.trim().take(120).ifBlank { "Member" },
                    "email" to email.trim().take(200),
                    "isOnline" to isOnline,
                    "lastActiveAtMillis" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanPhoto.isNotBlank()) updates["photoUrl"] = cleanPhoto
                memberRef.update(updates).await()
            } else {
                memberRef.set(
                    mapOf(
                        "uid" to uid,
                        "displayName" to displayName.trim().take(120).ifBlank { "Member" },
                        "email" to email.trim().take(200),
                        "photoUrl" to cleanPhoto,
                        "role" to "MEMBER",
                        "roleTag" to "Member",
                        "customStatus" to "Available",
                        "isOnline" to isOnline,
                        "lastActiveAtMillis" to now,
                        "joinedAtMillis" to now,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, memberRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTeamMemberProfile(
        teamId: String,
        targetUid: String,
        roleTag: String,
        customStatus: String,
        role: String? = null
    ): Result<Unit> {
        try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val memberRef = db.collection("teams").document(sanitizeId(teamId))
            .collection("members").document(sanitizeId(targetUid))
        return try {
            val updates = mutableMapOf<String, Any>(
                "roleTag" to roleTag.trim().take(60),
                "customStatus" to customStatus.trim().take(80).ifBlank { "Available" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (role != null && role in setOf("OWNER", "ADMIN", "MEMBER")) {
                updates["role"] = role
            }
            memberRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeTeamMember(teamId: String, targetUid: String): Result<Unit> {
        try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanTeamId = sanitizeId(teamId)
        val cleanTargetUid = sanitizeId(targetUid)
        val teamRef = db.collection("teams").document(cleanTeamId)
        val memberRef = teamRef.collection("members").document(cleanTargetUid)
        return try {
            teamRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(cleanTargetUid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            memberRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, teamRef.path)
            Result.failure(e)
        }
    }

    suspend fun inviteTeamMemberByEmail(
        teamId: String,
        teamName: String,
        inviteCode: String,
        email: String,
        invitedRole: String = "MEMBER",
        roleTag: String = "Member",
        invitedByName: String
    ): Result<TeamEmailInviteEntity> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            return Result.failure(IllegalArgumentException("Enter a valid Gmail address."))
        }
        val cleanTeamId = sanitizeId(teamId)
        val cleanCode = inviteCode.trim().uppercase().take(32).ifBlank { "DEEDOX-000" }
        val validRole = if (invitedRole in setOf("ADMIN", "MEMBER")) invitedRole else "MEMBER"
        val cleanTag = roleTag.trim().take(60).ifBlank { if (validRole == "ADMIN") "Admin" else "Member" }
        val inviteId = sanitizeId("inv_${cleanTeamId}_${UUID.randomUUID().toString().take(8)}")
        val now = System.currentTimeMillis()
        val inviteRef = db.collection("team_email_invites").document(inviteId)
        return try {
            inviteRef.set(
                mapOf(
                    "teamId" to cleanTeamId,
                    "teamName" to teamName.trim().take(120).ifBlank { "Team Workspace" },
                    "inviteCode" to cleanCode,
                    "invitedEmail" to cleanEmail.take(200),
                    "invitedRole" to validRole,
                    "roleTag" to cleanTag,
                    "invitedByUid" to uid,
                    "invitedByName" to invitedByName.trim().take(120).ifBlank { "Team Admin" },
                    "status" to "PENDING",
                    "createdAtMillis" to now,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(
                TeamEmailInviteEntity(
                    id = inviteId,
                    teamId = cleanTeamId,
                    teamName = teamName.trim().take(120).ifBlank { "Team Workspace" },
                    inviteCode = cleanCode,
                    invitedEmail = cleanEmail.take(200),
                    invitedRole = validRole,
                    roleTag = cleanTag,
                    invitedByUid = uid,
                    invitedByName = invitedByName.trim().take(120).ifBlank { "Team Admin" },
                    status = "PENDING",
                    createdAtMillis = now
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, inviteRef.path)
            Result.failure(e)
        }
    }

    suspend fun acceptEmailInvite(
        invite: TeamEmailInviteEntity,
        memberName: String,
        memberEmail: String,
        memberPhotoUrl: String = ""
    ): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val joinedTeamId = joinTeamByInviteCode(
            inviteCode = invite.inviteCode,
            memberName = memberName,
            memberEmail = memberEmail,
            memberPhotoUrl = memberPhotoUrl
        ).getOrElse { return Result.failure(it) }
        val inviteRef = db.collection("team_email_invites").document(sanitizeId(invite.id))
        return try {
            inviteRef.update(
                mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (invite.roleTag.isNotBlank()) {
                db.collection("teams").document(joinedTeamId)
                    .collection("members").document(uid)
                    .update(
                        mapOf(
                            "roleTag" to invite.roleTag.take(60),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(joinedTeamId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, inviteRef.path)
            Result.success(joinedTeamId)
        }
    }

    suspend fun revokeEmailInvite(inviteId: String): Result<Unit> {
        try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val inviteRef = db.collection("team_email_invites").document(sanitizeId(inviteId))
        return try {
            inviteRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, inviteRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushSharedTask(task: TeamSharedTaskEntity): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanTeamId = sanitizeId(task.teamId)
        val docRef = db.collection("teams").document(cleanTeamId)
            .collection("schedules").document(sanitizeId(task.id))
        return try {
            val existing = docRef.get().await()
            val validPriority = if (task.priority in setOf("HIGH", "MEDIUM", "LOW")) task.priority else "MEDIUM"
            val validDay = if (task.dayOfWeek in VALID_DAYS_OF_WEEK) task.dayOfWeek else "Mon"
            val validStatus = when {
                task.status in VALID_SCHEDULE_STATUSES -> task.status
                task.isCompleted -> "done"
                else -> "todo"
            }
            val assignedUids = task.assignedToUidsCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOfNotNull(task.assigneeUid.trim().takeIf { it.isNotBlank() }) }
                .distinct()
                .take(50)
            val assignedNames = task.assignedToNamesCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOfNotNull(task.assigneeName.trim().takeIf { it.isNotBlank() }) }
                .take(50)

            val payload = mutableMapOf<String, Any?>(
                "teamId" to cleanTeamId,
                "title" to task.title.trim().take(240).ifBlank { "Scheduled Task" },
                "description" to task.description.trim().take(2000),
                "dayOfWeek" to validDay,
                "dateLabel" to task.dateLabel.trim().take(60),
                "startTime" to task.startTime.trim().take(32),
                "endTime" to task.endTime.trim().take(32),
                "assignedTo" to assignedUids,
                "assignedToNames" to assignedNames,
                "status" to validStatus,
                "priority" to validPriority,
                "createdByUid" to task.createdByUid.ifBlank { uid },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            task.deadlineMillis?.takeIf { it >= 0L }?.let { payload["deadlineMillis"] = it }
            task.linkedDriveUrl?.trim()?.takeIf { it.isNotBlank() }?.let { payload["linkedDriveUrl"] = it.take(2048) }
            task.sourceBusinessId?.trim()?.takeIf { it.isNotBlank() }?.let { payload["sourceBusinessId"] = it.take(128) }

            if (existing.exists()) {
                payload.remove("teamId")
                payload.remove("createdByUid")
                if (task.deadlineMillis == null) payload["deadlineMillis"] = null
                if (task.linkedDriveUrl.isNullOrBlank()) payload["linkedDriveUrl"] = null
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

    suspend fun deleteSharedTask(teamId: String, taskId: String): Result<Unit> {
        try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("teams").document(sanitizeId(teamId))
            .collection("schedules").document(sanitizeId(taskId))
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun pushSharedDoc(doc: TeamSharedDocEntity): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanTeamId = sanitizeId(doc.teamId)
        val docRef = db.collection("teams").document(cleanTeamId)
            .collection("shared_notes").document(sanitizeId(doc.id))
        return try {
            val existing = docRef.get().await()
            val editorName = doc.lastEditedBy.trim().take(120)
                .ifBlank { doc.authorName.trim().take(120).ifBlank { "Teammate" } }
            val payload = mutableMapOf<String, Any?>(
                "teamId" to cleanTeamId,
                "title" to doc.title.trim().take(240).ifBlank { "Shared Note" },
                "content" to doc.content.trim().take(20000),
                "blocksJson" to doc.blocksJson.take(50000).ifBlank { "[]" },
                "tagsCsv" to doc.tagsCsv.trim().take(500),
                "icon" to doc.icon.trim().take(16).ifBlank { "📄" },
                "isPinned" to doc.isPinned,
                "authorUid" to doc.authorUid.ifBlank { uid },
                "authorName" to doc.authorName.trim().take(120).ifBlank { "Teammate" },
                "lastEditedBy" to editorName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            doc.resourceUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
                payload["resourceUrl"] = it.take(2048)
            }
            doc.parentId?.trim()?.takeIf { it.isNotBlank() && it != doc.id }?.let {
                payload["parentId"] = it.take(128)
            }

            if (existing.exists()) {
                payload.remove("teamId")
                payload.remove("authorUid")
                payload.remove("authorName")
                if (doc.parentId.isNullOrBlank()) payload["parentId"] = null
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

    suspend fun deleteSharedDoc(teamId: String, docId: String): Result<Unit> {
        try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("teams").document(sanitizeId(teamId))
            .collection("shared_notes").document(sanitizeId(docId))
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendTeamNotification(notif: TeamNotificationEntity): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanTeamId = sanitizeId(notif.teamId)
        val docRef = db.collection("teams").document(cleanTeamId)
            .collection("notices").document(sanitizeId(notif.id))
        val taggedUids = notif.taggedUidsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(50)
        val taggedNames = notif.taggedNamesCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(50)
        val validNoticeType = if (notif.noticeType == "poll") "poll" else "notice"
        return try {
            docRef.set(
                mapOf(
                    "teamId" to cleanTeamId,
                    "title" to notif.title.trim().take(200).ifBlank { "Team Notice" },
                    "message" to notif.message.trim().take(1000).ifBlank { "Check the shared workspace." },
                    "authorId" to uid,
                    "authorName" to notif.senderName.trim().take(120).ifBlank { "Teammate" },
                    "taggedUsers" to taggedUids,
                    "taggedNames" to taggedNames,
                    "noticeType" to validNoticeType,
                    "pollOptionsJson" to notif.pollOptionsJson.take(10000).ifBlank { "[]" },
                    "pollVotesJson" to notif.pollVotesJson.take(20000).ifBlank { "{}" },
                    "createdAtMillis" to notif.createdAtMillis.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun voteOnTeamPoll(teamId: String, noticeId: String, optionIndex: Int): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("teams").document(sanitizeId(teamId))
            .collection("notices").document(sanitizeId(noticeId))
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) return Result.failure(IllegalStateException("Poll not found."))
            val currentVotes = snap.getString("pollVotesJson") ?: "{}"
            val updatedVotes = PollVotesCodec.vote(currentVotes, uid, optionIndex)
            docRef.update("pollVotesJson", updatedVotes).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun logTeamActivity(activity: TeamActivityEntity): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanTeamId = sanitizeId(activity.teamId)
        val cleanActId = sanitizeId(activity.id)
        val validType = if (activity.actionType in VALID_ACTIVITY_TYPES) activity.actionType else "TEAM"
        val docRef = db.collection("teams").document(cleanTeamId)
            .collection("activity").document(cleanActId)
        return try {
            val payload = mutableMapOf<String, Any>(
                "teamId" to cleanTeamId,
                "actorUid" to uid,
                "actorName" to activity.actorName.trim().take(120).ifBlank { "Teammate" },
                "actionType" to validType,
                "summary" to activity.summary.trim().take(500).ifBlank { "Workspace updated" },
                "createdAtMillis" to activity.createdAtMillis.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            activity.targetId.trim().takeIf { it.isNotBlank() }?.let {
                payload["targetId"] = it.take(128)
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Real-Time Snapshot Listeners ---

    fun observeMyTeams(uid: String): Flow<List<TeamWorkspaceEntity>> = callbackFlow {
        val reg = db.collection("teams")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { doc ->
                    val members = (doc.get("memberUids") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    TeamWorkspaceEntity(
                        id = doc.id,
                        ownerId = doc.getString("ownerId").orEmpty(),
                        name = doc.getString("name").orEmpty(),
                        description = doc.getString("description").orEmpty(),
                        inviteCode = doc.getString("inviteCode").orEmpty(),
                        maxMembers = (doc.getLong("maxMembers") ?: 5L).toInt(),
                        memberUidsCsv = members.joinToString(","),
                        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }.sortedByDescending { it.updatedAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTeamMembers(teamId: String): Flow<List<TeamMemberEntity>> = callbackFlow {
        val cleanTeamId = sanitizeId(teamId)
        val reg = db.collection("teams").document(cleanTeamId)
            .collection("members")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents.orEmpty().map { doc ->
                    TeamMemberEntity(
                        teamId = cleanTeamId,
                        uid = doc.getString("uid") ?: doc.id,
                        displayName = doc.getString("displayName") ?: "Member",
                        email = doc.getString("email").orEmpty(),
                        photoUrl = doc.getString("photoUrl").orEmpty(),
                        role = doc.getString("role") ?: "MEMBER",
                        roleTag = doc.getString("roleTag").orEmpty(),
                        customStatus = doc.getString("customStatus").orEmpty().ifBlank { "Available" },
                        isOnline = doc.getBoolean("isOnline") ?: false,
                        lastActiveAtMillis = doc.getLong("lastActiveAtMillis") ?: 0L,
                        joinedAtMillis = doc.getLong("joinedAtMillis") ?: 0L
                    )
                }.sortedWith(compareBy<TeamMemberEntity> { it.role != "OWNER" }.thenBy { it.role != "ADMIN" }.thenBy { it.displayName.lowercase() })
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTeamTasks(teamId: String): Flow<List<TeamSharedTaskEntity>> = callbackFlow {
        val cleanTeamId = sanitizeId(teamId)
        val reg = db.collection("teams").document(cleanTeamId)
            .collection("schedules")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents.orEmpty().map { doc ->
                    val assignedUids = (doc.get("assignedTo") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    val assignedNames = (doc.get("assignedToNames") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    val status = doc.getString("status") ?: "todo"
                    TeamSharedTaskEntity(
                        id = doc.id,
                        teamId = cleanTeamId,
                        title = doc.getString("title").orEmpty(),
                        description = doc.getString("description").orEmpty(),
                        dayOfWeek = doc.getString("dayOfWeek") ?: "Mon",
                        dateLabel = doc.getString("dateLabel").orEmpty(),
                        startTime = doc.getString("startTime").orEmpty().ifBlank { "10:00 AM" },
                        endTime = doc.getString("endTime").orEmpty().ifBlank { "12:00 PM" },
                        assigneeUid = assignedUids.firstOrNull().orEmpty(),
                        assigneeName = assignedNames.joinToString(", "),
                        assignedToUidsCsv = assignedUids.joinToString(","),
                        assignedToNamesCsv = assignedNames.joinToString(","),
                        status = status,
                        priority = doc.getString("priority") ?: "MEDIUM",
                        isCompleted = status == "done",
                        createdByUid = doc.getString("createdByUid").orEmpty(),
                        deadlineMillis = doc.getLong("deadlineMillis"),
                        linkedDriveUrl = doc.getString("linkedDriveUrl"),
                        sourceBusinessId = doc.getString("sourceBusinessId"),
                        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }.sortedWith(
                    compareBy<TeamSharedTaskEntity> { it.status == "done" }
                        .thenByDescending { it.updatedAt }
                )
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTeamDocs(teamId: String): Flow<List<TeamSharedDocEntity>> = callbackFlow {
        val cleanTeamId = sanitizeId(teamId)
        val reg = db.collection("teams").document(cleanTeamId)
            .collection("shared_notes")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents.orEmpty().map { doc ->
                    val authorName = doc.getString("authorName") ?: "Teammate"
                    TeamSharedDocEntity(
                        id = doc.id,
                        teamId = cleanTeamId,
                        title = doc.getString("title").orEmpty(),
                        content = doc.getString("content").orEmpty(),
                        blocksJson = doc.getString("blocksJson").orEmpty().ifBlank { "[]" },
                        tagsCsv = doc.getString("tagsCsv").orEmpty(),
                        resourceUrl = doc.getString("resourceUrl"),
                        parentId = doc.getString("parentId")?.takeIf { it.isNotBlank() },
                        icon = doc.getString("icon").orEmpty().ifBlank { "📄" },
                        isPinned = doc.getBoolean("isPinned") ?: false,
                        authorUid = doc.getString("authorUid").orEmpty(),
                        authorName = authorName,
                        lastEditedBy = doc.getString("lastEditedBy").orEmpty().ifBlank { authorName },
                        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: System.currentTimeMillis(),
                        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }.sortedWith(compareByDescending<TeamSharedDocEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTeamNotifications(teamId: String): Flow<List<TeamNotificationEntity>> = callbackFlow {
        val cleanTeamId = sanitizeId(teamId)
        val reg = db.collection("teams").document(cleanTeamId)
            .collection("notices")
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(40)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents.orEmpty().map { doc ->
                    val taggedUids = (doc.get("taggedUsers") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    val taggedNames = (doc.get("taggedNames") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    TeamNotificationEntity(
                        id = doc.id,
                        teamId = cleanTeamId,
                        senderUid = doc.getString("authorId").orEmpty(),
                        senderName = doc.getString("authorName") ?: "Teammate",
                        title = doc.getString("title").orEmpty(),
                        message = doc.getString("message").orEmpty(),
                        taggedUidsCsv = taggedUids.joinToString(","),
                        taggedNamesCsv = taggedNames.joinToString(","),
                        noticeType = doc.getString("noticeType") ?: "notice",
                        pollOptionsJson = doc.getString("pollOptionsJson") ?: "[]",
                        pollVotesJson = doc.getString("pollVotesJson") ?: "{}",
                        createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTeamActivities(teamId: String): Flow<List<TeamActivityEntity>> = callbackFlow {
        val cleanTeamId = sanitizeId(teamId)
        val reg = db.collection("teams").document(cleanTeamId)
            .collection("activity")
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents.orEmpty().map { doc ->
                    TeamActivityEntity(
                        id = doc.id,
                        teamId = cleanTeamId,
                        actorUid = doc.getString("actorUid").orEmpty(),
                        actorName = doc.getString("actorName") ?: "Teammate",
                        actionType = doc.getString("actionType") ?: "TEAM",
                        summary = doc.getString("summary").orEmpty(),
                        targetId = doc.getString("targetId").orEmpty(),
                        createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTeamEmailInvites(teamId: String): Flow<List<TeamEmailInviteEntity>> = callbackFlow {
        val cleanTeamId = sanitizeId(teamId)
        val reg = db.collection("team_email_invites")
            .whereEqualTo("teamId", cleanTeamId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { doc ->
                    TeamEmailInviteEntity(
                        id = doc.id,
                        teamId = doc.getString("teamId").orEmpty(),
                        teamName = doc.getString("teamName").orEmpty(),
                        inviteCode = doc.getString("inviteCode").orEmpty(),
                        invitedEmail = doc.getString("invitedEmail").orEmpty(),
                        invitedRole = doc.getString("invitedRole") ?: "MEMBER",
                        roleTag = doc.getString("roleTag").orEmpty(),
                        invitedByUid = doc.getString("invitedByUid").orEmpty(),
                        invitedByName = doc.getString("invitedByName").orEmpty(),
                        status = doc.getString("status") ?: "PENDING",
                        createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
                    )
                }.sortedByDescending { it.createdAtMillis }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeIncomingEmailInvites(email: String): Flow<List<TeamEmailInviteEntity>> = callbackFlow {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val reg = db.collection("team_email_invites")
            .whereEqualTo("invitedEmail", cleanEmail)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { doc ->
                    TeamEmailInviteEntity(
                        id = doc.id,
                        teamId = doc.getString("teamId").orEmpty(),
                        teamName = doc.getString("teamName").orEmpty(),
                        inviteCode = doc.getString("inviteCode").orEmpty(),
                        invitedEmail = doc.getString("invitedEmail").orEmpty(),
                        invitedRole = doc.getString("invitedRole") ?: "MEMBER",
                        roleTag = doc.getString("roleTag").orEmpty(),
                        invitedByUid = doc.getString("invitedByUid").orEmpty(),
                        invitedByName = doc.getString("invitedByName").orEmpty(),
                        status = doc.getString("status") ?: "PENDING",
                        createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
                    )
                }.sortedByDescending { it.createdAtMillis }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    companion object {
        private val VALID_STARTUP_STAGES = setOf("IDEATION", "MVP", "BETA", "LIVE", "SCALING")
        private val VALID_PROJECT_TYPES = setOf("STARTUP", "AGENCY", "PRODUCT")
        private val VALID_TYPE_AFFINITIES = setOf("INCOME", "EXPENSE", "BOTH")
        private val VALID_TX_TYPES = setOf("INCOME", "EXPENSE", "TRANSFER")
        private val VALID_PAYMENT_METHODS = setOf("CASH", "BANK", "MOBILE_BANKING", "CARD")
        private val VALID_DAYS_OF_WEEK = setOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
        private val VALID_SCHEDULE_STATUSES = setOf("backlog", "todo", "in_progress", "review", "done")
        private val VALID_ACTIVITY_TYPES = setOf("PAGE", "TASK", "MEMBER", "NOTICE", "BUSINESS_SYNC", "TEAM")

        fun encodePhasesJson(phases: List<StartupPhaseItemEntity>): String {
            val arr = JSONArray()
            phases.forEach { p ->
                arr.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("startupId", p.startupId)
                        put("phaseNumber", p.phaseNumber)
                        put("phaseTitle", p.phaseTitle)
                        put("title", p.title)
                        put("notes", p.notes)
                        p.resourceUrl?.let { put("resourceUrl", it) }
                        put("isCompleted", p.isCompleted)
                        put("sortOrder", p.sortOrder)
                    }
                )
            }
            return arr.toString()
        }

        fun decodePhasesJson(startupId: String, json: String): List<StartupPhaseItemEntity> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val arr = JSONArray(json)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        add(
                            StartupPhaseItemEntity(
                                id = obj.optString("id"),
                                startupId = startupId,
                                phaseNumber = obj.optInt("phaseNumber", 1),
                                phaseTitle = obj.optString("phaseTitle", "Phase 1"),
                                title = obj.optString("title", "Step"),
                                notes = obj.optString("notes", ""),
                                resourceUrl = obj.optString("resourceUrl", "").takeIf { it.isNotBlank() },
                                isCompleted = obj.optBoolean("isCompleted", false),
                                sortOrder = obj.optInt("sortOrder", i)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeSprintTasksJson(tasks: List<StartupSprintTaskEntity>): String {
            val arr = JSONArray()
            tasks.forEach { t ->
                arr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("startupId", t.startupId)
                        put("title", t.title)
                        put("description", t.description)
                        put("columnStatus", t.columnStatus)
                        put("priority", t.priority)
                        put("tag", t.tag)
                        put("sortOrder", t.sortOrder)
                    }
                )
            }
            return arr.toString()
        }

        fun decodeSprintTasksJson(startupId: String, json: String): List<StartupSprintTaskEntity> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val arr = JSONArray(json)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        add(
                            StartupSprintTaskEntity(
                                id = obj.optString("id"),
                                startupId = startupId,
                                title = obj.optString("title", "Sprint Task"),
                                description = obj.optString("description", ""),
                                columnStatus = obj.optString("columnStatus", "BACKLOG"),
                                priority = obj.optString("priority", "MEDIUM"),
                                tag = obj.optString("tag", "Core"),
                                sortOrder = obj.optInt("sortOrder", i)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

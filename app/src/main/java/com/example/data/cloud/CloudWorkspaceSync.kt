package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.data.local.HabitEntity
import com.example.data.local.SmartReminderEntity
import com.example.data.local.WorkspaceNoteEntity
import com.example.data.local.WorkspaceTaskEntity
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
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class CloudWorkspaceNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val blocksJson: String = "[]",
    val tagsCsv: String = "",
    @get:PropertyName("isPinned") @field:PropertyName("isPinned") val isPinned: Boolean = false,
    val linkedItemId: String? = null,
    val audioDurationMs: Long = 0L,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudWorkspaceTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String = "",
    val dueDateEpochDay: Long = 0L,
    val priority: String = "MEDIUM",
    @get:PropertyName("isCompleted") @field:PropertyName("isCompleted") val isCompleted: Boolean = false,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudSmartReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "PERSONAL",
    val title: String = "",
    val notes: String = "",
    val priority: String = "MEDIUM",
    val targetItemId: String? = null,
    val targetNoteId: String? = null,
    val triggerAtMillis: Long = 0L,
    val recurrenceType: String = "ONCE",
    val intervalMinutes: Int = 0,
    val ringtoneTitle: String = "Default Alert",
    @get:PropertyName("isEnabled") @field:PropertyName("isEnabled") val isEnabled: Boolean = true,
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudHabitRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val category: String = "Focus",
    val targetPerWeek: Int = 7,
    val colorHex: String = "#E53935",
    val completedEpochDays: List<Long> = emptyList(),
    val deletedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudWorkspaceSync(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required for Workspace Cloud Sync.")
    }

    private fun sanitizeId(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "doc_${System.currentTimeMillis()}" }
    }

    suspend fun pushWorkspaceNote(note: WorkspaceNoteEntity): Result<Unit> {
        if (!note.cloudSync) return Result.success(Unit)
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("workspace_notes").document(sanitizeId(note.id))
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to note.title.take(300).ifBlank { "Untitled Note" },
                "blocksJson" to note.blocksJson.take(50000),
                "tagsCsv" to note.tagsCsv.take(1000),
                "isPinned" to note.isPinned,
                "audioDurationMs" to note.audioDurationMs.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            note.linkedItemId?.takeIf { it.isNotBlank() }?.let { payload["linkedItemId"] = sanitizeId(it) }
            note.deletedAt?.let { payload["deletedAtMillis"] = it }

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

    suspend fun fetchWorkspaceNotes(): Result<List<CloudWorkspaceNote>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val colRef = db.collection("users").document(uid).collection("workspace_notes")
        return try {
            val snap = colRef.get().await()
            val list = snap.documents.map { doc ->
                CloudWorkspaceNote(
                    id = doc.id,
                    userId = doc.getString("userId").orEmpty(),
                    title = doc.getString("title").orEmpty(),
                    blocksJson = doc.getString("blocksJson") ?: "[]",
                    tagsCsv = doc.getString("tagsCsv").orEmpty(),
                    isPinned = doc.getBoolean("isPinned") ?: false,
                    linkedItemId = doc.getString("linkedItemId"),
                    audioDurationMs = doc.getLong("audioDurationMs") ?: 0L,
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

    suspend fun pushWorkspaceTask(task: WorkspaceTaskEntity): Result<Unit> {
        if (!task.cloudSync) return Result.success(Unit)
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("workspace_tasks").document(sanitizeId(task.id))
        return try {
            val existing = docRef.get().await()
            val validPriority = if (task.priority in setOf("HIGH", "MEDIUM", "LOW")) task.priority else "MEDIUM"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to task.title.take(300).ifBlank { "Task" },
                "notes" to task.notes.take(5000),
                "dueDateEpochDay" to task.dueDateEpochDay.coerceAtLeast(0L),
                "priority" to validPriority,
                "isCompleted" to task.isCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            task.deletedAt?.let { payload["deletedAtMillis"] = it }

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

    suspend fun fetchWorkspaceTasks(): Result<List<CloudWorkspaceTask>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val colRef = db.collection("users").document(uid).collection("workspace_tasks")
        return try {
            val snap = colRef.get().await()
            val list = snap.documents.map { doc ->
                CloudWorkspaceTask(
                    id = doc.id,
                    userId = doc.getString("userId").orEmpty(),
                    title = doc.getString("title").orEmpty(),
                    notes = doc.getString("notes").orEmpty(),
                    dueDateEpochDay = doc.getLong("dueDateEpochDay") ?: 0L,
                    priority = doc.getString("priority") ?: "MEDIUM",
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
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

    suspend fun pushReminder(reminder: SmartReminderEntity): Result<Unit> {
        if (!reminder.cloudSync) return Result.success(Unit)
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("reminders").document(sanitizeId(reminder.id))
        return try {
            val existing = docRef.get().await()
            val validMode = if (reminder.mode in setOf("LIBRARY_CONTENT", "PERSONAL", "MONK_SCHEDULE")) reminder.mode else "PERSONAL"
            val validPriority = if (reminder.priority in setOf("HIGH", "MEDIUM", "LOW")) reminder.priority else "MEDIUM"
            val validRecurrence = if (reminder.recurrenceType in setOf("ONCE", "EVERY_2_HOURS", "DAILY", "WEEKLY", "CUSTOM_MINUTES")) {
                reminder.recurrenceType
            } else "ONCE"

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "mode" to validMode,
                "title" to reminder.title.take(300).ifBlank { "Reminder" },
                "notes" to reminder.notes.take(3000),
                "priority" to validPriority,
                "triggerAtMillis" to reminder.triggerAtMillis.coerceAtLeast(0L),
                "recurrenceType" to validRecurrence,
                "intervalMinutes" to reminder.intervalMinutes.coerceAtLeast(0),
                "ringtoneTitle" to reminder.ringtoneTitle.take(200).ifBlank { "Default Alert" },
                "isEnabled" to reminder.isEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            reminder.targetItemId?.takeIf { it.isNotBlank() }?.let { payload["targetItemId"] = sanitizeId(it) }
            reminder.targetNoteId?.takeIf { it.isNotBlank() }?.let { payload["targetNoteId"] = sanitizeId(it) }
            reminder.deletedAt?.let { payload["deletedAtMillis"] = it }

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

    suspend fun fetchReminders(): Result<List<CloudSmartReminder>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val colRef = db.collection("users").document(uid).collection("reminders")
        return try {
            val snap = colRef.get().await()
            val list = snap.documents.map { doc ->
                CloudSmartReminder(
                    id = doc.id,
                    userId = doc.getString("userId").orEmpty(),
                    mode = doc.getString("mode") ?: "PERSONAL",
                    title = doc.getString("title").orEmpty(),
                    notes = doc.getString("notes").orEmpty(),
                    priority = doc.getString("priority") ?: "MEDIUM",
                    targetItemId = doc.getString("targetItemId"),
                    targetNoteId = doc.getString("targetNoteId"),
                    triggerAtMillis = doc.getLong("triggerAtMillis") ?: 0L,
                    recurrenceType = doc.getString("recurrenceType") ?: "ONCE",
                    intervalMinutes = (doc.getLong("intervalMinutes") ?: 0L).toInt(),
                    ringtoneTitle = doc.getString("ringtoneTitle") ?: "Default Alert",
                    isEnabled = doc.getBoolean("isEnabled") ?: true,
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

    suspend fun pushHabit(habit: HabitEntity, completedEpochDays: List<Long>): Result<Unit> {
        if (!habit.cloudSync) return Result.success(Unit)
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("users").document(uid).collection("habits").document(sanitizeId(habit.id))
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to habit.title.take(200).ifBlank { "Daily Habit" },
                "subtitle" to habit.subtitle.take(400),
                "category" to habit.category.take(64).ifBlank { "Focus" },
                "targetPerWeek" to habit.targetPerWeek.coerceIn(1, 7),
                "colorHex" to habit.colorHex.take(16).ifBlank { "#E53935" },
                "completedEpochDays" to completedEpochDays.distinct().sortedDescending().take(366),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            habit.deletedAt?.let { payload["deletedAtMillis"] = it }

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

    suspend fun fetchHabits(): Result<List<CloudHabitRecord>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val colRef = db.collection("users").document(uid).collection("habits")
        return try {
            val snap = colRef.get().await()
            val list = snap.documents.map { doc ->
                val rawDays = (doc.get("completedEpochDays") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toLong() }
                    ?: emptyList()
                CloudHabitRecord(
                    id = doc.id,
                    userId = doc.getString("userId").orEmpty(),
                    title = doc.getString("title").orEmpty(),
                    subtitle = doc.getString("subtitle").orEmpty(),
                    category = doc.getString("category") ?: "Focus",
                    targetPerWeek = (doc.getLong("targetPerWeek") ?: 7L).toInt(),
                    colorHex = doc.getString("colorHex") ?: "#E53935",
                    completedEpochDays = rawDays,
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
}

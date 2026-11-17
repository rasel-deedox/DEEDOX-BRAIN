package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.cloud.CloudOsSync
import com.example.data.cloud.CloudWorkspaceSync
import com.example.data.local.AppDatabase
import com.example.data.local.BlockerRuleEntity
import com.example.data.local.DetoxRuntimeStore
import com.example.data.local.FocusSessionEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.SmartReminderEntity
import com.example.data.local.WorkspaceNoteEntity
import com.example.data.local.WorkspaceTaskEntity
import com.example.monk.LaunchableAppInfo
import com.example.monk.InstalledAppsRepository
import com.example.monk.MonkBlockerEngine
import com.example.reminders.ReminderScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class SlashBlockOption(
    val command: String,
    val type: String,
    val badge: String,
    val label: String,
    val description: String
)

data class WorkspaceBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: String = TYPE_TEXT,
    val text: String = "",
    val checked: Boolean = false,
    val linkedItemId: String? = null,
    val linkedItemTitle: String? = null,
    val pageIndex: Int = 0,
    val details: String = "",
    val isExpanded: Boolean = true,
    val metadataJson: String = ""
) {
    companion object {
        const val TYPE_H1 = "H1"
        const val TYPE_H2 = "H2"
        const val TYPE_H3 = "H3"
        const val TYPE_TEXT = "TEXT"
        const val TYPE_CHECKLIST = "CHECKLIST"
        const val TYPE_BULLET = "BULLET"
        const val TYPE_TOGGLE = "TOGGLE"
        const val TYPE_CALLOUT = "CALLOUT"
        const val TYPE_CODE = "CODE"
        const val TYPE_DIVIDER = "DIVIDER"
        const val TYPE_ITEM_REF = "ITEM_REF"
        const val TYPE_DRIVE_EMBED = "DRIVE_EMBED"

        // 6-Category Infinite Smart Notion Canvas Blocks
        const val TYPE_SUBPAGE = "SUBPAGE"
        const val TYPE_TABLE = "TABLE"
        const val TYPE_INVOICE = "INVOICE"
        const val TYPE_EMAIL = "EMAIL"
        const val TYPE_REMINDER = "REMINDER"
        const val TYPE_SCHEDULE = "SCHEDULE"
        const val TYPE_TIMER = "TIMER"
        const val TYPE_VOICE = "VOICE"
        const val TYPE_SECRET = "SECRET"
        const val TYPE_CLIENT = "CLIENT"
        const val TYPE_EMBED = "EMBED"
        const val TYPE_PAGE_META = "PAGE_META"

        val RICH_CARD_TYPES: Set<String> = setOf(
            TYPE_SUBPAGE,
            TYPE_TABLE,
            TYPE_INVOICE,
            TYPE_EMAIL,
            TYPE_REMINDER,
            TYPE_SCHEDULE,
            TYPE_TIMER,
            TYPE_VOICE,
            TYPE_SECRET,
            TYPE_CLIENT,
            TYPE_EMBED,
            TYPE_DRIVE_EMBED
        )

        val SLASH_COMMANDS = listOf(
            SlashBlockOption("/page", TYPE_SUBPAGE, "SUB", "Sub-Page / Folder", "Create a nested page inside this page"),
            SlashBlockOption("/table", TYPE_TABLE, "TBL", "Smart Table", "Editable grid with Sum / Avg / Count footer"),
            SlashBlockOption("/invoice", TYPE_INVOICE, "INV", "Invoice / Quotation", "Line items, tax, discount & PDF export"),
            SlashBlockOption("/email", TYPE_EMAIL, "MAIL", "Direct Gmail Composer", "Write & send email directly from this page"),
            SlashBlockOption("/remind", TYPE_REMINDER, "ALRT", "Reminder & @Member Ping", "Exact alarm + team notification trigger"),
            SlashBlockOption("/schedule", TYPE_SCHEDULE, "CAL", "Team Task & Time Slot", "Sat–Fri member slot & live status pills"),
            SlashBlockOption("/timer", TYPE_TIMER, "TMR", "Focus / Pomodoro / Stopwatch", "Track & log time spent on this page"),
            SlashBlockOption("/voice", TYPE_VOICE, "MIC", "Voice Audio Note", "Inline mic recording & waveform player"),
            SlashBlockOption("/secret", TYPE_SECRET, "KEY", "Password & API Vault", "Blurred liquid-glass secret with 1-tap copy"),
            SlashBlockOption("/client", TYPE_CLIENT, "DUE", "Client Budget & Due", "Track total, advance & sync to Money Ledger"),
            SlashBlockOption("/embed", TYPE_EMBED, "WEB", "Website / Code / Link", "Clean preview card for any reference URL"),
            SlashBlockOption("/drive", TYPE_DRIVE_EMBED, "DRV", "Google Drive Embed", "Embed Docs, Sheets, or Drive video URL"),
            SlashBlockOption("/code", TYPE_CODE, "</>", "Code Snippet", "Monospace block with 1-tap copy"),
            SlashBlockOption("/h1", TYPE_H1, "H1", "Heading 1", "Large section title"),
            SlashBlockOption("/h2", TYPE_H2, "H2", "Heading 2", "Medium sub-heading"),
            SlashBlockOption("/h3", TYPE_H3, "H3", "Heading 3", "Compact heading"),
            SlashBlockOption("/text", TYPE_TEXT, "TXT", "Paragraph", "Plain body text"),
            SlashBlockOption("/todo", TYPE_CHECKLIST, "CHK", "Checklist", "Interactive to-do checkbox"),
            SlashBlockOption("/bullet", TYPE_BULLET, "LST", "Bulleted List", "Simple bullet point"),
            SlashBlockOption("/toggle", TYPE_TOGGLE, "TGL", "Toggle Accordion", "Collapsible details section"),
            SlashBlockOption("/callout", TYPE_CALLOUT, "NTE", "Callout Box", "Highlighted Liquid Glass callout"),
            SlashBlockOption("/divider", TYPE_DIVIDER, "---", "Divider", "Horizontal separator line")
        )

        fun defaultMetadataForType(type: String): String = when (type) {
            TYPE_SUBPAGE -> SubpageBlockData().toJson()
            TYPE_TABLE -> TableBlockData().toJson()
            TYPE_INVOICE -> InvoiceBlockData().toJson()
            TYPE_EMAIL -> EmailBlockData().toJson()
            TYPE_REMINDER -> ReminderBlockData().toJson()
            TYPE_SCHEDULE -> ScheduleBlockData().toJson()
            TYPE_TIMER -> TimerBlockData().toJson()
            TYPE_VOICE -> VoiceBlockData().toJson()
            TYPE_SECRET -> SecretBlockData().toJson()
            TYPE_CLIENT -> ClientTrackerBlockData().toJson()
            TYPE_EMBED -> EmbedBlockData(embedKind = EmbedBlockData.KIND_WEBSITE).toJson()
            TYPE_DRIVE_EMBED -> EmbedBlockData(embedKind = EmbedBlockData.KIND_DRIVE).toJson()
            else -> ""
        }

        fun parseList(json: String): List<WorkspaceBlock> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val arr = JSONArray(json)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        add(
                            WorkspaceBlock(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                type = obj.optString("type", TYPE_TEXT),
                                text = obj.optString("text", ""),
                                checked = obj.optBoolean("checked", false),
                                linkedItemId = obj.optString("linkedItemId", "").takeIf { it.isNotBlank() },
                                linkedItemTitle = obj.optString("linkedItemTitle", "").takeIf { it.isNotBlank() },
                                pageIndex = obj.optInt("pageIndex", 0).coerceAtLeast(0),
                                details = obj.optString("details", ""),
                                isExpanded = obj.optBoolean("isExpanded", true),
                                metadataJson = obj.optString("metadataJson", "")
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                listOf(WorkspaceBlock(type = TYPE_TEXT, text = json))
            }
        }

        fun encodeList(blocks: List<WorkspaceBlock>): String {
            val arr = JSONArray()
            blocks.forEach { b ->
                arr.put(
                    JSONObject().apply {
                        put("id", b.id)
                        put("type", b.type)
                        put("text", b.text)
                        put("checked", b.checked)
                        b.linkedItemId?.let { put("linkedItemId", it) }
                        b.linkedItemTitle?.let { put("linkedItemTitle", it) }
                        if (b.pageIndex > 0) put("pageIndex", b.pageIndex)
                        if (b.details.isNotBlank()) put("details", b.details)
                        if (!b.isExpanded) put("isExpanded", false)
                        if (b.metadataJson.isNotBlank()) put("metadataJson", b.metadataJson)
                    }
                )
            }
            return arr.toString()
        }

        fun plainTextSummary(blocksJson: String): String {
            val blocks = parseList(blocksJson).filterNot { it.type == TYPE_PAGE_META }
            return blocks.joinToString(" • ") { b ->
                when (b.type) {
                    TYPE_DIVIDER -> "---"
                    TYPE_ITEM_REF -> b.linkedItemTitle ?: "Linked Resource"
                    TYPE_DRIVE_EMBED -> "Drive: ${b.text.ifBlank { b.details }}"
                    TYPE_EMBED -> "Link: ${b.text.ifBlank { b.details }}"
                    TYPE_TOGGLE -> "${b.text}" + if (b.details.isNotBlank()) " (${b.details.take(60)})" else ""
                    TYPE_CHECKLIST -> (if (b.checked) "[x] " else "[ ] ") + b.text
                    TYPE_TABLE -> TableBlockData.fromJson(b.metadataJson).let { t -> "${t.title} (${t.rows.size} rows)" }
                    TYPE_INVOICE -> InvoiceBlockData.fromJson(b.metadataJson).let { inv -> "${inv.docNumber}: ${inv.formatMoney(inv.grandTotal)}" }
                    TYPE_CLIENT -> ClientTrackerBlockData.fromJson(b.metadataJson).let { cl -> "Client ${cl.clientName.ifBlank { b.text }} (Due ${cl.formatDollars(cl.remainingDueCents)})" }
                    TYPE_SCHEDULE -> ScheduleBlockData.fromJson(b.metadataJson).let { sc -> "${sc.title.ifBlank { b.text }} [${sc.dayOfWeek} • ${sc.status}]" }
                    TYPE_SECRET -> "[Protected Vault: ${SecretBlockData.fromJson(b.metadataJson).label}]"
                    TYPE_SUBPAGE -> "Sub-Page: ${SubpageBlockData.fromJson(b.metadataJson, b.text).title}"
                    TYPE_EMAIL -> "Email: ${EmailBlockData.fromJson(b.metadataJson).subject.ifBlank { b.text }}"
                    TYPE_REMINDER -> "Reminder: ${ReminderBlockData.fromJson(b.metadataJson).title.ifBlank { b.text }}"
                    TYPE_TIMER -> TimerBlockData.fromJson(b.metadataJson).let { tm -> "Timer: ${tm.label} (${tm.totalLoggedSeconds / 60}m)" }
                    TYPE_VOICE -> "Voice Note: ${VoiceBlockData.fromJson(b.metadataJson).caption}"
                    else -> b.text
                }.trim()
            }.trim()
        }
    }
}

class WorkspaceRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val cloudWorkspaceSync: CloudWorkspaceSync = CloudWorkspaceSync(context.applicationContext),
    private val cloudOsSync: CloudOsSync? = null
) {
    private val dao = database.workspaceDao()

    val notes: Flow<List<WorkspaceNoteEntity>> = dao.observeWorkspaceNotes()
    val tasks: Flow<List<WorkspaceTaskEntity>> = dao.observeWorkspaceTasks()
    val reminders: Flow<List<SmartReminderEntity>> = dao.observeReminders()
    val habits: Flow<List<HabitEntity>> = dao.observeHabits()
    val habitCompletions: Flow<List<HabitCompletionEntity>> = dao.observeHabitCompletions()
    val blockerRules: Flow<List<BlockerRuleEntity>> = dao.observeBlockerRules()
    val focusSessions: Flow<List<FocusSessionEntity>> = dao.observeFocusSessions()

    private fun currentAuthUid(): String? = try {
        Firebase.auth.currentUser?.uid
    } catch (_: Exception) {
        null
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun loadLaunchableApps(): List<LaunchableAppInfo> = withContext(Dispatchers.IO) {
        InstalledAppsRepository.loadLaunchableApps(context)
    }

    // --- Notion-Style Workspace Notes & Voice Notes ---
    suspend fun saveWorkspaceNote(
        id: String? = null,
        title: String,
        blocks: List<WorkspaceBlock>,
        tagsCsv: String,
        isPinned: Boolean,
        cloudSync: Boolean,
        linkedItemId: String? = null,
        audioUri: String? = null,
        audioDurationMs: Long = 0L
    ): WorkspaceNoteEntity = withContext(Dispatchers.IO) {
        val existing = id?.let { dao.getWorkspaceNoteById(it) }
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim().ifBlank { "Untitled Note" }
        val syncState = when {
            !cloudSync -> "local_only"
            currentAuthUid() != null -> "sync_pending"
            else -> "local_only"
        }
        val entity = (existing ?: WorkspaceNoteEntity(title = cleanTitle)).copy(
            userId = currentAuthUid() ?: existing?.userId,
            title = cleanTitle,
            blocksJson = WorkspaceBlock.encodeList(blocks),
            tagsCsv = tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", "),
            isPinned = isPinned,
            cloudSync = cloudSync,
            linkedItemId = linkedItemId ?: existing?.linkedItemId,
            audioUri = audioUri ?: existing?.audioUri,
            audioDurationMs = if (audioUri != null) audioDurationMs else (existing?.audioDurationMs ?: 0L),
            syncState = syncState,
            updatedAt = now
        )
        dao.upsertWorkspaceNote(entity)
        if (entity.cloudSync && currentAuthUid() != null && isOnline()) {
            val pushRes = cloudWorkspaceSync.pushWorkspaceNote(entity)
            if (pushRes.isSuccess) {
                val synced = entity.copy(syncState = "synced")
                dao.upsertWorkspaceNote(synced)
                return@withContext synced
            }
        }
        entity
    }

    suspend fun toggleWorkspaceNotePin(note: WorkspaceNoteEntity) = withContext(Dispatchers.IO) {
        val updated = note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
        dao.upsertWorkspaceNote(updated)
        if (updated.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushWorkspaceNote(updated)
        }
    }

    suspend fun setWorkspaceNoteCloudSync(noteId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val existing = dao.getWorkspaceNoteById(noteId) ?: return@withContext
        val updated = existing.copy(
            cloudSync = enabled,
            syncState = when {
                !enabled -> "local_only"
                currentAuthUid() != null -> "sync_pending"
                else -> "local_only"
            },
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertWorkspaceNote(updated)
        if (enabled && currentAuthUid() != null && isOnline()) {
            if (cloudWorkspaceSync.pushWorkspaceNote(updated).isSuccess) {
                dao.upsertWorkspaceNote(updated.copy(syncState = "synced"))
            }
        }
    }

    suspend fun deleteWorkspaceNote(noteId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getWorkspaceNoteById(noteId) ?: return@withContext
        val deletedAt = System.currentTimeMillis()
        dao.softDeleteWorkspaceNote(noteId, deletedAt)
        if (existing.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushWorkspaceNote(existing.copy(deletedAt = deletedAt))
        }
    }

    // --- Workspace Tasks ---
    suspend fun saveWorkspaceTask(
        id: String? = null,
        title: String,
        notes: String = "",
        dueDateEpochDay: Long = todayEpochDay(),
        priority: String = "MEDIUM",
        cloudSync: Boolean = true,
        audioUri: String? = null,
        audioDurationMs: Long = 0L,
        habitId: String? = null
    ): WorkspaceTaskEntity = withContext(Dispatchers.IO) {
        val existing = id?.let { dao.getWorkspaceTaskById(it) }
        val now = System.currentTimeMillis()
        val validPriority = if (priority in setOf("HIGH", "MEDIUM", "LOW")) priority else "MEDIUM"
        val entity = (existing ?: WorkspaceTaskEntity(title = title)).copy(
            userId = currentAuthUid() ?: existing?.userId,
            title = title.trim().ifBlank { "Task" },
            notes = notes.trim(),
            dueDateEpochDay = dueDateEpochDay,
            priority = validPriority,
            cloudSync = cloudSync,
            audioUri = audioUri ?: existing?.audioUri,
            audioDurationMs = if (audioUri != null) audioDurationMs else (existing?.audioDurationMs ?: 0L),
            habitId = habitId ?: existing?.habitId,
            syncState = if (!cloudSync) "local_only" else if (currentAuthUid() != null) "sync_pending" else "local_only",
            updatedAt = now
        )
        dao.upsertWorkspaceTask(entity)
        if (entity.cloudSync && currentAuthUid() != null && isOnline()) {
            if (cloudWorkspaceSync.pushWorkspaceTask(entity).isSuccess) {
                val synced = entity.copy(syncState = "synced")
                dao.upsertWorkspaceTask(synced)
                return@withContext synced
            }
        }
        entity
    }

    suspend fun toggleTaskCompleted(task: WorkspaceTaskEntity) = withContext(Dispatchers.IO) {
        val updated = task.copy(
            isCompleted = !task.isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertWorkspaceTask(updated)
        if (updated.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushWorkspaceTask(updated)
        }
    }

    suspend fun deleteWorkspaceTask(taskId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getWorkspaceTaskById(taskId) ?: return@withContext
        val deletedAt = System.currentTimeMillis()
        dao.softDeleteWorkspaceTask(taskId, deletedAt)
        if (existing.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushWorkspaceTask(existing.copy(deletedAt = deletedAt))
        }
    }

    // --- Smart Reminders ---
    suspend fun saveSmartReminder(
        id: String? = null,
        mode: String,
        title: String,
        notes: String,
        priority: String,
        triggerAtMillis: Long,
        recurrenceType: String,
        intervalMinutes: Int,
        ringtoneUri: String?,
        ringtoneTitle: String,
        cloudSync: Boolean = true,
        targetItemId: String? = null,
        targetNoteId: String? = null,
        blockerRuleId: String? = null,
        isEnabled: Boolean = true
    ): SmartReminderEntity = withContext(Dispatchers.IO) {
        val existing = id?.let { dao.getReminderById(it) }
        val now = System.currentTimeMillis()
        val entity = (existing ?: SmartReminderEntity(title = title, triggerAtMillis = triggerAtMillis)).copy(
            userId = currentAuthUid() ?: existing?.userId,
            mode = mode,
            title = title.trim().ifBlank { "Smart Reminder" },
            notes = notes.trim(),
            priority = if (priority in setOf("HIGH", "MEDIUM", "LOW")) priority else "MEDIUM",
            targetItemId = targetItemId ?: existing?.targetItemId,
            targetNoteId = targetNoteId ?: existing?.targetNoteId,
            blockerRuleId = blockerRuleId ?: existing?.blockerRuleId,
            triggerAtMillis = triggerAtMillis,
            recurrenceType = recurrenceType,
            intervalMinutes = intervalMinutes.coerceAtLeast(0),
            ringtoneUri = ringtoneUri,
            ringtoneTitle = ringtoneTitle.ifBlank { "Default Alert" },
            isEnabled = isEnabled,
            cloudSync = cloudSync,
            updatedAt = now
        )
        dao.upsertReminder(entity)
        if (entity.isEnabled) {
            ReminderScheduler.scheduleReminder(context, entity)
        } else {
            ReminderScheduler.cancelReminder(context, entity.id)
        }
        if (entity.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushReminder(entity)
        }
        entity
    }

    suspend fun toggleReminderEnabled(reminder: SmartReminderEntity) = withContext(Dispatchers.IO) {
        val next = reminder.copy(
            isEnabled = !reminder.isEnabled,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertReminder(next)
        if (next.isEnabled) {
            ReminderScheduler.scheduleReminder(context, next)
        } else {
            ReminderScheduler.cancelReminder(context, next.id)
        }
        if (next.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushReminder(next)
        }
    }

    suspend fun deleteReminder(reminderId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getReminderById(reminderId)
        val deletedAt = System.currentTimeMillis()
        ReminderScheduler.cancelReminder(context, reminderId)
        dao.softDeleteReminder(reminderId, deletedAt)
        if (existing != null && existing.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushReminder(existing.copy(deletedAt = deletedAt, isEnabled = false))
        }
    }

    // --- Habit Tracker ---
    suspend fun createHabit(
        title: String,
        subtitle: String = "",
        category: String = "Focus",
        targetPerWeek: Int = 7,
        cloudSync: Boolean = true,
        reminderEnabled: Boolean = false,
        reminderHour: Int = 9,
        reminderMinute: Int = 0,
        recurrenceType: String = ReminderScheduler.RECURRENCE_DAILY,
        intervalMinutes: Int = 1440,
        ringtoneUri: String? = null,
        ringtoneTitle: String = "Default System Sound"
    ): HabitEntity = withContext(Dispatchers.IO) {
        val habit = HabitEntity(
            userId = currentAuthUid(),
            title = title.trim().ifBlank { "Daily Habit" },
            subtitle = subtitle.trim(),
            category = category.ifBlank { "Focus" },
            targetPerWeek = targetPerWeek.coerceIn(1, 7),
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour.coerceIn(0, 23),
            reminderMinute = reminderMinute.coerceIn(0, 59),
            recurrenceType = recurrenceType.ifBlank { ReminderScheduler.RECURRENCE_DAILY },
            intervalMinutes = intervalMinutes.coerceAtLeast(1),
            ringtoneUri = ringtoneUri,
            ringtoneTitle = ringtoneTitle.ifBlank { "Default System Sound" },
            cloudSync = cloudSync
        )
        dao.upsertHabit(habit)
        syncHabitLinkedReminder(habit)
        if (habit.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushHabit(habit, emptyList())
        }
        habit
    }

    suspend fun updateHabitReminder(
        habit: HabitEntity,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        recurrenceType: String,
        intervalMinutes: Int,
        ringtoneUri: String?,
        ringtoneTitle: String
    ): HabitEntity = withContext(Dispatchers.IO) {
        val updated = habit.copy(
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour.coerceIn(0, 23),
            reminderMinute = reminderMinute.coerceIn(0, 59),
            recurrenceType = recurrenceType.ifBlank { ReminderScheduler.RECURRENCE_DAILY },
            intervalMinutes = intervalMinutes.coerceAtLeast(1),
            ringtoneUri = ringtoneUri,
            ringtoneTitle = ringtoneTitle.ifBlank { "Default System Sound" },
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertHabit(updated)
        syncHabitLinkedReminder(updated)
        if (updated.cloudSync && currentAuthUid() != null && isOnline()) {
            val days = dao.getAllHabitCompletions().filter { it.habitId == updated.id }.map { it.epochDay }
            cloudWorkspaceSync.pushHabit(updated, days)
        }
        updated
    }

    private suspend fun syncHabitLinkedReminder(habit: HabitEntity) {
        val reminderId = "habit_rem_${habit.id}"
        if (habit.reminderEnabled && habit.deletedAt == null) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, habit.reminderHour.coerceIn(0, 23))
                set(Calendar.MINUTE, habit.reminderMinute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            saveSmartReminder(
                id = reminderId,
                mode = "PERSONAL",
                title = "Habit: ${habit.title}",
                notes = habit.subtitle.ifBlank { "Stay on track with your daily focus habit." },
                priority = "HIGH",
                triggerAtMillis = cal.timeInMillis,
                recurrenceType = habit.recurrenceType,
                intervalMinutes = habit.intervalMinutes,
                ringtoneUri = habit.ringtoneUri,
                ringtoneTitle = habit.ringtoneTitle,
                cloudSync = habit.cloudSync,
                isEnabled = true
            )
        } else {
            ReminderScheduler.cancelReminder(context, reminderId)
            dao.softDeleteReminder(reminderId)
        }
    }

    suspend fun toggleHabitCompletion(habitId: String, epochDay: Long = todayEpochDay()) = withContext(Dispatchers.IO) {
        val existing = dao.getHabitCompletion(habitId, epochDay)
        if (existing != null) {
            dao.deleteHabitCompletion(habitId, epochDay)
        } else {
            dao.upsertHabitCompletion(HabitCompletionEntity(habitId = habitId, epochDay = epochDay))
        }
        val habit = dao.getAllHabits().find { it.id == habitId }
        if (habit != null && habit.cloudSync && currentAuthUid() != null && isOnline()) {
            val allHabits = dao.getAllHabits().filter { it.deletedAt == null }
            val allCompletions = dao.getAllHabitCompletions()
            val days = allCompletions.filter { it.habitId == habitId }.map { it.epochDay }
            cloudWorkspaceSync.pushHabit(habit, days)
            val summary = HabitGamificationEngine.compute(allHabits, allCompletions)
            cloudOsSync?.pushGamificationStats(summary)
        }
    }

    suspend fun deleteHabit(habitId: String) = withContext(Dispatchers.IO) {
        val habit = dao.getAllHabits().find { it.id == habitId }
        val deletedAt = System.currentTimeMillis()
        dao.softDeleteHabit(habitId, deletedAt)
        val reminderId = "habit_rem_$habitId"
        ReminderScheduler.cancelReminder(context, reminderId)
        dao.softDeleteReminder(reminderId, deletedAt)
        if (habit != null && habit.cloudSync && currentAuthUid() != null && isOnline()) {
            cloudWorkspaceSync.pushHabit(habit.copy(deletedAt = deletedAt), emptyList())
        }
    }

    // --- Monk Mode App Blocker & Focus Sessions ---
    suspend fun saveBlockerRule(
        id: String? = null,
        title: String,
        mode: String,
        packages: Collection<String>,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        daysOfWeekCsv: String,
        delayDurationMinutes: Int,
        activeDurationMinutes: Int?,
        preReminderMinutes: Int,
        isEnabled: Boolean = true
    ): BlockerRuleEntity = withContext(Dispatchers.IO) {
        val existing = id?.let { dao.getBlockerRuleById(it) }
        val now = System.currentTimeMillis()
        val delayTrigger = if (mode == MonkBlockerEngine.MODE_COUNTDOWN_DELAY) {
            now + delayDurationMinutes.coerceAtLeast(1) * 60_000L
        } else {
            null
        }
        val activeUntil = activeDurationMinutes?.takeIf { it > 0 }?.let { mins ->
            val base = delayTrigger ?: now
            base + mins * 60_000L
        }
        val rule = (existing ?: BlockerRuleEntity(title = title)).copy(
            title = title.trim().ifBlank { "Monk Rule" },
            mode = mode,
            blockedPackagesJson = MonkBlockerEngine.encodePackages(packages),
            startMinuteOfDay = startMinuteOfDay.coerceIn(0, 1439),
            endMinuteOfDay = endMinuteOfDay.coerceIn(0, 1439),
            daysOfWeekCsv = daysOfWeekCsv.ifBlank { "1,2,3,4,5,6,7" },
            delayTriggerAtMillis = delayTrigger,
            delayDurationMinutes = delayDurationMinutes.coerceAtLeast(1),
            activeUntilMillis = activeUntil,
            preReminderMinutes = preReminderMinutes.coerceAtLeast(0),
            isEnabled = isEnabled,
            updatedAt = now
        )
        dao.upsertBlockerRule(rule)

        // Create or cancel pre-block Smart Reminder if scheduled with preReminderMinutes > 0
        val preReminderId = "monk_pre_${rule.id}"
        if (isEnabled && mode == MonkBlockerEngine.MODE_SCHEDULED && preReminderMinutes > 0) {
            val nextStartMs = computeNextStartMillis(startMinuteOfDay, preReminderMinutes)
            saveSmartReminder(
                id = preReminderId,
                mode = "MONK_SCHEDULE",
                title = "Monk Mode starts in ${preReminderMinutes}m: ${rule.title}",
                notes = "Distracting apps will lock automatically at ${MonkBlockerEngine.formatMinuteOfDay(startMinuteOfDay)}.",
                priority = "HIGH",
                triggerAtMillis = nextStartMs,
                recurrenceType = ReminderScheduler.RECURRENCE_DAILY,
                intervalMinutes = 0,
                ringtoneUri = null,
                ringtoneTitle = "Default Alert",
                cloudSync = false,
                blockerRuleId = rule.id,
                isEnabled = true
            )
        } else {
            ReminderScheduler.cancelReminder(context, preReminderId)
            dao.softDeleteReminder(preReminderId)
        }

        syncMonitorServiceState()
        rule
    }

    suspend fun toggleBlockerRule(rule: BlockerRuleEntity) = withContext(Dispatchers.IO) {
        val updated = rule.copy(isEnabled = !rule.isEnabled, updatedAt = System.currentTimeMillis())
        dao.upsertBlockerRule(updated)
        syncMonitorServiceState()
    }

    suspend fun deleteBlockerRule(ruleId: String) = withContext(Dispatchers.IO) {
        dao.deleteBlockerRule(ruleId)
        val preId = "monk_pre_$ruleId"
        ReminderScheduler.cancelReminder(context, preId)
        dao.softDeleteReminder(preId)
        syncMonitorServiceState()
    }

    suspend fun startFocusSession(label: String, plannedMinutes: Int): FocusSessionEntity = withContext(Dispatchers.IO) {
        val current = dao.getActiveFocusSession()
        if (current != null) {
            dao.upsertFocusSession(current.copy(endedAt = System.currentTimeMillis(), completed = false))
        }
        val session = FocusSessionEntity(
            label = label.trim().ifBlank { "Deep Focus" },
            plannedMinutes = plannedMinutes.coerceIn(1, 360),
            startedAt = System.currentTimeMillis()
        )
        dao.upsertFocusSession(session)
        syncMonitorServiceState()
        session
    }

    suspend fun completeActiveFocusSession(emergencyUnlocked: Boolean = false) = withContext(Dispatchers.IO) {
        val current = dao.getActiveFocusSession() ?: return@withContext
        val now = System.currentTimeMillis()
        val elapsedMins = ((now - current.startedAt) / 60_000L).toInt().coerceAtLeast(1)
        dao.upsertFocusSession(
            current.copy(
                endedAt = now,
                actualMinutes = elapsedMins,
                completed = !emergencyUnlocked,
                emergencyUnlocked = emergencyUnlocked
            )
        )
        if (emergencyUnlocked) {
            // Also pause instant rules when unlocked via 3-stage emergency exit
            dao.getAllBlockerRules()
                .filter { it.isEnabled && it.mode == MonkBlockerEngine.MODE_INSTANT }
                .forEach { rule ->
                    dao.upsertBlockerRule(rule.copy(isEnabled = false, updatedAt = now))
                }
        }
        syncMonitorServiceState()
    }

    suspend fun syncMonitorServiceState() = withContext(Dispatchers.IO) {
        val rules = dao.getAllBlockerRules()
        val session = dao.getActiveFocusSession()
        val detox = DetoxRuntimeStore.read(context)
        if (MonkBlockerEngine.hasAnyActiveMonitoringNeed(rules, session, detoxState = detox)) {
            MonkBlockerEngine.ensureMonitorRunning(context)
        } else {
            MonkBlockerEngine.stopMonitorIfIdle(context)
        }
    }

    // --- Cloud Sync for Workspace (respects cloudSync == false local-only notes/tasks/habits) ---
    suspend fun syncWorkspaceNow(): Int = withContext(Dispatchers.IO) {
        if (currentAuthUid() == null || !isOnline()) return@withContext 0
        var syncedCount = 0

        dao.getAllWorkspaceNotes().forEach { note ->
            if (!note.cloudSync) return@forEach
            if (cloudWorkspaceSync.pushWorkspaceNote(note).isSuccess) {
                dao.upsertWorkspaceNote(note.copy(syncState = "synced"))
                syncedCount++
            }
        }
        dao.getAllWorkspaceTasks().forEach { task ->
            if (!task.cloudSync) return@forEach
            if (cloudWorkspaceSync.pushWorkspaceTask(task).isSuccess) {
                dao.upsertWorkspaceTask(task.copy(syncState = "synced"))
                syncedCount++
            }
        }
        dao.getAllReminders().forEach { rem ->
            if (!rem.cloudSync) return@forEach
            if (cloudWorkspaceSync.pushReminder(rem).isSuccess) syncedCount++
        }
        val completionsByHabit = dao.getAllHabitCompletions().groupBy({ it.habitId }, { it.epochDay })
        dao.getAllHabits().forEach { habit ->
            if (!habit.cloudSync) return@forEach
            if (cloudWorkspaceSync.pushHabit(habit, completionsByHabit[habit.id].orEmpty()).isSuccess) {
                syncedCount++
            }
        }

        // Pull cloud workspace records missing locally
        cloudWorkspaceSync.fetchWorkspaceNotes().getOrNull()?.forEach { remote ->
            if (remote.deletedAtMillis == null && dao.getWorkspaceNoteById(remote.id) == null) {
                val createdMs = remote.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                val updatedMs = remote.updatedAt?.toDate()?.time ?: createdMs
                dao.upsertWorkspaceNote(
                    WorkspaceNoteEntity(
                        id = remote.id,
                        userId = remote.userId,
                        title = remote.title,
                        blocksJson = remote.blocksJson,
                        tagsCsv = remote.tagsCsv,
                        isPinned = remote.isPinned,
                        cloudSync = true,
                        linkedItemId = remote.linkedItemId,
                        audioDurationMs = remote.audioDurationMs,
                        syncState = "synced",
                        createdAt = createdMs,
                        updatedAt = updatedMs
                    )
                )
            }
        }
        cloudWorkspaceSync.fetchWorkspaceTasks().getOrNull()?.forEach { remote ->
            if (remote.deletedAtMillis == null && dao.getWorkspaceTaskById(remote.id) == null) {
                dao.upsertWorkspaceTask(
                    WorkspaceTaskEntity(
                        id = remote.id,
                        userId = remote.userId,
                        title = remote.title,
                        notes = remote.notes,
                        dueDateEpochDay = remote.dueDateEpochDay,
                        priority = remote.priority,
                        isCompleted = remote.isCompleted,
                        cloudSync = true,
                        syncState = "synced"
                    )
                )
            }
        }
        cloudWorkspaceSync.fetchHabits().getOrNull()?.forEach { remote ->
            if (remote.deletedAtMillis == null && dao.getAllHabits().none { it.id == remote.id }) {
                dao.upsertHabit(
                    HabitEntity(
                        id = remote.id,
                        userId = remote.userId,
                        title = remote.title,
                        subtitle = remote.subtitle,
                        category = remote.category,
                        targetPerWeek = remote.targetPerWeek,
                        colorHex = remote.colorHex,
                        cloudSync = true
                    )
                )
                remote.completedEpochDays.forEach { day ->
                    dao.upsertHabitCompletion(HabitCompletionEntity(habitId = remote.id, epochDay = day))
                }
            }
        }
        val allHabits = dao.getAllHabits().filter { it.deletedAt == null }
        val allCompletions = dao.getAllHabitCompletions()
        if (allHabits.isNotEmpty() || allCompletions.isNotEmpty()) {
            val summary = HabitGamificationEngine.compute(allHabits, allCompletions)
            cloudOsSync?.pushGamificationStats(summary)
        }
        syncedCount
    }

    private fun computeNextStartMillis(startMinuteOfDay: Int, preReminderMinutes: Int): Long {
        val cal = Calendar.getInstance()
        val targetMin = (startMinuteOfDay - preReminderMinutes).let { if (it < 0) it + 1440 else it }
        cal.set(Calendar.HOUR_OF_DAY, targetMin / 60)
        cal.set(Calendar.MINUTE, targetMin % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    companion object {
        fun todayEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val localOffset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
            return (nowMillis + localOffset) / 86_400_000L
        }

        fun computeHabitStreak(
            completedDays: Set<Long>,
            todayDay: Long = todayEpochDay()
        ): Int {
            var cursor = when {
                todayDay in completedDays -> todayDay
                (todayDay - 1L) in completedDays -> todayDay - 1L
                else -> return 0
            }
            var streak = 0
            while (cursor in completedDays && streak < 3650) {
                streak++
                cursor--
            }
            return streak
        }
    }
}

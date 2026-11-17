package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiPagePolishMode
import com.example.data.local.HabitEntity
import com.example.data.local.ItemEntity
import com.example.data.local.SmartReminderEntity
import com.example.data.local.WorkspaceNoteEntity
import com.example.data.local.WorkspaceTaskEntity
import com.example.data.media.VoicePlaybackState
import com.example.data.media.VoiceRecorderState
import com.example.data.repository.NoteBacklinksParser
import com.example.data.repository.ReminderBlockData
import com.example.data.repository.ScheduleBlockData
import com.example.data.repository.WorkspaceBlock
import com.example.data.repository.WorkspaceRepository
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.LiquidDeleteConfirmationDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.VoiceAudioPlayerCard
import com.example.ui.components.VoiceNoteRecorderPanel
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.viewmodel.WorkspaceSnapshot
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private enum class WorkspaceTab(val label: String) {
    NOTEBOOK("Notebook"),
    TASKS_REMINDERS("Tasks & Alerts"),
    HABITS("Habits")
}

private enum class TaskScheduleBucket(val label: String) {
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    UPCOMING("Upcoming"),
    REMINDERS("Reminders")
}

@Composable
fun WorkspaceScreen(
    workspaceSnapshot: WorkspaceSnapshot,
    libraryItems: List<ItemEntity>,
    recorderState: VoiceRecorderState,
    playbackState: VoicePlaybackState,
    onStartVoiceRecording: () -> Unit,
    onPauseVoiceRecording: () -> Unit,
    onResumeVoiceRecording: () -> Unit,
    onDiscardVoiceRecording: () -> Unit,
    onSaveVoiceToNote: (String) -> Unit,
    onSaveVoiceToTodo: (String) -> Unit,
    onToggleVoicePlayback: (String) -> Unit,
    onSeekVoicePlayback: (String, Long) -> Unit,
    onSaveNote: (
        id: String?,
        title: String,
        blocks: List<WorkspaceBlock>,
        tagsCsv: String,
        isPinned: Boolean,
        cloudSync: Boolean,
        linkedItemId: String?,
        audioUri: String?,
        audioDurationMs: Long
    ) -> Unit,
    onToggleNotePin: (WorkspaceNoteEntity) -> Unit,
    onToggleNoteCloudSync: (WorkspaceNoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSaveTask: (id: String?, title: String, notes: String, dueDateEpochDay: Long, priority: String, cloudSync: Boolean) -> Unit,
    onToggleTaskCompleted: (WorkspaceTaskEntity) -> Unit,
    onDeleteTask: (String) -> Unit,
    onOpenReminderSheet: (initialTitle: String, targetItemId: String?, targetNoteId: String?) -> Unit,
    onToggleReminder: (SmartReminderEntity) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onCreateHabit: (
        title: String,
        subtitle: String,
        category: String,
        targetPerWeek: Int,
        cloudSync: Boolean,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        recurrenceType: String,
        intervalMinutes: Int,
        ringtoneUri: String?,
        ringtoneTitle: String
    ) -> Unit,
    onToggleHabitToday: (String) -> Unit,
    onUpdateHabitReminder: (
        habit: HabitEntity,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        recurrenceType: String,
        intervalMinutes: Int,
        ringtoneUri: String?,
        ringtoneTitle: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteHabit: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onPolishPageWithAi: (suspend (String, List<WorkspaceBlock>, AiPagePolishMode) -> Result<List<WorkspaceBlock>>)? = null,
    onLogClientPaymentToLedger: (clientName: String, projectName: String, amountCents: Long) -> Unit = { _, _, _ -> },
    onScheduleInlineReminder: (ReminderBlockData, String?) -> Unit = { _, _ -> },
    onSyncScheduleTask: (ScheduleBlockData) -> Unit = {},
    onLogFocusTimerSeconds: (String, Long) -> Unit = { _, _ -> },
    onStartInlineVoiceRecord: () -> Unit = {},
    onStopInlineVoiceRecord: () -> Pair<String, Long>? = { null }
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val palette = LiquidTheme.palette
    var activeTab by remember { mutableStateOf(WorkspaceTab.NOTEBOOK) }
    var taskBucket by remember { mutableStateOf(TaskScheduleBucket.TODAY) }
    var showVoiceRecorder by remember { mutableStateOf(false) }

    var editingNote by remember { mutableStateOf<WorkspaceNoteEntity?>(null) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showNewTaskDialog by remember { mutableStateOf(false) }
    var showNewHabitDialog by remember { mutableStateOf(false) }
    var editingHabitForReminder by remember { mutableStateOf<HabitEntity?>(null) }
    var recentXpBanner by remember { mutableStateOf<String?>(null) }

    var pendingDeleteTitle by remember { mutableStateOf("") }
    var pendingDeleteMessage by remember { mutableStateOf("") }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(recentXpBanner) {
        if (recentXpBanner != null) {
            delay(2600L)
            recentXpBanner = null
        }
    }

    var noteQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }

    val availableNoteTags = remember(workspaceSnapshot.notes) {
        workspaceSnapshot.notes
            .flatMap { it.tagsCsv.split(',') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredNotes = remember(workspaceSnapshot.notes, noteQuery, selectedTagFilter) {
        workspaceSnapshot.notes.filter { note ->
            val matchesQuery = noteQuery.isBlank() ||
                note.title.contains(noteQuery, ignoreCase = true) ||
                note.tagsCsv.contains(noteQuery, ignoreCase = true) ||
                note.blocksJson.contains(noteQuery, ignoreCase = true)
            val matchesTag = selectedTagFilter == null || note.tagsCsv
                .split(',')
                .any { it.trim().equals(selectedTagFilter, ignoreCase = true) }
            matchesQuery && matchesTag
        }
    }

    val todayDay = WorkspaceRepository.todayEpochDay()
    val tomorrowDay = todayDay + 1L

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("workspace_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "workspace_header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        FloatingGlassIconButton(
                            iconType = MinimalIconType.BACK,
                            contentDescription = "Back to Vault",
                            onClick = onBack,
                            modifier = Modifier.testTag("workspace_back_button")
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notes & Habits",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "Full-page notes, voice memos, tasks & XP streaks",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                    TactilePrimaryPillButton(
                        text = "Voice",
                        iconType = MinimalIconType.MIC,
                        onClick = { showVoiceRecorder = !showVoiceRecorder },
                        modifier = Modifier.testTag("workspace_voice_button")
                    )
                }
            }

            if (showVoiceRecorder) {
                item(key = "workspace_voice_panel", contentType = "voice_panel") {
                    VoiceNoteRecorderPanel(
                        recorderState = recorderState,
                        onStartRecording = onStartVoiceRecording,
                        onPauseRecording = onPauseVoiceRecording,
                        onResumeRecording = onResumeVoiceRecording,
                        onDiscardRecording = onDiscardVoiceRecording,
                        onSaveToNote = onSaveVoiceToNote,
                        onSaveToTodo = onSaveVoiceToTodo,
                        onDismiss = { showVoiceRecorder = false }
                    )
                }
            }

            item(key = "workspace_segment", contentType = "segment_switcher") {
                RecessedFramingBezel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 9999.dp,
                    innerPadding = PaddingValues(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WorkspaceTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            Surface(
                                onClick = { activeTab = tab },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (selected) palette.primaryButtonBg else Color.Transparent,
                                border = if (selected) BorderStroke(1.dp, palette.specularRimOutline) else null,
                                shadowElevation = if (selected) 4.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("workspace_tab_${tab.name.lowercase()}")
                            ) {
                                Box(modifier = Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selected) palette.primaryButtonContent else palette.secondaryInk,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when (activeTab) {
                WorkspaceTab.NOTEBOOK -> {
                    item(key = "notebook_toolbar", contentType = "notebook_toolbar") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = noteQuery,
                                    onValueChange = { noteQuery = it },
                                    placeholder = { Text("Search Notion pages or tags...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(9999.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = palette.cardSurfaceTop,
                                        unfocusedContainerColor = palette.cardSurfaceTop,
                                        focusedBorderColor = CrimsonAccent,
                                        unfocusedBorderColor = palette.inputBorder,
                                        focusedTextColor = palette.primaryInk,
                                        unfocusedTextColor = palette.primaryInk
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                TactilePrimaryPillButton(
                                    text = "+ Page",
                                    iconType = MinimalIconType.PLUS,
                                    onClick = {
                                        editingNote = null
                                        showNoteEditor = true
                                    },
                                    modifier = Modifier.testTag("notebook_new_page_button")
                                )
                            }

                            if (availableNoteTags.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { selectedTagFilter = null },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (selectedTagFilter == null) palette.primaryButtonBg else palette.recessedTrayBg
                                    ) {
                                        Text(
                                            text = "All",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selectedTagFilter == null) palette.primaryButtonContent else palette.secondaryInk,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                    availableNoteTags.forEach { tag ->
                                        val selected = selectedTagFilter == tag
                                        Surface(
                                            onClick = { selectedTagFilter = if (selected) null else tag },
                                            shape = RoundedCornerShape(9999.dp),
                                            color = if (selected) CrimsonAccent else palette.recessedTrayBg
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) Color.White else palette.secondaryInk,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredNotes.isEmpty()) {
                        item(key = "notebook_empty", contentType = "empty_card") {
                            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Your Notion Notebook is ready", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                    Text(
                                        "Create block-based study guides, project outlines, or capture voice notes with per-note Cloud Synced or Local Device Only storage.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )
                                }
                            }
                        }
                    } else {
                        items(items = filteredNotes, key = { it.id }, contentType = { "notion_card" }) { note ->
                            WorkspaceNoteCard(
                                note = note,
                                allNotes = workspaceSnapshot.notes,
                                playbackState = playbackState,
                                onTogglePlayback = onToggleVoicePlayback,
                                onSeekPlayback = onSeekVoicePlayback,
                                onOpenEditor = {
                                    editingNote = note
                                    showNoteEditor = true
                                },
                                onTogglePin = { onToggleNotePin(note) },
                                onToggleCloudSync = { onToggleNoteCloudSync(note) },
                                onAttachReminder = { onOpenReminderSheet(note.title, note.linkedItemId, note.id) },
                                onDelete = {
                                    pendingDeleteTitle = "Delete Note?"
                                    pendingDeleteMessage = "Permanently delete \"${note.title}\" and all its pages?"
                                    pendingDeleteAction = { onDeleteNote(note.id) }
                                }
                            )
                        }
                    }
                }

                WorkspaceTab.TASKS_REMINDERS -> {
                    item(key = "tasks_buckets_bar", contentType = "tasks_buckets") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TaskScheduleBucket.entries.forEach { bucket ->
                                    val selected = taskBucket == bucket
                                    Surface(
                                        onClick = { taskBucket = bucket },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (selected) palette.primaryButtonBg else palette.frostedPillBg,
                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                        modifier = Modifier.testTag("task_bucket_${bucket.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = bucket.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) palette.primaryButtonContent else palette.secondaryInk,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                TactilePrimaryPillButton(
                                    text = if (taskBucket == TaskScheduleBucket.REMINDERS) "+ Smart Reminder" else "+ New Task",
                                    iconType = MinimalIconType.PLUS,
                                    onClick = {
                                        if (taskBucket == TaskScheduleBucket.REMINDERS) {
                                            onOpenReminderSheet("", null, null)
                                        } else {
                                            showNewTaskDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tasks_primary_add_button")
                                )
                                OutlinedButton(
                                    onClick = { onOpenReminderSheet("", null, null) },
                                    shape = RoundedCornerShape(9999.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    MinimalIcon(type = MinimalIconType.BELL, tint = palette.primaryInk, size = 16.dp)
                                    Text(" + Alert", color = palette.primaryInk)
                                }
                            }
                        }
                    }

                    if (taskBucket == TaskScheduleBucket.REMINDERS) {
                        if (workspaceSnapshot.reminders.isEmpty()) {
                            item(key = "reminders_empty", contentType = "empty_card") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("No Smart Reminders scheduled", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                        Text(
                                            "Schedule Library Content alerts or Personal Life reminders with custom ringtones and recurring intervals.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.secondaryInk
                                        )
                                    }
                                }
                            }
                        } else {
                            items(items = workspaceSnapshot.reminders, key = { it.id }, contentType = { "reminder_card" }) { rem ->
                                val timeText = remember(rem.triggerAtMillis) {
                                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                        .format(Date(rem.triggerAtMillis))
                                }
                                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = if (rem.priority == "HIGH") CrimsonAccent else palette.accentSoftBg
                                                ) {
                                                    Text(
                                                        text = rem.priority,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (rem.priority == "HIGH") Color.White else palette.accentSoftInk,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(shape = RoundedCornerShape(9999.dp), color = palette.recessedTrayBg) {
                                                    Text(
                                                        text = if (rem.mode == "LIBRARY_CONTENT") "Library" else "Personal",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.secondaryInk,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(rem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                            Text("$timeText • ${rem.recurrenceType} • ${rem.ringtoneTitle}", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(checked = rem.isEnabled, onCheckedChange = { onToggleReminder(rem) })
                                            IconButton(
                                                onClick = {
                                                    pendingDeleteTitle = "Delete Reminder?"
                                                    pendingDeleteMessage = "Remove smart alert \"${rem.title}\"?"
                                                    pendingDeleteAction = { onDeleteReminder(rem.id) }
                                                }
                                            ) {
                                                MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 16.dp, contentDescription = "Delete reminder")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val bucketTasks = workspaceSnapshot.tasks.filter { task ->
                            when (taskBucket) {
                                TaskScheduleBucket.TODAY -> task.dueDateEpochDay <= todayDay
                                TaskScheduleBucket.TOMORROW -> task.dueDateEpochDay == tomorrowDay
                                TaskScheduleBucket.UPCOMING -> task.dueDateEpochDay > tomorrowDay
                                TaskScheduleBucket.REMINDERS -> false
                            }
                        }
                        if (bucketTasks.isEmpty()) {
                            item(key = "tasks_empty", contentType = "empty_card") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("No tasks for ${taskBucket.label}", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                        Text("Tap \"+ New Task\" or record a \"Voice\" memo to add an action item.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                    }
                                }
                            }
                        } else {
                            items(items = bucketTasks, key = { it.id }, contentType = { "task_card" }) { task ->
                                WorkspaceTaskCard(
                                    task = task,
                                    playbackState = playbackState,
                                    onToggleCompleted = { onToggleTaskCompleted(task) },
                                    onTogglePlayback = onToggleVoicePlayback,
                                    onSeekPlayback = onSeekVoicePlayback,
                                    onDelete = {
                                        pendingDeleteTitle = "Delete Task?"
                                        pendingDeleteMessage = "Delete task \"${task.title}\"?"
                                        pendingDeleteAction = { onDeleteTask(task.id) }
                                    }
                                )
                            }
                        }
                    }
                }

                WorkspaceTab.HABITS -> {
                    habitsTabItems(
                        habits = workspaceSnapshot.habits,
                        completions = workspaceSnapshot.habitCompletions,
                        recentXpBanner = recentXpBanner,
                        onToggleToday = { habitId, nowDone ->
                            onToggleHabitToday(habitId)
                            recentXpBanner = if (nowDone) "+25 XP Earned!" else null
                        },
                        onEditHabitReminder = { habit -> editingHabitForReminder = habit },
                        onDeleteHabit = { habit ->
                            pendingDeleteTitle = "Delete Habit?"
                            pendingDeleteMessage = "Delete \"${habit.title}\"? Your streak history for this habit will be removed."
                            pendingDeleteAction = { onDeleteHabit(habit.id) }
                        },
                        onOpenNewHabitDialog = { showNewHabitDialog = true }
                    )
                }
            }
        }
    }

    if (showNoteEditor) {
        NotionEditorSheet(
            existingNote = editingNote,
            availableItems = libraryItems,
            allNotes = workspaceSnapshot.notes,
            playbackState = playbackState,
            onTogglePlayback = onToggleVoicePlayback,
            onSeekPlayback = onSeekVoicePlayback,
            onAttachReminder = { title, noteId -> onOpenReminderSheet(title, null, noteId) },
            onOpenLinkedNote = { target ->
                editingNote = target
                showNoteEditor = true
            },
            onSave = onSaveNote,
            onDismiss = { showNoteEditor = false },
            onPolishPageWithAi = onPolishPageWithAi,
            onLogClientPaymentToLedger = onLogClientPaymentToLedger,
            onScheduleInlineReminder = onScheduleInlineReminder,
            onSyncScheduleTask = onSyncScheduleTask,
            onLogFocusTimerSeconds = onLogFocusTimerSeconds,
            onStartVoiceRecord = onStartInlineVoiceRecord,
            onStopVoiceRecord = onStopInlineVoiceRecord,
            onCreateNestedSubpage = { sub ->
                val subTitle = sub.title.ifBlank { "Untitled Sub-page" }
                val matched = workspaceSnapshot.notes.firstOrNull {
                    it.deletedAt == null && (it.id == sub.subpageId || it.title.equals(subTitle, ignoreCase = true))
                }
                if (matched != null) {
                    editingNote = matched
                } else {
                    onSaveNote(
                        null,
                        subTitle,
                        listOf(WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = subTitle)),
                        "subpage",
                        false,
                        true,
                        editingNote?.linkedItemId,
                        null,
                        0L
                    )
                }
                showNoteEditor = true
            }
        )
    }

    if (showNewTaskDialog) {
        CreateTaskDialog(
            defaultEpochDay = when (taskBucket) {
                TaskScheduleBucket.TOMORROW -> tomorrowDay
                TaskScheduleBucket.UPCOMING -> todayDay + 3L
                else -> todayDay
            },
            onDismiss = { showNewTaskDialog = false },
            onSave = { title, notes, day, priority, cloudSync ->
                onSaveTask(null, title, notes, day, priority, cloudSync)
            }
        )
    }

    if (showNewHabitDialog) {
        CreateHabitDialog(
            onDismiss = { showNewHabitDialog = false },
            onCreate = onCreateHabit
        )
    }

    editingHabitForReminder?.let { targetHabit ->
        EditHabitReminderDialog(
            habit = targetHabit,
            onDismiss = { editingHabitForReminder = null },
            onSave = { enabled, hour, minute, recurrenceType, intervalMinutes, ringtoneUri, ringtoneTitle ->
                onUpdateHabitReminder(
                    targetHabit,
                    enabled,
                    hour,
                    minute,
                    recurrenceType,
                    intervalMinutes,
                    ringtoneUri,
                    ringtoneTitle
                )
            }
        )
    }

    pendingDeleteAction?.let { confirmedAction ->
        LiquidDeleteConfirmationDialog(
            title = pendingDeleteTitle.ifBlank { "Confirm Deletion" },
            message = pendingDeleteMessage.ifBlank { "Are you sure you want to delete this item?" },
            onConfirm = {
                confirmedAction()
                pendingDeleteAction = null
            },
            onDismiss = { pendingDeleteAction = null }
        )
    }
}

@Composable
private fun WorkspaceNoteCard(
    note: WorkspaceNoteEntity,
    allNotes: List<WorkspaceNoteEntity> = emptyList(),
    playbackState: VoicePlaybackState,
    onTogglePlayback: (String) -> Unit,
    onSeekPlayback: (String, Long) -> Unit,
    onOpenEditor: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleCloudSync: () -> Unit,
    onAttachReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LiquidTheme.palette
    val blocks = remember(note.blocksJson) { WorkspaceBlock.parseList(note.blocksJson) }
    val outgoingCount = remember(blocks) { NoteBacklinksParser.extractLinkedTitlesFromBlocks(blocks).size }
    val backlinksCount = remember(note.id, note.title, allNotes) {
        NoteBacklinksParser.findBacklinksForNote(note, allNotes).size
    }
    val timestampLine = remember(note.createdAt, note.updatedAt) {
        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        "Created ${fmt.format(Date(note.createdAt))} • Edited ${fmt.format(Date(note.updatedAt))}"
    }
    TactileGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notion_note_card_${note.id}"),
        cornerRadius = 26.dp,
        onClick = onOpenEditor
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onToggleCloudSync,
                        shape = RoundedCornerShape(9999.dp),
                        color = if (note.cloudSync) palette.accentSoftBg else palette.recessedTrayBg,
                        modifier = Modifier.testTag("note_sync_pill_${note.id}")
                    ) {
                        Text(
                            text = if (note.cloudSync) "Cloud Synced" else "Local Device Only",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (note.cloudSync) Color(0xFF10B981) else palette.secondaryInk,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    if (note.isPinned) {
                        Surface(shape = RoundedCornerShape(9999.dp), color = CrimsonAccent) {
                            Text(
                                text = "Pinned",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (outgoingCount + backlinksCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.frostedPillBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            modifier = Modifier.testTag("note_links_badge_${note.id}")
                        ) {
                            Text(
                                text = "[[$outgoingCount • $backlinksCount back]]",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accentSoftInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                        MinimalIcon(
                            type = MinimalIconType.BOOKMARK,
                            active = note.isPinned,
                            tint = if (note.isPinned) CrimsonAccent else palette.secondaryInk,
                            size = 16.dp,
                            contentDescription = "Pin note"
                        )
                    }
                    IconButton(onClick = onAttachReminder, modifier = Modifier.size(32.dp)) {
                        MinimalIcon(type = MinimalIconType.BELL, tint = palette.primaryInk, size = 16.dp, contentDescription = "Remind")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 16.dp, contentDescription = "Delete note")
                    }
                }
            }

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )

            Text(
                text = timestampLine,
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryInk
            )

            val previewText = blocks.firstOrNull { it.text.isNotBlank() }?.text.orEmpty()
            if (previewText.isNotBlank()) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!note.audioUri.isNullOrBlank()) {
                VoiceAudioPlayerCard(
                    audioUri = note.audioUri,
                    durationMs = note.audioDurationMs,
                    playbackState = playbackState,
                    onTogglePlayback = onTogglePlayback,
                    onSeekTo = onSeekPlayback
                )
            }

            if (note.tagsCsv.isNotBlank()) {
                Text(
                    text = note.tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentSoftInk
                )
            }
        }
    }
}

@Composable
private fun WorkspaceTaskCard(
    task: WorkspaceTaskEntity,
    playbackState: VoicePlaybackState,
    onToggleCompleted: () -> Unit,
    onTogglePlayback: (String) -> Unit,
    onSeekPlayback: (String, Long) -> Unit,
    onDelete: () -> Unit
) {
    val palette = LiquidTheme.palette
    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompleted) CrimsonAccent else palette.recessedTrayBg)
                        .clickable(onClick = onToggleCompleted)
                        .testTag("task_toggle_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        MinimalIcon(type = MinimalIconType.CHECK, active = true, tint = Color.White, size = 16.dp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "${task.priority} • ${if (task.cloudSync) "Cloud" else "Local"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryInk
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 16.dp, contentDescription = "Delete task")
                }
            }
            if (!task.audioUri.isNullOrBlank()) {
                VoiceAudioPlayerCard(
                    audioUri = task.audioUri,
                    durationMs = task.audioDurationMs,
                    playbackState = playbackState,
                    onTogglePlayback = onTogglePlayback,
                    onSeekTo = onSeekPlayback
                )
            }
        }
    }
}

@Composable
private fun CreateTaskDialog(
    defaultEpochDay: Long,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String, dueDateEpochDay: Long, priority: String, cloudSync: Boolean) -> Unit
) {
    val palette = LiquidTheme.palette
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var cloudSync by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = { Text("New Task", color = palette.primaryInk) },
        text = {
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.cardSurfaceTop,
                unfocusedContainerColor = palette.cardSurfaceTop,
                focusedBorderColor = CrimsonAccent,
                unfocusedBorderColor = palette.inputBorder,
                focusedTextColor = palette.primaryInk,
                unfocusedTextColor = palette.primaryInk,
                focusedLabelColor = palette.secondaryInk,
                unfocusedLabelColor = palette.secondaryInk
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task title") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("task_title_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    colors = fieldColors,
                    shape = RoundedCornerShape(18.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { lvl ->
                        val active = priority == lvl
                        Surface(
                            onClick = { priority = lvl },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) CrimsonAccent else palette.recessedTrayBg,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = lvl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else palette.secondaryInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (cloudSync) "Cloud Synced" else "Local Device Only", color = palette.primaryInk)
                    Switch(checked = cloudSync, onCheckedChange = { cloudSync = it })
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = "Add Task",
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(title.trim(), notes.trim(), defaultEpochDay, priority, cloudSync)
                    onDismiss()
                },
                modifier = Modifier.testTag("task_confirm_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryInk) }
        }
    )
}

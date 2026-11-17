package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.repository.HabitGamificationEngine
import com.example.data.repository.WorkspaceRepository
import com.example.reminders.ReminderScheduler
import com.example.ui.components.FeaturedCrimsonBronzeCard
import com.example.ui.components.LiquidTimePickerDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.RingtonePreviewPlayer
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.VerticalLiquidCapsuleGauges
import com.example.ui.components.formatTime12h
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

fun LazyListScope.habitsTabItems(
    habits: List<HabitEntity>,
    completions: List<HabitCompletionEntity>,
    recentXpBanner: String? = null,
    onToggleToday: (habitId: String, nowDone: Boolean) -> Unit,
    onEditHabitReminder: (HabitEntity) -> Unit,
    onDeleteHabit: (HabitEntity) -> Unit,
    onOpenNewHabitDialog: () -> Unit
) {
    val todayDay = WorkspaceRepository.todayEpochDay()
    val completedDaysByHabit = completions.groupBy({ it.habitId }, { it.epochDay })
    val completionCountByDay = completions.groupingBy { it.epochDay }.eachCount()
    val gamification = HabitGamificationEngine.compute(habits, completions, todayDay)

    // Hero Gamification Progress & Level Card
    item(key = "habits_gamification_hero", contentType = "gamification_hero") {
        val palette = LiquidTheme.palette
        FeaturedCrimsonBronzeCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("habits_gamification_card"),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = CrimsonAccent
                            ) {
                                Text(
                                    text = "LV. ${gamification.level}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = gamification.rankTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                        }
                        Text(
                            text = "${gamification.bestStreak}d best streak • ${gamification.totalCompletions} check-ins",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.frostedPillBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${gamification.totalXp} XP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = palette.accentSoftInk,
                                modifier = Modifier.testTag("habits_total_xp_text")
                            )
                            Text(
                                text = "Focus Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryInk
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Level ${gamification.level + 1} Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk
                        )
                        Text(
                            text = "${gamification.xpIntoLevel} / ${gamification.xpPerLevel} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                    }
                    LinearProgressIndicator(
                        progress = { gamification.levelProgress },
                        color = CrimsonAccent,
                        trackColor = palette.barTrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(9999.dp))
                    )
                }

                AnimatedVisibility(
                    visible = !recentXpBanner.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = CrimsonAccent,
                        modifier = Modifier.testTag("habit_xp_celebration_badge")
                    ) {
                        Text(
                            text = recentXpBanner.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = "+${HabitGamificationEngine.XP_PER_COMPLETION} XP per check-in • +${HabitGamificationEngine.XP_STREAK_BONUS} XP every 3-day streak tier",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryInk
                )
            }
        }
    }

    item(key = "habits_header_cta", contentType = "habits_header") {
        val palette = LiquidTheme.palette
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daily Habits & Streaks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk
                )
                Text(
                    text = "${habits.size} active habits",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryInk
                )
            }
            TactilePrimaryPillButton(
                text = "+ New Habit",
                iconType = MinimalIconType.PLUS,
                onClick = onOpenNewHabitDialog,
                modifier = Modifier.testTag("habit_add_button")
            )
        }
    }

    if (habits.isNotEmpty()) {
        item(key = "habits_capsule_gauges", contentType = "habits_gauges") {
            val palette = LiquidTheme.palette
            val gaugeValues = habits.take(5).map { habit ->
                val daysSet = completedDaysByHabit[habit.id].orEmpty().toSet()
                val weekHits = (0..6).count { offset -> (todayDay - offset) in daysSet }
                val frac = (weekHits.toFloat() / habit.targetPerWeek.coerceAtLeast(1).toFloat()).coerceIn(0.14f, 1f)
                habit.title.take(7) to frac
            }
            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Weekly Habit Completion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    VerticalLiquidCapsuleGauges(values = gaugeValues)
                }
            }
        }
    }

    // 7x15 Activity Heatmap Matrix (105 days)
    item(key = "habits_heatmap_matrix", contentType = "habits_heatmap") {
        HabitActivityHeatmapCard(
            todayEpochDay = todayDay,
            completionCountByDay = completionCountByDay
        )
    }

    if (habits.isEmpty()) {
        item(key = "habits_empty", contentType = "empty_card") {
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Build your daily rhythm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "Create habits like Deep Reading, 20m Focus, or Review Archive to earn XP, level up, and grow your 105-day heatmap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                }
            }
        }
    } else {
        items(items = habits, key = { it.id }, contentType = { "habit_card" }) { habit ->
            val daysSet = completedDaysByHabit[habit.id].orEmpty().toSet()
            val doneToday = todayDay in daysSet
            val streak = WorkspaceRepository.computeHabitStreak(daysSet, todayDay)
            val weekHits = (0..6).count { offset -> (todayDay - offset) in daysSet }
            HabitRowCard(
                habit = habit,
                doneToday = doneToday,
                streakDays = streak,
                weekHits = weekHits,
                onToggleToday = { onToggleToday(habit.id, !doneToday) },
                onEditReminder = { onEditHabitReminder(habit) },
                onDelete = { onDeleteHabit(habit) }
            )
        }
    }
}

@Composable
private fun HabitRowCard(
    habit: HabitEntity,
    doneToday: Boolean,
    streakDays: Int,
    weekHits: Int,
    onToggleToday: () -> Unit,
    onEditReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LiquidTheme.palette
    TactileGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        cornerRadius = 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (doneToday) CrimsonAccent else palette.recessedTrayBg)
                    .border(1.5.dp, if (doneToday) CrimsonAccent else palette.inputBorder, CircleShape)
                    .clickable(onClick = onToggleToday)
                    .testTag("habit_toggle_${habit.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (doneToday) {
                    MinimalIcon(type = MinimalIconType.CHECK, active = true, tint = Color.White, size = 20.dp, contentDescription = "Completed today")
                } else {
                    Text(
                        text = "+25",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentSoftInk
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk
                )
                if (habit.subtitle.isNotBlank()) {
                    Text(
                        text = habit.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(9999.dp), color = palette.accentSoftBg) {
                        Text(
                            text = "${streakDays}d streak • +${HabitGamificationEngine.XP_PER_COMPLETION} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentSoftInk,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        onClick = onEditReminder,
                        shape = RoundedCornerShape(9999.dp),
                        color = palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.testTag("habit_reminder_chip_${habit.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MinimalIcon(type = MinimalIconType.BELL, tint = if (habit.reminderEnabled) CrimsonAccent else palette.secondaryInk, size = 11.dp)
                            Text(
                                text = if (habit.reminderEnabled) {
                                    "${formatTime12h(habit.reminderHour, habit.reminderMinute)} • ${habit.ringtoneTitle.take(14)}"
                                } else {
                                    "Set alert"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (habit.reminderEnabled) palette.primaryInk else palette.secondaryInk
                            )
                        }
                    }
                    Text(
                        text = "$weekHits/${habit.targetPerWeek}w",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryInk
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("habit_delete_button_${habit.id}")
            ) {
                MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 16.dp, contentDescription = "Delete habit")
            }
        }
    }
}

@Composable
private fun HabitActivityHeatmapCard(
    todayEpochDay: Long,
    completionCountByDay: Map<Long, Int>
) {
    val palette = LiquidTheme.palette
    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Activity Matrix (7 × 15)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                Text("105 days", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until 7) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until 15) {
                            val daysAgo = (14 - col) * 7 + (6 - row)
                            val day = todayEpochDay - daysAgo
                            val count = completionCountByDay[day] ?: 0
                            val cellColor = when {
                                count <= 0 -> palette.recessedTrayBg
                                count == 1 -> CrimsonAccent.copy(alpha = 0.35f)
                                count == 2 -> CrimsonAccent.copy(alpha = 0.68f)
                                else -> CrimsonAccent
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateHabitDialog(
    onDismiss: () -> Unit,
    onCreate: (
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
    ) -> Unit
) {
    val palette = LiquidTheme.palette
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose { RingtonePreviewPlayer.stop() }
    }

    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var targetPerWeek by remember { mutableIntStateOf(5) }
    var cloudSync by remember { mutableStateOf(true) }

    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderHour by remember { mutableIntStateOf(9) }
    var reminderMinute by remember { mutableIntStateOf(0) }
    var recurrenceType by remember { mutableStateOf(ReminderScheduler.RECURRENCE_DAILY) }
    var customMinutesText by remember { mutableStateOf("180") }
    var ringtoneUri by remember { mutableStateOf<String?>(null) }
    var ringtoneTitle by remember { mutableStateOf("Default System Sound") }
    var showTimePicker by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val picked = res.data?.let { data ->
                IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            if (picked != null) {
                ringtoneUri = picked.toString()
                ringtoneTitle = runCatching {
                    RingtoneManager.getRingtone(context, picked)?.getTitle(context)
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Selected Ringtone"
                RingtonePreviewPlayer.stop()
                isPreviewPlaying = false
            }
        }
    }

    val customAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            ringtoneUri = uri.toString()
            ringtoneTitle = uri.lastPathSegment?.substringAfterLast('/') ?: "Custom Audio"
            RingtonePreviewPlayer.stop()
            isPreviewPlaying = false
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.cardSurfaceTop,
        unfocusedContainerColor = palette.cardSurfaceTop,
        focusedBorderColor = CrimsonAccent,
        unfocusedBorderColor = palette.inputBorder,
        focusedTextColor = palette.primaryInk,
        unfocusedTextColor = palette.primaryInk,
        focusedLabelColor = palette.accentSoftInk,
        unfocusedLabelColor = palette.secondaryInk
    )

    AlertDialog(
        onDismissRequest = {
            RingtonePreviewPlayer.stop()
            onDismiss()
        },
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Create Habit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit name") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_title_input")
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Motivation / cue (optional)") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Weekly target: $targetPerWeek days", color = palette.primaryInk)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { targetPerWeek = (targetPerWeek - 1).coerceAtLeast(1) },
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) { Text("-", color = palette.primaryInk) }
                        OutlinedButton(
                            onClick = { targetPerWeek = (targetPerWeek + 1).coerceAtMost(7) },
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) { Text("+", color = palette.primaryInk) }
                    }
                }

                // Custom Reminder Time & Custom Ringtone Controls
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.recessedTrayBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Custom Reminder Alert",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryInk
                            )
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it },
                                modifier = Modifier.testTag("habit_reminder_switch")
                            )
                        }

                        if (reminderEnabled) {
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(9999.dp),
                                border = BorderStroke(1.dp, CrimsonAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("habit_time_picker_button")
                            ) {
                                Text(
                                    text = "Time: ${formatTime12h(reminderHour, reminderMinute)} (Tap to pick)",
                                    color = CrimsonAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    ReminderScheduler.RECURRENCE_DAILY to "Daily",
                                    ReminderScheduler.RECURRENCE_EVERY_2_HOURS to "Every 2h",
                                    ReminderScheduler.RECURRENCE_WEEKLY to "Weekly",
                                    ReminderScheduler.RECURRENCE_CUSTOM_MINUTES to "Custom"
                                ).forEach { (code, lbl) ->
                                    val active = recurrenceType == code
                                    Surface(
                                        onClick = { recurrenceType = code },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (active) palette.primaryButtonBg else palette.cardSurfaceTop,
                                        border = BorderStroke(1.dp, palette.specularRimOutline)
                                    ) {
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (active) palette.primaryButtonContent else palette.primaryInk,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            if (recurrenceType == ReminderScheduler.RECURRENCE_CUSTOM_MINUTES) {
                                OutlinedTextField(
                                    value = customMinutesText,
                                    onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Repeat every N minutes") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sound: $ringtoneTitle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryInk,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        if (isPreviewPlaying) {
                                            RingtonePreviewPlayer.stop()
                                            isPreviewPlaying = false
                                        } else {
                                            isPreviewPlaying = RingtonePreviewPlayer.playPreview(context, ringtoneUri)
                                        }
                                    }
                                ) {
                                    Text(if (isPreviewPlaying) "Stop" else "Preview", color = CrimsonAccent)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                            ringtoneUri?.let {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                            }
                                        }
                                        runCatching { ringtonePickerLauncher.launch(intent) }
                                    },
                                    shape = RoundedCornerShape(9999.dp)
                                ) { Text("System Sound", color = palette.primaryInk) }

                                OutlinedButton(
                                    onClick = { customAudioLauncher.launch(arrayOf("audio/*")) },
                                    shape = RoundedCornerShape(9999.dp)
                                ) { Text("Custom Audio", color = palette.primaryInk) }
                            }
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
                text = "Save Habit",
                enabled = title.isNotBlank(),
                onClick = {
                    RingtonePreviewPlayer.stop()
                    onCreate(
                        title.trim(),
                        subtitle.trim(),
                        "focus",
                        targetPerWeek,
                        cloudSync,
                        reminderEnabled,
                        reminderHour,
                        reminderMinute,
                        recurrenceType,
                        customMinutesText.toIntOrNull()?.coerceAtLeast(1) ?: 1440,
                        ringtoneUri,
                        ringtoneTitle
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("habit_confirm_button")
            )
        },
        dismissButton = {
            TextButton(
                onClick = {
                    RingtonePreviewPlayer.stop()
                    onDismiss()
                }
            ) { Text("Cancel", color = palette.secondaryInk) }
        }
    )

    if (showTimePicker) {
        LiquidTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            title = "Habit Reminder Time",
            onConfirm = { h, m ->
                reminderHour = h
                reminderMinute = m
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun EditHabitReminderDialog(
    habit: HabitEntity,
    onDismiss: () -> Unit,
    onSave: (
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        recurrenceType: String,
        intervalMinutes: Int,
        ringtoneUri: String?,
        ringtoneTitle: String
    ) -> Unit
) {
    val palette = LiquidTheme.palette
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose { RingtonePreviewPlayer.stop() }
    }

    var reminderEnabled by remember(habit.id) { mutableStateOf(habit.reminderEnabled) }
    var reminderHour by remember(habit.id) { mutableIntStateOf(habit.reminderHour) }
    var reminderMinute by remember(habit.id) { mutableIntStateOf(habit.reminderMinute) }
    var recurrenceType by remember(habit.id) { mutableStateOf(habit.recurrenceType) }
    var customMinutesText by remember(habit.id) { mutableStateOf(habit.intervalMinutes.coerceAtLeast(1).toString()) }
    var ringtoneUri by remember(habit.id) { mutableStateOf(habit.ringtoneUri) }
    var ringtoneTitle by remember(habit.id) { mutableStateOf(habit.ringtoneTitle) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val picked = res.data?.let { data ->
                IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            if (picked != null) {
                ringtoneUri = picked.toString()
                ringtoneTitle = runCatching {
                    RingtoneManager.getRingtone(context, picked)?.getTitle(context)
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Selected Ringtone"
                RingtonePreviewPlayer.stop()
                isPreviewPlaying = false
            }
        }
    }

    val customAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            ringtoneUri = uri.toString()
            ringtoneTitle = uri.lastPathSegment?.substringAfterLast('/') ?: "Custom Audio"
            RingtonePreviewPlayer.stop()
            isPreviewPlaying = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            RingtonePreviewPlayer.stop()
            onDismiss()
        },
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Habit Alert: ${habit.title}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Habit Alarm", color = palette.primaryInk)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }

                if (reminderEnabled) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, CrimsonAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Alert Time: ${formatTime12h(reminderHour, reminderMinute)}",
                            color = CrimsonAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            ReminderScheduler.RECURRENCE_DAILY to "Daily",
                            ReminderScheduler.RECURRENCE_EVERY_2_HOURS to "Every 2h",
                            ReminderScheduler.RECURRENCE_WEEKLY to "Weekly",
                            ReminderScheduler.RECURRENCE_CUSTOM_MINUTES to "Custom"
                        ).forEach { (code, lbl) ->
                            val active = recurrenceType == code
                            Surface(
                                onClick = { recurrenceType = code },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (active) palette.primaryButtonBg else palette.cardSurfaceTop,
                                border = BorderStroke(1.dp, palette.specularRimOutline)
                            ) {
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) palette.primaryButtonContent else palette.primaryInk,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (recurrenceType == ReminderScheduler.RECURRENCE_CUSTOM_MINUTES) {
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Repeat every N minutes") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.cardSurfaceTop,
                                unfocusedContainerColor = palette.cardSurfaceTop,
                                focusedBorderColor = CrimsonAccent,
                                unfocusedBorderColor = palette.inputBorder,
                                focusedTextColor = palette.primaryInk,
                                unfocusedTextColor = palette.primaryInk
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sound: $ringtoneTitle",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                if (isPreviewPlaying) {
                                    RingtonePreviewPlayer.stop()
                                    isPreviewPlaying = false
                                } else {
                                    isPreviewPlaying = RingtonePreviewPlayer.playPreview(context, ringtoneUri)
                                }
                            }
                        ) {
                            Text(if (isPreviewPlaying) "Stop" else "Preview", color = CrimsonAccent)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    ringtoneUri?.let {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                    }
                                }
                                runCatching { ringtonePickerLauncher.launch(intent) }
                            },
                            shape = RoundedCornerShape(9999.dp)
                        ) { Text("System Sound", color = palette.primaryInk) }

                        OutlinedButton(
                            onClick = { customAudioLauncher.launch(arrayOf("audio/*")) },
                            shape = RoundedCornerShape(9999.dp)
                        ) { Text("Custom Audio", color = palette.primaryInk) }
                    }
                }
            }
        },
        confirmButton = {
            TactilePrimaryPillButton(
                text = "Save Alert",
                onClick = {
                    RingtonePreviewPlayer.stop()
                    onSave(
                        reminderEnabled,
                        reminderHour,
                        reminderMinute,
                        recurrenceType,
                        customMinutesText.toIntOrNull()?.coerceAtLeast(1) ?: 1440,
                        ringtoneUri,
                        ringtoneTitle
                    )
                    onDismiss()
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = {
                    RingtonePreviewPlayer.stop()
                    onDismiss()
                }
            ) { Text("Cancel", color = palette.secondaryInk) }
        }
    )

    if (showTimePicker) {
        LiquidTimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            title = "Habit Alert Time",
            onConfirm = { h, m ->
                reminderHour = h
                reminderMinute = m
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

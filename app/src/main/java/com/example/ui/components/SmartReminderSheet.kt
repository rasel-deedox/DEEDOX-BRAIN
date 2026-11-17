package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.example.data.local.ItemEntity
import com.example.reminders.ReminderScheduler
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReminderSheet(
    initialTitle: String = "",
    initialTargetItemId: String? = null,
    initialTargetNoteId: String? = null,
    availableItems: List<ItemEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveReminder: (
        mode: String,
        title: String,
        notes: String,
        priority: String,
        triggerAtMillis: Long,
        recurrenceType: String,
        intervalMinutes: Int,
        ringtoneUri: String?,
        ringtoneTitle: String,
        cloudSync: Boolean,
        targetItemId: String?,
        targetNoteId: String?
    ) -> Unit
) {
    val palette = LiquidTheme.palette
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(Unit) {
        onDispose { RingtonePreviewPlayer.stop() }
    }

    var mode by remember(initialTargetItemId, initialTargetNoteId) {
        mutableStateOf(
            if (initialTargetItemId != null || initialTargetNoteId != null) "LIBRARY_CONTENT" else "PERSONAL"
        )
    }
    var selectedItemId by remember(initialTargetItemId) { mutableStateOf(initialTargetItemId) }
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var recurrenceType by remember { mutableStateOf(ReminderScheduler.RECURRENCE_ONCE) }
    var customIntervalMinText by remember { mutableStateOf("90") }
    var cloudSync by remember { mutableStateOf(true) }

    var triggerAtMillis by remember {
        mutableLongStateOf(System.currentTimeMillis() + 60 * 60_000L)
    }
    var customHour by remember {
        val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY))
    }
    var customMinute by remember {
        val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
        mutableIntStateOf(cal.get(Calendar.MINUTE))
    }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    var ringtoneUri by remember { mutableStateOf<String?>(null) }
    var ringtoneTitle by remember { mutableStateOf("Default System Sound") }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val canExactAlarms = remember(context) { ReminderScheduler.canScheduleExact(context) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val picked = res.data?.let { data ->
                IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            if (picked != null) {
                ringtoneUri = picked.toString()
                val label = runCatching {
                    RingtoneManager.getRingtone(context, picked)?.getTitle(context)
                }.getOrNull()
                ringtoneTitle = label?.takeIf { it.isNotBlank() } ?: "Selected Ringtone"
                isPreviewPlaying = false
                RingtonePreviewPlayer.stop()
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
            isPreviewPlaying = false
            RingtonePreviewPlayer.stop()
        }
    }

    fun applyHourMinute(hour24: Int, minute: Int, addDays: Int = 0) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = triggerAtMillis
            if (addDays > 0) add(Calendar.DAY_OF_YEAR, addDays)
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis() && addDays == 0) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        triggerAtMillis = cal.timeInMillis
        customHour = cal.get(Calendar.HOUR_OF_DAY)
        customMinute = cal.get(Calendar.MINUTE)
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

    ModalBottomSheet(
        onDismissRequest = {
            RingtonePreviewPlayer.stop()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .testTag("smart_reminder_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Smart Reminder",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )

            // Mode selector: Library Content vs Personal Life
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val modes = listOf("PERSONAL" to "Personal Life", "LIBRARY_CONTENT" to "Library Content")
                modes.forEach { (code, label) ->
                    val active = mode == code
                    Surface(
                        onClick = { mode = code },
                        shape = RoundedCornerShape(9999.dp),
                        color = if (active) palette.primaryButtonBg else palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reminder_mode_${code.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) palette.primaryButtonContent else palette.secondaryInk,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            if (mode == "LIBRARY_CONTENT" && availableItems.isNotEmpty()) {
                Text("Link Saved Resource", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableItems.take(10).forEach { candidate ->
                        val selected = selectedItemId == candidate.id
                        Surface(
                            onClick = {
                                selectedItemId = candidate.id
                                if (title.isBlank()) title = "Review: ${candidate.title}"
                            },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (selected) CrimsonAccent else palette.recessedTrayBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(
                                text = candidate.title.take(24),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder title") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / checklist context (optional)") },
                minLines = 2,
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            // Priority chips
            Text("Priority", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("HIGH", "MEDIUM", "LOW").forEach { level ->
                    val active = priority == level
                    Surface(
                        onClick = { priority = level },
                        shape = RoundedCornerShape(9999.dp),
                        color = when {
                            active && level == "HIGH" -> CrimsonAccent
                            active -> palette.primaryButtonBg
                            else -> palette.recessedTrayBg
                        },
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                active && level == "HIGH" -> Color.White
                                active -> palette.primaryButtonContent
                                else -> palette.secondaryInk
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Trigger timing presets + Exact Material 3 TimePicker button
            val formattedTrigger = remember(triggerAtMillis) {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(triggerAtMillis))
            }
            Text("Schedule: $formattedTrigger", style = MaterialTheme.typography.labelLarge, color = palette.primaryInk)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showTimePickerDialog = true },
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.2.dp, CrimsonAccent),
                    modifier = Modifier.testTag("reminder_time_picker_button")
                ) {
                    Text("Pick Exact Time (${formatTime12h(customHour, customMinute)})", color = CrimsonAccent, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        triggerAtMillis = System.currentTimeMillis() + 15 * 60_000L
                        val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
                        customHour = cal.get(Calendar.HOUR_OF_DAY)
                        customMinute = cal.get(Calendar.MINUTE)
                    },
                    shape = RoundedCornerShape(9999.dp)
                ) { Text("+15 min", color = palette.primaryInk) }

                OutlinedButton(
                    onClick = {
                        triggerAtMillis = System.currentTimeMillis() + 60 * 60_000L
                        val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
                        customHour = cal.get(Calendar.HOUR_OF_DAY)
                        customMinute = cal.get(Calendar.MINUTE)
                    },
                    shape = RoundedCornerShape(9999.dp)
                ) { Text("+1 hour", color = palette.primaryInk) }

                OutlinedButton(
                    onClick = { applyHourMinute(20, 0) },
                    shape = RoundedCornerShape(9999.dp)
                ) { Text("Tonight 8 PM", color = palette.primaryInk) }

                OutlinedButton(
                    onClick = { applyHourMinute(9, 0, addDays = 1) },
                    shape = RoundedCornerShape(9999.dp)
                ) { Text("Tomorrow 9 AM", color = palette.primaryInk) }
            }

            // Recurrence
            Text("Recurrence", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ReminderScheduler.RECURRENCE_ONCE to "Once",
                    ReminderScheduler.RECURRENCE_EVERY_2_HOURS to "Every 2h",
                    ReminderScheduler.RECURRENCE_DAILY to "Daily",
                    ReminderScheduler.RECURRENCE_WEEKLY to "Weekly",
                    ReminderScheduler.RECURRENCE_CUSTOM_MINUTES to "Custom min"
                ).forEach { (code, label) ->
                    val active = recurrenceType == code
                    Surface(
                        onClick = { recurrenceType = code },
                        shape = RoundedCornerShape(9999.dp),
                        color = if (active) palette.primaryButtonBg else palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) palette.primaryButtonContent else palette.secondaryInk,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (recurrenceType == ReminderScheduler.RECURRENCE_CUSTOM_MINUTES) {
                OutlinedTextField(
                    value = customIntervalMinText,
                    onValueChange = { customIntervalMinText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Repeat every N minutes") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Custom Ringtone & Sound Picker + Live Audio Preview
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
                            text = "Ringtone: $ringtoneTitle",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryInk,
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
                            },
                            modifier = Modifier.testTag("reminder_ringtone_preview_button")
                        ) {
                            Text(
                                text = if (isPreviewPlaying) "Stop" else "Preview",
                                color = CrimsonAccent,
                                fontWeight = FontWeight.Bold
                            )
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
                        ) {
                            Text("System Sounds", color = palette.primaryInk)
                        }
                        OutlinedButton(
                            onClick = { customAudioLauncher.launch(arrayOf("audio/*")) },
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Text("Custom Audio", color = palette.primaryInk)
                        }
                    }
                }
            }

            // Notification / Exact Alarm Permission Helpers (Play compliant)
            if (!hasNotifPermission || !canExactAlarms) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.accentSoftBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (!hasNotifPermission) "Allow notifications for reminder alerts" else "Enable exact alarms for precise firing",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.primaryInk,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }
                                }
                            }
                        ) { Text("Grant", color = CrimsonAccent, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (cloudSync) "Cloud Synced" else "Local Device Only",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.primaryInk
                )
                Switch(checked = cloudSync, onCheckedChange = { cloudSync = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        RingtonePreviewPlayer.stop()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) { Text("Cancel", color = palette.primaryInk) }

                TactilePrimaryPillButton(
                    text = "Save Reminder",
                    enabled = title.isNotBlank(),
                    onClick = {
                        RingtonePreviewPlayer.stop()
                        onSaveReminder(
                            mode,
                            title.trim(),
                            notes.trim(),
                            priority,
                            triggerAtMillis,
                            recurrenceType,
                            customIntervalMinText.toIntOrNull()?.coerceAtLeast(1) ?: 60,
                            ringtoneUri,
                            ringtoneTitle,
                            cloudSync,
                            if (mode == "LIBRARY_CONTENT") selectedItemId else null,
                            initialTargetNoteId
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reminder_save_button")
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showTimePickerDialog) {
        LiquidTimePickerDialog(
            initialHour = customHour,
            initialMinute = customMinute,
            title = "Reminder Alert Time",
            onConfirm = { h, m -> applyHourMinute(h, m) },
            onDismiss = { showTimePickerDialog = false }
        )
    }
}

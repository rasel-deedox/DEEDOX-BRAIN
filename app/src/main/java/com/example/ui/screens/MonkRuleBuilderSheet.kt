package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.monk.LaunchableAppInfo
import com.example.monk.MonkBlockerEngine
import com.example.ui.components.LiquidTimePickerDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.formatTime12h
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonkRuleBuilderSheet(
    launchableApps: List<LaunchableAppInfo>,
    onDismiss: () -> Unit,
    onSaveRule: (
        title: String,
        mode: String,
        packages: List<String>,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        daysCsv: String,
        delayMinutes: Int,
        activeDurationMinutes: Int?,
        preReminderMinutes: Int
    ) -> Unit
) {
    val palette = LiquidTheme.palette
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("Deep Work Lock") }
    var mode by remember { mutableStateOf(MonkBlockerEngine.MODE_SCHEDULED) }
    var appSearch by remember { mutableStateOf("") }
    val selectedPackages = remember { mutableStateListOf<String>() }

    var startMin by remember { mutableIntStateOf(21 * 60) }
    var endMin by remember { mutableIntStateOf(7 * 60) }
    var pickingStartTime by remember { mutableStateOf(false) }
    var pickingEndTime by remember { mutableStateOf(false) }

    val selectedDays = remember { mutableStateListOf(1, 2, 3, 4, 5, 6, 7) }
    var remindBeforeLock by remember { mutableStateOf(true) }
    var preReminderMinutes by remember { mutableIntStateOf(10) }

    var delayMinutes by remember { mutableIntStateOf(5) }
    var activeDurationMinutes by remember { mutableIntStateOf(60) }

    val filteredApps = remember(launchableApps, appSearch) {
        if (appSearch.isBlank()) launchableApps
        else launchableApps.filter {
            it.label.contains(appSearch, ignoreCase = true) ||
                it.packageName.contains(appSearch, ignoreCase = true)
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
        unfocusedLabelColor = palette.secondaryInk,
        focusedPlaceholderColor = palette.mutedIcon,
        unfocusedPlaceholderColor = palette.mutedIcon
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .testTag("monk_rule_builder_sheet")
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("monk_rule_apps_lazy_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "rule_header", contentType = "rule_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Create App Blocker Rule",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Rule Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Blocking Mode", style = MaterialTheme.typography.labelLarge, color = palette.primaryInk)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                MonkBlockerEngine.MODE_SCHEDULED to "Scheduled",
                                MonkBlockerEngine.MODE_COUNTDOWN_DELAY to "Delay",
                                MonkBlockerEngine.MODE_INSTANT to "Instant"
                            ).forEach { (code, label) ->
                                val active = mode == code
                                Surface(
                                    onClick = { mode = code },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (active) palette.primaryButtonBg else palette.recessedTrayBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("rule_mode_${code.lowercase()}")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) palette.primaryButtonContent else palette.secondaryInk,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        when (mode) {
                            MonkBlockerEngine.MODE_SCHEDULED -> {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = palette.recessedTrayBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Lock Window: ${formatTime12h(startMin / 60, startMin % 60)} -> ${formatTime12h(endMin / 60, endMin % 60)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.primaryInk
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { pickingStartTime = true },
                                                shape = RoundedCornerShape(9999.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Start: ${formatTime12h(startMin / 60, startMin % 60)}", color = palette.primaryInk, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = { pickingEndTime = true },
                                                shape = RoundedCornerShape(9999.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("End: ${formatTime12h(endMin / 60, endMin % 60)}", color = palette.primaryInk, maxLines = 1)
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S").forEach { (d, lbl) ->
                                                val on = d in selectedDays
                                                Surface(
                                                    onClick = {
                                                        if (on && selectedDays.size > 1) selectedDays.remove(d)
                                                        else if (!on) selectedDays.add(d)
                                                    },
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = if (on) CrimsonAccent else palette.cardSurfaceTop
                                                ) {
                                                    Text(
                                                        text = lbl,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (on) Color.White else palette.primaryInk,
                                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Remind $preReminderMinutes min before lock", style = MaterialTheme.typography.bodySmall, color = palette.primaryInk)
                                            Switch(checked = remindBeforeLock, onCheckedChange = { remindBeforeLock = it })
                                        }
                                        if (remindBeforeLock) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                listOf(5, 10, 15).forEach { lead ->
                                                    val active = preReminderMinutes == lead
                                                    Surface(
                                                        onClick = { preReminderMinutes = lead },
                                                        shape = RoundedCornerShape(9999.dp),
                                                        color = if (active) palette.primaryButtonBg else palette.cardSurfaceTop,
                                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                                        modifier = Modifier.testTag("pre_reminder_${lead}m")
                                                    ) {
                                                        Text(
                                                            text = "${lead}m before",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (active) palette.primaryButtonContent else palette.primaryInk,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            MonkBlockerEngine.MODE_COUNTDOWN_DELAY -> {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = palette.recessedTrayBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Lock starts in $delayMinutes min • Active for $activeDurationMinutes min",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.primaryInk
                                        )
                                        Text("Start Delay", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(2, 5, 10, 20, 60).forEach { mins ->
                                                val active = delayMinutes == mins
                                                Surface(
                                                    onClick = { delayMinutes = mins },
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = if (active) palette.primaryButtonBg else palette.cardSurfaceTop,
                                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                                    modifier = Modifier.testTag("delay_preset_${mins}m")
                                                ) {
                                                    Text(
                                                        text = "${mins}m",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (active) palette.primaryButtonContent else palette.primaryInk,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text("Lock Duration", style = MaterialTheme.typography.labelMedium, color = palette.secondaryInk)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(30, 60, 120).forEach { mins ->
                                                val active = activeDurationMinutes == mins
                                                Surface(
                                                    onClick = { activeDurationMinutes = mins },
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = if (active) CrimsonAccent else palette.cardSurfaceTop,
                                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                                ) {
                                                    Text(
                                                        text = "${mins}m lock",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (active) Color.White else palette.primaryInk,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            MonkBlockerEngine.MODE_INSTANT -> {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = palette.recessedTrayBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Instant Lock Duration: $activeDurationMinutes min", color = palette.primaryInk)
                                        OutlinedButton(onClick = { activeDurationMinutes = if (activeDurationMinutes >= 180) 30 else activeDurationMinutes + 30 }) {
                                            Text("+30m", color = palette.primaryInk)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Apps to Block (${selectedPackages.size} selected)",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "${filteredApps.size} of ${launchableApps.size} apps",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryInk
                            )
                        }

                        OutlinedTextField(
                            value = appSearch,
                            onValueChange = { appSearch = it },
                            placeholder = { Text("Search all installed apps...") },
                            leadingIcon = {
                                MinimalIcon(type = MinimalIconType.SEARCH, tint = palette.secondaryInk, size = 18.dp)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(9999.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("monk_app_search_input")
                        )
                    }
                }

                if (filteredApps.isEmpty()) {
                    item(key = "no_apps_found", contentType = "empty") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.recessedTrayBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No matching launchable apps detected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                        contentType = { "installed_app_row" }
                    ) { app ->
                        val checked = app.packageName in selectedPackages
                        Surface(
                            onClick = {
                                if (checked) selectedPackages.remove(app.packageName)
                                else selectedPackages.add(app.packageName)
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (checked) palette.accentSoftBg else palette.cardSurfaceTop,
                            border = BorderStroke(
                                width = if (checked) 1.4.dp else 1.dp,
                                color = if (checked) CrimsonAccent else palette.specularRimOutline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("monk_app_row_${app.packageName}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Real cached high-resolution App Icon with crisp fallback badge
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(palette.recessedTrayBg)
                                        .border(1.dp, palette.specularRimOutline, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!app.iconUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(app.iconUri)
                                                .size(96, 96)
                                                .crossfade(false)
                                                .build(),
                                            contentDescription = "${app.label} icon",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .testTag("monk_app_icon_${app.packageName}")
                                        )
                                    } else {
                                        Text(
                                            text = app.label.take(1).uppercase().ifBlank { "A" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonAccent
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.primaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (app.isSuggestedDistraction) {
                                            Surface(
                                                shape = RoundedCornerShape(9999.dp),
                                                color = palette.accentSoftBg
                                            ) {
                                                Text(
                                                    text = "Suggested",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.accentSoftInk,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondaryInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Checkbox(
                                    checked = checked,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CrimsonAccent,
                                        uncheckedColor = palette.secondaryInk,
                                        checkmarkColor = Color.White
                                    ),
                                    onCheckedChange = { on ->
                                        if (on && !selectedPackages.contains(app.packageName)) selectedPackages.add(app.packageName)
                                        else if (!on) selectedPackages.remove(app.packageName)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom sticky action bar
            Surface(
                color = palette.secondaryPillBg,
                border = BorderStroke(1.dp, palette.specularRimOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) { Text("Cancel", color = palette.primaryInk) }

                    TactilePrimaryPillButton(
                        text = "Activate Rule (${selectedPackages.size})",
                        enabled = selectedPackages.isNotEmpty() && title.isNotBlank(),
                        onClick = {
                            onSaveRule(
                                title.trim(),
                                mode,
                                selectedPackages.toList(),
                                startMin,
                                endMin,
                                selectedDays.sorted().joinToString(","),
                                delayMinutes,
                                if (mode == MonkBlockerEngine.MODE_SCHEDULED) null else activeDurationMinutes,
                                if (mode == MonkBlockerEngine.MODE_SCHEDULED && remindBeforeLock) preReminderMinutes else 0
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("monk_activate_rule_button")
                    )
                }
            }
        }
    }

    if (pickingStartTime) {
        LiquidTimePickerDialog(
            initialHour = startMin / 60,
            initialMinute = startMin % 60,
            title = "Lock Start Time",
            onConfirm = { h, m -> startMin = h * 60 + m },
            onDismiss = { pickingStartTime = false }
        )
    }

    if (pickingEndTime) {
        LiquidTimePickerDialog(
            initialHour = endMin / 60,
            initialMinute = endMin % 60,
            title = "Lock End Time",
            onConfirm = { h, m -> endMin = h * 60 + m },
            onDismiss = { pickingEndTime = false }
        )
    }
}

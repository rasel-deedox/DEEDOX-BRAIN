package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.BlockerRuleEntity
import com.example.data.local.DetoxSessionState
import com.example.data.media.VideoThumbnailGenerator
import com.example.monk.LaunchableAppInfo
import com.example.monk.MonkBlockerEngine
import com.example.monk.MonkPermissionsStatus
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.EmergencyExitDialog
import com.example.ui.components.FeaturedCrimsonBronzeCard
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.LiquidDeleteConfirmationDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.viewmodel.WorkspaceSnapshot
import kotlinx.coroutines.delay

@Composable
fun MonkModeScreen(
    workspaceSnapshot: WorkspaceSnapshot,
    permissionsStatus: MonkPermissionsStatus,
    launchableApps: List<LaunchableAppInfo>,
    onRefreshMonkState: () -> Unit,
    onStartFocusSession: (label: String, plannedMinutes: Int) -> Unit,
    onCompleteFocusSession: (emergencyUnlocked: Boolean) -> Unit,
    onSaveBlockerRule: (
        title: String,
        mode: String,
        packages: List<String>,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        daysCsv: String,
        delayMinutes: Int,
        activeDurationMinutes: Int?,
        preReminderMinutes: Int
    ) -> Unit,
    onToggleBlockerRule: (BlockerRuleEntity) -> Unit,
    onDeleteBlockerRule: (String) -> Unit,
    detoxState: DetoxSessionState = DetoxSessionState(),
    onStartDetox: (hours: Int, greyscaleUi: Boolean, suppressNotifications: Boolean, whitelistPackages: Set<String>) -> Unit = { _, _, _, _ -> },
    onEmergencyUnlockDetox: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val palette = LiquidTheme.palette
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        onRefreshMonkState()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshMonkState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onRefreshMonkState() }

    val activeSession = remember(workspaceSnapshot.focusSessions) {
        workspaceSnapshot.focusSessions.firstOrNull { it.endedAt == null }
    }
    var remainingMs by remember(activeSession?.id) { mutableLongStateOf(0L) }
    LaunchedEffect(activeSession?.id, activeSession?.startedAt, activeSession?.plannedMinutes) {
        while (activeSession != null) {
            val endAt = activeSession.startedAt + activeSession.plannedMinutes * 60_000L
            val diff = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
            remainingMs = diff
            if (diff == 0L) {
                onCompleteFocusSession(false)
                break
            }
            delay(1000L)
        }
    }

    var selectedPresetMin by remember { mutableIntStateOf(25) }
    var sessionLabel by remember { mutableStateOf("Deep Focus Sprint") }
    var showPermissionsChecklist by remember { mutableStateOf(false) }
    var showRuleBuilder by remember { mutableStateOf(false) }
    var showEmergencyExit by remember { mutableStateOf(false) }
    var showDetoxEmergencyExit by remember { mutableStateOf(false) }
    var deleteTargetRule by remember { mutableStateOf<BlockerRuleEntity?>(null) }

    // Digital Detox state
    var detoxHours by remember { mutableIntStateOf(2) }
    var detoxGreyscale by remember { mutableStateOf(true) }
    var detoxSuppressNotif by remember { mutableStateOf(true) }
    val detoxWhitelist = remember { mutableStateListOf<String>() }
    var showWhitelistPicker by remember { mutableStateOf(false) }
    var detoxRemainingMs by remember(detoxState.endsAtMillis, detoxState.isActive) {
        mutableLongStateOf((detoxState.endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
    }
    LaunchedEffect(detoxState.isActive, detoxState.endsAtMillis) {
        while (detoxState.isActive) {
            val diff = (detoxState.endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            detoxRemainingMs = diff
            if (diff == 0L) break
            delay(1000L)
        }
    }
    val detoxRunning = detoxState.isActive && detoxRemainingMs > 0L

    val readyCount = remember(permissionsStatus) {
        listOf(
            permissionsStatus.hasNotifications,
            permissionsStatus.hasExactAlarms,
            permissionsStatus.hasUsageStats,
            permissionsStatus.hasOverlay,
            permissionsStatus.hasAccessibility,
            permissionsStatus.hasBatteryExemption
        ).count { it }
    }

    val completedSessions = remember(workspaceSnapshot.focusSessions) {
        workspaceSnapshot.focusSessions.filter { it.endedAt != null }
    }
    val totalFocusMinutes = remember(completedSessions) {
        completedSessions.sumOf { it.plannedMinutes }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .testTag("monk_mode_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "monk_header", contentType = "header") {
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
                            modifier = Modifier.testTag("monk_back_button")
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Monk Mode",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "Distraction shield, strict app blocker & anti-kill protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                }
            }

            // Live Focus Timer Card
            item(key = "monk_timer_card", contentType = "timer_card") {
                FeaturedCrimsonBronzeCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 30.dp) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (activeSession != null) activeSession.label else "MONK FOCUS TIMER",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonAccent
                        )
                        Text(
                            text = if (activeSession != null) {
                                VideoThumbnailGenerator.formatDurationLabel(remainingMs)
                            } else {
                                VideoThumbnailGenerator.formatDurationLabel(selectedPresetMin * 60_000L)
                            },
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = palette.primaryInk,
                            modifier = Modifier.testTag("monk_timer_readout")
                        )
                        Text(
                            text = "${completedSessions.size} sessions • ${totalFocusMinutes}m focused",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )

                        if (activeSession == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 25, 45, 60, 90).forEach { mins ->
                                    val active = selectedPresetMin == mins
                                    Surface(
                                        onClick = { selectedPresetMin = mins },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (active) palette.primaryButtonBg else palette.frostedPillBg
                                    ) {
                                        Text(
                                            text = "${mins}m",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) palette.primaryButtonContent else palette.primaryInk,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = sessionLabel,
                                onValueChange = { sessionLabel = it },
                                label = { Text("Focus goal") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = palette.cardSurfaceTop,
                                    unfocusedContainerColor = palette.cardSurfaceTop,
                                    focusedBorderColor = CrimsonAccent,
                                    unfocusedBorderColor = palette.inputBorder,
                                    focusedTextColor = palette.primaryInk,
                                    unfocusedTextColor = palette.primaryInk,
                                    focusedLabelColor = palette.secondaryInk,
                                    unfocusedLabelColor = palette.secondaryInk
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            TactilePrimaryPillButton(
                                text = "Start Monk Session ($selectedPresetMin min)",
                                iconType = MinimalIconType.SHIELD,
                                onClick = { onStartFocusSession(sessionLabel.ifBlank { "Monk Focus" }, selectedPresetMin) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("monk_start_session_button")
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                TactilePrimaryPillButton(
                                    text = "Complete Now",
                                    onClick = { onCompleteFocusSession(false) },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showEmergencyExit = true },
                                    shape = RoundedCornerShape(9999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("monk_emergency_exit_button")
                                ) {
                                    Text("Emergency Exit", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Digital Detox Full Phone Lockdown Card
            item(key = "monk_digital_detox_card", contentType = "detox_card") {
                TactileGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("digital_detox_card"),
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
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Digital Detox Lockdown",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = if (detoxRunning) {
                                        "Active ${detoxState.durationHours}h lock • All non-whitelisted apps shielded"
                                    } else {
                                        "Block every distracting app except essentials for 2h, 6h, or 24h"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = if (detoxRunning) CrimsonAccent else palette.accentSoftBg
                            ) {
                                Text(
                                    text = if (detoxRunning) "LOCKED" else "STANDBY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (detoxRunning) Color.White else palette.accentSoftInk,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        if (detoxRunning) {
                            val hrs = detoxRemainingMs / 3_600_000L
                            val mins = (detoxRemainingMs % 3_600_000L) / 60_000L
                            val secs = (detoxRemainingMs % 60_000L) / 1000L
                            Text(
                                text = String.format("%02d:%02d:%02d", hrs, mins, secs),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = CrimsonAccent,
                                modifier = Modifier.testTag("detox_countdown_readout")
                            )
                            Text(
                                text = buildString {
                                    append("Whitelisted apps: ${detoxState.whitelistedSet().size}")
                                    if (detoxState.greyscaleUi) append(" • Greyscale Active")
                                    if (detoxState.suppressNotifications) append(" • Silent Shield")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryInk
                            )
                            Button(
                                onClick = { showDetoxEmergencyExit = true },
                                shape = RoundedCornerShape(9999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("detox_emergency_unlock_button")
                            ) {
                                Text("Emergency Unlock (3-Step Challenge)", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 6, 24).forEach { hrs ->
                                    val selected = detoxHours == hrs
                                    Surface(
                                        onClick = { detoxHours = hrs },
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (selected) palette.primaryButtonBg else palette.recessedTrayBg,
                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("detox_preset_${hrs}h")
                                    ) {
                                        Text(
                                            text = "${hrs}h Lock",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Greyscale UI Calm Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.primaryInk)
                                    Text("Desaturate Monk view to curb dopamine hooks", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                }
                                Switch(
                                    checked = detoxGreyscale,
                                    onCheckedChange = { detoxGreyscale = it },
                                    modifier = Modifier.testTag("detox_greyscale_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Suppress Distracting Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.primaryInk)
                                    Text("Keep notifications muted during Detox window", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                }
                                Switch(
                                    checked = detoxSuppressNotif,
                                    onCheckedChange = { detoxSuppressNotif = it },
                                    modifier = Modifier.testTag("detox_suppress_notif_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showWhitelistPicker = true },
                                    shape = RoundedCornerShape(9999.dp),
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("detox_whitelist_button")
                                ) {
                                    Text("Whitelist (${detoxWhitelist.size})", color = palette.primaryInk, fontWeight = FontWeight.SemiBold)
                                }

                                TactilePrimaryPillButton(
                                    text = "Lock (${detoxHours}h)",
                                    iconType = MinimalIconType.LOCK,
                                    onClick = {
                                        onStartDetox(
                                            detoxHours,
                                            detoxGreyscale,
                                            detoxSuppressNotif,
                                            detoxWhitelist.toSet()
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detox_start_button")
                                )
                            }
                        }
                    }
                }
            }

            // Anti-Kill Permission Onboarding Banner + Expandable Cards
            item(key = "monk_permissions_card", contentType = "permissions_card") {
                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Anti-Kill Protection ($readyCount/6 Ready)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                Text("Grant system permissions so the shield stays active cleanly.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                            }
                            TextButton(
                                onClick = { showPermissionsChecklist = !showPermissionsChecklist },
                                modifier = Modifier.testTag("monk_toggle_permissions_button")
                            ) {
                                Text(if (showPermissionsChecklist) "Hide" else "Configure", color = CrimsonAccent, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (showPermissionsChecklist) {
                            PermissionOnboardingRow("Notifications", permissionsStatus.hasNotifications) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            PermissionOnboardingRow("Exact Alarms", permissionsStatus.hasExactAlarms) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                                    }
                                }
                            }
                            PermissionOnboardingRow("Usage Stats Access", permissionsStatus.hasUsageStats) {
                                runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                            }
                            PermissionOnboardingRow("Display Over Other Apps", permissionsStatus.hasOverlay) {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                }
                            }
                            PermissionOnboardingRow("Accessibility Shield Service", permissionsStatus.hasAccessibility) {
                                runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                            }
                            PermissionOnboardingRow("Ignore Battery Optimization", permissionsStatus.hasBatteryExemption) {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
                                }
                            }
                        }
                    }
                }
            }

            // App Blocker Rules Header + CTA
            item(key = "monk_rules_header", contentType = "rules_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("App Blocker Rules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                        Text("${workspaceSnapshot.blockerRules.size} rules • Multi-mode lock", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                    }
                    TactilePrimaryPillButton(
                        text = "+ Add Rule",
                        iconType = MinimalIconType.PLUS,
                        onClick = { showRuleBuilder = true },
                        modifier = Modifier.testTag("monk_add_rule_button")
                    )
                }
            }

            if (workspaceSnapshot.blockerRules.isEmpty()) {
                item(key = "monk_rules_empty", contentType = "empty_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No blocker rules configured", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                            Text("Block distracting apps via Scheduled Windows, Countdown Delay, or Instant Lock.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                        }
                    }
                }
            } else {
                items(items = workspaceSnapshot.blockerRules, key = { it.id }, contentType = { "blocker_rule" }) { rule ->
                    val pkgCount = remember(rule.blockedPackagesJson) { MonkBlockerEngine.parsePackages(rule.blockedPackagesJson).size }
                    val subtitle = when (rule.mode) {
                        MonkBlockerEngine.MODE_SCHEDULED -> "${MonkBlockerEngine.formatMinuteOfDay(rule.startMinuteOfDay)}–${MonkBlockerEngine.formatMinuteOfDay(rule.endMinuteOfDay)} • $pkgCount apps"
                        MonkBlockerEngine.MODE_COUNTDOWN_DELAY -> "Delay Lock • $pkgCount apps"
                        else -> "Instant Lock • $pkgCount apps"
                    }
                    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = rule.isEnabled, onCheckedChange = { onToggleBlockerRule(rule) })
                                IconButton(onClick = {
                                    if (rule.isEnabled) showEmergencyExit = true else deleteTargetRule = rule
                                }) {
                                    MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 16.dp, contentDescription = "Remove rule")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRuleBuilder) {
        MonkRuleBuilderSheet(
            launchableApps = launchableApps,
            onDismiss = { showRuleBuilder = false },
            onSaveRule = onSaveBlockerRule
        )
    }

    if (showEmergencyExit) {
        EmergencyExitDialog(
            targetRuleTitle = activeSession?.label ?: "Monk Shield Rules",
            onDismiss = { showEmergencyExit = false },
            onUnlockConfirmed = {
                showEmergencyExit = false
                if (activeSession != null) {
                    onCompleteFocusSession(true)
                }
                workspaceSnapshot.blockerRules.filter { it.isEnabled }.forEach { onToggleBlockerRule(it) }
            }
        )
    }

    if (showDetoxEmergencyExit) {
        EmergencyExitDialog(
            targetRuleTitle = "${detoxState.durationHours}h Digital Detox Lockdown",
            onDismiss = { showDetoxEmergencyExit = false },
            onUnlockConfirmed = {
                showDetoxEmergencyExit = false
                onEmergencyUnlockDetox()
            }
        )
    }

    if (showWhitelistPicker) {
        Dialog(
            onDismissRequest = { showWhitelistPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = palette.secondaryPillBg,
                border = BorderStroke(1.dp, palette.specularBorderTop)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Essential Detox Whitelist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "Selected apps stay accessible during Digital Detox. System dialer and home launcher always remain allowed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(launchableApps, key = { it.packageName }) { app ->
                            val allowed = app.packageName in detoxWhitelist
                            Surface(
                                onClick = {
                                    if (allowed) detoxWhitelist.remove(app.packageName)
                                    else detoxWhitelist.add(app.packageName)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (allowed) palette.accentSoftBg else palette.cardSurfaceTop,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.primaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Checkbox(
                                        checked = allowed,
                                        colors = CheckboxDefaults.colors(checkedColor = CrimsonAccent),
                                        onCheckedChange = { checked ->
                                            if (checked && !detoxWhitelist.contains(app.packageName)) detoxWhitelist.add(app.packageName)
                                            else if (!checked) detoxWhitelist.remove(app.packageName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    TactilePrimaryPillButton(
                        text = "Done (${detoxWhitelist.size} Allowed)",
                        onClick = { showWhitelistPicker = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    deleteTargetRule?.let { targetRule ->
        LiquidDeleteConfirmationDialog(
            title = "Delete Blocker Rule?",
            message = "Permanently remove \"${targetRule.title}\" from Monk Mode?",
            onConfirm = {
                onDeleteBlockerRule(targetRule.id)
                deleteTargetRule = null
            },
            onDismiss = { deleteTargetRule = null }
        )
    }
}

@Composable
private fun PermissionOnboardingRow(
    title: String,
    granted: Boolean,
    onGrantClick: () -> Unit
) {
    val palette = LiquidTheme.palette
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.recessedTrayBg,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.primaryInk)
                Text(
                    text = if (granted) "Ready" else "Action needed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) Color(0xFF10B981) else CrimsonAccent
                )
            }
            if (!granted) {
                OutlinedButton(onClick = onGrantClick, shape = RoundedCornerShape(9999.dp)) {
                    Text("Grant", color = palette.primaryInk)
                }
            }
        }
    }
}

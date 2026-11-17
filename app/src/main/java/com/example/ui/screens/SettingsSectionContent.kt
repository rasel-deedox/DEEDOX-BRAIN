package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.UserSettingsEntity
import com.example.ui.components.AppLogo
import com.example.ui.components.LiquidThemePillSwitcher
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.example.ui.theme.LiquidTheme

fun LazyListScope.settingsSectionItems(
    userDisplayName: String,
    userEmail: String?,
    isSignedIn: Boolean,
    themeMode: AppThemeMode,
    accentPreset: LiquidAccentPreset,
    vaultPin: String?,
    syncBadgeState: SyncBadgeState,
    settings: UserSettingsEntity,
    onUpdateProfileName: (String) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectAccentPreset: (LiquidAccentPreset) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onOpenDeviceSyncModal: () -> Unit,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit,
    onSecretAdminTrigger: () -> Unit
) {
    item(key = "settings_profile") {
        SettingsProfileCard(
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            isSignedIn = isSignedIn,
            syncBadgeState = syncBadgeState,
            onUpdateProfileName = onUpdateProfileName,
            onGoogleSignInClick = onGoogleSignInClick,
            onSignOutClick = onSignOutClick,
            onOpenDeviceSyncModal = onOpenDeviceSyncModal
        )
    }

    item(key = "settings_appearance") {
        SettingsThemeAndAppearanceCard(
            themeMode = themeMode,
            accentPreset = accentPreset,
            hasVaultPin = !vaultPin.isNullOrBlank(),
            onSelectThemeMode = onSelectThemeMode,
            onSelectAccentPreset = onSelectAccentPreset
        )
    }

    item(key = "settings_preferences") {
        SettingsPreferencesCard(
            settings = settings,
            onUpdateSettings = onUpdateSettings
        )
    }

    item(key = "settings_version") {
        SettingsVersionFooterCard(onSecretAdminTrigger = onSecretAdminTrigger)
    }
}

@Composable
private fun SettingsProfileCard(
    userDisplayName: String,
    userEmail: String?,
    isSignedIn: Boolean,
    syncBadgeState: SyncBadgeState,
    onUpdateProfileName: (String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onOpenDeviceSyncModal: () -> Unit
) {
    val palette = LiquidTheme.palette
    var draftName by remember(userDisplayName) { mutableStateOf(userDisplayName) }
    val initial = userDisplayName.trim().take(1).ifBlank { "A" }.uppercase()

    TactileGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_profile_card"),
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(10.dp, CircleShape, ambientColor = palette.accentCrimson.copy(alpha = 0.25f))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.accentTerracottaTop, palette.accentTerracottaBottom)
                                )
                            )
                            .border(1.2.dp, palette.specularBorderTop, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile & Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = if (isSignedIn) (userEmail ?: "Google Account Connected")
                            else "Guest Mode • Local SQLite Vault",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                SyncStatusPill(badgeState = syncBadgeState, onClick = onOpenDeviceSyncModal)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.recessedTrayBg,
                        unfocusedContainerColor = palette.recessedTrayBg,
                        focusedBorderColor = palette.accentCrimson,
                        unfocusedBorderColor = palette.specularRimOutline,
                        focusedTextColor = palette.primaryInk,
                        unfocusedTextColor = palette.primaryInk
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_display_name_input")
                )
                Button(
                    onClick = { onUpdateProfileName(draftName) },
                    enabled = draftName.trim().isNotBlank() && draftName.trim() != userDisplayName.trim(),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonBg,
                        contentColor = palette.primaryButtonContent
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("settings_save_name_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isSignedIn) {
                    GoogleSignInLiquidPillButton(
                        text = "Sign in with Google",
                        onClick = onGoogleSignInClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_google_signin_button")
                    )
                } else {
                    OutlinedButton(
                        onClick = onOpenDeviceSyncModal,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Devices & Sync", color = palette.primaryInk)
                    }
                    OutlinedButton(
                        onClick = onSignOutClick,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("settings_signout_button")
                    ) {
                        Text("Sign Out", color = palette.accentCrimson)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsThemeAndAppearanceCard(
    themeMode: AppThemeMode,
    accentPreset: LiquidAccentPreset,
    hasVaultPin: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectAccentPreset: (LiquidAccentPreset) -> Unit
) {
    val palette = LiquidTheme.palette
    TactileGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_theme_appearance_card"),
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinimalIcon(
                        type = MinimalIconType.SPARKLE,
                        active = true,
                        tint = palette.accentSoftInk,
                        size = 20.dp
                    )
                    Column {
                        Text(
                            text = "Theme & Appearance",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "Personal AI Library • Liquid Glass System",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Surface Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.secondaryInk
                )
                LiquidThemePillSwitcher(
                    selectedMode = themeMode,
                    onSelectMode = onSelectThemeMode,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Curated Liquid Glass Accent",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.secondaryInk
                )
                RecessedFramingBezel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidAccentPreset.entries.forEach { preset ->
                            val selected = preset == accentPreset
                            Surface(
                                onClick = { onSelectAccentPreset(preset) },
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) palette.cardSurfaceTop else Color.Transparent,
                                border = BorderStroke(
                                    width = if (selected) 1.4.dp else 1.dp,
                                    color = if (selected) preset.primaryAccent else palette.specularRimOutline
                                ),
                                shadowElevation = if (selected) 4.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("accent_preset_${preset.code.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .shadow(
                                                elevation = if (selected) 8.dp else 2.dp,
                                                shape = CircleShape,
                                                ambientColor = preset.primaryAccent
                                            )
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(preset.gradientTop, preset.gradientBottom)
                                                )
                                            )
                                            .border(1.2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primaryInk
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (selected) {
                                        MinimalIcon(
                                            type = MinimalIconType.CHECK,
                                            active = true,
                                            tint = preset.primaryAccent,
                                            size = 18.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.recessedTrayBg,
                border = BorderStroke(1.dp, palette.specularRimOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinimalIcon(
                            type = MinimalIconType.LOCK,
                            active = hasVaultPin,
                            tint = if (hasVaultPin) palette.accentSoftInk else palette.secondaryInk,
                            size = 16.dp
                        )
                        Text(
                            text = if (hasVaultPin) "Vault 4-Digit PIN Active" else "Vault PIN Not Configured",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryInk
                        )
                    }
                    Text(
                        text = "Manage in Vault",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryInk
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPreferencesCard(
    settings: UserSettingsEntity,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit
) {
    val palette = LiquidTheme.palette
    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MinimalIcon(
                    type = MinimalIconType.SHIELD,
                    active = true,
                    tint = palette.accentSoftInk,
                    size = 20.dp
                )
                Text(
                    text = "Privacy, AI & Sync Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.primaryInk
                )
            }

            SettingsPreferenceToggleRow(
                title = "Online AI Features",
                subtitle = "Allow AI Assistant, Online Rerank & Organization Preview when online.",
                checked = settings.aiEnabled,
                testTag = "settings_toggle_ai_enabled",
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(aiEnabled = enabled) } }
            )
            SettingsPreferenceToggleRow(
                title = "Include Personal Notes in AI Context",
                subtitle = "Default OFF for privacy. Enable only if you want AI to read notes.",
                checked = settings.includeNotesInAi,
                testTag = "settings_toggle_notes_in_ai",
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(includeNotesInAi = enabled) } }
            )
            SettingsPreferenceToggleRow(
                title = "Auto Metadata Enrichment",
                subtitle = "Fetch page titles and previews in background when online.",
                checked = settings.autoMetadataEnrichment,
                testTag = "settings_toggle_auto_enrichment",
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(autoMetadataEnrichment = enabled) } }
            )
            SettingsPreferenceToggleRow(
                title = "Cloud Sync Opt-In (Metadata Only)",
                subtitle = "Sync bookmarks, folders & theme across devices via Firestore.",
                checked = settings.cloudSyncOptIn,
                testTag = "settings_toggle_cloud_sync",
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(cloudSyncOptIn = enabled) } }
            )
        }
    }
}

@Composable
private fun SettingsPreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LiquidTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.accentCrimson,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = palette.recessedTrayBg
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

private enum class AdminUnlockStage {
    IDLE,
    SUBVERSION_METRICS,
    PRIMARY_ERROR,
    SECONDARY_EXIT_PROMPT
}

private data class SubVersionMetricRow(
    val label: String,
    val detail: String
)

private val SUBVERSION_METRICS = listOf(
    SubVersionMetricRow("Core Runtime Kernel", "v1.0.4-arm64"),
    SubVersionMetricRow("SQLite FTS5 Indexer", "Schema Rev 2"),
    SubVersionMetricRow("Integrity Guard Build", "SHA-256 / Vault"),
    SubVersionMetricRow("Telemetry Sync Bus", "TLS 1.3 Live")
)

@Composable
internal fun SettingsVersionFooterCard(
    onSecretAdminTrigger: () -> Unit,
    clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    val palette = LiquidTheme.palette
    var stage by remember { mutableStateOf(AdminUnlockStage.IDLE) }

    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastVersionTapMs by remember { mutableLongStateOf(0L) }

    var thirdMetricTapCount by remember { mutableIntStateOf(0) }
    var lastMetricTapMs by remember { mutableLongStateOf(0L) }

    var errorBannerTapCount by remember { mutableIntStateOf(0) }
    var lastErrorTapMs by remember { mutableLongStateOf(0L) }

    var exitTapCount by remember { mutableIntStateOf(0) }
    var lastExitTapMs by remember { mutableLongStateOf(0L) }

    fun resetAllStages() {
        stage = AdminUnlockStage.IDLE
        versionTapCount = 0
        thirdMetricTapCount = 0
        errorBannerTapCount = 0
        exitTapCount = 0
    }

    TactileGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_app_version_row")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(),
                        onClick = {
                            val now = clockMillis()
                            if (stage != AdminUnlockStage.IDLE) {
                                resetAllStages()
                                return@clickable
                            }
                            val next = if (now - lastVersionTapMs <= 3500L) versionTapCount + 1 else 1
                            lastVersionTapMs = now
                            if (next >= 5) {
                                versionTapCount = 0
                                thirdMetricTapCount = 0
                                stage = AdminUnlockStage.SUBVERSION_METRICS
                            } else {
                                versionTapCount = next
                            }
                        }
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AppLogo(
                        size = 44.dp,
                        cornerRadius = 14.dp,
                        testTag = "settings_about_app_logo"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Personal AI Library",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "Version 1.0 (Build 100) • Offline-first SQLite & Firestore",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = palette.recessedTrayBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline)
                ) {
                    Text(
                        text = "v1.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.secondaryInk,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            // Step 1 -> Reveal hidden sub-version metrics panel
            AnimatedVisibility(
                visible = stage != AdminUnlockStage.IDLE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .testTag("settings_subversion_metrics_panel"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecessedFramingBezel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        innerPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SUBVERSION_METRICS.forEachIndexed { index, metric ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.cardSurfaceTop.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, palette.specularRimOutline.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_subversion_metric_$index")
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = androidx.compose.material3.ripple(),
                                            onClick = {
                                                val now = clockMillis()
                                                if (index == 2 && stage == AdminUnlockStage.SUBVERSION_METRICS) {
                                                    val next = if (now - lastMetricTapMs <= 3500L) thirdMetricTapCount + 1 else 1
                                                    lastMetricTapMs = now
                                                    if (next >= 3) {
                                                        thirdMetricTapCount = 0
                                                        errorBannerTapCount = 0
                                                        stage = AdminUnlockStage.PRIMARY_ERROR
                                                    } else {
                                                        thirdMetricTapCount = next
                                                    }
                                                } else {
                                                    thirdMetricTapCount = 0
                                                }
                                            }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = metric.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.primaryInk
                                        )
                                        Text(
                                            text = metric.detail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryInk
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Step 2 -> Simulated System Error Banner (tap 3x to reveal secondary Exit prompt)
                    AnimatedVisibility(
                        visible = stage == AdminUnlockStage.PRIMARY_ERROR,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.accentSoftBg,
                            border = BorderStroke(1.dp, palette.accentCrimson.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_hidden_error_banner")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple(),
                                    onClick = {
                                        val now = clockMillis()
                                        val next = if (now - lastErrorTapMs <= 3500L) errorBannerTapCount + 1 else 1
                                        lastErrorTapMs = now
                                        if (next >= 3) {
                                            errorBannerTapCount = 0
                                            exitTapCount = 0
                                            stage = AdminUnlockStage.SECONDARY_EXIT_PROMPT
                                        } else {
                                            errorBannerTapCount = next
                                        }
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "System Warning: Runtime Checksum Drift (ERR-0x4B)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentSoftInk
                                )
                                Text(
                                    text = "Integrity subsystem reported a stale build hash. Tap banner to inspect diagnostic status.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.primaryInk
                                )
                            }
                        }
                    }

                    // Step 3 -> Secondary Error Prompt with "Exit" action (3 rapid Exit taps -> Admin Password Modal)
                    AnimatedVisibility(
                        visible = stage == AdminUnlockStage.SECONDARY_EXIT_PROMPT,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.cardSurfaceTop,
                            border = BorderStroke(1.2.dp, palette.accentCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_hidden_secondary_error_prompt")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Critical Diagnostic Fault: Session Halted",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentCrimson
                                    )
                                    Text(
                                        text = "Extended trace buffer exceeded threshold. Close diagnostic trace to return safely.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            val now = clockMillis()
                                            val next = if (now - lastExitTapMs <= 1800L) exitTapCount + 1 else 1
                                            lastExitTapMs = now
                                            if (next >= 3) {
                                                resetAllStages()
                                                onSecretAdminTrigger()
                                            } else {
                                                exitTapCount = next
                                            }
                                        },
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.accentCrimson,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("settings_hidden_error_exit_button")
                                    ) {
                                        Text(
                                            text = "Exit",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

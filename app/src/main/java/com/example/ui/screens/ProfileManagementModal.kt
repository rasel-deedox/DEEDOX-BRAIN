package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.cloud.SyncBadgeState
import com.example.ui.components.AppLogo
import com.example.data.local.UserSettingsEntity
import com.example.ui.components.LiquidThemePillSwitcher
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.ProfileAvatarBadge
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidAccentPreset
import com.example.ui.theme.LiquidTheme

private data class AvatarPresetOption(
    val value: String,
    val label: String,
    val swatchColors: List<Color>
)

private val AVATAR_PRESETS = listOf(
    AvatarPresetOption("preset:terracotta", "Terracotta", listOf(Color(0xFFD65A38), Color(0xFFB54122))),
    AvatarPresetOption("preset:crimson", "Crimson", listOf(Color(0xFFE53935), Color(0xFF8E1B18))),
    AvatarPresetOption("preset:obsidian", "Obsidian", listOf(Color(0xFF3A3533), Color(0xFF141214))),
    AvatarPresetOption("preset:pearl", "Pearl", listOf(Color(0xFFE8DFD3), Color(0xFFC2B3A1)))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileManagementModal(
    userDisplayName: String,
    userEmail: String?,
    profileAvatar: String,
    pendingPickedAvatarUri: Uri?,
    isSignedIn: Boolean,
    themeMode: AppThemeMode,
    accentPreset: LiquidAccentPreset,
    syncBadgeState: SyncBadgeState,
    settings: UserSettingsEntity,
    onDismiss: () -> Unit,
    onPickAvatarPhotoClick: () -> Unit,
    onClearPendingPickedAvatar: () -> Unit,
    onSaveProfile: (name: String, email: String, avatarValue: String?, pickedUri: Uri?) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectAccentPreset: (LiquidAccentPreset) -> Unit,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onOpenDeviceSyncModal: () -> Unit,
    onOpenFullSettings: () -> Unit
) {
    val palette = LiquidTheme.palette
    var draftName by remember(userDisplayName) { mutableStateOf(userDisplayName) }
    var draftEmail by remember(userEmail) { mutableStateOf(userEmail.orEmpty()) }
    var selectedAvatar by remember(profileAvatar) { mutableStateOf(profileAvatar) }

    val effectivePreviewAvatar = pendingPickedAvatarUri?.toString() ?: selectedAvatar
    val modalShape = remember { RoundedCornerShape(34.dp) }
    val glassBrush = remember(palette.cardSurfaceTop, palette.cardSurfaceBottom) {
        Brush.verticalGradient(listOf(palette.cardSurfaceTop, palette.cardSurfaceBottom))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .heightIn(max = 760.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = modalShape,
                        ambientColor = if (palette.isDark) Color(0x95000000) else Color(0x282D261E),
                        spotColor = if (palette.isDark) Color(0x95000000) else Color(0x282D261E)
                    )
                    .clip(modalShape)
                    .background(glassBrush)
                    .border(1.dp, palette.specularRimOutline, modalShape)
                    .drawWithCache {
                        val inset = 28.dp.toPx()
                        val start = Offset(inset, 1.dp.toPx())
                        val end = Offset((size.width - inset).coerceAtLeast(start.x), 1.dp.toPx())
                        val strokeW = 1.4.dp.toPx()
                        onDrawBehind {
                            drawLine(
                                color = palette.specularBorderTop,
                                start = start,
                                end = end,
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    .testTag("profile_management_modal")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppLogo(
                                size = 40.dp,
                                cornerRadius = 13.dp,
                                testTag = "profile_modal_app_logo"
                            )
                            Column {
                                Text(
                                    text = "PROFILE & ACCOUNT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentSoftInk
                                )
                                Text(
                                    text = "Personal Vault Identity",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.recessedTrayBg)
                                .border(1.dp, palette.specularRimOutline, CircleShape)
                                .testTag("profile_modal_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close profile modal",
                                tint = palette.primaryInk
                            )
                        }
                    }

                    // Avatar Picker + Presets Section
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                ProfileAvatarBadge(
                                    avatarValue = effectivePreviewAvatar,
                                    initial = draftName.trim().take(1).ifBlank { "C" },
                                    size = 66.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = draftName.ifBlank { "Creator" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = onPickAvatarPhotoClick,
                                            shape = RoundedCornerShape(9999.dp),
                                            border = BorderStroke(1.dp, palette.specularRimOutline),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = palette.frostedPillBg
                                            ),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .testTag("profile_modal_pick_photo_button")
                                        ) {
                                            MinimalIcon(
                                                type = MinimalIconType.EDIT,
                                                active = true,
                                                tint = palette.accentSoftInk,
                                                size = 15.dp
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Upload Photo",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.primaryInk
                                            )
                                        }
                                        SyncStatusPill(
                                            badgeState = syncBadgeState,
                                            onClick = {
                                                onDismiss()
                                                onOpenDeviceSyncModal()
                                            }
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Monogram Glass Presets",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryInk
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AVATAR_PRESETS.forEach { option ->
                                    val isSelected = pendingPickedAvatarUri == null && selectedAvatar == option.value
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (isSelected) palette.accentSoftBg else palette.frostedPillBg,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) palette.accentCrimson else palette.specularRimOutline
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9999.dp))
                                            .clickable {
                                                onClearPendingPickedAvatar()
                                                selectedAvatar = option.value
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.linearGradient(option.swatchColors))
                                                    .border(0.6.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                            )
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = palette.primaryInk
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Name & Email Editor
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.recessedTrayBg,
                        unfocusedContainerColor = palette.recessedTrayBg,
                        focusedBorderColor = CrimsonAccent,
                        unfocusedBorderColor = palette.inputBorder,
                        focusedTextColor = palette.primaryInk,
                        unfocusedTextColor = palette.primaryInk,
                        focusedLabelColor = palette.accentSoftInk,
                        unfocusedLabelColor = palette.secondaryInk
                    )

                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_modal_name_input")
                    )

                    OutlinedTextField(
                        value = draftEmail,
                        onValueChange = { draftEmail = it },
                        label = {
                            Text(if (isSignedIn) "Account Email (Google Connected)" else "Contact Email")
                        },
                        placeholder = { Text("name@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_modal_email_input")
                    )

                    TactilePrimaryPillButton(
                        text = "Save Profile Details",
                        iconType = MinimalIconType.CHECK,
                        enabled = draftName.trim().isNotBlank(),
                        onClick = {
                            onSaveProfile(
                                draftName,
                                draftEmail,
                                if (pendingPickedAvatarUri != null) null else selectedAvatar,
                                pendingPickedAvatarUri
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("profile_modal_save_button")
                    )

                    // Appearance & Quick Preferences
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Appearance & Preferences",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                            LiquidThemePillSwitcher(
                                selectedMode = themeMode,
                                onSelectMode = onSelectThemeMode,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiquidAccentPreset.entries.forEach { preset ->
                                    val selected = preset == accentPreset
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (selected) palette.accentSoftBg else palette.frostedPillBg,
                                        border = BorderStroke(
                                            width = if (selected) 1.4.dp else 1.dp,
                                            color = if (selected) palette.accentCrimson else palette.specularRimOutline
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9999.dp))
                                            .clickable { onSelectAccentPreset(preset) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(preset.primaryAccent)
                                            )
                                            Text(
                                                text = preset.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.primaryInk,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            ProfileSwitchRow(
                                title = "Google Cloud Sync",
                                subtitle = "Sync folders, bookmarks & appearance automatically",
                                checked = settings.cloudSyncOptIn,
                                onCheckedChange = { checked ->
                                    onUpdateSettings { it.copy(cloudSyncOptIn = checked) }
                                }
                            )
                            ProfileSwitchRow(
                                title = "Rich Link Thumbnails",
                                subtitle = "Fetch Open Graph, YouTube & GitHub card previews",
                                checked = settings.autoMetadataEnrichment,
                                onCheckedChange = { checked ->
                                    onUpdateSettings { it.copy(autoMetadataEnrichment = checked) }
                                }
                            )
                            ProfileSwitchRow(
                                title = "AI Smart Tagging & Search",
                                subtitle = "Enable natural language Q&A and inbox organization",
                                checked = settings.aiEnabled,
                                onCheckedChange = { checked ->
                                    onUpdateSettings { it.copy(aiEnabled = checked) }
                                }
                            )
                        }
                    }

                    // Google Account & Workspace Actions
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Account & Sync Actions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                            if (isSignedIn) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onSyncNowClick,
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = palette.frostedPillBg
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Sync Now", color = palette.primaryInk)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenDeviceSyncModal()
                                        },
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = palette.frostedPillBg
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("PC & Devices", color = palette.primaryInk)
                                    }
                                }
                            } else {
                                GoogleSignInLiquidPillButton(
                                    text = "Connect Google Account",
                                    onClick = onGoogleSignInClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_modal_google_signin_button")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenFullSettings()
                                    },
                                    modifier = Modifier.testTag("profile_modal_full_settings_button")
                                ) {
                                    Text(
                                        text = "Open Full Settings →",
                                        color = palette.accentSoftInk,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (isSignedIn) {
                                    TextButton(
                                        onClick = onSignOutClick,
                                        modifier = Modifier.testTag("profile_modal_signout_button")
                                    ) {
                                        Text(
                                            text = "Sign Out",
                                            color = CrimsonAccent,
                                            fontWeight = FontWeight.SemiBold
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

@Composable
private fun ProfileSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LiquidTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryInk
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryInk
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.accentCrimson,
                uncheckedThumbColor = palette.secondaryInk,
                uncheckedTrackColor = palette.barTrack
            )
        )
    }
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.AppDatabase
import com.example.data.local.ItemEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.storage.FileHealthStatus
import com.example.ui.components.AccentActionPill
import com.example.ui.components.AppLogo
import com.example.ui.components.CrimsonActionPill
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.ResourceCardDisplayMeta
import com.example.ui.components.ResourceItemCard
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.formatRelativeDate
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiquidAccentPreset
import com.example.ui.theme.LiquidTheme
import com.example.ui.viewmodel.LibrarySnapshot

enum class HubSection(val label: String) {
    ALL_PAGES("All Pages"),
    INBOX("Inbox"),
    FILES("Local Files"),
    SYNC("Sync & AI"),
    SETTINGS("Settings"),
    BACKUP("Backup & Logs")
}

private enum class InboxFilter(val label: String, val statusCode: String?) {
    ALL("All Inbox", null),
    UNSORTED("Unsorted", "unsorted"),
    PROCESSING("Processing", "processing"),
    NEEDS_REVIEW("Needs Review", "needs_review"),
    FAILED_META("Failed Metadata", "failed_metadata"),
    DUPLICATE("Duplicate", "duplicate_candidate")
}

@Composable
fun LibraryHubScreen(
    snapshot: LibrarySnapshot,
    initialSection: HubSection,
    themeMode: AppThemeMode,
    accentPreset: LiquidAccentPreset,
    vaultPin: String?,
    isSignedIn: Boolean,
    userDisplayName: String,
    userEmail: String?,
    isOnline: Boolean,
    syncBadgeState: SyncBadgeState,
    isSyncing: Boolean,
    syncMessage: String?,
    isAiBusy: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectAccentPreset: (LiquidAccentPreset) -> Unit,
    onUpdateProfileName: (String) -> Unit,
    onSecretAdminTrigger: () -> Unit,
    onOpenItem: (String) -> Unit,
    onToggleFavorite: (ItemEntity) -> Unit,
    onSendItemToDevice: (ItemEntity) -> Unit = {},
    onOpenDeviceSyncModal: () -> Unit = {},
    onOrganizeInboxClick: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onPickLocalFileClick: () -> Unit,
    onPickPhotoVideoClick: () -> Unit,
    onConnectFolderTreeClick: () -> Unit,
    onRefreshFileHealthClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onClearDiagnosticsClick: () -> Unit,
    onEditItem: (ItemEntity) -> Unit = {},
    onDeleteItem: (ItemEntity) -> Unit = {},
    launcherMetrics: LauncherMetrics = LauncherMetrics(),
    onOpenMonkMode: () -> Unit = {},
    onOpenWorkspace: () -> Unit = {},
    onOpenStartupOs: () -> Unit = {},
    onOpenMoneyLedger: () -> Unit = {},
    onOpenTeamCollab: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    var activeSection by remember(initialSection) { mutableStateOf(initialSection) }
    var inboxFilter by remember { mutableStateOf(InboxFilter.ALL) }

    val folderNameMap = remember(snapshot.folders) { snapshot.folders.associate { it.id to it.name } }
    val foldersByItem = remember(snapshot.itemFolders) {
        snapshot.itemFolders.groupBy({ it.itemId }, { it.folderId })
    }
    val tagNameMap = remember(snapshot.tags) { snapshot.tags.associate { it.id to it.name } }
    val tagsByItem = remember(snapshot.itemTags) {
        snapshot.itemTags.groupBy({ it.itemId }, { it.tagId })
    }
    val fileSourceByItem = remember(snapshot.fileSources) {
        snapshot.fileSources.associateBy { it.itemId }
    }

    val inboxItemIds = remember(snapshot.itemFolders) {
        snapshot.itemFolders.filter { it.folderId == AppDatabase.INBOX_FOLDER_ID }.map { it.itemId }.toSet()
    }
    val inboxItems = remember(snapshot.items, inboxItemIds, inboxFilter) {
        snapshot.items.filter { item ->
            val inInbox = item.id in inboxItemIds || item.status == "unsorted"
            val matchesFilter = inboxFilter.statusCode == null || item.status == inboxFilter.statusCode
            inInbox && matchesFilter
        }
    }
    val localItems = remember(snapshot.items) {
        snapshot.items.filter { it.isDeviceLocal }
    }
    val inboxCardMeta = remember(inboxItems, foldersByItem, folderNameMap, tagsByItem, tagNameMap, fileSourceByItem) {
        inboxItems.associate { item ->
            val health = fileSourceByItem[item.id]?.availabilityStatus?.let { code ->
                FileHealthStatus.entries.find { it.code == code }?.label
            }
            item.id to ResourceCardDisplayMeta(
                folderNames = foldersByItem[item.id].orEmpty().mapNotNull { folderNameMap[it] },
                tagNames = tagsByItem[item.id].orEmpty().mapNotNull { tagNameMap[it] },
                healthStatusLabel = health
            )
        }
    }
    val localCardMeta = remember(localItems, foldersByItem, folderNameMap, tagsByItem, tagNameMap, fileSourceByItem) {
        localItems.associate { item ->
            val health = fileSourceByItem[item.id]?.availabilityStatus?.let { code ->
                FileHealthStatus.entries.find { it.code == code }?.label
            } ?: "Not Yet Checked"
            item.id to ResourceCardDisplayMeta(
                folderNames = foldersByItem[item.id].orEmpty().mapNotNull { folderNameMap[it] },
                tagNames = tagsByItem[item.id].orEmpty().mapNotNull { tagNameMap[it] },
                healthStatusLabel = health
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "hub_header", contentType = "header") {
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
                            size = 42.dp,
                            cornerRadius = 13.dp,
                            testTag = "library_hub_app_logo"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "AI & Hub",
                                style = MaterialTheme.typography.headlineLarge,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "Inbox, SAF storage, multi-device sync & privacy",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk
                            )
                        }
                    }
                    AccentActionPill(
                        text = "Assistant",
                        onClick = onOpenAiAssistant,
                        modifier = Modifier.testTag("hub_open_assistant_button")
                    )
                }
            }

            // Section selector pills inside recessed bezel
            item(key = "hub_section_tabs", contentType = "segment_switcher") {
                RecessedFramingBezel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 9999.dp,
                    innerPadding = PaddingValues(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HubSection.entries.forEach { sec ->
                            val selected = activeSection == sec
                            Surface(
                                onClick = { activeSection = sec },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (selected) palette.primaryButtonBg else Color.Transparent,
                                border = if (selected) BorderStroke(1.dp, palette.specularRimOutline) else null,
                                shadowElevation = if (selected) 4.dp else 0.dp,
                                modifier = Modifier.testTag("hub_tab_${sec.name.lowercase()}")
                            ) {
                                Text(
                                    text = sec.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (selected) palette.primaryButtonContent else palette.secondaryInk,
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            when (activeSection) {
                HubSection.ALL_PAGES -> {
                    item(key = "hub_all_pages_grid", contentType = "bento_launcher") {
                        AllPagesLauncherGrid(
                            metrics = launcherMetrics,
                            onOpenMonkMode = onOpenMonkMode,
                            onOpenWorkspace = onOpenWorkspace,
                            onOpenStartupOs = onOpenStartupOs,
                            onOpenMoneyLedger = onOpenMoneyLedger,
                            onOpenTeamCollab = onOpenTeamCollab
                        )
                    }
                }
                HubSection.INBOX -> {
                    item(key = "hub_inbox_header", contentType = "inbox_header") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Universal Inbox",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = "Items shared from browsers, quick captures, and connected local files land here until organized.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InboxFilter.entries.forEach { f ->
                                        val selected = inboxFilter == f
                                        Surface(
                                            onClick = { inboxFilter = f },
                                            shape = RoundedCornerShape(9999.dp),
                                            color = if (selected) palette.primaryButtonBg else palette.recessedTrayBg,
                                            border = BorderStroke(1.dp, palette.specularRimOutline)
                                        ) {
                                            Text(
                                                text = f.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) palette.primaryButtonContent else palette.secondaryInk,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                                AccentActionPill(
                                    text = if (isAiBusy) "Planning Organization..." else "Preview AI Organization Plan",
                                    enabled = !isAiBusy && inboxItems.isNotEmpty() && isOnline && snapshot.settings.aiEnabled,
                                    onClick = onOrganizeInboxClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("hub_inbox_organize_button")
                                )
                            }
                        }
                    }

                    if (inboxItems.isEmpty()) {
                        item(key = "hub_inbox_empty", contentType = "empty_card") {
                            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Inbox is clear", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                    Text(
                                        "No items match the current Inbox filter.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = inboxItems,
                            key = { it.id },
                            contentType = { "resource_card" }
                        ) { item ->
                            val meta = inboxCardMeta[item.id] ?: ResourceCardDisplayMeta()
                            ResourceItemCard(
                                item = item,
                                folderNames = meta.folderNames,
                                tagNames = meta.tagNames,
                                healthStatusLabel = meta.healthStatusLabel,
                                onOpenClick = { onOpenItem(item.id) },
                                onFavoriteClick = { onToggleFavorite(item) },
                                onShareToDeviceClick = { onSendItemToDevice(item) },
                                onEditClick = { onEditItem(item) },
                                onDeleteClick = { onDeleteItem(item) }
                            )
                        }
                    }
                }

                HubSection.FILES -> {
                    item(key = "hub_files_notice", contentType = "notice_card") {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = palette.accentSoftBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = palette.accentSoftInk)
                                    Text(
                                        text = "Android 11+ Storage Access Notice",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryInk
                                    )
                                }
                                Text(
                                    text = "Android restricts apps from scanning the root Download or Android/data folders as a tree. This app stores reference URIs only (never copies, moves, or deletes your originals) using native SAF & zero-permission Photo Picker.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.primaryInk
                                )
                            }
                        }
                    }

                    item(key = "hub_files_actions", contentType = "files_actions") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Add Local References", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onPickLocalFileClick,
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primaryButtonBg,
                                            contentColor = palette.primaryButtonContent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_pick_file_button")
                                    ) {
                                        Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(" Pick File")
                                    }
                                    Button(
                                        onClick = onPickPhotoVideoClick,
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.accentCrimson,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_pick_media_button")
                                    ) {
                                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(" Photo Picker", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onConnectFolderTreeClick,
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, palette.inputBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_connect_folder_button")
                                    ) {
                                        MinimalIcon(type = MinimalIconType.FOLDER, active = false, tint = palette.primaryInk, size = 16.dp)
                                        Text(" Connect Folder", color = palette.primaryInk)
                                    }
                                    OutlinedButton(
                                        onClick = onRefreshFileHealthClick,
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, palette.inputBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_verify_health_button")
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = palette.primaryInk, modifier = Modifier.size(16.dp))
                                        Text(" Verify Health", color = palette.primaryInk)
                                    }
                                }
                            }
                        }
                    }

                    if (snapshot.connectedSources.isNotEmpty()) {
                        item(key = "hub_connected_folders_title", contentType = "section_title") {
                            Text("Connected Folders", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                        }
                        items(
                            items = snapshot.connectedSources,
                            key = { it.id },
                            contentType = { "connected_source_card" }
                        ) { src ->
                            TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(src.displayLabel, style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                        Text(
                                            "${src.itemCount} indexed refs • ${src.scanStatus}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.secondaryInk
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = palette.accentSoftBg
                                    ) {
                                        Text(
                                            text = if (src.grantedPermissionFlags > 0) "Persisted" else "Session Grant",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.accentSoftInk,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (localItems.isEmpty()) {
                        item(key = "hub_local_files_empty", contentType = "empty_card") {
                            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("No local file references yet", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                    Text(
                                        "Use Pick File, Photo Picker, or Connect Folder above to index local files safely by reference.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = localItems,
                            key = { "local_${it.id}" },
                            contentType = { "resource_card" }
                        ) { item ->
                            val meta = localCardMeta[item.id] ?: ResourceCardDisplayMeta(healthStatusLabel = "Not Yet Checked")
                            ResourceItemCard(
                                item = item,
                                folderNames = meta.folderNames,
                                tagNames = meta.tagNames,
                                healthStatusLabel = meta.healthStatusLabel,
                                onOpenClick = { onOpenItem(item.id) },
                                onFavoriteClick = { onToggleFavorite(item) },
                                onShareToDeviceClick = { onSendItemToDevice(item) },
                                onEditClick = { onEditItem(item) },
                                onDeleteClick = { onDeleteItem(item) }
                            )
                        }
                    }
                }

                HubSection.SYNC -> {
                    item(key = "hub_sync_account_card", contentType = "sync_account") {
                        RecessedFramingBezel(modifier = Modifier.fillMaxWidth(), innerPadding = PaddingValues(12.dp)) {
                            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            MinimalIcon(
                                                type = MinimalIconType.CLOUD_SYNC,
                                                active = isSignedIn,
                                                tint = palette.accentSoftInk,
                                                size = 24.dp
                                            )
                                            Column {
                                                Text(
                                                    text = if (isSignedIn) userDisplayName else "Guest Mode (Local-Only)",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.primaryInk
                                                )
                                                Text(
                                                    text = userEmail ?: "No cloud account connected",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.secondaryInk
                                                )
                                            }
                                        }
                                        SyncStatusPill(badgeState = syncBadgeState, onClick = onOpenDeviceSyncModal)
                                    }

                                    Text(
                                        text = "Pending outbox events: ${snapshot.pendingSyncCount}" +
                                            (snapshot.settings.lastSyncedAt?.let { " • Last sync ${formatRelativeDate(it)}" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )

                                    if (!syncMessage.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = palette.accentSoftBg
                                        ) {
                                            Text(
                                                text = syncMessage,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.primaryInk,
                                                modifier = Modifier.padding(12.dp)
                                            )
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
                                                    .testTag("hub_google_signin_button")
                                            )
                                        } else {
                                            Button(
                                                onClick = onSyncNowClick,
                                                enabled = !isSyncing && snapshot.settings.cloudSyncOptIn,
                                                shape = RoundedCornerShape(9999.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = palette.primaryButtonBg,
                                                    contentColor = palette.primaryButtonContent
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("hub_sync_now_button")
                                            ) {
                                                if (isSyncing) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = palette.primaryButtonContent
                                                    )
                                                } else {
                                                    Text("Sync Now", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = onSignOutClick,
                                                shape = RoundedCornerShape(9999.dp),
                                                border = BorderStroke(1.dp, palette.inputBorder),
                                                modifier = Modifier.testTag("hub_signout_button")
                                            ) {
                                                Text("Sign Out", color = palette.primaryInk)
                                            }
                                        }
                                    }

                                    CrimsonActionPill(
                                        text = "Sync & Send to PC / Phone",
                                        onClick = onOpenDeviceSyncModal,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    item(key = "hub_sync_privacy_controls", contentType = "privacy_controls") {
                        val settings = snapshot.settings
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                        size = 22.dp
                                    )
                                    Text("Privacy & AI Controls", style = MaterialTheme.typography.titleLarge, color = palette.primaryInk)
                                }

                                SettingsToggleRow(
                                    title = "Online AI Features",
                                    subtitle = "Allow AI Assistant, Online Rerank & Organization Preview when online.",
                                    checked = settings.aiEnabled,
                                    testTag = "toggle_ai_enabled",
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(aiEnabled = enabled) }
                                    }
                                )
                                SettingsToggleRow(
                                    title = "Include Personal Notes in AI Context",
                                    subtitle = "Default OFF for privacy. Enable only if you want AI to read your notes.",
                                    checked = settings.includeNotesInAi,
                                    testTag = "toggle_notes_in_ai",
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(includeNotesInAi = enabled) }
                                    }
                                )
                                SettingsToggleRow(
                                    title = "Auto Metadata Enrichment",
                                    subtitle = "Fetch page titles and previews in background when online.",
                                    checked = settings.autoMetadataEnrichment,
                                    testTag = "toggle_auto_enrichment",
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(autoMetadataEnrichment = enabled) }
                                    }
                                )
                                SettingsToggleRow(
                                    title = "Cloud Sync Opt-In (Metadata Only)",
                                    subtitle = "Sync folders, bookmarks & notes to Firestore. Local file binaries stay on device.",
                                    checked = settings.cloudSyncOptIn,
                                    testTag = "toggle_cloud_sync",
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(cloudSyncOptIn = enabled) }
                                    }
                                )
                                SettingsToggleRow(
                                    title = "Redacted Diagnostic Logs",
                                    subtitle = "Keep sanitized local error logs for troubleshooting.",
                                    checked = settings.diagnosticsEnabled,
                                    testTag = "toggle_diagnostics",
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(diagnosticsEnabled = enabled) }
                                    }
                                )
                            }
                        }
                    }
                }

                HubSection.SETTINGS -> {
                    settingsSectionItems(
                        userDisplayName = userDisplayName,
                        userEmail = userEmail,
                        isSignedIn = isSignedIn,
                        themeMode = themeMode,
                        accentPreset = accentPreset,
                        vaultPin = vaultPin,
                        syncBadgeState = syncBadgeState,
                        settings = snapshot.settings,
                        onUpdateProfileName = onUpdateProfileName,
                        onSelectThemeMode = onSelectThemeMode,
                        onSelectAccentPreset = onSelectAccentPreset,
                        onGoogleSignInClick = onGoogleSignInClick,
                        onSignOutClick = onSignOutClick,
                        onOpenDeviceSyncModal = onOpenDeviceSyncModal,
                        onUpdateSettings = onUpdateSettings,
                        onSecretAdminTrigger = onSecretAdminTrigger
                    )
                }

                HubSection.BACKUP -> {
                    item(key = "hub_backup_actions", contentType = "backup_actions") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("JSON Backup & Restore", style = MaterialTheme.typography.titleLarge, color = palette.primaryInk)
                                Text(
                                    "Export your entire library (items, folders, tags, notes, collections) to a portable JSON file or restore from backup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onExportBackupClick,
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primaryButtonBg,
                                            contentColor = palette.primaryButtonContent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_export_backup_button")
                                    ) {
                                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(" Export JSON")
                                    }
                                    OutlinedButton(
                                        onClick = onImportBackupClick,
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, palette.inputBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hub_import_backup_button")
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null, tint = palette.primaryInk, modifier = Modifier.size(16.dp))
                                        Text(" Restore JSON", color = palette.primaryInk)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "hub_backup_audit", contentType = "audit_card") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("AI Organization Audit Trail", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                if (snapshot.auditLogs.isEmpty()) {
                                    Text(
                                        "No AI organization actions recorded yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )
                                } else {
                                    snapshot.auditLogs.take(8).forEach { log ->
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = "${log.actionType} • ${formatRelativeDate(log.createdAt)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.accentSoftInk
                                            )
                                            Text(
                                                text = log.summary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.primaryInk
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "hub_backup_diagnostics", contentType = "diagnostics_card") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.BugReport, contentDescription = null, tint = palette.accentCrimson)
                                        Text("Redacted Diagnostics", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                    }
                                    TextButton(onClick = onClearDiagnosticsClick) {
                                        Text("Clear", color = palette.accentSoftInk)
                                    }
                                }
                                if (snapshot.diagnosticLogs.isEmpty()) {
                                    Text(
                                        "No diagnostic errors logged.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )
                                } else {
                                    snapshot.diagnosticLogs.take(8).forEach { diag ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = palette.accentSoftBg,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "${diag.operationId} (${diag.errorCode}) • ${formatRelativeDate(diag.createdAt)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.primaryInk
                                                )
                                                Text(
                                                    text = diag.sanitizedMessage,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.primaryInk
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "hub_backup_about", contentType = "about_card") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = palette.accentSoftInk)
                                    Text("About & Privacy Summary", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                }
                                Text(
                                    text = "AI Library v1.0 • Offline-first local SQLite vault. Local files are indexed by Android SAF content URIs only—no local binaries are ever uploaded or duplicated.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
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
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryInk
            )
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

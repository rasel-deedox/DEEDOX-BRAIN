package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CloudDeviceSession
import com.example.data.cloud.SyncBadgeState
import com.example.data.local.AppDatabase
import com.example.data.local.ItemEntity
import com.example.data.local.LibraryReview
import com.example.data.storage.FileHealthStatus
import com.example.ui.components.AccordionsAndTelemetrySection
import com.example.ui.components.AiAssistantAndFocusCards
import com.example.ui.components.HomeHeaderBar
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.QuickCaptureAndBentoGrid
import com.example.ui.components.ResourceCardDisplayMeta
import com.example.ui.components.ResourceItemCard
import com.example.ui.components.StreakAndHeatmapSection
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.liquidReviewSectionItems
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.viewmodel.LibrarySnapshot

private enum class HomeQuickFilter(val label: String) {
    ALL("All"),
    INBOX("Inbox"),
    FAVORITES("Favorites"),
    NOTES("Notes"),
    LOCAL("Local Files"),
    RECENT("Recent")
}

@Composable
fun HomeScreen(
    snapshot: LibrarySnapshot,
    themeMode: AppThemeMode,
    reviews: List<LibraryReview>,
    connectedDevices: List<CloudDeviceSession>,
    pendingTransfersCount: Int,
    userDisplayName: String,
    profileAvatar: String = "preset:terracotta",
    isSignedIn: Boolean,
    isOnline: Boolean,
    syncBadgeState: SyncBadgeState,
    isAiBusy: Boolean,
    aiError: String?,
    lastUndoablePlanId: String?,
    onOpenProfileModal: () -> Unit = {},
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onOpenDeviceSyncModal: () -> Unit,
    onSendItemToDevice: (ItemEntity) -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenCapture: () -> Unit,
    onSaveQuickInput: (String) -> Unit,
    onSpeakCapture: () -> Unit,
    onUploadMedia: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenItem: (String) -> Unit,
    onToggleFavorite: (ItemEntity) -> Unit,
    onOrganizeInboxClick: () -> Unit,
    onUndoLastPlanClick: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onWriteReviewClick: () -> Unit,
    onToggleReviewHelpful: (String) -> Unit,
    onEditItem: (ItemEntity) -> Unit = {},
    onDeleteItem: (ItemEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    var selectedFilter by remember { mutableStateOf(HomeQuickFilter.ALL) }
    var showFiltersRow by remember { mutableStateOf(true) }

    val folderNameMap = remember(snapshot.folders) {
        snapshot.folders.associate { it.id to it.name }
    }
    val foldersByItem = remember(snapshot.itemFolders) {
        snapshot.itemFolders.groupBy({ it.itemId }, { it.folderId })
    }
    val tagNameMap = remember(snapshot.tags) {
        snapshot.tags.associate { it.id to it.name }
    }
    val tagsByItem = remember(snapshot.itemTags) {
        snapshot.itemTags.groupBy({ it.itemId }, { it.tagId })
    }
    val fileSourceByItem = remember(snapshot.fileSources) {
        snapshot.fileSources.associateBy { it.itemId }
    }

    val inboxItemIds = remember(snapshot.itemFolders) {
        snapshot.itemFolders
            .filter { it.folderId == AppDatabase.INBOX_FOLDER_ID }
            .map { it.itemId }
            .toSet()
    }

    val inboxCount = remember(snapshot.items, inboxItemIds) {
        snapshot.items.count { it.id in inboxItemIds || it.status == "unsorted" }
    }
    val localCount = remember(snapshot.items) {
        snapshot.items.count { it.isDeviceLocal }
    }
    val syncedCount = remember(snapshot.items) {
        snapshot.items.count { it.syncState == "synced" }
    }

    val filteredItems = remember(snapshot.items, selectedFilter, inboxItemIds) {
        when (selectedFilter) {
            HomeQuickFilter.ALL -> snapshot.items
            HomeQuickFilter.INBOX -> snapshot.items.filter { it.id in inboxItemIds || it.status == "unsorted" }
            HomeQuickFilter.FAVORITES -> snapshot.items.filter { it.isFavorite }
            HomeQuickFilter.NOTES -> snapshot.items.filter { it.contentType == "note" }
            HomeQuickFilter.LOCAL -> snapshot.items.filter { it.isDeviceLocal }
            HomeQuickFilter.RECENT -> snapshot.items.take(15)
        }
    }
    val itemCardMeta = remember(
        filteredItems,
        foldersByItem,
        folderNameMap,
        tagsByItem,
        tagNameMap,
        fileSourceByItem
    ) {
        filteredItems.associate { item ->
            val folderNames = foldersByItem[item.id].orEmpty().mapNotNull { folderNameMap[it] }
            val tagNames = tagsByItem[item.id].orEmpty().mapNotNull { tagNameMap[it] }
            val fileHealth = fileSourceByItem[item.id]?.availabilityStatus?.let { code ->
                FileHealthStatus.entries.find { it.code == code }?.label
            }
            item.id to ResourceCardDisplayMeta(
                folderNames = folderNames,
                tagNames = tagNames,
                healthStatusLabel = fileHealth
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
                .fillMaxSize()
                .testTag("home_feed_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "home_header", contentType = "header") {
                HomeHeaderBar(
                    userDisplayName = userDisplayName,
                    profileAvatar = profileAvatar,
                    isSignedIn = isSignedIn,
                    isOnline = isOnline,
                    themeMode = themeMode,
                    syncBadgeState = syncBadgeState,
                    notificationCount = pendingTransfersCount + if (inboxCount > 0) 1 else 0,
                    showFiltersExpanded = showFiltersRow,
                    onProfileAvatarClick = onOpenProfileModal,
                    onToggleFiltersExpanded = { showFiltersRow = !showFiltersRow },
                    onBellClick = {
                        if (pendingTransfersCount > 0) onOpenDeviceSyncModal() else onOpenInbox()
                    },
                    onOpenDeviceSyncModal = onOpenDeviceSyncModal,
                    onSelectThemeMode = onSelectThemeMode,
                    onOpenSyncSettings = onOpenSyncSettings
                )
            }

            if (!isOnline) {
                item(key = "home_offline_banner", contentType = "banner") { OfflineStatusBanner() }
            }

            if (!aiError.isNullOrBlank()) {
                item(key = "home_ai_error", contentType = "banner") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiError,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.primaryInk,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (isAiBusy) {
                item(key = "home_ai_busy", contentType = "banner") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = palette.accentSoftInk
                            )
                            Text(
                                text = "Building preview organization plan (zero changes yet)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.primaryInk
                            )
                        }
                    }
                }
            }

            if (lastUndoablePlanId != null) {
                item(key = "home_undo_plan", contentType = "banner") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent AI organization applied.",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primaryInk
                            )
                            TextButton(
                                onClick = onUndoLastPlanClick,
                                modifier = Modifier.testTag("home_undo_plan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = "Undo AI plan",
                                    tint = palette.accentSoftInk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = " Undo",
                                    color = palette.accentSoftInk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Row 1: Streak Counters + 7x15 Activity Heatmap Matrix
            item(key = "home_streak_heatmap", contentType = "heatmap_widget") {
                StreakAndHeatmapSection(snapshot = snapshot)
            }

            // Row 2: Your AI Assistant Card + Today's Focus Card
            item(key = "home_ai_focus", contentType = "ai_focus_widget") {
                AiAssistantAndFocusCards(
                    snapshot = snapshot,
                    inboxCount = inboxCount,
                    isOnline = isOnline,
                    isAiBusy = isAiBusy,
                    onAskAiClick = onOpenAiAssistant,
                    onOrganizeInboxClick = onOrganizeInboxClick
                )
            }

            // Row 3: Quick Capture Box + 2x3 Suggested Actions Bento Grid
            item(key = "home_quick_capture", contentType = "quick_capture_widget") {
                QuickCaptureAndBentoGrid(
                    onSaveQuickInput = onSaveQuickInput,
                    onOpenFullCapture = onOpenCapture,
                    onSpeakCapture = onSpeakCapture,
                    onUploadMedia = onUploadMedia,
                    onOpenAiAssistant = onOpenAiAssistant,
                    onOpenSearch = onOpenSearch,
                    onFilterNotes = {
                        selectedFilter = HomeQuickFilter.NOTES
                        showFiltersRow = true
                    }
                )
            }

            // Row 4: 2 Collapsible Accordions + Featured Warm Crimson-Bronze Telemetry Card
            item(key = "home_accordions_telemetry", contentType = "telemetry_widget") {
                AccordionsAndTelemetrySection(
                    snapshot = snapshot,
                    inboxCount = inboxCount,
                    localCount = localCount,
                    syncedCount = syncedCount,
                    connectedDevices = connectedDevices,
                    onOpenItem = onOpenItem,
                    onOpenDeviceSyncModal = onOpenDeviceSyncModal
                )
            }

            // Filter Pills Row (toggleable via Top-Right Sliders Squircle)
            item(key = "home_quick_filters", contentType = "filter_row") {
                AnimatedVisibility(visible = showFiltersRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HomeQuickFilter.entries.forEach { filter ->
                            val active = selectedFilter == filter
                            Surface(
                                onClick = { selectedFilter = filter },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (active) palette.primaryButtonBg else palette.frostedPillBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                shadowElevation = if (active) 6.dp else 2.dp,
                                modifier = Modifier.testTag("home_filter_${filter.name.lowercase()}")
                            ) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) palette.primaryButtonContent else palette.secondaryInk,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item(key = "home_empty_state", contentType = "empty_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 30.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentSoftBg),
                                contentAlignment = Alignment.Center
                            ) {
                                MinimalIcon(
                                    type = MinimalIconType.FOLDER,
                                    active = true,
                                    tint = palette.accentSoftInk,
                                    cutoutColor = palette.accentSoftBg,
                                    size = 28.dp
                                )
                            }
                            Text(
                                text = "No resources here yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "Capture a web link, note, PDF, or photo to start building your offline-first library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryInk
                            )
                            Spacer(Modifier.height(4.dp))
                            TactilePrimaryPillButton(
                                text = "Capture First Resource",
                                iconType = MinimalIconType.PLUS,
                                onClick = onOpenCapture,
                                modifier = Modifier.testTag("home_empty_capture_button")
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredItems,
                    key = { it.id },
                    contentType = { "resource_card" }
                ) { item ->
                    val meta = itemCardMeta[item.id] ?: ResourceCardDisplayMeta()
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

            // Liquid Glass Review & Rating Summary + Virtualized Interactive Review Cards
            liquidReviewSectionItems(
                reviews = reviews,
                onWriteReviewClick = onWriteReviewClick,
                onToggleHelpful = onToggleReviewHelpful
            )
        }
    }
}

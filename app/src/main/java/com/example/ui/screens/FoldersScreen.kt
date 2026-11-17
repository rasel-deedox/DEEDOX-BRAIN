package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntity
import com.example.data.local.FolderEntity
import com.example.data.local.ItemEntity
import com.example.data.local.TagEntity
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.FolderPocketCard
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.ResourceCardDisplayMeta
import com.example.ui.components.ResourceItemCard
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.VaultHeroCardStack
import com.example.ui.components.VaultKeypadSurface
import com.example.ui.components.VaultPinDrawerModal
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.components.LiquidDeleteConfirmationDialog
import com.example.ui.viewmodel.LibrarySnapshot

private enum class FoldersSegment(val label: String) {
    FOLDERS("Folders"),
    TAGS("Tags"),
    COLLECTIONS("Collections")
}

@Composable
fun FoldersScreen(
    snapshot: LibrarySnapshot,
    vaultPin: String?,
    isVaultLocked: Boolean,
    onSetVaultPin: (String?) -> Unit,
    onSetVaultLocked: (Boolean) -> Unit,
    onCreateFolder: (name: String, parentId: String?) -> Unit,
    onRenameFolder: (folderId: String, newName: String) -> Unit,
    onDeleteFolder: (folderId: String, moveTargetFolderId: String?) -> Unit,
    onAddStarterFolders: () -> Unit,
    onRenameTag: (tagId: String, newName: String) -> Unit,
    onDeleteTag: (tagId: String) -> Unit,
    onCreateCollection: (name: String, description: String) -> Unit,
    onOpenItem: (String) -> Unit,
    onToggleFavorite: (ItemEntity) -> Unit,
    onSendItemToDevice: (ItemEntity) -> Unit = {},
    onEditItem: (ItemEntity) -> Unit = {},
    onDeleteItem: (ItemEntity) -> Unit = {},
    onQuickAddToFolder: (folderId: String) -> Unit = {},
    onPickMediaForTarget: (folderId: String, collectionId: String?) -> Unit = { _, _ -> },
    onPickFileForTarget: (folderId: String, collectionId: String?) -> Unit = { _, _ -> },
    onQuickAddToCollection: (collectionId: String) -> Unit = {},
    onRemoveItemFromCollection: (collectionId: String, itemId: String) -> Unit = { _, _ -> },
    onDeleteCollection: (collectionId: String) -> Unit = {},
    onOpenWorkspace: () -> Unit = {},
    onOpenMonkMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    var segment by remember { mutableStateOf(FoldersSegment.FOLDERS) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var addTargetDialogState by remember { mutableStateOf<Pair<String, String?>?>(null) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderParentId by remember { mutableStateOf<String?>(null) }
    var renameTargetFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var deleteTargetFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var renameTargetTag by remember { mutableStateOf<TagEntity?>(null) }
    var deleteTargetTag by remember { mutableStateOf<TagEntity?>(null) }
    var deleteTargetCollection by remember { mutableStateOf<CollectionEntity?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    var showPinDrawer by remember { mutableStateOf(false) }
    var showVaultDetailsDialog by remember { mutableStateOf(false) }
    var unlockAttemptPin by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedFolderId != null || selectedCollectionId != null) {
        if (selectedCollectionId != null) {
            selectedCollectionId = null
        } else {
            val currentFolder = snapshot.folders.find { it.id == selectedFolderId }
            selectedFolderId = currentFolder?.parentFolderId
        }
    }

    val folderNameMap = remember(snapshot.folders) { snapshot.folders.associate { it.id to it.name } }
    val foldersByItem = remember(snapshot.itemFolders) {
        snapshot.itemFolders.groupBy({ it.itemId }, { it.folderId })
    }
    val itemIdsByFolder = remember(snapshot.itemFolders) {
        snapshot.itemFolders.groupBy({ it.folderId }, { it.itemId })
    }
    val itemsById = remember(snapshot.items) {
        snapshot.items.associateBy { it.id }
    }
    val itemCountByFolder = remember(snapshot.itemFolders) {
        snapshot.itemFolders.groupingBy { it.folderId }.eachCount()
    }
    val tagNameMap = remember(snapshot.tags) { snapshot.tags.associate { it.id to it.name } }
    val tagsByItem = remember(snapshot.itemTags) {
        snapshot.itemTags.groupBy({ it.itemId }, { it.tagId })
    }
    val itemCountByTag = remember(snapshot.itemTags) {
        snapshot.itemTags.groupingBy { it.tagId }.eachCount()
    }
    val previewItemsMap = remember(snapshot.folders, itemIdsByFolder, itemsById) {
        snapshot.folders.associate { folder ->
            folder.id to itemIdsByFolder[folder.id].orEmpty().mapNotNull { itemsById[it] }
        }
    }
    val collectionItemCounts = remember(snapshot.collectionItems) {
        snapshot.collectionItems.groupingBy { it.collectionId }.eachCount()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Locked Vault Keypad Gate
        if (isVaultLocked && !vaultPin.isNullOrBlank()) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "vault_lock_keypad", contentType = "vault_lock") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 32.dp) {
                        Box(modifier = Modifier.padding(24.dp)) {
                            VaultKeypadSurface(
                                headline = "Vault Locked",
                                subtitle = "Enter your 4-digit PIN to inspect collections and folders.",
                                enteredPin = unlockAttemptPin,
                                errorText = unlockError,
                                confirmLabel = "Unlock Vault",
                                onDigitClick = {
                                    unlockError = null
                                    unlockAttemptPin += it
                                },
                                onBackspaceClick = {
                                    if (unlockAttemptPin.isNotEmpty()) {
                                        unlockAttemptPin = unlockAttemptPin.dropLast(1)
                                    }
                                },
                                onClearClick = {
                                    unlockError = null
                                    unlockAttemptPin = ""
                                },
                                onConfirmClick = {
                                    if (unlockAttemptPin == vaultPin) {
                                        unlockAttemptPin = ""
                                        unlockError = null
                                        onSetVaultLocked(false)
                                    } else {
                                        unlockAttemptPin = ""
                                        unlockError = "Incorrect PIN. Try again."
                                    }
                                }
                            )
                        }
                    }
                }
            }
            return@Box
        }

        val activeFolder = remember(snapshot.folders, selectedFolderId) {
            snapshot.folders.find { it.id == selectedFolderId }
        }
        val activeCollection = remember(snapshot.collections, selectedCollectionId) {
            snapshot.collections.find { it.id == selectedCollectionId }
        }

        if (activeCollection != null) {
            val colItemIds = remember(snapshot.collectionItems, activeCollection.id) {
                snapshot.collectionItems
                    .filter { it.collectionId == activeCollection.id }
                    .map { it.itemId }
                    .toSet()
            }
            val collectionItems = remember(snapshot.items, colItemIds) {
                snapshot.items.filter { it.id in colItemIds }
            }
            val collectionItemMeta = remember(collectionItems, foldersByItem, folderNameMap, tagsByItem, tagNameMap) {
                collectionItems.associate { item ->
                    item.id to ResourceCardDisplayMeta(
                        folderNames = foldersByItem[item.id].orEmpty().mapNotNull { folderNameMap[it] },
                        tagNames = tagsByItem[item.id].orEmpty().mapNotNull { tagNameMap[it] }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .testTag("collection_detail_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "collection_detail_header", contentType = "collection_detail_header") {
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
                            FloatingGlassIconButton(
                                iconType = MinimalIconType.BACK,
                                contentDescription = "Back to collections",
                                onClick = { selectedCollectionId = null },
                                modifier = Modifier.testTag("collection_detail_back_button")
                            )
                            Column {
                                Text(
                                    text = activeCollection.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = "${collectionItems.size} item(s)${if (activeCollection.description.isNotBlank()) " • ${activeCollection.description}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                            }
                        }
                        FloatingGlassIconButton(
                            iconType = MinimalIconType.TRASH,
                            contentDescription = "Delete collection",
                            tint = CrimsonAccent,
                            onClick = { deleteTargetCollection = activeCollection },
                            modifier = Modifier.testTag("collection_delete_button")
                        )
                    }
                }

                item(key = "collection_add_cta", contentType = "collection_add_cta") {
                    TactilePrimaryPillButton(
                        text = "+ Add to Collection",
                        iconType = MinimalIconType.PLUS,
                        onClick = { addTargetDialogState = AppDatabase.INBOX_FOLDER_ID to activeCollection.id },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collection_add_item_button")
                    )
                }

                if (collectionItems.isEmpty()) {
                    item(key = "collection_empty", contentType = "empty_card") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("This collection is empty", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                Text(
                                    "Tap \"+ Add to Collection\" to capture a link, note, video, or local file straight into ${activeCollection.name}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = collectionItems,
                        key = { "col_${activeCollection.id}_${it.id}" },
                        contentType = { "resource_card" }
                    ) { item ->
                        val meta = collectionItemMeta[item.id] ?: ResourceCardDisplayMeta()
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ResourceItemCard(
                                item = item,
                                folderNames = meta.folderNames,
                                tagNames = meta.tagNames,
                                onOpenClick = { onOpenItem(item.id) },
                                onFavoriteClick = { onToggleFavorite(item) },
                                onShareToDeviceClick = { onSendItemToDevice(item) },
                                onEditClick = { onEditItem(item) },
                                onDeleteClick = { onDeleteItem(item) }
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onRemoveItemFromCollection(activeCollection.id, item.id) }) {
                                    Text("Remove from collection", color = palette.secondaryInk)
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeFolder != null) {
            val subFolders = remember(snapshot.folders, activeFolder.id) {
                snapshot.folders.filter { it.parentFolderId == activeFolder.id }
            }
            val folderItems = remember(snapshot.items, itemIdsByFolder, activeFolder.id) {
                val ids = itemIdsByFolder[activeFolder.id].orEmpty().toSet()
                snapshot.items.filter { it.id in ids }
            }
            val subPairs = remember(subFolders) { subFolders.chunked(2) }
            val folderItemMeta = remember(folderItems, foldersByItem, folderNameMap, tagsByItem, tagNameMap) {
                folderItems.associate { item ->
                    item.id to ResourceCardDisplayMeta(
                        folderNames = foldersByItem[item.id].orEmpty().mapNotNull { folderNameMap[it] },
                        tagNames = tagsByItem[item.id].orEmpty().mapNotNull { tagNameMap[it] }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "folder_detail_header", contentType = "folder_detail_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FloatingGlassIconButton(
                                iconType = MinimalIconType.BACK,
                                contentDescription = "Back",
                                onClick = { selectedFolderId = activeFolder.parentFolderId },
                                modifier = Modifier.testTag("folder_detail_back_button")
                            )
                            Column {
                                Text(
                                    text = activeFolder.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = "${folderItems.size} item(s) • ${subFolders.size} subfolder(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FloatingGlassIconButton(
                                iconType = MinimalIconType.PLUS,
                                contentDescription = "Add subfolder",
                                onClick = {
                                    newFolderParentId = activeFolder.id
                                    showCreateFolderDialog = true
                                }
                            )
                            if (!activeFolder.isInbox) {
                                FloatingGlassIconButton(
                                    iconType = MinimalIconType.EDIT,
                                    contentDescription = "Rename folder",
                                    onClick = { renameTargetFolder = activeFolder }
                                )
                                FloatingGlassIconButton(
                                    iconType = MinimalIconType.TRASH,
                                    contentDescription = "Delete folder",
                                    tint = CrimsonAccent,
                                    onClick = { deleteTargetFolder = activeFolder }
                                )
                            }
                        }
                    }
                }

                item(key = "folder_add_cta", contentType = "folder_add_cta") {
                    TactilePrimaryPillButton(
                        text = "+ Add to This Folder",
                        iconType = MinimalIconType.PLUS,
                        onClick = { addTargetDialogState = activeFolder.id to null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_add_item_button")
                    )
                }

                if (subFolders.isNotEmpty()) {
                    items(
                        items = subPairs,
                        key = { "subrow_${it.first().id}" },
                        contentType = { "folder_row" }
                    ) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { sub ->
                                FolderPocketCard(
                                    folder = sub,
                                    previewItems = previewItemsMap[sub.id].orEmpty(),
                                    itemCount = itemCountByFolder[sub.id] ?: 0,
                                    onOpen = { selectedFolderId = sub.id },
                                    onRename = { renameTargetFolder = sub },
                                    onDelete = { deleteTargetFolder = sub },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (folderItems.isEmpty() && subFolders.isEmpty()) {
                    item(key = "folder_empty", contentType = "empty_card") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("This folder is empty", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                Text(
                                    "Save links or move items here from Inbox or AI Organization.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = folderItems,
                        key = { it.id },
                        contentType = { "resource_card" }
                    ) { item ->
                        val meta = folderItemMeta[item.id] ?: ResourceCardDisplayMeta()
                        ResourceItemCard(
                            item = item,
                            folderNames = meta.folderNames,
                            tagNames = meta.tagNames,
                            onOpenClick = { onOpenItem(item.id) },
                            onFavoriteClick = { onToggleFavorite(item) },
                            onShareToDeviceClick = { onSendItemToDevice(item) },
                            onEditClick = { onEditItem(item) },
                            onDeleteClick = { onDeleteItem(item) }
                        )
                    }
                }
            }
        } else {
            val rootFolders = remember(snapshot.folders) { snapshot.folders.filter { it.parentFolderId == null } }
            val folderRows = remember(rootFolders) { rootFolders.chunked(2) }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .testTag("folders_root_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Hero 3D Tilted Card Stack + Quick Vault Action Buttons
                item(key = "vault_hero", contentType = "vault_hero") {
                    VaultHeroCardStack(
                        snapshot = snapshot,
                        hasPin = !vaultPin.isNullOrBlank(),
                        isVaultLocked = isVaultLocked,
                        onPinVaultClick = { showPinDrawer = true },
                        onVaultDetailsClick = { showVaultDetailsDialog = true },
                        onToggleLockClick = {
                            if (vaultPin.isNullOrBlank()) {
                                showPinDrawer = true
                            } else {
                                onSetVaultLocked(!isVaultLocked)
                            }
                        },
                        onOpenWorkspaceClick = onOpenWorkspace,
                        onOpenMonkModeClick = onOpenMonkMode
                    )
                }

                // Header + Create CTA
                item(key = "folders_header", contentType = "folders_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Folders & Cards",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "${snapshot.folders.size} folders • ${snapshot.tags.size} tags",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk
                            )
                        }
                        TactilePrimaryPillButton(
                            text = if (segment == FoldersSegment.COLLECTIONS) "New Collection" else "New Folder",
                            iconType = MinimalIconType.PLUS,
                            onClick = {
                                when (segment) {
                                    FoldersSegment.COLLECTIONS -> showCreateCollectionDialog = true
                                    else -> {
                                        newFolderParentId = null
                                        showCreateFolderDialog = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("folders_create_button")
                        )
                    }
                }

                // Recessed Segment Switcher
                item(key = "folders_segment", contentType = "segment_switcher") {
                    RecessedFramingBezel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 9999.dp,
                        innerPadding = PaddingValues(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FoldersSegment.entries.forEach { seg ->
                                val active = segment == seg
                                Surface(
                                    onClick = { segment = seg },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (active) palette.primaryButtonBg else Color.Transparent,
                                    border = if (active) BorderStroke(1.dp, palette.specularRimOutline) else null,
                                    shadowElevation = if (active) 4.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("segment_${seg.name.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 11.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = seg.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (active) palette.primaryButtonContent else palette.secondaryInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                when (segment) {
                    FoldersSegment.FOLDERS -> {
                        if (snapshot.folders.size <= 2) {
                            item(key = "starter_folders", contentType = "starter_card") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Starter Folders", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                            Text(
                                                "Quickly populate AI & Tech, GitHub, Design & Learning folders.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.secondaryInk
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = onAddStarterFolders,
                                            shape = RoundedCornerShape(9999.dp),
                                            border = BorderStroke(1.dp, palette.inputBorder),
                                            modifier = Modifier.testTag("add_starter_folders_button")
                                        ) {
                                            Text("Add", color = palette.primaryInk)
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = folderRows,
                            key = { "row_${it.first().id}" },
                            contentType = { "folder_row" }
                        ) { rowFolders ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowFolders.forEach { folder ->
                                    FolderPocketCard(
                                        folder = folder,
                                        previewItems = previewItemsMap[folder.id].orEmpty(),
                                        itemCount = itemCountByFolder[folder.id] ?: 0,
                                        onOpen = { selectedFolderId = folder.id },
                                        onRename = { renameTargetFolder = folder },
                                        onDelete = { deleteTargetFolder = folder },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowFolders.size == 1) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    FoldersSegment.TAGS -> {
                        if (snapshot.tags.isEmpty()) {
                            item(key = "empty_tags", contentType = "empty_card") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("No tags yet", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                        Text(
                                            "Tags appear automatically when you tag items or approve AI tag suggestions.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.secondaryInk
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = snapshot.tags,
                                key = { it.id },
                                contentType = { "tag_card" }
                            ) { tag ->
                                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
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
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.accentSoftBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                MinimalIcon(
                                                    type = MinimalIconType.SPARKLE,
                                                    active = tag.origin == "ai",
                                                    tint = palette.accentSoftInk,
                                                    cutoutColor = palette.accentSoftBg,
                                                    size = 18.dp
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "#${tag.name}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = palette.primaryInk
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        text = "${itemCountByTag[tag.id] ?: 0} items",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.secondaryInk
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(9999.dp),
                                                        color = if (tag.origin == "ai") palette.accentSoftBg else palette.recessedTrayBg
                                                    ) {
                                                        Text(
                                                            text = if (tag.origin == "ai") "AI Tag" else "User Tag",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = if (tag.origin == "ai") palette.accentSoftInk else palette.secondaryInk,
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FloatingGlassIconButton(
                                                iconType = MinimalIconType.EDIT,
                                                contentDescription = "Rename tag",
                                                size = 38.dp,
                                                onClick = { renameTargetTag = tag }
                                            )
                                            FloatingGlassIconButton(
                                                iconType = MinimalIconType.TRASH,
                                                contentDescription = "Delete tag",
                                                tint = CrimsonAccent,
                                                size = 38.dp,
                                                onClick = { deleteTargetTag = tag }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FoldersSegment.COLLECTIONS -> {
                        if (snapshot.collections.isEmpty()) {
                            item(key = "empty_collections", contentType = "empty_card") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("No collections yet", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                        Text(
                                            "Group cross-folder items for projects, study guides, or reading lists.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.secondaryInk
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = snapshot.collections,
                                key = { it.id },
                                contentType = { "collection_card" }
                            ) { col ->
                                val colCount = collectionItemCounts[col.id] ?: 0
                                TactileGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("collection_card_${col.id}"),
                                    cornerRadius = 26.dp,
                                    onClick = { selectedCollectionId = col.id }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(15.dp))
                                                .background(palette.accentSoftBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            MinimalIcon(
                                                type = MinimalIconType.LAYERS,
                                                active = true,
                                                tint = palette.accentSoftInk,
                                                cutoutColor = palette.accentSoftBg,
                                                size = 22.dp
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(col.name, style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                            if (col.description.isNotBlank()) {
                                                Text(col.description, style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                            }
                                            Text("$colCount item(s) • Tap to open", style = MaterialTheme.typography.labelMedium, color = palette.accentSoftInk)
                                        }
                                        MinimalIcon(
                                            type = MinimalIconType.ARROW_UP_RIGHT,
                                            tint = palette.secondaryInk,
                                            size = 18.dp,
                                            contentDescription = "Open collection"
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

    if (showPinDrawer) {
        VaultPinDrawerModal(
            existingPin = vaultPin,
            onDismiss = { showPinDrawer = false },
            onSavePin = { newPin -> onSetVaultPin(newPin) },
            onClearPin = {
                onSetVaultLocked(false)
                onSetVaultPin(null)
            }
        )
    }

    deleteTargetTag?.let { tag ->
        LiquidDeleteConfirmationDialog(
            title = "Delete Tag?",
            message = "Remove tag \"#${tag.name}\" from all items in your library?",
            onConfirm = {
                onDeleteTag(tag.id)
                deleteTargetTag = null
            },
            onDismiss = { deleteTargetTag = null }
        )
    }

    deleteTargetCollection?.let { col ->
        LiquidDeleteConfirmationDialog(
            title = "Delete Collection?",
            message = "Delete collection \"${col.name}\"? Saved items inside will remain in your library.",
            onConfirm = {
                onDeleteCollection(col.id)
                if (selectedCollectionId == col.id) selectedCollectionId = null
                deleteTargetCollection = null
            },
            onDismiss = { deleteTargetCollection = null }
        )
    }

    if (showVaultDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showVaultDetailsDialog = false },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Vault Details", color = palette.primaryInk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Items: ${snapshot.items.size}", color = palette.primaryInk)
                    Text("Folders & Subfolders: ${snapshot.folders.size}", color = palette.primaryInk)
                    Text("Collections: ${snapshot.collections.size}", color = palette.primaryInk)
                    Text("Tags: ${snapshot.tags.size}", color = palette.primaryInk)
                    Text(
                        "Local SAF File References: ${snapshot.items.count { it.isDeviceLocal }}",
                        color = palette.secondaryInk
                    )
                    Text(
                        "PIN Protection: ${if (vaultPin.isNullOrBlank()) "Not set" else "Enabled"}",
                        color = palette.accentSoftInk
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showVaultDetailsDialog = false }) {
                    Text("Close", color = palette.primaryInk)
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text(if (newFolderParentId == null) "Create Folder" else "Create Subfolder", color = palette.primaryInk) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("create_folder_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateFolder(name.trim(), newFolderParentId)
                            showCreateFolderDialog = false
                        }
                    },
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonBg,
                        contentColor = palette.primaryButtonContent
                    ),
                    modifier = Modifier.testTag("create_folder_confirm_button")
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel", color = palette.secondaryInk) }
            }
        )
    }

    renameTargetFolder?.let { folder ->
        var newName by remember(folder.id) { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { renameTargetFolder = null },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Rename Folder", color = palette.primaryInk) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New name") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRenameFolder(folder.id, newName.trim())
                            renameTargetFolder = null
                        }
                    },
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonBg,
                        contentColor = palette.primaryButtonContent
                    )
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetFolder = null }) { Text("Cancel", color = palette.secondaryInk) }
            }
        )
    }

    deleteTargetFolder?.let { folder ->
        val otherFolders = snapshot.folders.filter { it.id != folder.id && !it.isInbox }
        var moveTargetId by remember(folder.id) { mutableStateOf<String?>(AppDatabase.INBOX_FOLDER_ID) }
        AlertDialog(
            onDismissRequest = { deleteTargetFolder = null },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete \"${folder.name}\"?", color = palette.primaryInk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose what happens to items inside this folder (items will NOT be permanently deleted):",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = moveTargetId == AppDatabase.INBOX_FOLDER_ID,
                            onClick = { moveTargetId = AppDatabase.INBOX_FOLDER_ID }
                        )
                        Text("Move items to Inbox", color = palette.primaryInk)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = moveTargetId == null,
                            onClick = { moveTargetId = null }
                        )
                        Text("Leave items unfiled", color = palette.primaryInk)
                    }
                    otherFolders.take(4).forEach { candidate ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = moveTargetId == candidate.id,
                                onClick = { moveTargetId = candidate.id }
                            )
                            Text("Move to ${candidate.name}", color = palette.primaryInk)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolder(folder.id, moveTargetId)
                        if (selectedFolderId == folder.id) selectedFolderId = null
                        deleteTargetFolder = null
                    },
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
                ) { Text("Delete Folder", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetFolder = null }) { Text("Cancel", color = palette.secondaryInk) }
            }
        )
    }

    renameTargetTag?.let { tag ->
        var newName by remember(tag.id) { mutableStateOf(tag.name) }
        AlertDialog(
            onDismissRequest = { renameTargetTag = null },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Rename or Merge Tag", color = palette.primaryInk) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tag name (matching existing merges)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRenameTag(tag.id, newName.trim())
                            renameTargetTag = null
                        }
                    },
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonBg,
                        contentColor = palette.primaryButtonContent
                    )
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetTag = null }) { Text("Cancel", color = palette.secondaryInk) }
            }
        )
    }

    if (showCreateCollectionDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Create Collection", color = palette.primaryInk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Collection name") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description (optional)") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateCollection(name.trim(), desc.trim())
                            showCreateCollectionDialog = false
                        }
                    },
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonBg,
                        contentColor = palette.primaryButtonContent
                    )
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) { Text("Cancel", color = palette.secondaryInk) }
            }
        )
    }

    addTargetDialogState?.let { (targetFolderId, targetCollectionId) ->
        AlertDialog(
            onDismissRequest = { addTargetDialogState = null },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = if (targetCollectionId != null) "Add to Collection" else "Add to Folder",
                    color = palette.primaryInk
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose how you want to capture directly into this destination:",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                    TactilePrimaryPillButton(
                        text = "Add Link or Note",
                        iconType = MinimalIconType.EDIT,
                        onClick = {
                            addTargetDialogState = null
                            if (targetCollectionId != null) {
                                onQuickAddToCollection(targetCollectionId)
                            } else {
                                onQuickAddToFolder(targetFolderId)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_add_link_button")
                    )
                    OutlinedButton(
                        onClick = {
                            addTargetDialogState = null
                            onPickMediaForTarget(targetFolderId, targetCollectionId)
                        },
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_add_media_button")
                    ) {
                        Text("Pick Photo / Video", color = palette.primaryInk)
                    }
                    OutlinedButton(
                        onClick = {
                            addTargetDialogState = null
                            onPickFileForTarget(targetFolderId, targetCollectionId)
                        },
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_add_file_button")
                    ) {
                        Text("Pick Local File (SAF)", color = palette.primaryInk)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addTargetDialogState = null }) {
                    Text("Close", color = palette.secondaryInk)
                }
            }
        )
    }
}

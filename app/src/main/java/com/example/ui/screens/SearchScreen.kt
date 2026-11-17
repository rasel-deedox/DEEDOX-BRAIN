package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ItemEntity
import com.example.data.storage.FileHealthStatus
import com.example.ui.components.AppLogo
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.ResourceCardDisplayMeta
import com.example.ui.components.ResourceItemCard
import com.example.ui.components.TactileGlassCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.theme.TerracottaBottom
import com.example.ui.theme.TerracottaTop
import com.example.ui.viewmodel.LibrarySnapshot
import com.example.ui.viewmodel.SearchUiState

private val CONTENT_TYPE_FILTERS = listOf(
    "ALL" to "All Types",
    "video" to "Video",
    "github" to "GitHub",
    "article" to "Article",
    "document" to "Document",
    "image" to "Image",
    "note" to "Note",
    "local_file" to "Local File"
)

@Composable
fun SearchScreen(
    snapshot: LibrarySnapshot,
    searchState: SearchUiState,
    isOnline: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleAiMode: (Boolean) -> Unit,
    onSelectTypeFilter: (String) -> Unit,
    onSelectFolderFilter: (String?) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onOpenItem: (String) -> Unit,
    onToggleFavorite: (ItemEntity) -> Unit,
    onSendItemToDevice: (ItemEntity) -> Unit = {},
    onEditItem: (ItemEntity) -> Unit = {},
    onDeleteItem: (ItemEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val itemById = remember(snapshot.items) { snapshot.items.associateBy { it.id } }
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

    val displayedLocalHits = remember(
        searchState.localHits,
        searchState.contentTypeFilter,
        searchState.favoritesOnly,
        searchState.folderFilter,
        itemById,
        foldersByItem
    ) {
        searchState.localHits.filter { hit ->
            val item = itemById[hit.itemId] ?: return@filter false
            val matchesType = searchState.contentTypeFilter == "ALL" || item.contentType == searchState.contentTypeFilter
            val matchesFav = !searchState.favoritesOnly || item.isFavorite
            val matchesFolder = searchState.folderFilter == null ||
                foldersByItem[item.id].orEmpty().contains(searchState.folderFilter)
            matchesType && matchesFav && matchesFolder
        }
    }

    val itemCardMeta = remember(
        displayedLocalHits,
        itemById,
        foldersByItem,
        folderNameMap,
        tagsByItem,
        tagNameMap,
        fileSourceByItem
    ) {
        displayedLocalHits.mapNotNull { hit -> itemById[hit.itemId] }.associate { item ->
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

    val aiMatchedById = remember(searchState.aiResponse) {
        searchState.aiResponse?.matches.orEmpty().associateBy { it.itemId }
    }

    val displayedResultIds = remember(displayedLocalHits, searchState.isOnlineAiMode, aiMatchedById, itemById, searchState.contentTypeFilter, searchState.favoritesOnly, searchState.folderFilter) {
        val localIds = displayedLocalHits.map { it.itemId }
        if (!searchState.isOnlineAiMode || aiMatchedById.isEmpty()) {
            localIds
        } else {
            val localSet = localIds.toSet()
            val extraAiIds = aiMatchedById.keys.filter { id ->
                if (id in localSet) return@filter false
                val item = itemById[id] ?: return@filter false
                val matchesType = searchState.contentTypeFilter == "ALL" || item.contentType == searchState.contentTypeFilter
                val matchesFav = !searchState.favoritesOnly || item.isFavorite
                val matchesFolder = searchState.folderFilter == null ||
                    foldersByItem[item.id].orEmpty().contains(searchState.folderFilter)
                matchesType && matchesFav && matchesFolder
            }
            localIds + extraAiIds
        }
    }

    val localHitById = remember(displayedLocalHits) { displayedLocalHits.associateBy { it.itemId } }

    val semanticItemMeta = remember(
        displayedResultIds,
        itemById,
        foldersByItem,
        folderNameMap,
        tagsByItem,
        tagNameMap,
        fileSourceByItem
    ) {
        displayedResultIds.mapNotNull { id -> itemById[id] }.associate { item ->
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
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "search_header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Search Library",
                            style = MaterialTheme.typography.headlineLarge,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "Deterministic offline full-text index & optional online Gemini reranking",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                    AppLogo(
                        size = 42.dp,
                        cornerRadius = 13.dp,
                        testTag = "search_header_app_logo"
                    )
                }
            }

            // Recessed Framing Bezel around Search Bar & Mode Switcher
            item(key = "search_bar_bezel", contentType = "search_bar") {
                RecessedFramingBezel(
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = searchState.query,
                            onValueChange = onQueryChange,
                            placeholder = {
                                Text(
                                    "Search titles, URLs, notes & local files...",
                                    color = palette.secondaryInk
                                )
                            },
                            leadingIcon = {
                                MinimalIcon(
                                    type = MinimalIconType.SEARCH,
                                    active = searchState.query.isNotBlank(),
                                    tint = if (searchState.isOnlineAiMode) palette.accentSoftInk else palette.primaryInk,
                                    size = 20.dp
                                )
                            },
                            trailingIcon = {
                                if (searchState.query.isNotEmpty()) {
                                    IconButton(onClick = { onQueryChange("") }) {
                                        Icon(
                                            Icons.Rounded.Clear,
                                            contentDescription = "Clear search",
                                            tint = palette.secondaryInk
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(9999.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.cardSurfaceTop,
                                unfocusedContainerColor = palette.cardSurfaceTop,
                                focusedBorderColor = if (searchState.isOnlineAiMode) CrimsonAccent else palette.primaryInk,
                                unfocusedBorderColor = palette.inputBorder,
                                focusedTextColor = palette.primaryInk,
                                unfocusedTextColor = palette.primaryInk
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(9999.dp), ambientColor = Color(0x0E0F172A))
                                .testTag("search_input")
                        )

                        // Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = { onToggleAiMode(false) },
                                shape = RoundedCornerShape(9999.dp),
                                color = if (!searchState.isOnlineAiMode) palette.primaryButtonBg else palette.frostedPillBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_mode_local_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Local Index (Offline)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!searchState.isOnlineAiMode) palette.primaryButtonContent else palette.primaryInk
                                    )
                                }
                            }

                            val aiShape = RoundedCornerShape(9999.dp)
                            Surface(
                                onClick = { onToggleAiMode(true) },
                                shape = aiShape,
                                color = Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (searchState.isOnlineAiMode) palette.specularBorderTop else palette.specularRimOutline
                                ),
                                shadowElevation = if (searchState.isOnlineAiMode) 10.dp else 2.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        brush = if (searchState.isOnlineAiMode) {
                                            Brush.linearGradient(listOf(TerracottaTop, TerracottaBottom))
                                        } else {
                                            Brush.linearGradient(listOf(palette.frostedPillBg, palette.frostedPillBg))
                                        },
                                        shape = aiShape
                                    )
                                    .testTag("search_mode_ai_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MinimalIcon(
                                        type = MinimalIconType.SPARKLE,
                                        active = true,
                                        tint = if (searchState.isOnlineAiMode) Color.White else palette.accentSoftInk,
                                        cutoutColor = if (searchState.isOnlineAiMode) TerracottaBottom else palette.frostedPillBg,
                                        size = 16.dp
                                    )
                                    Text(
                                        text = "Ask AI (Online)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (searchState.isOnlineAiMode) Color.White else palette.primaryInk
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isOnline) {
                item(key = "search_offline_banner", contentType = "banner") {
                    OfflineStatusBanner(
                        message = if (searchState.isOnlineAiMode) {
                            "Offline: Online AI search unavailable. Local search results remain active."
                        } else {
                            "Offline Mode • Deterministic SQLite search running locally."
                        }
                    )
                }
            }

            // Content-type & Favorites Filter Pills
            item(key = "search_type_filters", contentType = "filter_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchPillChip(
                        selected = searchState.favoritesOnly,
                        label = "Favorites",
                        onClick = onToggleFavoritesOnly,
                        modifier = Modifier.testTag("search_filter_favorites")
                    )
                    CONTENT_TYPE_FILTERS.forEach { (code, label) ->
                        SearchPillChip(
                            selected = searchState.contentTypeFilter == code,
                            label = label,
                            onClick = { onSelectTypeFilter(code) },
                            modifier = Modifier.testTag("search_filter_$code")
                        )
                    }
                }
            }

            // Folder filter pills
            if (snapshot.folders.isNotEmpty()) {
                item(key = "search_folder_filters", contentType = "filter_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchPillChip(
                            selected = searchState.folderFilter == null,
                            label = "All Folders",
                            onClick = { onSelectFolderFilter(null) }
                        )
                        snapshot.folders.forEach { folder ->
                            SearchPillChip(
                                selected = searchState.folderFilter == folder.id,
                                label = folder.name,
                                onClick = {
                                    onSelectFolderFilter(
                                        if (searchState.folderFilter == folder.id) null else folder.id
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (searchState.isSearching) {
                item(key = "search_progress", contentType = "status_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = palette.accentSoftInk
                            )
                            Text(
                                text = if (searchState.isOnlineAiMode) "Running Online AI reranking..." else "Searching local index...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.primaryInk
                            )
                        }
                    }
                }
            }

            if (!searchState.errorMessage.isNullOrBlank()) {
                item(key = "search_error", contentType = "banner") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = palette.primaryInk)
                            Text(
                                text = searchState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.primaryInk
                            )
                        }
                    }
                }
            }

            // Online AI Summary Answer
            if (searchState.isOnlineAiMode && searchState.aiResponse != null) {
                item(key = "search_ai_summary", contentType = "ai_summary_card") {
                    TactileGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_ai_summary_card"),
                        containerColor = palette.accentSoftBg
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MinimalIcon(
                                    type = MinimalIconType.SPARKLE,
                                    active = true,
                                    tint = palette.accentSoftInk,
                                    cutoutColor = palette.accentSoftBg,
                                    size = 18.dp
                                )
                                Text(
                                    text = "Online AI Answer",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.accentSoftInk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = searchState.aiResponse.summaryAnswer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.primaryInk
                            )
                        }
                    }
                }
            }

            if (searchState.query.isBlank()) {
                item(key = "search_empty_prompt", contentType = "info_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Instant Local & AI Search",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "Type keywords to search titles, URLs, domains, notes, tags, and attached local filenames offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk
                            )
                        }
                    }
                }
            } else if (displayedResultIds.isEmpty() && !searchState.isSearching) {
                item(key = "search_no_results", contentType = "empty_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No matching items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                            Text(
                                text = "Try broader keywords, switch filters, or try Ask AI (Online) for semantic matching.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk
                            )
                        }
                    }
                }
            } else {
                items(
                    items = displayedResultIds,
                    key = { it },
                    contentType = { "resource_card" }
                ) { itemId ->
                    val item = itemById[itemId] ?: return@items
                    val hit = localHitById[itemId]
                    val aiMatch = aiMatchedById[itemId]
                    val meta = itemCardMeta[item.id] ?: semanticItemMeta[item.id] ?: ResourceCardDisplayMeta()

                    ResourceItemCard(
                        item = item,
                        folderNames = meta.folderNames,
                        tagNames = meta.tagNames,
                        healthStatusLabel = meta.healthStatusLabel,
                        matchLabel = aiMatch?.matchStrength ?: hit?.matchStrength?.label ?: "AI Match",
                        matchReason = aiMatch?.reason ?: hit?.matchReason,
                        onOpenClick = { onOpenItem(item.id) },
                        onFavoriteClick = { onToggleFavorite(item) },
                        onShareToDeviceClick = { onSendItemToDevice(item) },
                        onEditClick = { onEditItem(item) },
                        onDeleteClick = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPillChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9999.dp),
        color = if (selected) palette.primaryButtonBg else palette.frostedPillBg,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        shadowElevation = if (selected) 5.dp else 2.dp,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) palette.primaryButtonContent else palette.secondaryInk,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
        )
    }
}

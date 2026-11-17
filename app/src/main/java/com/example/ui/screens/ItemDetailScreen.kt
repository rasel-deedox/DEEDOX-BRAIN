package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ai.AiTagSummarySuggestion
import com.example.data.local.ItemEntity
import com.example.data.local.LibraryReview
import com.example.data.local.NoteEntity
import com.example.data.local.SmartReminderEntity
import com.example.data.media.VideoThumbnailGenerator
import com.example.data.storage.FileHealthStatus
import com.example.data.util.UrlNormalizer
import java.text.DateFormat
import java.util.Date
import com.example.ui.components.AccentActionPill
import com.example.ui.components.EditResourceDialog
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.LiquidDeleteConfirmationDialog
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.formatRelativeDate
import com.example.ui.components.liquidReviewSectionItems
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.viewmodel.LibrarySnapshot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    item: ItemEntity?,
    notes: List<NoteEntity>,
    itemReviews: List<LibraryReview>,
    snapshot: LibrarySnapshot,
    isOnline: Boolean,
    isAiBusy: Boolean,
    aiSuggestion: AiTagSummarySuggestion?,
    aiError: String?,
    onBack: () -> Unit,
    onToggleFavorite: (ItemEntity) -> Unit,
    onSendToDevice: (ItemEntity) -> Unit,
    onWriteReviewClick: (String) -> Unit,
    onToggleReviewHelpful: (String) -> Unit,
    onSoftDelete: (String) -> Unit,
    onAddToFolder: (String, String) -> Unit,
    onRemoveFromFolder: (String, String) -> Unit,
    onAddTag: (String, String, String) -> Unit,
    onRemoveTag: (String, String) -> Unit,
    onSaveNote: (String, String, String?) -> Unit,
    onDeleteNote: (String, String) -> Unit,
    onEnrichMetadata: (String) -> Unit,
    onSuggestAiTags: (String) -> Unit,
    onRegrantLocalAccess: () -> Unit,
    onUpdateItem: (itemId: String, title: String, url: String?, description: String) -> Unit = { _, _, _, _ -> },
    linkedReminders: List<SmartReminderEntity> = emptyList(),
    onCreateReminderClick: (ItemEntity) -> Unit = {},
    onToggleReminder: (SmartReminderEntity) -> Unit = {},
    onDeleteReminder: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val palette = LiquidTheme.palette
    val context = LocalContext.current
    var showFolderPicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var newNoteInput by remember { mutableStateOf("") }
    var openError by remember { mutableStateOf<String?>(null) }
    var pendingDeleteTitle by remember { mutableStateOf("") }
    var pendingDeleteMessage by remember { mutableStateOf("") }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    if (item == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Item not found or moved to trash", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                TactilePrimaryPillButton(text = "Back to Library", onClick = onBack)
            }
        }
        return
    }

    val itemFolderIds = remember(snapshot.itemFolders, item.id) {
        snapshot.itemFolders.filter { it.itemId == item.id }.map { it.folderId }.toSet()
    }
    val itemFolders = remember(snapshot.folders, itemFolderIds) {
        snapshot.folders.filter { it.id in itemFolderIds }
    }
    val itemTagIds = remember(snapshot.itemTags, item.id) {
        snapshot.itemTags.filter { it.itemId == item.id }.map { it.tagId }.toSet()
    }
    val itemTags = remember(snapshot.tags, itemTagIds) {
        snapshot.tags.filter { it.id in itemTagIds }
    }
    val fileSource = remember(snapshot.fileSources, item.id) {
        snapshot.fileSources.find { it.itemId == item.id }
    }
    val effectiveThumbUrl = remember(item.thumbnailUrl, item.originalUrl, item.canonicalUrl, item.domain) {
        item.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: (item.originalUrl ?: item.canonicalUrl)
                ?.takeIf { UrlNormalizer.isSafeRemoteUrl(it) }
                ?.let { UrlNormalizer.resolveSmartThumbnailUrl(it, item.domain) }
    }
    val isFaviconPreview = remember(effectiveThumbUrl) {
        UrlNormalizer.isFaviconFallbackUrl(effectiveThumbUrl)
    }
    val durationLabel = remember(item.metadataJson) {
        VideoThumbnailGenerator.extractDurationLabel(item.metadataJson)
    }
    var heroImageFailed by remember(item.id, effectiveThumbUrl) { mutableStateOf(false) }
    val heroImageRequest = remember(effectiveThumbUrl, context) {
        effectiveThumbUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                )
                .addHeader("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .crossfade(false)
                .build()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top floating glass bar
            item(key = "detail_top_bar", contentType = "top_bar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingGlassIconButton(
                            iconType = MinimalIconType.BACK,
                            contentDescription = "Back",
                            onClick = onBack,
                            modifier = Modifier.testTag("detail_back_button")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FloatingGlassIconButton(
                                iconType = MinimalIconType.EDIT,
                                contentDescription = "Edit item",
                                tint = palette.primaryInk,
                                onClick = { showEditDialog = true },
                                modifier = Modifier.testTag("detail_edit_button")
                            )
                            FloatingGlassIconButton(
                                iconType = MinimalIconType.DEVICE_SYNC,
                                contentDescription = "Send to PC or Device",
                                tint = palette.primaryInk,
                                onClick = { onSendToDevice(item) },
                                modifier = Modifier.testTag("detail_send_device_button")
                            )
                            FloatingGlassIconButton(
                                iconType = MinimalIconType.BOOKMARK,
                                contentDescription = "Toggle bookmark",
                                active = item.isFavorite,
                                tint = if (item.isFavorite) CrimsonAccent else palette.primaryInk,
                                onClick = { onToggleFavorite(item) },
                                modifier = Modifier.testTag("detail_favorite_button")
                            )
                            FloatingGlassIconButton(
                                iconType = MinimalIconType.TRASH,
                                contentDescription = "Delete item",
                                tint = CrimsonAccent,
                                onClick = {
                                    pendingDeleteTitle = "Delete Resource?"
                                    pendingDeleteMessage = "Move \"${item.title}\" to trash?"
                                    pendingDeleteAction = {
                                        onSoftDelete(item.id)
                                        onBack()
                                    }
                                },
                                modifier = Modifier.testTag("detail_delete_button")
                            )
                        }
                    }
                }

                // Hero Header Card
                item(key = "detail_hero_card", contentType = "hero_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 30.dp) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (heroImageRequest != null && !heroImageFailed) {
                                if (isFaviconPreview) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(148.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(palette.recessedTrayBg)
                                            .border(1.dp, palette.specularRimOutline, RoundedCornerShape(22.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = palette.frostedPillBg,
                                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                                shadowElevation = 6.dp
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .padding(10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = heroImageRequest,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Fit,
                                                        onError = { heroImageFailed = true },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                    )
                                                }
                                            }
                                            if (!item.domain.isNullOrBlank()) {
                                                Text(
                                                    text = item.domain,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.secondaryInk
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(184.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                    ) {
                                        AsyncImage(
                                            model = heroImageRequest,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            onError = { heroImageFailed = true },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (item.contentType == "video" && !durationLabel.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(9999.dp),
                                                color = Color(0xCC0D0D10),
                                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.28f)),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = durationLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                if (!item.canonicalUrl.isNullOrBlank()) {
                                    Text(
                                        text = item.canonicalUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.accentSoftInk
                                    )
                                }
                                if (item.description.isNotBlank()) {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.secondaryInk
                                    )
                                }

                                if (!openError.isNullOrBlank()) {
                                    Text(
                                        text = openError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrimsonAccent
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val targetUriString = item.originalUrl ?: item.canonicalUrl ?: fileSource?.contentUri
                                    if (!targetUriString.isNullOrBlank()) {
                                        TactilePrimaryPillButton(
                                            text = if (fileSource != null) "Open File" else "Open Link",
                                            iconType = MinimalIconType.OPEN_EXTERNAL,
                                            onClick = {
                                                openError = null
                                                try {
                                                    val uri = Uri.parse(targetUriString)
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        if (fileSource != null) {
                                                            setDataAndType(uri, fileSource.mimeType)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        } else {
                                                            data = uri
                                                        }
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    openError = "Unable to open resource on this device (${e.localizedMessage ?: "no handler"})."
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("detail_open_external_button")
                                        )
                                    }

                                    if (!item.originalUrl.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = { onEnrichMetadata(item.id) },
                                            enabled = isOnline,
                                            shape = RoundedCornerShape(9999.dp),
                                            border = BorderStroke(1.dp, palette.inputBorder),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("detail_refresh_meta_button")
                                        ) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = palette.primaryInk, modifier = Modifier.size(16.dp))
                                            Text(" Refresh Meta", color = palette.primaryInk)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Local File Reference Health Card
                if (fileSource != null) {
                    item(key = "detail_file_health", contentType = "detail_card") {
                        val health = FileHealthStatus.entries.find { it.code == fileSource.availabilityStatus }
                            ?: FileHealthStatus.NOT_YET_CHECKED
                        val isHealthy = health == FileHealthStatus.AVAILABLE
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Local File Reference (SAF)", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (isHealthy) palette.accentSoftBg else CrimsonAccent
                                    ) {
                                        Text(
                                            text = health.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isHealthy) palette.accentSoftInk else Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${fileSource.displayName} • ${fileSource.mimeType} • ${if (fileSource.persistedPermission) "Persisted Read Access" else "Temporary Access"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                                if (!isHealthy || !fileSource.persistedPermission) {
                                    OutlinedButton(
                                        onClick = onRegrantLocalAccess,
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, palette.inputBorder),
                                        modifier = Modifier.testTag("detail_regrant_saf_button")
                                    ) {
                                        MinimalIcon(type = MinimalIconType.FOLDER, active = false, tint = palette.primaryInk, size = 16.dp)
                                        Text(" Re-grant File Access", color = palette.primaryInk)
                                    }
                                }
                            }
                        }
                    }
                }

                // Smart Content Reminder Card
                item(key = "detail_reminders", contentType = "detail_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Smart Reminders", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                    Text(
                                        text = "Revisit or review this resource on schedule",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )
                                }
                                TactilePrimaryPillButton(
                                    text = "Set Reminder",
                                    iconType = MinimalIconType.BELL,
                                    onClick = { onCreateReminderClick(item) },
                                    modifier = Modifier.testTag("detail_set_reminder_button")
                                )
                            }
                            if (linkedReminders.isNotEmpty()) {
                                linkedReminders.forEach { rem ->
                                    val timeStr = remember(rem.triggerAtMillis) {
                                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                            .format(Date(rem.triggerAtMillis))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = palette.recessedTrayBg,
                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rem.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = palette.primaryInk,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "$timeStr • ${rem.recurrenceType} • ${rem.ringtoneTitle}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.secondaryInk
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                androidx.compose.material3.Switch(
                                                    checked = rem.isEnabled,
                                                    onCheckedChange = { onToggleReminder(rem) }
                                                )
                                                IconButton(
                                                    onClick = {
                                                        pendingDeleteTitle = "Delete Reminder?"
                                                        pendingDeleteMessage = "Cancel and delete \"${rem.title}\"?"
                                                        pendingDeleteAction = { onDeleteReminder(rem.id) }
                                                    }
                                                ) {
                                                    MinimalIcon(
                                                        type = MinimalIconType.TRASH,
                                                        tint = CrimsonAccent,
                                                        size = 16.dp,
                                                        contentDescription = "Delete reminder"
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

                // AI Summary & Tag Suggestions
                item(key = "detail_ai_summary", contentType = "detail_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AI Summary & Tags", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                AccentActionPill(
                                    text = if (isAiBusy) "Thinking..." else "Suggest",
                                    enabled = !isAiBusy && isOnline && snapshot.settings.aiEnabled,
                                    onClick = { onSuggestAiTags(item.id) },
                                    modifier = Modifier.testTag("detail_ai_suggest_button")
                                )
                            }
                            if (isAiBusy) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = palette.accentSoftInk)
                                    Text("Generating metadata suggestions...", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                                }
                            }
                            if (!aiError.isNullOrBlank()) {
                                Text(aiError, style = MaterialTheme.typography.bodySmall, color = CrimsonAccent)
                            }
                            if (aiSuggestion != null) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = palette.accentSoftBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(aiSuggestion.summary, style = MaterialTheme.typography.bodySmall, color = palette.primaryInk)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            aiSuggestion.suggestedTags.forEach { tag ->
                                                InputChip(
                                                    selected = false,
                                                    onClick = { onAddTag(item.id, tag, "ai") },
                                                    label = { Text("+ #$tag") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Folders Card
                item(key = "detail_folders", contentType = "detail_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Folders", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                TextButton(
                                    onClick = { showFolderPicker = true },
                                    modifier = Modifier.testTag("detail_assign_folder_button")
                                ) {
                                    MinimalIcon(type = MinimalIconType.PLUS, active = false, tint = palette.accentSoftInk, size = 16.dp)
                                    Text(" Assign Folder", color = palette.accentSoftInk)
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemFolders.forEach { folder ->
                                    InputChip(
                                        selected = true,
                                        onClick = { onRemoveFromFolder(item.id, folder.id) },
                                        label = { Text(folder.name) },
                                        trailingIcon = {
                                            Icon(Icons.Rounded.Close, contentDescription = "Remove from folder", modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Tags Card
                item(key = "detail_tags", contentType = "detail_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Tags", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemTags.forEach { tag ->
                                    InputChip(
                                        selected = tag.origin == "ai",
                                        onClick = { onRemoveTag(item.id, tag.id) },
                                        label = {
                                            Text(if (tag.origin == "ai") "#${tag.name} (AI)" else "#${tag.name}")
                                        },
                                        trailingIcon = {
                                            Icon(Icons.Rounded.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTagInput,
                                    onValueChange = { newTagInput = it },
                                    placeholder = { Text("Add tag...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(9999.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = palette.cardSurfaceTop,
                                        unfocusedContainerColor = palette.cardSurfaceTop,
                                        focusedBorderColor = CrimsonAccent,
                                        unfocusedBorderColor = palette.inputBorder,
                                        focusedTextColor = palette.primaryInk,
                                        unfocusedTextColor = palette.primaryInk
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_add_tag_input")
                                )
                                TactilePrimaryPillButton(
                                    text = "Add",
                                    onClick = {
                                        if (newTagInput.isNotBlank()) {
                                            onAddTag(item.id, newTagInput.trim(), "user")
                                            newTagInput = ""
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Notes Card
                item(key = "detail_notes", contentType = "detail_card") {
                    TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Personal Notes (${notes.size})", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                            notes.forEach { note ->
                                key(note.id) {
                                    val noteDateLabel = remember(note.updatedAt) { formatRelativeDate(note.updatedAt) }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (note.isConflictCopy) palette.accentSoftBg else palette.recessedTrayBg,
                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (note.isConflictCopy) "Sync Conflict Copy • $noteDateLabel"
                                                    else noteDateLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (note.isConflictCopy) palette.accentSoftInk else palette.secondaryInk
                                                )
                                                IconButton(
                                                    onClick = {
                                                        pendingDeleteTitle = "Delete Note?"
                                                        pendingDeleteMessage = "Permanently delete this note from \"${item.title}\"?"
                                                        pendingDeleteAction = { onDeleteNote(note.id, item.id) }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 16.dp, contentDescription = "Delete note")
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(note.body, style = MaterialTheme.typography.bodyMedium, color = palette.primaryInk)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newNoteInput,
                                onValueChange = { newNoteInput = it },
                                placeholder = { Text("Write a searchable note...") },
                                minLines = 2,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = palette.cardSurfaceTop,
                                    unfocusedContainerColor = palette.cardSurfaceTop,
                                    focusedBorderColor = CrimsonAccent,
                                    unfocusedBorderColor = palette.inputBorder,
                                    focusedTextColor = palette.primaryInk,
                                    unfocusedTextColor = palette.primaryInk
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_note_input")
                            )
                            TactilePrimaryPillButton(
                                text = "Save Note",
                                onClick = {
                                    if (newNoteInput.isNotBlank()) {
                                        onSaveNote(item.id, newNoteInput.trim(), null)
                                        newNoteInput = ""
                                    }
                                },
                                modifier = Modifier.testTag("detail_save_note_button")
                            )
                        }
                    }
                }

                // Item-Specific Liquid Glass Review & Rating Section (Virtualized)
                liquidReviewSectionItems(
                    reviews = itemReviews,
                    onWriteReviewClick = { onWriteReviewClick(item.id) },
                    onToggleHelpful = onToggleReviewHelpful
                )
            }
        }

    if (showFolderPicker) {
        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Assign to Folder", color = palette.primaryInk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    snapshot.folders.forEach { folder ->
                        val assigned = folder.id in itemFolderIds
                        TextButton(
                            onClick = {
                                if (assigned) onRemoveFromFolder(item.id, folder.id)
                                else onAddToFolder(item.id, folder.id)
                                showFolderPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (assigned) "✓ ${folder.name}" else folder.name,
                                color = palette.primaryInk,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderPicker = false }) { Text("Done", color = palette.accentSoftInk) }
            }
        )
    }

    if (showEditDialog) {
        EditResourceDialog(
            item = item,
            onDismiss = { showEditDialog = false },
            onSave = { title, url, description ->
                onUpdateItem(item.id, title, url, description)
                showEditDialog = false
            }
        )
    }

    pendingDeleteAction?.let { confirmedAction ->
        LiquidDeleteConfirmationDialog(
            title = pendingDeleteTitle.ifBlank { "Confirm Deletion" },
            message = pendingDeleteMessage.ifBlank { "Are you sure you want to delete this item? This action cannot be undone." },
            onConfirm = {
                confirmedAction()
                pendingDeleteAction = null
            },
            onDismiss = { pendingDeleteAction = null }
        )
    }
}

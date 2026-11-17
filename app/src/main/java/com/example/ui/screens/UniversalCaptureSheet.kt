package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.AppDatabase
import com.example.data.local.FolderEntity
import com.example.data.util.UrlNormalizer
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.viewmodel.CaptureDraft
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalCaptureSheet(
    draft: CaptureDraft,
    folders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onPreviewUrl: (String) -> Unit = {},
    onSaveWebOrNote: (
        rawInput: String,
        title: String,
        description: String,
        folderId: String,
        tagsCsv: String,
        note: String,
        preloadedThumb: String?,
        allowDuplicate: Boolean
    ) -> Unit,
    onOpenExistingDuplicate: (String) -> Unit,
    onAddDuplicateToFolder: (existingItemId: String, folderId: String) -> Unit,
    onPickLocalFile: (folderId: String) -> Unit,
    onPickPhotoVideo: (folderId: String) -> Unit,
    onConnectFolderTree: () -> Unit,
    onSaveSharedUris: (List<Uri>, String) -> Unit
) {
    val palette = LiquidTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var rawInput by remember(draft.initialText) { mutableStateOf(draft.initialText) }
    var customTitle by remember(draft.initialTitle) {
        mutableStateOf(
            draft.initialTitle.takeUnless { UrlNormalizer.isGenericPlatformTitle(it) }.orEmpty()
        )
    }
    var descriptionText by remember(draft.initialText) { mutableStateOf("") }
    var userEditedTitle by remember(draft.initialText) {
        mutableStateOf(
            draft.initialTitle.isNotBlank() && !UrlNormalizer.isGenericPlatformTitle(draft.initialTitle)
        )
    }
    var userEditedDescription by remember(draft.initialText) { mutableStateOf(false) }
    var tagsCsv by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedFolderId by remember(draft.targetFolderId) { mutableStateOf(draft.targetFolderId) }

    val extractedUrl = remember(rawInput) { UrlNormalizer.extractFirstWebUrl(rawInput) }
    val parsedUrlInfo = remember(extractedUrl) { extractedUrl?.let { UrlNormalizer.normalize(it) } }

    // Debounced auto-scraping whenever a link is pasted or entered
    LaunchedEffect(extractedUrl) {
        if (extractedUrl != null) {
            delay(320)
            onPreviewUrl(rawInput)
        } else {
            onPreviewUrl("")
        }
    }

    // Automatically populate title & description as soon as metadata preview resolves
    LaunchedEffect(draft.previewMetadata, draft.previewUrl) {
        val meta = draft.previewMetadata ?: return@LaunchedEffect
        val scrapedTitle = meta.title?.trim()?.takeUnless { UrlNormalizer.isGenericPlatformTitle(it) }
        val scrapedDesc = meta.description?.trim()
        if ((!userEditedTitle || UrlNormalizer.isGenericPlatformTitle(customTitle)) && !scrapedTitle.isNullOrBlank()) {
            customTitle = scrapedTitle
        }
        if (!userEditedDescription && !scrapedDesc.isNullOrBlank()) {
            descriptionText = scrapedDesc
        }
    }

    val previewThumbUrl = remember(draft.previewMetadata, extractedUrl) {
        draft.previewMetadata?.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: extractedUrl?.let { UrlNormalizer.resolveSmartThumbnailUrl(it, parsedUrlInfo?.domain) }
    }

    val previewImageRequest = remember(previewThumbUrl, context) {
        previewThumbUrl?.takeIf { it.isNotBlank() }?.let { thumb ->
            ImageRequest.Builder(context)
                .data(thumb)
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                )
                .addHeader("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .crossfade(true)
                .build()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.cardSurfaceTop,
        unfocusedContainerColor = palette.cardSurfaceTop,
        focusedBorderColor = CrimsonAccent,
        unfocusedBorderColor = palette.inputBorder,
        focusedTextColor = palette.primaryInk,
        unfocusedTextColor = palette.primaryInk
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Quick Capture",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )

            if (draft.targetCollectionId != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.accentSoftBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Saving directly into selected Collection",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accentSoftInk,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            if (draft.sharedUris.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = palette.accentSoftBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${draft.sharedUris.size} shared file URI(s) ready to reference locally",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.primaryInk
                        )
                        TactilePrimaryPillButton(
                            text = "Save Shared File Reference(s)",
                            onClick = { onSaveSharedUris(draft.sharedUris, selectedFolderId) }
                        )
                    }
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
                        val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                        if (pasted.isNotEmpty()) {
                            rawInput = pasted
                        }
                    },
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, palette.inputBorder),
                    modifier = Modifier.testTag("capture_paste_clipboard_button")
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(16.dp))
                    Text(" Paste Link", color = palette.primaryInk)
                }
                OutlinedButton(
                    onClick = { onPickLocalFile(selectedFolderId) },
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, palette.inputBorder),
                    modifier = Modifier.testTag("capture_pick_file_button")
                ) {
                    Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = palette.primaryInk, modifier = Modifier.size(16.dp))
                    Text(" Local File (SAF)", color = palette.primaryInk)
                }
                OutlinedButton(
                    onClick = { onPickPhotoVideo(selectedFolderId) },
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, palette.inputBorder),
                    modifier = Modifier.testTag("capture_pick_media_button")
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = palette.primaryInk, modifier = Modifier.size(16.dp))
                    Text(" Photo / Video", color = palette.primaryInk)
                }
                OutlinedButton(
                    onClick = onConnectFolderTree,
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, palette.inputBorder),
                    modifier = Modifier.testTag("capture_connect_folder_button")
                ) {
                    MinimalIcon(type = MinimalIconType.FOLDER, active = false, tint = palette.primaryInk, size = 16.dp)
                    Text(" Connect Folder", color = palette.primaryInk)
                }
            }

            OutlinedTextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                label = { Text("Paste URL or write quick note") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (rawInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    rawInput = ""
                                    if (!userEditedTitle) customTitle = ""
                                    if (!userEditedDescription) descriptionText = ""
                                }
                            ) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear input", tint = palette.secondaryInk, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                    if (pasted.isNotEmpty()) {
                                        rawInput = pasted
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste link", tint = CrimsonAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                minLines = 2,
                shape = RoundedCornerShape(20.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capture_raw_input")
            )

            // Live Auto-Scraped Video / Link Preview Card
            AnimatedVisibility(visible = extractedUrl != null) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = palette.cardSurfaceTop,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_link_preview_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = CrimsonAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = parsedUrlInfo?.inferredSourceType?.replaceFirstChar { it.uppercase() } ?: "Web Link",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                parsedUrlInfo?.domain?.let { dom ->
                                    Text(
                                        text = "• $dom",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondaryInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (draft.isPreviewLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = CrimsonAccent
                                    )
                                    Text(
                                        text = "Auto-scraping...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonAccent
                                    )
                                }
                            } else if (draft.previewMetadata != null) {
                                Text(
                                    text = "Auto-filled",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accentSoftInk
                                )
                            }
                        }

                        var previewImgFailed by remember(previewThumbUrl) { mutableStateOf(false) }
                        val isIconOnly = UrlNormalizer.isFaviconFallbackUrl(previewThumbUrl)
                        if (previewImageRequest != null && !previewImgFailed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isIconOnly) 64.dp else 148.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.recessedTrayBg)
                                    .border(1.dp, palette.specularRimOutline, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isIconOnly) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        AsyncImage(
                                            model = previewImageRequest,
                                            contentDescription = "Link icon",
                                            contentScale = ContentScale.Fit,
                                            onError = { previewImgFailed = true },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Text(
                                            text = if (draft.isPreviewLoading) {
                                                "Fetching main thumbnail & details..."
                                            } else {
                                                parsedUrlInfo?.domain ?: "Web preview"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.secondaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = previewImageRequest,
                                        contentDescription = draft.previewMetadata?.title ?: "Video thumbnail preview",
                                        contentScale = ContentScale.Crop,
                                        onError = { previewImgFailed = true },
                                        modifier = Modifier.matchParentSize()
                                    )
                                    if (parsedUrlInfo?.inferredContentType == "video") {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xAA101012)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Video thumbnail",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val previewTitle = draft.previewMetadata?.title?.takeIf { it.isNotBlank() }
                        val previewDesc = draft.previewMetadata?.description?.takeIf { it.isNotBlank() }
                        if (!previewTitle.isNullOrBlank() || !previewDesc.isNullOrBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!previewTitle.isNullOrBlank()) {
                                    Text(
                                        text = previewTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.primaryInk,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!previewDesc.isNullOrBlank()) {
                                    Text(
                                        text = previewDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else if (!draft.previewError.isNullOrBlank() && !draft.isPreviewLoading) {
                            Text(
                                text = draft.previewError,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryInk
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = customTitle,
                onValueChange = {
                    userEditedTitle = it.isNotBlank()
                    customTitle = it
                },
                label = { Text("Title (auto-scraped for links)") },
                singleLine = true,
                shape = RoundedCornerShape(9999.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capture_title_input")
            )

            OutlinedTextField(
                value = descriptionText,
                onValueChange = {
                    userEditedDescription = it.isNotBlank()
                    descriptionText = it
                },
                label = { Text("Description (auto-scraped for links)") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capture_description_input")
            )

            OutlinedTextField(
                value = tagsCsv,
                onValueChange = { tagsCsv = it },
                label = { Text("Tags (comma separated, optional)") },
                singleLine = true,
                shape = RoundedCornerShape(9999.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capture_tags_input")
            )

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Personal note (optional)") },
                minLines = 2,
                shape = RoundedCornerShape(20.dp),
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("capture_note_input")
            )

            Text("Save to Folder", style = MaterialTheme.typography.labelLarge, color = palette.primaryInk)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                folders.forEach { folder ->
                    val selected = selectedFolderId == folder.id
                    Surface(
                        onClick = { selectedFolderId = folder.id },
                        shape = RoundedCornerShape(9999.dp),
                        color = if (selected) palette.primaryButtonBg else palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.testTag("capture_folder_${folder.id}")
                    ) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) palette.primaryButtonContent else palette.secondaryInk,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            if (!draft.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.accentSoftBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = draft.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonAccent,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            draft.duplicateState?.let { dup ->
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = palette.accentSoftBg,
                    border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_duplicate_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = CrimsonAccent)
                            Text(
                                text = "Duplicate URL Detected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryInk
                            )
                        }
                        Text(
                            text = "Already saved as \"${dup.existingItem.title}\" (${dup.normalizedUrl}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.primaryInk
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TactilePrimaryPillButton(
                                text = "Open Existing",
                                onClick = { onOpenExistingDuplicate(dup.existingItem.id) },
                                modifier = Modifier.testTag("duplicate_open_existing_button")
                            )
                            OutlinedButton(
                                onClick = { onAddDuplicateToFolder(dup.existingItem.id, selectedFolderId) },
                                shape = RoundedCornerShape(9999.dp),
                                border = BorderStroke(1.dp, palette.inputBorder),
                                modifier = Modifier.testTag("duplicate_add_to_folder_button")
                            ) {
                                Text("Add to Folder", color = palette.primaryInk)
                            }
                            TextButton(
                                onClick = {
                                    onSaveWebOrNote(
                                        rawInput,
                                        customTitle,
                                        descriptionText,
                                        selectedFolderId,
                                        tagsCsv,
                                        noteText,
                                        previewThumbUrl,
                                        true
                                    )
                                },
                                modifier = Modifier.testTag("duplicate_save_separate_button")
                            ) {
                                Text("Save Separate", color = palette.accentSoftInk)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, palette.inputBorder),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Cancel", color = palette.primaryInk)
                }
                TactilePrimaryPillButton(
                    text = if (draft.isSaving) "Saving..." else "Save to Library",
                    enabled = rawInput.isNotBlank() && !draft.isSaving,
                    onClick = {
                        onSaveWebOrNote(
                            rawInput,
                            customTitle,
                            descriptionText,
                            selectedFolderId,
                            tagsCsv,
                            noteText,
                            previewThumbUrl,
                            false
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("capture_save_button")
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

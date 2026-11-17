package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.AiPagePolishMode
import com.example.data.export.SmartCanvasPdfAndShare
import com.example.data.local.ItemEntity
import com.example.data.local.WorkspaceNoteEntity
import com.example.data.media.VoicePlaybackState
import com.example.data.repository.CanvasIconKeys
import com.example.data.repository.EmailBlockData
import com.example.data.repository.NoteBacklinksParser
import com.example.data.repository.PageMetaBlockData
import com.example.data.repository.ReminderBlockData
import com.example.data.repository.ScheduleBlockData
import com.example.data.repository.SubpageBlockData
import com.example.data.repository.WorkspaceBlock
import com.example.ui.components.AiPolishModalDialog
import com.example.ui.components.CanvasPageIconBadge
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.SmartCanvasCallbacks
import com.example.ui.components.SmartCanvasCoverBanner
import com.example.ui.components.SmartCanvasHeaderTimerPill
import com.example.ui.components.SmartCanvasRichBlockCard
import com.example.ui.components.SmartCanvasSlashDrawer
import com.example.ui.components.StarterTemplatesStrip
import com.example.ui.components.TactileAlabasterBackground
import com.example.ui.components.TactileGlassCard
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.components.VoiceAudioPlayerCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun NotionEditorSheet(
    existingNote: WorkspaceNoteEntity?,
    availableItems: List<ItemEntity>,
    playbackState: VoicePlaybackState,
    onTogglePlayback: (String) -> Unit,
    onSeekPlayback: (String, Long) -> Unit,
    onAttachReminder: (title: String, noteId: String?) -> Unit,
    onSave: (
        id: String?,
        title: String,
        blocks: List<WorkspaceBlock>,
        tagsCsv: String,
        isPinned: Boolean,
        cloudSync: Boolean,
        linkedItemId: String?,
        audioUri: String?,
        audioDurationMs: Long
    ) -> Unit,
    onDismiss: () -> Unit,
    allNotes: List<WorkspaceNoteEntity> = emptyList(),
    onOpenLinkedNote: (WorkspaceNoteEntity) -> Unit = {},
    onPolishPageWithAi: (suspend (String, List<WorkspaceBlock>, AiPagePolishMode) -> Result<List<WorkspaceBlock>>)? = null,
    onLogClientPaymentToLedger: (clientName: String, projectName: String, amountCents: Long) -> Unit = { _, _, _ -> },
    onScheduleInlineReminder: (ReminderBlockData, String?) -> Unit = { _, _ -> },
    onSyncScheduleTask: (ScheduleBlockData) -> Unit = {},
    onLogFocusTimerSeconds: (String, Long) -> Unit = { _, _ -> },
    onStartVoiceRecord: () -> Unit = {},
    onStopVoiceRecord: () -> Pair<String, Long>? = { null },
    onCreateNestedSubpage: ((SubpageBlockData) -> Unit)? = null
) {
    val context = LocalContext.current
    val palette = LiquidTheme.palette
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var title by remember(existingNote?.id) { mutableStateOf(existingNote?.title.orEmpty()) }
    var tagsCsv by remember(existingNote?.id) { mutableStateOf(existingNote?.tagsCsv.orEmpty()) }
    var isPinned by remember(existingNote?.id) { mutableStateOf(existingNote?.isPinned ?: false) }
    var cloudSync by remember(existingNote?.id) { mutableStateOf(existingNote?.cloudSync ?: true) }
    var linkedItemId by remember(existingNote?.id) { mutableStateOf(existingNote?.linkedItemId) }
    var showSlashMenu by remember { mutableStateOf(false) }
    var showItemPicker by remember { mutableStateOf(false) }
    var showNoteOptions by remember { mutableStateOf(false) }
    var showNoteLinkPicker by remember { mutableStateOf(false) }
    var showAiPolishModal by remember { mutableStateOf(false) }
    var showTemplates by remember(existingNote?.id) { mutableStateOf(existingNote == null) }
    var linkTargetBlockIndex by remember { mutableIntStateOf(0) }
    var triggeredByDoubleBracket by remember { mutableStateOf(false) }

    val parsedInitialAll = remember(existingNote?.id) {
        existingNote?.let { WorkspaceBlock.parseList(it.blocksJson) }.orEmpty()
    }
    val initialMeta = remember(existingNote?.id) {
        parsedInitialAll.firstOrNull { it.type == WorkspaceBlock.TYPE_PAGE_META }
            ?.let { PageMetaBlockData.fromJson(it.metadataJson) }
            ?: PageMetaBlockData()
    }
    var pageMeta by remember(existingNote?.id) { mutableStateOf(initialMeta) }

    val initialBlocks = remember(existingNote?.id) {
        parsedInitialAll.filterNot { it.type == WorkspaceBlock.TYPE_PAGE_META }.ifEmpty {
            listOf(WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = "", pageIndex = 0))
        }
    }
    val blocks = remember(existingNote?.id) {
        mutableStateListOf<WorkspaceBlock>().apply { addAll(initialBlocks) }
    }
    var pageCount by remember(existingNote?.id) {
        mutableIntStateOf(((initialBlocks.maxOfOrNull { it.pageIndex } ?: 0) + 1).coerceAtLeast(1))
    }
    var activePageIndex by remember(existingNote?.id) { mutableIntStateOf(0) }

    // Live header page focus timer
    var headerTimerRunning by remember(existingNote?.id) { mutableStateOf(false) }
    var headerTimerSeconds by remember(existingNote?.id) { mutableLongStateOf(initialMeta.focusSecondsSpent) }
    LaunchedEffect(headerTimerRunning) {
        while (headerTimerRunning) {
            delay(1000L)
            headerTimerSeconds += 1L
        }
    }

    fun addBlock(
        type: String,
        defaultText: String = "",
        linkedId: String? = null,
        pageIndex: Int = activePageIndex,
        metadataJson: String = WorkspaceBlock.defaultMetadataForType(type)
    ) {
        blocks.add(
            WorkspaceBlock(
                type = type,
                text = defaultText,
                checked = false,
                linkedItemId = linkedId,
                linkedItemTitle = if (linkedId != null) defaultText else null,
                pageIndex = pageIndex,
                metadataJson = metadataJson
            )
        )
        showSlashMenu = false
    }

    fun addNewPage() {
        val nextPageIdx = pageCount
        pageCount += 1
        activePageIndex = nextPageIdx
        blocks.add(WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = "Page ${nextPageIdx + 1}", pageIndex = nextPageIdx))
        blocks.add(WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = "", pageIndex = nextPageIdx))
        scope.launch {
            listState.animateScrollToItem((blocks.size + 4).coerceAtMost(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1))
        }
    }

    fun buildBlocksWithMeta(): List<WorkspaceBlock> {
        val updatedMeta = pageMeta.copy(focusSecondsSpent = headerTimerSeconds)
        return listOf(
            WorkspaceBlock(
                type = WorkspaceBlock.TYPE_PAGE_META,
                text = updatedMeta.iconKey,
                metadataJson = updatedMeta.toJson()
            )
        ) + blocks.toList()
    }

    val canvasCallbacks = remember(existingNote?.id, allNotes, title) {
        SmartCanvasCallbacks(
            onOpenSubpage = { sub ->
                val target = allNotes.firstOrNull {
                    it.id == sub.subpageId || (sub.title.isNotBlank() && it.title.equals(sub.title, ignoreCase = true))
                }
                if (target != null) {
                    onOpenLinkedNote(target)
                } else if (onCreateNestedSubpage != null) {
                    onCreateNestedSubpage(sub)
                } else {
                    addNewPage()
                }
            },
            onExportInvoicePdf = { inv ->
                val file = SmartCanvasPdfAndShare.generateStandaloneInvoicePdfFile(context, inv)
                SmartCanvasPdfAndShare.sharePdfFile(context, file, "Invoice ${inv.docNumber}")
            },
            onEmailInvoice = { inv ->
                val launched = SmartCanvasPdfAndShare.launchEmailComposer(
                    context = context,
                    toEmail = inv.clientEmail,
                    subject = inv.toEmailSubject(),
                    body = inv.toEmailBody()
                )
                if (!launched) {
                    Toast.makeText(context, "No email app found on device", Toast.LENGTH_SHORT).show()
                }
            },
            onSendDirectEmail = { mail: EmailBlockData ->
                val launched = SmartCanvasPdfAndShare.launchEmailComposer(
                    context = context,
                    toEmail = mail.toEmail,
                    subject = mail.subject.ifBlank { title.ifBlank { "Shared Note" } },
                    body = mail.body
                )
                if (!launched) {
                    Toast.makeText(context, "No email app found on device", Toast.LENGTH_SHORT).show()
                }
            },
            onScheduleInlineReminder = { rem ->
                onScheduleInlineReminder(rem, existingNote?.id)
                Toast.makeText(context, "Reminder scheduled (${rem.dateTimeLabel})", Toast.LENGTH_SHORT).show()
            },
            onSyncScheduleTask = { sched ->
                onSyncScheduleTask(sched)
                Toast.makeText(context, "Synced task to schedule", Toast.LENGTH_SHORT).show()
            },
            onLogFocusTimerSeconds = onLogFocusTimerSeconds,
            onStartVoiceRecord = onStartVoiceRecord,
            onStopVoiceRecord = onStopVoiceRecord,
            onToggleVoicePlayback = onTogglePlayback,
            onSeekVoicePlayback = onSeekPlayback,
            onLogClientPaymentToLedger = { cName, pName, cents ->
                onLogClientPaymentToLedger(cName, pName, cents)
                Toast.makeText(context, "Payment synced to Money Ledger", Toast.LENGTH_SHORT).show()
            },
            onBuildPageEmailSummary = {
                WorkspaceBlock.plainTextSummary(WorkspaceBlock.encodeList(blocks.toList()))
            }
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedBorderColor = CrimsonAccent.copy(alpha = 0.65f),
        unfocusedBorderColor = Color.Transparent,
        focusedTextColor = palette.primaryInk,
        unfocusedTextColor = palette.primaryInk,
        focusedPlaceholderColor = palette.mutedIcon,
        unfocusedPlaceholderColor = palette.mutedIcon
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler { onDismiss() }

        TactileAlabasterBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .testTag("notion_editor_sheet"),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 800.dp)
                        .fillMaxSize()
                ) {
                    // Full-Page Liquid Glass Top Bar
                    Surface(
                        color = palette.dockBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    FloatingGlassIconButton(
                                        iconType = MinimalIconType.BACK,
                                        contentDescription = "Close notepad",
                                        onClick = onDismiss,
                                        size = 38.dp,
                                        modifier = Modifier.testTag("notion_close_button")
                                    )
                                    CanvasPageIconBadge(
                                        iconKey = pageMeta.iconKey,
                                        size = 32.dp,
                                        iconSize = 17.dp,
                                        active = pageMeta.coverEnabled
                                    )
                                    Column {
                                        Text(
                                            text = if (existingNote == null) "Smart Canvas" else "Smart Page",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primaryInk
                                        )
                                        Text(
                                            text = "$pageCount ${if (pageCount == 1) "page" else "pages"} • ${if (cloudSync) "Cloud" else "Local"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryInk
                                        )
                                    }
                                }

                                TactilePrimaryPillButton(
                                    text = "Save",
                                    enabled = title.isNotBlank() || blocks.any { it.text.isNotBlank() || it.metadataJson.isNotBlank() },
                                    onClick = {
                                        if (headerTimerRunning && headerTimerSeconds >= 5L) {
                                            onLogFocusTimerSeconds(title.ifBlank { "Notion Page" }, headerTimerSeconds)
                                        }
                                        onSave(
                                            existingNote?.id,
                                            title.trim().ifBlank { "Untitled Page" },
                                            buildBlocksWithMeta(),
                                            tagsCsv.trim(),
                                            isPinned,
                                            cloudSync,
                                            linkedItemId,
                                            existingNote?.audioUri,
                                            existingNote?.audioDurationMs ?: 0L
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier.testTag("notion_save_button")
                                )
                            }

                            // Compact Action Strip: AI Polish, PDF, Share Link, Cover/Icon, Focus Timer, Pin, Remind, Options
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { showAiPolishModal = true },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.accentSoftBg,
                                    border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.55f)),
                                    modifier = Modifier.testTag("notion_ai_polish_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(14.dp))
                                        Text("AI Polish", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CrimsonAccent)
                                    }
                                }

                                SmartCanvasHeaderTimerPill(
                                    isRunning = headerTimerRunning,
                                    elapsedSeconds = headerTimerSeconds,
                                    onToggleRunning = {
                                        if (headerTimerRunning && headerTimerSeconds >= 5L) {
                                            onLogFocusTimerSeconds(title.ifBlank { "Notion Page" }, headerTimerSeconds)
                                        }
                                        headerTimerRunning = !headerTimerRunning
                                    }
                                )

                                Surface(
                                    onClick = {
                                        val file = SmartCanvasPdfAndShare.generatePagePdfFile(
                                            context = context,
                                            title = title.ifBlank { "Untitled Page" },
                                            subtitle = if (tagsCsv.isNotBlank()) "Tags: $tagsCsv" else "Personal AI Library",
                                            blocks = blocks.toList()
                                        )
                                        SmartCanvasPdfAndShare.sharePdfFile(context, file, title.ifBlank { "Untitled Page" })
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.testTag("notion_export_pdf_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Export PDF", tint = palette.primaryInk, modifier = Modifier.size(14.dp))
                                        Text("PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = palette.primaryInk)
                                    }
                                }

                                Surface(
                                    onClick = {
                                        val link = SmartCanvasPdfAndShare.generateAndCopyShareLink(
                                            context = context,
                                            pageId = existingNote?.id ?: "draft",
                                            title = title.ifBlank { "Untitled Page" },
                                            blocks = blocks.toList()
                                        )
                                        pageMeta = pageMeta.copy(shareLinkUrl = link)
                                        Toast.makeText(context, "Read-only share link copied", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.testTag("notion_share_link_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = "Share link", tint = palette.primaryInk, modifier = Modifier.size(14.dp))
                                        Text("Share Link", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = palette.primaryInk)
                                    }
                                }

                                Surface(
                                    onClick = { pageMeta = pageMeta.copy(coverEnabled = !pageMeta.coverEnabled) },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (pageMeta.coverEnabled) palette.accentSoftBg else palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Image, contentDescription = null, tint = palette.primaryInk, modifier = Modifier.size(14.dp))
                                        Text("Cover", style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
                                    }
                                }

                                Surface(
                                    onClick = { showTemplates = !showTemplates },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (showTemplates) palette.accentSoftBg else palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                ) {
                                    Text(
                                        text = "Templates",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (showTemplates) CrimsonAccent else palette.primaryInk,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                FloatingGlassIconButton(
                                    iconType = MinimalIconType.BOOKMARK,
                                    contentDescription = if (isPinned) "Unpin page" else "Pin page",
                                    active = isPinned,
                                    tint = if (isPinned) CrimsonAccent else palette.primaryInk,
                                    onClick = { isPinned = !isPinned },
                                    size = 32.dp
                                )
                                FloatingGlassIconButton(
                                    iconType = MinimalIconType.BELL,
                                    contentDescription = "Set reminder for note",
                                    tint = palette.primaryInk,
                                    onClick = {
                                        onAttachReminder(title.ifBlank { "Note Reminder" }, existingNote?.id)
                                    },
                                    size = 32.dp
                                )
                                FloatingGlassIconButton(
                                    iconType = MinimalIconType.SLIDERS,
                                    contentDescription = "Page sync & tags options",
                                    active = showNoteOptions,
                                    tint = if (showNoteOptions) CrimsonAccent else palette.primaryInk,
                                    onClick = { showNoteOptions = !showNoteOptions },
                                    size = 32.dp
                                )
                            }
                        }
                    }

                    // Page Jump Strip when multi-page
                    if (pageCount > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (p in 0 until pageCount) {
                                val selected = activePageIndex == p
                                Surface(
                                    onClick = {
                                        activePageIndex = p
                                        val firstBlockIndex = blocks.indexOfFirst { it.pageIndex == p }
                                        if (firstBlockIndex >= 0) {
                                            scope.launch {
                                                listState.animateScrollToItem(firstBlockIndex + 2)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (selected) palette.primaryButtonBg else palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.testTag("notion_page_pill_${p + 1}")
                                ) {
                                    Text(
                                        text = "Page ${p + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Infinite Vertical 120Hz Smart Canvas
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("notion_canvas_list"),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pageMeta.coverEnabled) {
                            item(key = "notion_cover_banner", contentType = "cover") {
                                SmartCanvasCoverBanner(
                                    coverVariant = pageMeta.coverVariant,
                                    pageIconKey = pageMeta.iconKey,
                                    onSelectVariant = { v -> pageMeta = pageMeta.copy(coverVariant = v) },
                                    onSelectIconKey = { k -> pageMeta = pageMeta.copy(iconKey = k) },
                                    onHideCover = { pageMeta = pageMeta.copy(coverEnabled = false) }
                                )
                            }
                        }

                        if (showNoteOptions) {
                            item(key = "notion_options_card", contentType = "options") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (cloudSync) "Cloud Synced" else "Local Device Only",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (cloudSync) CrimsonAccent else palette.primaryInk
                                                )
                                                Text(
                                                    text = if (cloudSync) "Synced to Firestore across signed-in devices" else "Private offline Room note",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.secondaryInk
                                                )
                                            }
                                            Switch(
                                                checked = cloudSync,
                                                onCheckedChange = { cloudSync = it },
                                                modifier = Modifier.testTag("notion_cloud_sync_switch")
                                            )
                                        }
                                        OutlinedTextField(
                                            value = tagsCsv,
                                            onValueChange = { tagsCsv = it },
                                            label = { Text("Tags (comma separated: study, client, sprint)") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
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
                                                .testTag("notion_tags_input")
                                        )
                                    }
                                }
                            }
                        }

                        if (showTemplates) {
                            item(key = "notion_starter_templates", contentType = "templates") {
                                StarterTemplatesStrip(
                                    onApplyTemplate = { tpl ->
                                        if (tpl.defaultTitle.isNotBlank()) title = tpl.defaultTitle
                                        if (tpl.tagsCsv.isNotBlank()) tagsCsv = tpl.tagsCsv
                                        pageMeta = pageMeta.copy(iconKey = tpl.iconKey)
                                        blocks.clear()
                                        blocks.addAll(tpl.buildBlocks())
                                        showTemplates = false
                                    }
                                )
                            }
                        }

                        // Borderless Spacious Title Header
                        item(key = "notion_title_header", contentType = "title") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (title.isBlank()) {
                                        Text(
                                            text = "Untitled Page...",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.mutedIcon
                                        )
                                    }
                                    BasicTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primaryInk
                                        ),
                                        cursorBrush = SolidColor(CrimsonAccent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("notion_title_input")
                                    )
                                }

                                val timestampLabel = remember(existingNote?.createdAt, existingNote?.updatedAt) {
                                    val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    val createdMs = existingNote?.createdAt ?: System.currentTimeMillis()
                                    val editedMs = existingNote?.updatedAt ?: createdMs
                                    "Created ${fmt.format(Date(createdMs))} • Edited ${fmt.format(Date(editedMs))}"
                                }
                                Text(
                                    text = timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryInk,
                                    modifier = Modifier.testTag("notion_timestamps_text")
                                )

                                if (!existingNote?.audioUri.isNullOrBlank()) {
                                    VoiceAudioPlayerCard(
                                        audioUri = existingNote!!.audioUri!!,
                                        durationMs = existingNote.audioDurationMs,
                                        playbackState = playbackState,
                                        onTogglePlayback = onTogglePlayback,
                                        onSeekTo = onSeekPlayback
                                    )
                                }

                                HorizontalDivider(color = palette.specularRimOutline)
                            }
                        }

                        // Editable Infinite Canvas Blocks
                        itemsIndexed(
                            items = blocks,
                            key = { _, b -> b.id },
                            contentType = { _, b -> b.type }
                        ) { idx, block ->
                            val prevPage = if (idx > 0) blocks[idx - 1].pageIndex else -1
                            val showPageDividerHeader = block.pageIndex > 0 && block.pageIndex != prevPage

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (showPageDividerHeader) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = CrimsonAccent.copy(alpha = 0.45f))
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = palette.accentSoftBg,
                                            border = BorderStroke(1.dp, palette.specularRimOutline)
                                        ) {
                                            Text(
                                                text = "Page ${block.pageIndex + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.accentSoftInk,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = CrimsonAccent.copy(alpha = 0.45f))
                                    }
                                }

                                if (block.type in WorkspaceBlock.RICH_CARD_TYPES) {
                                    SmartCanvasRichBlockCard(
                                        block = block,
                                        index = idx,
                                        teamMembers = emptyList(),
                                        playbackState = playbackState,
                                        callbacks = canvasCallbacks,
                                        onUpdateBlock = { updated -> blocks[idx] = updated },
                                        onDeleteBlock = if (blocks.size > 1) { { blocks.removeAt(idx) } } else null
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = when (block.type) {
                                            WorkspaceBlock.TYPE_CALLOUT -> palette.accentSoftBg
                                            WorkspaceBlock.TYPE_CODE -> palette.recessedTrayBg
                                            else -> palette.cardSurfaceTop.copy(alpha = 0.78f)
                                        },
                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (block.type == WorkspaceBlock.TYPE_DIVIDER) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                HorizontalDivider(modifier = Modifier.weight(1f), color = palette.specularRimOutline)
                                                IconButton(
                                                    onClick = { if (blocks.size > 1) blocks.removeAt(idx) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 14.dp, contentDescription = "Remove divider")
                                                }
                                            }
                                        } else {
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    when (block.type) {
                                                        WorkspaceBlock.TYPE_CHECKLIST -> {
                                                            Checkbox(
                                                                checked = block.checked,
                                                                colors = CheckboxDefaults.colors(
                                                                    checkedColor = CrimsonAccent,
                                                                    uncheckedColor = palette.secondaryInk,
                                                                    checkmarkColor = Color.White
                                                                ),
                                                                onCheckedChange = { checked -> blocks[idx] = block.copy(checked = checked) }
                                                            )
                                                        }
                                                        WorkspaceBlock.TYPE_BULLET -> {
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(top = 18.dp, start = 6.dp)
                                                                    .size(8.dp)
                                                                    .clip(CircleShape)
                                                                    .background(CrimsonAccent)
                                                            )
                                                        }
                                                        WorkspaceBlock.TYPE_H1, WorkspaceBlock.TYPE_H2, WorkspaceBlock.TYPE_H3 -> {
                                                            Text(
                                                                text = block.type,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = CrimsonAccent,
                                                                modifier = Modifier.padding(top = 16.dp, start = 4.dp)
                                                            )
                                                        }
                                                        WorkspaceBlock.TYPE_CODE -> {
                                                            Text(
                                                                text = "</>",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = palette.accentSoftInk,
                                                                modifier = Modifier.padding(top = 16.dp, start = 4.dp)
                                                            )
                                                        }
                                                        WorkspaceBlock.TYPE_ITEM_REF -> {
                                                            Box(modifier = Modifier.padding(top = 14.dp, start = 4.dp)) {
                                                                MinimalIcon(type = MinimalIconType.BOOKMARK, active = true, tint = CrimsonAccent, size = 16.dp)
                                                            }
                                                        }
                                                    }

                                                    val blockTextStyle: TextStyle = when (block.type) {
                                                        WorkspaceBlock.TYPE_H1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                                        WorkspaceBlock.TYPE_H2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                                        WorkspaceBlock.TYPE_H3 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                                        WorkspaceBlock.TYPE_CODE -> TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                                        else -> MaterialTheme.typography.bodyLarge
                                                    }

                                                    OutlinedTextField(
                                                        value = block.text,
                                                        onValueChange = { updated ->
                                                            activePageIndex = block.pageIndex
                                                            if (updated.endsWith("/") && updated.length <= 2) {
                                                                showSlashMenu = true
                                                            }
                                                            if (updated.endsWith("[[") && !block.text.endsWith("[[")) {
                                                                linkTargetBlockIndex = idx
                                                                triggeredByDoubleBracket = true
                                                                showNoteLinkPicker = true
                                                            }
                                                            blocks[idx] = block.copy(text = updated)
                                                        },
                                                        placeholder = { Text("Write notes, '/' for smart blocks, or '[[' to link...") },
                                                        textStyle = blockTextStyle,
                                                        minLines = if (idx == 0 && blocks.size == 1) 5 else 1,
                                                        colors = fieldColors,
                                                        shape = RoundedCornerShape(14.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("notion_block_input_$idx")
                                                    )

                                                    if (block.type == WorkspaceBlock.TYPE_CODE) {
                                                        IconButton(
                                                            onClick = {
                                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                                cm?.setPrimaryClip(ClipData.newPlainText("Code", block.text))
                                                                Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.padding(top = 6.dp).size(30.dp)
                                                        ) {
                                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy code", tint = palette.primaryInk, modifier = Modifier.size(14.dp))
                                                        }
                                                    }

                                                    if (blocks.size > 1) {
                                                        IconButton(
                                                            onClick = { blocks.removeAt(idx) },
                                                            modifier = Modifier.padding(top = 6.dp).size(30.dp)
                                                        ) {
                                                            MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 14.dp, contentDescription = "Remove block")
                                                        }
                                                    }
                                                }

                                                if (block.type == WorkspaceBlock.TYPE_TOGGLE) {
                                                    OutlinedTextField(
                                                        value = block.details,
                                                        onValueChange = { d -> blocks[idx] = block.copy(details = d) },
                                                        placeholder = { Text("Nested toggle details...") },
                                                        colors = fieldColors,
                                                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, bottom = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Outgoing [[Note Links]] & Incoming Backlinks Graph Card
                        val outgoingTitles = NoteBacklinksParser.extractLinkedTitlesFromBlocks(blocks.toList())
                        val resolvedOutgoing = NoteBacklinksParser.resolveLinks(
                            titles = outgoingTitles,
                            allNotes = allNotes,
                            excludeNoteId = existingNote?.id
                        )
                        val backlinks = existingNote?.let { current ->
                            NoteBacklinksParser.findBacklinksForNote(current, allNotes)
                        }.orEmpty()

                        if (resolvedOutgoing.isNotEmpty() || backlinks.isNotEmpty()) {
                            item(key = "notion_backlinks_graph", contentType = "backlinks_card") {
                                TactileGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notion_backlinks_card"),
                                    cornerRadius = 22.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (resolvedOutgoing.isNotEmpty()) {
                                            Text(
                                                text = "Outgoing Links (${resolvedOutgoing.size})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primaryInk
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                resolvedOutgoing.forEach { link ->
                                                    val target = link.targetNote
                                                    Surface(
                                                        onClick = { if (target != null) onOpenLinkedNote(target) },
                                                        enabled = target != null,
                                                        shape = RoundedCornerShape(9999.dp),
                                                        color = if (target != null) palette.accentSoftBg else palette.recessedTrayBg,
                                                        border = BorderStroke(1.dp, palette.specularRimOutline),
                                                        modifier = Modifier.testTag("outgoing_link_pill_${link.rawTitle}")
                                                    ) {
                                                        Text(
                                                            text = "[[${link.rawTitle}]]",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (target != null) palette.accentSoftInk else palette.secondaryInk,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (backlinks.isNotEmpty()) {
                                            Text(
                                                text = "Backlinks (${backlinks.size})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primaryInk
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                backlinks.forEach { sourceNote ->
                                                    Surface(
                                                        onClick = { onOpenLinkedNote(sourceNote) },
                                                        shape = RoundedCornerShape(9999.dp),
                                                        color = palette.frostedPillBg,
                                                        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.45f)),
                                                        modifier = Modifier.testTag("backlink_pill_${sourceNote.id}")
                                                    ) {
                                                        Text(
                                                            text = "<- [[${sourceNote.title}]]",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = CrimsonAccent,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // [ + Add New Page ] Button at Bottom of Notepad
                        item(key = "add_new_page_footer", contentType = "add_page_footer") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OutlinedButton(
                                    onClick = { addNewPage() },
                                    shape = RoundedCornerShape(9999.dp),
                                    border = BorderStroke(1.2.dp, CrimsonAccent.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("notion_add_new_page_button")
                                ) {
                                    MinimalIcon(type = MinimalIconType.PLUS, tint = CrimsonAccent, size = 18.dp)
                                    Text(
                                        text = "  + Add New Page",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryInk
                                    )
                                }
                            }
                        }
                    }

                    if (showSlashMenu) {
                        SmartCanvasSlashDrawer(
                            onSelectCommand = { cmd ->
                                val lastIdx = blocks.lastIndex
                                val meta = WorkspaceBlock.defaultMetadataForType(cmd.type)
                                if (lastIdx >= 0 && blocks[lastIdx].text.trim() == "/") {
                                    blocks[lastIdx] = blocks[lastIdx].copy(
                                        type = cmd.type,
                                        text = if (cmd.type == WorkspaceBlock.TYPE_DIVIDER) "---" else "",
                                        metadataJson = meta
                                    )
                                    showSlashMenu = false
                                } else {
                                    addBlock(
                                        type = cmd.type,
                                        defaultText = if (cmd.type == WorkspaceBlock.TYPE_DIVIDER) "---" else "",
                                        metadataJson = meta
                                    )
                                }
                            },
                            onDismiss = { showSlashMenu = false },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Docked Smart Canvas Block Toolbar above Keyboard / Nav Bar
                    Surface(
                        color = palette.dockBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { showSlashMenu = !showSlashMenu },
                                shape = RoundedCornerShape(9999.dp),
                                color = CrimsonAccent,
                                modifier = Modifier.testTag("block_tool_slash_drawer")
                            ) {
                                Text(
                                    text = "/ All Blocks",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            listOf(
                                WorkspaceBlock.TYPE_TABLE to "Table",
                                WorkspaceBlock.TYPE_INVOICE to "Invoice",
                                WorkspaceBlock.TYPE_EMAIL to "Gmail",
                                WorkspaceBlock.TYPE_SCHEDULE to "Task Slot",
                                WorkspaceBlock.TYPE_REMINDER to "Remind",
                                WorkspaceBlock.TYPE_SECRET to "Vault",
                                WorkspaceBlock.TYPE_CLIENT to "Client Due",
                                WorkspaceBlock.TYPE_VOICE to "Voice",
                                WorkspaceBlock.TYPE_TIMER to "Timer",
                                WorkspaceBlock.TYPE_SUBPAGE to "Sub-Page",
                                WorkspaceBlock.TYPE_TEXT to "Text",
                                WorkspaceBlock.TYPE_H1 to "H1",
                                WorkspaceBlock.TYPE_H2 to "H2",
                                WorkspaceBlock.TYPE_CHECKLIST to "Checklist",
                                WorkspaceBlock.TYPE_BULLET to "Bullet",
                                WorkspaceBlock.TYPE_CALLOUT to "Callout",
                                WorkspaceBlock.TYPE_CODE to "Code",
                                WorkspaceBlock.TYPE_DIVIDER to "Divider"
                            ).forEach { (blockType, label) ->
                                Surface(
                                    onClick = { addBlock(blockType) },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.testTag("block_tool_${blockType.lowercase()}")
                                ) {
                                    Text(
                                        text = "+ $label",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.primaryInk,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    linkTargetBlockIndex = blocks.lastIndex.coerceAtLeast(0)
                                    triggeredByDoubleBracket = false
                                    showNoteLinkPicker = true
                                },
                                shape = RoundedCornerShape(9999.dp),
                                color = palette.accentSoftBg,
                                border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.45f)),
                                modifier = Modifier.testTag("block_tool_link_note")
                            ) {
                                Text(
                                    text = "[[ Link Note ]]",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                            Surface(
                                onClick = { showItemPicker = true },
                                shape = RoundedCornerShape(9999.dp),
                                color = palette.accentSoftBg,
                                border = BorderStroke(1.dp, palette.specularRimOutline)
                            ) {
                                Text(
                                    text = "+ Attach Item",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentSoftInk,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAiPolishModal) {
        AiPolishModalDialog(
            onRunPolish = { mode ->
                val handler = onPolishPageWithAi
                if (handler != null) {
                    handler(title, blocks.toList(), mode)
                } else {
                    val summary = WorkspaceBlock.plainTextSummary(WorkspaceBlock.encodeList(blocks.toList()))
                    Result.success(com.example.data.ai.AiLibraryService.buildFallbackPolishedBlocks(title, summary, mode))
                }
            },
            onInsertBlocks = { generated, replace ->
                if (replace) blocks.clear()
                blocks.addAll(generated.map { it.copy(pageIndex = activePageIndex) })
            },
            onDismiss = { showAiPolishModal = false }
        )
    }

    if (showItemPicker) {
        AlertDialog(
            onDismissRequest = { showItemPicker = false },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Attach Saved Library Item", color = palette.primaryInk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (availableItems.isEmpty()) {
                        Text("No library items saved yet.", color = palette.secondaryInk)
                    } else {
                        availableItems.take(8).forEach { candidate ->
                            TextButton(
                                onClick = {
                                    linkedItemId = candidate.id
                                    addBlock(
                                        type = WorkspaceBlock.TYPE_ITEM_REF,
                                        defaultText = candidate.title,
                                        linkedId = candidate.id
                                    )
                                    showItemPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(candidate.title, color = palette.primaryInk, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showItemPicker = false }) { Text("Close", color = palette.secondaryInk) }
            }
        )
    }

    if (showNoteLinkPicker) {
        val linkableNotes = remember(allNotes, existingNote?.id) {
            allNotes.filter { it.deletedAt == null && it.id != existingNote?.id && it.title.isNotBlank() }
        }
        AlertDialog(
            onDismissRequest = { showNoteLinkPicker = false },
            containerColor = palette.secondaryPillBg,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Link Note [[...]]", color = palette.primaryInk, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (linkableNotes.isEmpty()) {
                        Text(
                            "Create another page first to link between notes using Obsidian [[Backlinks]].",
                            color = palette.secondaryInk,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        linkableNotes.take(12).forEach { target ->
                            Surface(
                                onClick = {
                                    val targetIdx = linkTargetBlockIndex.coerceIn(0, (blocks.size - 1).coerceAtLeast(0))
                                    if (blocks.isNotEmpty()) {
                                        val current = blocks[targetIdx]
                                        val token = "[[${target.title}]]"
                                        val nextText = if (triggeredByDoubleBracket && current.text.endsWith("[[")) {
                                            current.text.dropLast(2) + token
                                        } else if (current.text.isBlank()) {
                                            token
                                        } else {
                                            "${current.text.trimEnd()} $token"
                                        }
                                        blocks[targetIdx] = current.copy(text = nextText)
                                    } else {
                                        addBlock(WorkspaceBlock.TYPE_TEXT, "[[${target.title}]]")
                                    }
                                    triggeredByDoubleBracket = false
                                    showNoteLinkPicker = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = palette.cardSurfaceTop,
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_note_link_${target.id}")
                            ) {
                                Text(
                                    text = "[[${target.title}]]",
                                    color = palette.primaryInk,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        triggeredByDoubleBracket = false
                        showNoteLinkPicker = false
                    }
                ) { Text("Close", color = palette.secondaryInk) }
            }
        )
    }
}

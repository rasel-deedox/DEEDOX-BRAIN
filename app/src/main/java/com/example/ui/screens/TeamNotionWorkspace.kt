package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.AiPagePolishMode
import com.example.data.export.SmartCanvasPdfAndShare
import com.example.data.local.TeamMemberEntity
import com.example.data.local.TeamSharedDocEntity
import com.example.data.media.VoicePlaybackState
import com.example.data.repository.CanvasIconKeys
import com.example.data.repository.EmailBlockData
import com.example.data.repository.PageMetaBlockData
import com.example.data.repository.ReminderBlockData
import com.example.data.repository.ScheduleBlockData
import com.example.data.repository.SubpageBlockData
import com.example.data.repository.WorkspaceBlock
import com.example.ui.components.AiPolishModalDialog
import com.example.ui.components.CanvasPageIconBadge
import com.example.ui.components.CanvasPageIcons
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
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import kotlinx.coroutines.delay

private data class NestedDocNode(
    val doc: TeamSharedDocEntity,
    val depth: Int
)

private fun buildNestedPageTree(
    docs: List<TeamSharedDocEntity>,
    query: String
): List<NestedDocNode> {
    val cleanQuery = query.trim().lowercase()
    if (cleanQuery.isNotBlank()) {
        return docs.filter { d ->
            d.title.lowercase().contains(cleanQuery) ||
                d.content.lowercase().contains(cleanQuery) ||
                d.tagsCsv.lowercase().contains(cleanQuery)
        }.map { NestedDocNode(it, depth = 0) }
    }
    val docIds = docs.map { it.id }.toSet()
    val childrenMap = docs.groupBy { d ->
        d.parentId?.takeIf { it.isNotBlank() && it in docIds && it != d.id }
    }
    val result = mutableListOf<NestedDocNode>()
    val visited = mutableSetOf<String>()

    fun walk(parentId: String?, depth: Int) {
        val children = childrenMap[parentId].orEmpty()
            .sortedWith(compareByDescending<TeamSharedDocEntity> { it.isPinned }.thenByDescending { it.updatedAt })
        children.forEach { node ->
            if (visited.add(node.id)) {
                result.add(NestedDocNode(node, depth = depth.coerceAtMost(3)))
                walk(node.id, depth + 1)
            }
        }
    }
    walk(null, 0)
    docs.forEach { d ->
        if (visited.add(d.id)) {
            result.add(NestedDocNode(d, depth = 0))
        }
    }
    return result
}

fun LazyListScope.teamNotionPagesSection(
    docs: List<TeamSharedDocEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCreateRootPage: () -> Unit,
    onCreateSubPage: (parentDoc: TeamSharedDocEntity) -> Unit,
    onOpenPage: (TeamSharedDocEntity) -> Unit,
    onTogglePin: (TeamSharedDocEntity) -> Unit,
    onRequestDeletePage: (TeamSharedDocEntity) -> Unit
) {
    item(key = "team_pages_toolbar", contentType = "toolbar") {
        val palette = LiquidTheme.palette
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search team pages, tags & SOPs...") },
                singleLine = true,
                colors = teamFieldColors(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_pages_search_input")
            )
            TactilePrimaryPillButton(
                text = "+ New Smart Notion Page",
                iconType = MinimalIconType.PLUS,
                onClick = onCreateRootPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_add_doc_button")
            )
            if (docs.any { it.isPinned }) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pinned:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.secondaryInk
                    )
                    docs.filter { it.isPinned }.forEach { pinned ->
                        Surface(
                            onClick = { onOpenPage(pinned) },
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.accentSoftBg,
                            border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = CanvasPageIcons.forPageKey(pinned.icon),
                                    contentDescription = null,
                                    tint = CrimsonAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = pinned.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CrimsonAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val treeNodes = buildNestedPageTree(docs, searchQuery)
    if (treeNodes.isEmpty()) {
        item(key = "team_docs_empty", contentType = "empty") {
            val palette = LiquidTheme.palette
            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (searchQuery.isBlank()) "No shared Notion pages yet" else "No matching pages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    Text(
                        text = "Create Smart Pages with /table, /invoice, /email, /schedule, /remind, /secret, /client, and nested /page workspaces.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                }
            }
        }
    } else {
        items(items = treeNodes, key = { it.doc.id }, contentType = { "team_doc" }) { node ->
            TeamNotionPageCard(
                doc = node.doc,
                depth = node.depth,
                onOpenPage = { onOpenPage(node.doc) },
                onCreateSubPage = { onCreateSubPage(node.doc) },
                onTogglePin = { onTogglePin(node.doc) },
                onDeletePage = { onRequestDeletePage(node.doc) }
            )
        }
    }
}

@Composable
private fun TeamNotionPageCard(
    doc: TeamSharedDocEntity,
    depth: Int,
    onOpenPage: () -> Unit,
    onCreateSubPage: () -> Unit,
    onTogglePin: () -> Unit,
    onDeletePage: () -> Unit
) {
    val context = LocalContext.current
    val palette = LiquidTheme.palette
    val decodedBlocks = remember(doc.blocksJson) {
        WorkspaceBlock.parseList(doc.blocksJson).filterNot { it.type == WorkspaceBlock.TYPE_PAGE_META }
    }
    val tags = remember(doc.tagsCsv) { doc.tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() } }
    val toggleStates = remember(doc.id) { mutableStateMapOf<String, Boolean>() }

    TactileGlassCard(
        onClick = onOpenPage,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 18).dp)
            .testTag("team_page_card_${doc.id}"),
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (depth > 0) {
                        Icon(
                            imageVector = Icons.Outlined.SubdirectoryArrowRight,
                            contentDescription = "Nested sub-page",
                            tint = CrimsonAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    CanvasPageIconBadge(iconKey = doc.icon, size = 32.dp, iconSize = 17.dp, active = doc.isPinned)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "By ${doc.authorName.ifBlank { "Member" }} • Edited by ${doc.lastEditedBy.ifBlank { doc.authorName }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onTogglePin,
                        shape = RoundedCornerShape(9999.dp),
                        color = if (doc.isPinned) CrimsonAccent.copy(alpha = 0.16f) else palette.recessedTrayBg,
                        modifier = Modifier.testTag("pin_page_${doc.id}")
                    ) {
                        Text(
                            text = if (doc.isPinned) "Pinned" else "Pin",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (doc.isPinned) CrimsonAccent else palette.secondaryInk,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onOpenPage) {
                        MinimalIcon(type = MinimalIconType.EDIT, tint = palette.primaryInk, size = 16.dp, contentDescription = "Co-edit page")
                    }
                    IconButton(onClick = onDeletePage) {
                        MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 16.dp, contentDescription = "Delete page")
                    }
                }
            }

            if (decodedBlocks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    decodedBlocks.take(5).forEach { block ->
                        when (block.type) {
                            WorkspaceBlock.TYPE_DIVIDER -> {
                                HorizontalDivider(color = palette.specularRimOutline, thickness = 1.dp)
                            }
                            WorkspaceBlock.TYPE_TOGGLE -> {
                                val expanded = toggleStates[block.id] ?: block.isExpanded
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.recessedTrayBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { toggleStates[block.id] = !expanded }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "${if (expanded) "v" else ">"} ${block.text}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.primaryInk
                                        )
                                        if (expanded && block.details.isNotBlank()) {
                                            Text(
                                                text = block.details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.secondaryInk
                                            )
                                        }
                                    }
                                }
                            }
                            WorkspaceBlock.TYPE_CALLOUT -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.accentSoftBg,
                                    border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Info, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(15.dp))
                                        Text(
                                            text = block.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.primaryInk
                                        )
                                    }
                                }
                            }
                            WorkspaceBlock.TYPE_CODE -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.recessedTrayBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = block.text,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = palette.primaryInk,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            WorkspaceBlock.TYPE_DRIVE_EMBED, WorkspaceBlock.TYPE_EMBED -> {
                                val targetUrl = block.details.ifBlank { block.text }
                                Surface(
                                    onClick = {
                                        if (targetUrl.startsWith("http")) {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))) }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.cardSurfaceTop,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MinimalIcon(type = MinimalIconType.OPEN_EXTERNAL, tint = CrimsonAccent, size = 15.dp)
                                        Text(
                                            text = block.text.ifBlank { "External Resource" },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CrimsonAccent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            in WorkspaceBlock.RICH_CARD_TYPES -> {
                                val summary = WorkspaceBlock.plainTextSummary(WorkspaceBlock.encodeList(listOf(block)))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.recessedTrayBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = CanvasPageIcons.forBlockType(block.type),
                                            contentDescription = null,
                                            tint = CrimsonAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = summary.ifBlank { block.type },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.primaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            else -> {
                                val prefix = when (block.type) {
                                    WorkspaceBlock.TYPE_CHECKLIST -> if (block.checked) "[x] " else "[ ] "
                                    WorkspaceBlock.TYPE_BULLET -> "• "
                                    else -> ""
                                }
                                Text(
                                    text = "$prefix${block.text}",
                                    style = when (block.type) {
                                        WorkspaceBlock.TYPE_H1 -> MaterialTheme.typography.titleMedium
                                        WorkspaceBlock.TYPE_H2 -> MaterialTheme.typography.titleSmall
                                        WorkspaceBlock.TYPE_H3 -> MaterialTheme.typography.labelLarge
                                        else -> MaterialTheme.typography.bodySmall
                                    },
                                    fontWeight = if (block.type in setOf(WorkspaceBlock.TYPE_H1, WorkspaceBlock.TYPE_H2, WorkspaceBlock.TYPE_H3)) {
                                        FontWeight.Bold
                                    } else FontWeight.Normal,
                                    textDecoration = if (block.type == WorkspaceBlock.TYPE_CHECKLIST && block.checked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = palette.primaryInk
                                )
                            }
                        }
                    }
                }
            } else if (doc.content.isNotBlank()) {
                Text(doc.content, style = MaterialTheme.typography.bodyMedium, color = palette.primaryInk)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(9999.dp), color = palette.recessedTrayBg) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!doc.resourceUrl.isNullOrBlank()) {
                        Surface(
                            onClick = {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(doc.resourceUrl))) }
                            },
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.accentSoftBg,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MinimalIcon(type = MinimalIconType.OPEN_EXTERNAL, tint = CrimsonAccent, size = 12.dp)
                                Text("Drive / Link", style = MaterialTheme.typography.labelSmall, color = CrimsonAccent)
                            }
                        }
                    }
                }
                Surface(
                    onClick = onCreateSubPage,
                    shape = RoundedCornerShape(9999.dp),
                    color = palette.frostedPillBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier.testTag("add_subpage_${doc.id}")
                ) {
                    Text(
                        text = "+ Sub-page",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SharedNotionPageDialog(
    existingDoc: TeamSharedDocEntity?,
    initialParentId: String?,
    allDocs: List<TeamSharedDocEntity>,
    members: List<TeamMemberEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        content: String,
        url: String?,
        blocksJson: String,
        tagsCsv: String,
        parentId: String?,
        icon: String,
        isPinned: Boolean
    ) -> Unit,
    playbackState: VoicePlaybackState = VoicePlaybackState(),
    onOpenNestedDoc: (TeamSharedDocEntity) -> Unit = {},
    onCreateNestedSubpage: ((title: String, parentId: String?) -> Unit)? = null,
    onPolishPageWithAi: (suspend (String, List<WorkspaceBlock>, AiPagePolishMode) -> Result<List<WorkspaceBlock>>)? = null,
    onLogClientPaymentToLedger: (clientName: String, projectName: String, amountCents: Long) -> Unit = { _, _, _ -> },
    onScheduleInlineReminder: (ReminderBlockData) -> Unit = {},
    onSyncScheduleTask: (ScheduleBlockData) -> Unit = {},
    onLogFocusTimerSeconds: (String, Long) -> Unit = { _, _ -> },
    onStartVoiceRecord: () -> Unit = {},
    onStopVoiceRecord: () -> Pair<String, Long>? = { null },
    onToggleVoicePlayback: (String) -> Unit = {},
    onSeekVoicePlayback: (String, Long) -> Unit = { _, _ -> },
    initialTitle: String = ""
) {
    val context = LocalContext.current
    val palette = LiquidTheme.palette
    var title by remember(existingDoc?.id, initialParentId, initialTitle) {
        mutableStateOf(existingDoc?.title ?: initialTitle)
    }
    var iconKey by remember(existingDoc?.id, initialParentId, initialTitle) {
        mutableStateOf(CanvasIconKeys.normalize(existingDoc?.icon))
    }
    var isPinned by remember(existingDoc?.id, initialParentId, initialTitle) {
        mutableStateOf(existingDoc?.isPinned ?: false)
    }
    var parentId by remember(existingDoc?.id, initialParentId, initialTitle) {
        mutableStateOf(existingDoc?.parentId ?: initialParentId)
    }
    var tagsCsv by remember(existingDoc?.id, initialParentId, initialTitle) {
        mutableStateOf(existingDoc?.tagsCsv.orEmpty())
    }
    var url by remember(existingDoc?.id, initialParentId, initialTitle) {
        mutableStateOf(existingDoc?.resourceUrl.orEmpty())
    }
    var showSlashPalette by remember { mutableStateOf(false) }
    var showMetaDrawer by remember { mutableStateOf(false) }
    var showTemplates by remember(existingDoc?.id, initialParentId, initialTitle) { mutableStateOf(existingDoc == null) }
    var showAiPolishModal by remember { mutableStateOf(false) }
    var activeBlockIdx by remember { mutableIntStateOf(0) }

    val parsedAll = remember(existingDoc?.id, initialParentId, initialTitle) {
        existingDoc?.blocksJson?.let { WorkspaceBlock.parseList(it) }.orEmpty()
    }
    val initialMeta = remember(existingDoc?.id, initialParentId, initialTitle) {
        parsedAll.firstOrNull { it.type == WorkspaceBlock.TYPE_PAGE_META }
            ?.let { PageMetaBlockData.fromJson(it.metadataJson) }
            ?: PageMetaBlockData(iconKey = CanvasIconKeys.normalize(existingDoc?.icon))
    }
    var pageMeta by remember(existingDoc?.id, initialParentId, initialTitle) { mutableStateOf(initialMeta) }

    val initialBlocks = remember(existingDoc?.id, initialParentId, initialTitle) {
        val decoded = parsedAll.filterNot { it.type == WorkspaceBlock.TYPE_PAGE_META }
        if (decoded.isNotEmpty()) decoded
        else if (!existingDoc?.content.isNullOrBlank()) listOf(WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = existingDoc!!.content))
        else listOf(
            WorkspaceBlock(type = WorkspaceBlock.TYPE_H2, text = initialTitle.ifBlank { "Overview" }),
            WorkspaceBlock(type = WorkspaceBlock.TYPE_CHECKLIST, text = "Define sprint deliverables", checked = false)
        )
    }
    val blocks = remember(existingDoc?.id, initialParentId, initialTitle) {
        mutableStateListOf<WorkspaceBlock>().apply { addAll(initialBlocks) }
    }
    val parentChoices = remember(allDocs, existingDoc) {
        allDocs.filter { it.id != existingDoc?.id }
    }

    var headerTimerRunning by remember(existingDoc?.id) { mutableStateOf(false) }
    var headerTimerSeconds by remember(existingDoc?.id) { mutableLongStateOf(initialMeta.focusSecondsSpent) }
    LaunchedEffect(headerTimerRunning) {
        while (headerTimerRunning) {
            delay(1000L)
            headerTimerSeconds += 1L
        }
    }

    fun addSmartBlock(type: String) {
        val meta = WorkspaceBlock.defaultMetadataForType(type)
        val newBlock = WorkspaceBlock(
            type = type,
            text = if (type == WorkspaceBlock.TYPE_DIVIDER) "---" else "",
            metadataJson = meta
        )
        val idx = activeBlockIdx.coerceIn(0, (blocks.size - 1).coerceAtLeast(0))
        if (blocks.isNotEmpty() && blocks[idx].text.trim().startsWith("/")) {
            blocks[idx] = newBlock.copy(id = blocks[idx].id)
        } else {
            blocks.add(newBlock)
            activeBlockIdx = blocks.lastIndex
        }
        showSlashPalette = false
    }

    val canvasCallbacks = remember(existingDoc?.id, allDocs, title) {
        SmartCanvasCallbacks(
            onOpenSubpage = { sub ->
                val target = allDocs.firstOrNull {
                    it.id == sub.subpageId || (sub.title.isNotBlank() && it.title.equals(sub.title, ignoreCase = true))
                }
                if (target != null) {
                    onOpenNestedDoc(target)
                } else {
                    onCreateNestedSubpage?.invoke(sub.title.ifBlank { "Team Sub-Page" }, existingDoc?.id ?: parentId)
                }
            },
            onExportInvoicePdf = { inv ->
                val file = SmartCanvasPdfAndShare.generateStandaloneInvoicePdfFile(context, inv)
                SmartCanvasPdfAndShare.sharePdfFile(context, file, "Invoice ${inv.docNumber}")
            },
            onEmailInvoice = { inv ->
                SmartCanvasPdfAndShare.launchEmailComposer(context, inv.clientEmail, inv.toEmailSubject(), inv.toEmailBody())
            },
            onSendDirectEmail = { mail: EmailBlockData ->
                SmartCanvasPdfAndShare.launchEmailComposer(
                    context = context,
                    toEmail = mail.toEmail,
                    subject = mail.subject.ifBlank { title.ifBlank { "Team Update" } },
                    body = mail.body
                )
            },
            onScheduleInlineReminder = { rem ->
                onScheduleInlineReminder(rem)
                Toast.makeText(context, "Reminder & team ping scheduled", Toast.LENGTH_SHORT).show()
            },
            onSyncScheduleTask = { sched ->
                onSyncScheduleTask(sched)
                Toast.makeText(context, "Synced to Team Schedule", Toast.LENGTH_SHORT).show()
            },
            onLogFocusTimerSeconds = onLogFocusTimerSeconds,
            onStartVoiceRecord = onStartVoiceRecord,
            onStopVoiceRecord = onStopVoiceRecord,
            onToggleVoicePlayback = onToggleVoicePlayback,
            onSeekVoicePlayback = onSeekVoicePlayback,
            onLogClientPaymentToLedger = { cName, pName, cents ->
                onLogClientPaymentToLedger(cName, pName, cents)
                Toast.makeText(context, "Payment logged to Money Ledger", Toast.LENGTH_SHORT).show()
            },
            onBuildPageEmailSummary = {
                WorkspaceBlock.plainTextSummary(WorkspaceBlock.encodeList(blocks.toList()))
            }
        )
    }

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
                    .testTag("team_notion_canvas_sheet"),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxSize()
                ) {
                    // Full-Sheet Liquid Glass Header
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
                                        contentDescription = "Close team page",
                                        onClick = onDismiss,
                                        size = 38.dp
                                    )
                                    CanvasPageIconBadge(iconKey = iconKey, size = 32.dp, iconSize = 17.dp, active = pageMeta.coverEnabled)
                                    Column {
                                        Text(
                                            text = if (existingDoc == null) "New Team Page" else "Team Smart Canvas",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primaryInk
                                        )
                                        Text(
                                            text = "${members.size} collaborators • Live Firestore",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryInk
                                        )
                                    }
                                }

                                TactilePrimaryPillButton(
                                    text = if (existingDoc == null) "Publish" else "Save",
                                    enabled = title.isNotBlank(),
                                    onClick = {
                                        val cleanedBlocks = blocks.filter {
                                            it.text.isNotBlank() || it.details.isNotBlank() || it.metadataJson.isNotBlank() || it.type == WorkspaceBlock.TYPE_DIVIDER
                                        }.ifEmpty { listOf(WorkspaceBlock(type = WorkspaceBlock.TYPE_TEXT, text = title.trim())) }
                                        val updatedMeta = pageMeta.copy(iconKey = iconKey, focusSecondsSpent = headerTimerSeconds)
                                        val encodedAll = listOf(
                                            WorkspaceBlock(
                                                type = WorkspaceBlock.TYPE_PAGE_META,
                                                text = iconKey,
                                                metadataJson = updatedMeta.toJson()
                                            )
                                        ) + cleanedBlocks
                                        val plainSummary = WorkspaceBlock.plainTextSummary(WorkspaceBlock.encodeList(cleanedBlocks))
                                        onSave(
                                            title.trim(),
                                            plainSummary,
                                            url.trim().takeIf { it.isNotBlank() },
                                            WorkspaceBlock.encodeList(encodedAll),
                                            tagsCsv.trim(),
                                            parentId,
                                            iconKey,
                                            isPinned
                                        )
                                    },
                                    modifier = Modifier.testTag("team_confirm_doc_button")
                                )
                            }

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
                                    modifier = Modifier.testTag("team_ai_polish_button")
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
                                            onLogFocusTimerSeconds(title.ifBlank { "Team Canvas" }, headerTimerSeconds)
                                        }
                                        headerTimerRunning = !headerTimerRunning
                                    }
                                )

                                Surface(
                                    onClick = {
                                        val file = SmartCanvasPdfAndShare.generatePagePdfFile(
                                            context = context,
                                            title = title.ifBlank { "Team Page" },
                                            subtitle = "Team Collaboration Document",
                                            blocks = blocks.toList()
                                        )
                                        SmartCanvasPdfAndShare.sharePdfFile(context, file, title.ifBlank { "Team Page" })
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Export PDF", tint = palette.primaryInk, modifier = Modifier.size(14.dp))
                                        Text("PDF", style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
                                    }
                                }

                                Surface(
                                    onClick = {
                                        SmartCanvasPdfAndShare.generateAndCopyShareLink(
                                            context = context,
                                            pageId = existingDoc?.id ?: "team_draft",
                                            title = title.ifBlank { "Team Page" },
                                            blocks = blocks.toList()
                                        )
                                        Toast.makeText(context, "Read-only share link copied", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = "Share link", tint = palette.primaryInk, modifier = Modifier.size(14.dp))
                                        Text("Share", style = MaterialTheme.typography.labelSmall, color = palette.primaryInk)
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

                                Surface(
                                    onClick = { isPinned = !isPinned },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (isPinned) CrimsonAccent else palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                ) {
                                    Text(
                                        text = if (isPinned) "Pinned" else "Pin",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPinned) Color.White else palette.primaryInk,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    onClick = { showMetaDrawer = !showMetaDrawer },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (showMetaDrawer) palette.accentSoftBg else palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                ) {
                                    Text(
                                        text = "Tags & Nest",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primaryInk,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Spacious Vertical Canvas
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pageMeta.coverEnabled) {
                            item(key = "team_cover_banner") {
                                SmartCanvasCoverBanner(
                                    coverVariant = pageMeta.coverVariant,
                                    pageIconKey = iconKey,
                                    onSelectVariant = { v -> pageMeta = pageMeta.copy(coverVariant = v) },
                                    onSelectIconKey = { k ->
                                        iconKey = k
                                        pageMeta = pageMeta.copy(iconKey = k)
                                    },
                                    onHideCover = { pageMeta = pageMeta.copy(coverEnabled = false) }
                                )
                            }
                        }

                        if (showMetaDrawer) {
                            item(key = "team_meta_drawer") {
                                TactileGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (parentChoices.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Nest under:", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                                Surface(
                                                    onClick = { parentId = null },
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = if (parentId == null) palette.primaryButtonBg else palette.cardSurfaceTop,
                                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                                ) {
                                                    Text(
                                                        text = "Root",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (parentId == null) palette.primaryButtonContent else palette.primaryInk,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                                    )
                                                }
                                                parentChoices.take(8).forEach { candidate ->
                                                    val selected = parentId == candidate.id
                                                    Surface(
                                                        onClick = { parentId = candidate.id },
                                                        shape = RoundedCornerShape(9999.dp),
                                                        color = if (selected) palette.primaryButtonBg else palette.cardSurfaceTop,
                                                        border = BorderStroke(1.dp, palette.specularRimOutline)
                                                    ) {
                                                        Text(
                                                            text = candidate.title.take(18),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = tagsCsv,
                                            onValueChange = { tagsCsv = it },
                                            label = { Text("Tags (comma-separated, e.g. api, sop)") },
                                            singleLine = true,
                                            colors = teamFieldColors(),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = url,
                                            onValueChange = { url = it },
                                            label = { Text("Primary Drive / Reference URL (optional)") },
                                            singleLine = true,
                                            colors = teamFieldColors(),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        if (showTemplates) {
                            item(key = "team_starter_templates") {
                                StarterTemplatesStrip(
                                    onApplyTemplate = { tpl ->
                                        if (tpl.defaultTitle.isNotBlank()) title = tpl.defaultTitle
                                        if (tpl.tagsCsv.isNotBlank()) tagsCsv = tpl.tagsCsv
                                        iconKey = tpl.iconKey
                                        pageMeta = pageMeta.copy(iconKey = tpl.iconKey)
                                        blocks.clear()
                                        blocks.addAll(tpl.buildBlocks())
                                        showTemplates = false
                                    }
                                )
                            }
                        }

                        // Title Header
                        item(key = "team_doc_title_block") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (title.isBlank()) {
                                        Text(
                                            text = "Team Page Title...",
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
                                            .testTag("team_doc_title_input")
                                    )
                                }

                                if (members.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Mention:", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
                                        members.forEach { m ->
                                            Surface(
                                                onClick = {
                                                    if (blocks.isNotEmpty()) {
                                                        val idx = activeBlockIdx.coerceIn(0, blocks.lastIndex)
                                                        val cur = blocks[idx]
                                                        val spacer = if (cur.text.isBlank() || cur.text.endsWith(" ")) "" else " "
                                                        blocks[idx] = cur.copy(text = "${cur.text}$spacer@${m.displayName} ")
                                                    }
                                                },
                                                shape = RoundedCornerShape(9999.dp),
                                                color = palette.accentSoftBg
                                            ) {
                                                Text(
                                                    text = "@${m.displayName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CrimsonAccent,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = palette.specularRimOutline)
                            }
                        }

                        // Blocks
                        itemsIndexed(
                            items = blocks,
                            key = { _, b -> b.id },
                            contentType = { _, b -> b.type }
                        ) { index, block ->
                            if (block.type in WorkspaceBlock.RICH_CARD_TYPES) {
                                SmartCanvasRichBlockCard(
                                    block = block,
                                    index = index,
                                    teamMembers = members,
                                    playbackState = playbackState,
                                    callbacks = canvasCallbacks,
                                    onUpdateBlock = { updated -> blocks[index] = updated },
                                    onDeleteBlock = if (blocks.size > 1) { { blocks.removeAt(index) } } else null
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
                                            if (blocks.size > 1) {
                                                IconButton(onClick = { blocks.removeAt(index) }, modifier = Modifier.size(30.dp)) {
                                                    MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 14.dp, contentDescription = "Remove divider")
                                                }
                                            }
                                        }
                                    } else {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                when (block.type) {
                                                    WorkspaceBlock.TYPE_CHECKLIST -> {
                                                        Checkbox(
                                                            checked = block.checked,
                                                            onCheckedChange = { checked -> blocks[index] = block.copy(checked = checked) },
                                                            colors = CheckboxDefaults.colors(
                                                                checkedColor = CrimsonAccent,
                                                                uncheckedColor = palette.secondaryInk,
                                                                checkmarkColor = Color.White
                                                            )
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
                                                    else -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(9.dp),
                                                            color = palette.recessedTrayBg,
                                                            modifier = Modifier.padding(top = 10.dp)
                                                        ) {
                                                            Text(
                                                                text = when (block.type) {
                                                                    WorkspaceBlock.TYPE_H1 -> "H1"
                                                                    WorkspaceBlock.TYPE_H2 -> "H2"
                                                                    WorkspaceBlock.TYPE_H3 -> "H3"
                                                                    WorkspaceBlock.TYPE_TOGGLE -> "TGL"
                                                                    WorkspaceBlock.TYPE_CALLOUT -> "NTE"
                                                                    WorkspaceBlock.TYPE_CODE -> "</>"
                                                                    else -> "TXT"
                                                                },
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = CrimsonAccent,
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                OutlinedTextField(
                                                    value = block.text,
                                                    onValueChange = { next ->
                                                        activeBlockIdx = index
                                                        blocks[index] = block.copy(text = next)
                                                        if (next == "/") showSlashPalette = true
                                                    },
                                                    placeholder = { Text("Write or type '/' for smart blocks...") },
                                                    textStyle = when (block.type) {
                                                        WorkspaceBlock.TYPE_H1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                                        WorkspaceBlock.TYPE_H2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                                        WorkspaceBlock.TYPE_CODE -> MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                                        else -> MaterialTheme.typography.bodyLarge
                                                    },
                                                    colors = teamFieldColors(),
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier.weight(1f)
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
                                                        onClick = { blocks.removeAt(index) },
                                                        modifier = Modifier.padding(top = 6.dp).size(30.dp)
                                                    ) {
                                                        MinimalIcon(type = MinimalIconType.TRASH, tint = CrimsonAccent, size = 15.dp, contentDescription = "Remove block")
                                                    }
                                                }
                                            }
                                            if (block.type == WorkspaceBlock.TYPE_TOGGLE) {
                                                OutlinedTextField(
                                                    value = block.details,
                                                    onValueChange = { d -> blocks[index] = block.copy(details = d) },
                                                    label = { Text("Toggle nested details") },
                                                    colors = teamFieldColors(),
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 18.dp, end = 12.dp, bottom = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showSlashPalette) {
                        SmartCanvasSlashDrawer(
                            onSelectCommand = { cmd -> addSmartBlock(cmd.type) },
                            onDismiss = { showSlashPalette = false },
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("team_slash_command_palette")
                        )
                    }

                    // Docked Smart Block Bar
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
                                onClick = { showSlashPalette = !showSlashPalette },
                                shape = RoundedCornerShape(9999.dp),
                                color = CrimsonAccent,
                                modifier = Modifier.testTag("team_slash_palette_toggle")
                            ) {
                                Text(
                                    text = if (showSlashPalette) "Hide / Menu" else "/ Smart Blocks",
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
                                WorkspaceBlock.TYPE_CHECKLIST to "Checklist",
                                WorkspaceBlock.TYPE_CODE to "Code"
                            ).forEach { (bType, lbl) ->
                                Surface(
                                    onClick = { addSmartBlock(bType) },
                                    shape = RoundedCornerShape(9999.dp),
                                    color = palette.frostedPillBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline)
                                ) {
                                    Text(
                                        text = "+ $lbl",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.primaryInk,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                    )
                                }
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
                blocks.addAll(generated)
            },
            onDismiss = { showAiPolishModal = false }
        )
    }
}

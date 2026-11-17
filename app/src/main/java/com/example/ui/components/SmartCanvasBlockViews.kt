package com.example.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.TeamMemberEntity
import com.example.data.media.VoicePlaybackState
import com.example.data.repository.ClientTrackerBlockData
import com.example.data.repository.EmailBlockData
import com.example.data.repository.EmbedBlockData
import com.example.data.repository.InvoiceBlockData
import com.example.data.repository.InvoiceLineItem
import com.example.data.repository.ReminderBlockData
import com.example.data.repository.ScheduleBlockData
import com.example.data.repository.SecretBlockData
import com.example.data.repository.SubpageBlockData
import com.example.data.repository.TableBlockData
import com.example.data.repository.TimerBlockData
import com.example.data.repository.VoiceBlockData
import com.example.data.repository.WorkspaceBlock
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import kotlinx.coroutines.delay

data class SmartCanvasCallbacks(
    val onOpenSubpage: (SubpageBlockData) -> Unit = {},
    val onExportInvoicePdf: (InvoiceBlockData) -> Unit = {},
    val onEmailInvoice: (InvoiceBlockData) -> Unit = {},
    val onSendDirectEmail: (EmailBlockData) -> Unit = {},
    val onScheduleInlineReminder: (ReminderBlockData) -> Unit = {},
    val onSyncScheduleTask: (ScheduleBlockData) -> Unit = {},
    val onLogFocusTimerSeconds: (label: String, seconds: Long) -> Unit = { _, _ -> },
    val onStartVoiceRecord: () -> Unit = {},
    val onStopVoiceRecord: () -> Pair<String, Long>? = { null },
    val onToggleVoicePlayback: (String) -> Unit = {},
    val onSeekVoicePlayback: (String, Long) -> Unit = { _, _ -> },
    val onLogClientPaymentToLedger: (clientName: String, projectName: String, amountCents: Long) -> Unit = { _, _, _ -> },
    val onBuildPageEmailSummary: () -> String = { "" }
)

@Composable
fun SmartCanvasRichBlockCard(
    block: WorkspaceBlock,
    index: Int,
    teamMembers: List<TeamMemberEntity>,
    playbackState: VoicePlaybackState,
    callbacks: SmartCanvasCallbacks,
    onUpdateBlock: (WorkspaceBlock) -> Unit,
    onDeleteBlock: (() -> Unit)?
) {
    val palette = LiquidTheme.palette
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.cardSurfaceTop.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, palette.specularRimOutline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rich_block_${block.type.lowercase()}_$index")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RichBlockHeaderRow(
                blockType = block.type,
                label = richBlockDisplayTitle(block.type),
                onDeleteBlock = onDeleteBlock
            )

            when (block.type) {
                WorkspaceBlock.TYPE_SUBPAGE -> {
                    val data = remember(block.id, block.metadataJson, block.text) {
                        SubpageBlockData.fromJson(block.metadataJson, block.text)
                    }
                    OutlinedTextField(
                        value = data.title,
                        onValueChange = { nextTitle ->
                            val updated = data.copy(title = nextTitle)
                            onUpdateBlock(block.copy(text = nextTitle, metadataJson = updated.toJson()))
                        },
                        label = { Text("Sub-Page Title") },
                        singleLine = true,
                        colors = canvasInputColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk,
                            modifier = Modifier.weight(1f)
                        )
                        TactilePrimaryPillButton(
                            text = "Open Page",
                            onClick = { callbacks.onOpenSubpage(data) },
                            modifier = Modifier.testTag("open_subpage_btn_$index")
                        )
                    }
                }

                WorkspaceBlock.TYPE_TABLE -> {
                    SmartTableBlockContent(
                        block = block,
                        onUpdateBlock = onUpdateBlock
                    )
                }

                WorkspaceBlock.TYPE_INVOICE -> {
                    InvoiceBlockContent(
                        block = block,
                        callbacks = callbacks,
                        onUpdateBlock = onUpdateBlock
                    )
                }

                WorkspaceBlock.TYPE_EMAIL -> {
                    val data = remember(block.id, block.metadataJson) {
                        EmailBlockData.fromJson(block.metadataJson)
                    }
                    OutlinedTextField(
                        value = data.toEmail,
                        onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = data.copy(toEmail = v).toJson())) },
                        label = { Text("To (Client or Team Gmail)") },
                        singleLine = true,
                        colors = canvasInputColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = data.subject,
                        onValueChange = { v -> onUpdateBlock(block.copy(text = v, metadataJson = data.copy(subject = v).toJson())) },
                        label = { Text("Email Subject") },
                        singleLine = true,
                        colors = canvasInputColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = data.body,
                        onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = data.copy(body = v).toJson())) },
                        label = { Text("Email Body") },
                        minLines = 3,
                        colors = canvasInputColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val summary = callbacks.onBuildPageEmailSummary()
                                if (summary.isNotBlank()) {
                                    onUpdateBlock(block.copy(metadataJson = data.copy(body = summary).toJson()))
                                }
                            },
                            shape = RoundedCornerShape(9999.dp),
                            border = BorderStroke(1.dp, palette.specularRimOutline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pull Page Notes", color = palette.primaryInk, maxLines = 1)
                        }
                        TactilePrimaryPillButton(
                            text = "Send Gmail",
                            onClick = { callbacks.onSendDirectEmail(data) },
                            modifier = Modifier.weight(1f).testTag("send_email_block_btn_$index")
                        )
                    }
                }

                WorkspaceBlock.TYPE_REMINDER -> {
                    InlineReminderBlockContent(
                        block = block,
                        teamMembers = teamMembers,
                        onScheduleReminder = callbacks.onScheduleInlineReminder,
                        onUpdateBlock = onUpdateBlock
                    )
                }

                WorkspaceBlock.TYPE_SCHEDULE -> {
                    ScheduleTaskBlockContent(
                        block = block,
                        teamMembers = teamMembers,
                        onSyncScheduleTask = callbacks.onSyncScheduleTask,
                        onUpdateBlock = onUpdateBlock
                    )
                }

                WorkspaceBlock.TYPE_TIMER -> {
                    FocusTimerBlockContent(
                        block = block,
                        onLogTimerSeconds = callbacks.onLogFocusTimerSeconds,
                        onUpdateBlock = onUpdateBlock
                    )
                }

                WorkspaceBlock.TYPE_VOICE -> {
                    VoiceCommentBlockContent(
                        block = block,
                        playbackState = playbackState,
                        callbacks = callbacks,
                        onUpdateBlock = onUpdateBlock
                    )
                }

                WorkspaceBlock.TYPE_SECRET -> {
                    SecretVaultBlockContent(
                        block = block,
                        index = index,
                        context = context,
                        onUpdateBlock = onUpdateBlock
                    )
                }

                WorkspaceBlock.TYPE_CLIENT -> {
                    ClientTrackerBlockContent(
                        block = block,
                        onLogPaymentToLedger = callbacks.onLogClientPaymentToLedger,
                        onUpdateBlock = onUpdateBlock
                    )
                }

                WorkspaceBlock.TYPE_EMBED, WorkspaceBlock.TYPE_DRIVE_EMBED -> {
                    val defaultKind = if (block.type == WorkspaceBlock.TYPE_DRIVE_EMBED) {
                        EmbedBlockData.KIND_DRIVE
                    } else {
                        EmbedBlockData.KIND_WEBSITE
                    }
                    val data = remember(block.id, block.metadataJson, block.text, block.details) {
                        val parsed = EmbedBlockData.fromJson(block.metadataJson, defaultKind)
                        if (block.metadataJson.isBlank()) {
                            parsed.copy(title = block.text, url = block.details)
                        } else parsed
                    }
                    OutlinedTextField(
                        value = data.title,
                        onValueChange = { v ->
                            val next = data.copy(title = v)
                            onUpdateBlock(block.copy(text = v, metadataJson = next.toJson()))
                        },
                        label = { Text(if (defaultKind == EmbedBlockData.KIND_DRIVE) "Drive File / Video Title" else "Website / Resource Title") },
                        singleLine = true,
                        colors = canvasInputColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = data.url,
                        onValueChange = { v ->
                            val next = data.copy(url = v)
                            onUpdateBlock(block.copy(details = v, metadataJson = next.toJson()))
                        },
                        label = { Text("https:// URL") },
                        singleLine = true,
                        colors = canvasInputColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val targetUrl = data.url.trim()
                    if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                        Surface(
                            onClick = {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))) }
                            },
                            shape = RoundedCornerShape(9999.dp),
                            color = palette.accentSoftBg,
                            border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(15.dp))
                                Text("Open Resource", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CrimsonAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RichBlockHeaderRow(
    blockType: String,
    label: String,
    onDeleteBlock: (() -> Unit)?
) {
    val palette = LiquidTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = palette.accentSoftBg,
                border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.35f))
            ) {
                Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    MonochromeVectorIcon(
                        imageVector = CanvasPageIcons.forBlockType(blockType),
                        contentDescription = null,
                        tint = CrimsonAccent,
                        size = 15.dp
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
        }
        if (onDeleteBlock != null) {
            IconButton(onClick = onDeleteBlock, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove block", tint = CrimsonAccent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SmartTableBlockContent(
    block: WorkspaceBlock,
    onUpdateBlock: (WorkspaceBlock) -> Unit
) {
    val palette = LiquidTheme.palette
    val table = remember(block.id, block.metadataJson) { TableBlockData.fromJson(block.metadataJson) }

    OutlinedTextField(
        value = table.title,
        onValueChange = { nextTitle ->
            val updated = table.copy(title = nextTitle)
            onUpdateBlock(block.copy(text = nextTitle, metadataJson = updated.toJson()))
        },
        label = { Text("Table Name") },
        singleLine = true,
        colors = canvasInputColors(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header Row
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            table.columns.forEachIndexed { cIdx, colName ->
                OutlinedTextField(
                    value = colName,
                    onValueChange = { next ->
                        val updated = table.updateHeader(cIdx, next)
                        onUpdateBlock(block.copy(metadataJson = updated.toJson()))
                    },
                    singleLine = true,
                    colors = canvasInputColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(124.dp)
                )
            }
        }

        // Editable Data Rows
        table.rows.forEachIndexed { rIdx, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                table.columns.indices.forEach { cIdx ->
                    OutlinedTextField(
                        value = row.getOrElse(cIdx) { "" },
                        onValueChange = { next ->
                            val updated = table.updateCell(rIdx, cIdx, next)
                            onUpdateBlock(block.copy(metadataJson = updated.toJson()))
                        },
                        singleLine = true,
                        colors = canvasInputColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(124.dp)
                    )
                }
                if (table.rows.size > 1) {
                    IconButton(
                        onClick = {
                            val updated = table.removeRow(rIdx)
                            onUpdateBlock(block.copy(metadataJson = updated.toJson()))
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete row", tint = CrimsonAccent, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // Auto-Calculating Footer Row
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            table.columns.indices.forEach { cIdx ->
                val agg = table.normalizedAggregations().getOrElse(cIdx) { com.example.data.repository.TableAggregation.NONE }
                Surface(
                    onClick = {
                        val updated = table.cycleAggregation(cIdx)
                        onUpdateBlock(block.copy(metadataJson = updated.toJson()))
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (agg == com.example.data.repository.TableAggregation.NONE) palette.recessedTrayBg else palette.accentSoftBg,
                    border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .width(124.dp)
                        .testTag("table_agg_cell_$cIdx")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text(
                            text = agg.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryInk
                        )
                        Text(
                            text = table.computeColumnSummary(cIdx),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmallPillAction("+ Row") {
            onUpdateBlock(block.copy(metadataJson = table.addRow().toJson()))
        }
        SmallPillAction("+ Column") {
            onUpdateBlock(block.copy(metadataJson = table.addColumn().toJson()))
        }
        if (table.columns.size > 1) {
            SmallPillAction("- Last Col") {
                onUpdateBlock(block.copy(metadataJson = table.removeColumn(table.columns.lastIndex).toJson()))
            }
        }
    }
}

@Composable
private fun InvoiceBlockContent(
    block: WorkspaceBlock,
    callbacks: SmartCanvasCallbacks,
    onUpdateBlock: (WorkspaceBlock) -> Unit
) {
    val palette = LiquidTheme.palette
    val inv = remember(block.id, block.metadataJson) { InvoiceBlockData.fromJson(block.metadataJson) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(InvoiceBlockData.MODE_INVOICE to "Invoice", InvoiceBlockData.MODE_QUOTATION to "Quotation").forEach { (mode, label) ->
            val selected = inv.mode == mode
            Surface(
                onClick = { onUpdateBlock(block.copy(metadataJson = inv.copy(mode = mode).toJson())) },
                shape = RoundedCornerShape(9999.dp),
                color = if (selected) palette.primaryButtonBg else palette.recessedTrayBg,
                border = BorderStroke(1.dp, palette.specularRimOutline),
                modifier = Modifier.weight(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) palette.primaryButtonContent else palette.primaryInk
                    )
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = inv.docNumber,
            onValueChange = { v -> onUpdateBlock(block.copy(text = v, metadataJson = inv.copy(docNumber = v).toJson())) },
            label = { Text("Doc #") },
            singleLine = true,
            colors = canvasInputColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(0.9f)
        )
        OutlinedTextField(
            value = inv.clientName,
            onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = inv.copy(clientName = v).toJson())) },
            label = { Text("Client Name") },
            singleLine = true,
            colors = canvasInputColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.3f)
        )
    }

    OutlinedTextField(
        value = inv.clientEmail,
        onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = inv.copy(clientEmail = v).toJson())) },
        label = { Text("Client Gmail") },
        singleLine = true,
        colors = canvasInputColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )

    // Line items
    inv.items.forEachIndexed { i, item ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = item.description,
                onValueChange = { v ->
                    val updated = inv.items.mapIndexed { idx, x -> if (idx == i) x.copy(description = v) else x }
                    onUpdateBlock(block.copy(metadataJson = inv.copy(items = updated).toJson()))
                },
                label = { Text("Item ${i + 1}") },
                singleLine = true,
                colors = canvasInputColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.6f)
            )
            OutlinedTextField(
                value = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString(),
                onValueChange = { v ->
                    val parsed = v.toDoubleOrNull() ?: 0.0
                    val updated = inv.items.mapIndexed { idx, x -> if (idx == i) x.copy(qty = parsed) else x }
                    onUpdateBlock(block.copy(metadataJson = inv.copy(items = updated).toJson()))
                },
                label = { Text("Qty") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = canvasInputColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.6f)
            )
            OutlinedTextField(
                value = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString(),
                onValueChange = { v ->
                    val parsed = v.toDoubleOrNull() ?: 0.0
                    val updated = inv.items.mapIndexed { idx, x -> if (idx == i) x.copy(unitPrice = parsed) else x }
                    onUpdateBlock(block.copy(metadataJson = inv.copy(items = updated).toJson()))
                },
                label = { Text("Rate") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = canvasInputColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.9f)
            )
            if (inv.items.size > 1) {
                IconButton(
                    onClick = {
                        val updated = inv.items.filterIndexed { idx, _ -> idx != i }
                        onUpdateBlock(block.copy(metadataJson = inv.copy(items = updated).toJson()))
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove line", tint = CrimsonAccent, modifier = Modifier.size(15.dp))
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (inv.taxPercent == 0.0) "0" else inv.taxPercent.toString(),
            onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = inv.copy(taxPercent = v.toDoubleOrNull() ?: 0.0).toJson())) },
            label = { Text("Tax %") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = canvasInputColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = if (inv.discountAmount == 0.0) "0" else inv.discountAmount.toString(),
            onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = inv.copy(discountAmount = v.toDoubleOrNull() ?: 0.0).toJson())) },
            label = { Text("Discount ($)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = canvasInputColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallPillAction("+ Line Item") {
            val updated = inv.items + InvoiceLineItem(description = "Service", qty = 1.0, unitPrice = 100.0)
            onUpdateBlock(block.copy(metadataJson = inv.copy(items = updated).toJson()))
        }
        Text(
            text = "Total: ${inv.formatMoney(inv.grandTotal)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CrimsonAccent
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { callbacks.onExportInvoicePdf(inv) },
            shape = RoundedCornerShape(9999.dp),
            border = BorderStroke(1.dp, palette.specularRimOutline),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = palette.primaryInk, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Download PDF", color = palette.primaryInk, maxLines = 1)
        }
        TactilePrimaryPillButton(
            text = "Email Invoice",
            onClick = { callbacks.onEmailInvoice(inv) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InlineReminderBlockContent(
    block: WorkspaceBlock,
    teamMembers: List<TeamMemberEntity>,
    onScheduleReminder: (ReminderBlockData) -> Unit,
    onUpdateBlock: (WorkspaceBlock) -> Unit
) {
    val palette = LiquidTheme.palette
    val rem = remember(block.id, block.metadataJson) { ReminderBlockData.fromJson(block.metadataJson) }

    OutlinedTextField(
        value = rem.title,
        onValueChange = { v -> onUpdateBlock(block.copy(text = v, metadataJson = rem.copy(title = v).toJson())) },
        label = { Text("Reminder Title") },
        singleLine = true,
        colors = canvasInputColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )

    if (teamMembers.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ping:", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
            teamMembers.forEach { m ->
                val selected = rem.taggedMemberUid == m.uid
                Surface(
                    onClick = {
                        val next = if (selected) rem.copy(taggedMemberUid = "", taggedMemberName = "")
                        else rem.copy(taggedMemberUid = m.uid, taggedMemberName = m.displayName)
                        onUpdateBlock(block.copy(metadataJson = next.toJson()))
                    },
                    shape = RoundedCornerShape(9999.dp),
                    color = if (selected) CrimsonAccent else palette.recessedTrayBg
                ) {
                    Text(
                        text = "@${m.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else palette.primaryInk,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            "In 15m" to 900_000L,
            "In 1 hour" to 3_600_000L,
            "In 3 hours" to 10_800_000L,
            "Tomorrow" to 86_400_000L
        ).forEach { (label, offsetMs) ->
            val selected = rem.dateTimeLabel == label
            Surface(
                onClick = {
                    val next = rem.copy(dateTimeLabel = label, triggerAtMillis = System.currentTimeMillis() + offsetMs)
                    onUpdateBlock(block.copy(metadataJson = next.toJson()))
                },
                shape = RoundedCornerShape(9999.dp),
                color = if (selected) palette.primaryButtonBg else palette.recessedTrayBg,
                border = BorderStroke(1.dp, palette.specularRimOutline)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tone:", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
        listOf("Default System Sound", "Crisp Chime", "Soft Pulse").forEach { tone ->
            val selected = rem.ringtoneTitle == tone
            Surface(
                onClick = { onUpdateBlock(block.copy(metadataJson = rem.copy(ringtoneTitle = tone).toJson())) },
                shape = RoundedCornerShape(9999.dp),
                color = if (selected) palette.accentSoftBg else palette.recessedTrayBg,
                border = BorderStroke(1.dp, if (selected) CrimsonAccent else palette.specularRimOutline)
            ) {
                Text(
                    text = tone,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) CrimsonAccent else palette.primaryInk,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }

    TactilePrimaryPillButton(
        text = if (rem.isScheduled) "Scheduled (${rem.dateTimeLabel})" else "Set Reminder & Ping",
        onClick = {
            val next = rem.copy(isScheduled = true)
            onUpdateBlock(block.copy(metadataJson = next.toJson()))
            onScheduleReminder(next)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ScheduleTaskBlockContent(
    block: WorkspaceBlock,
    teamMembers: List<TeamMemberEntity>,
    onSyncScheduleTask: (ScheduleBlockData) -> Unit,
    onUpdateBlock: (WorkspaceBlock) -> Unit
) {
    val palette = LiquidTheme.palette
    val sched = remember(block.id, block.metadataJson) { ScheduleBlockData.fromJson(block.metadataJson) }

    OutlinedTextField(
        value = sched.title,
        onValueChange = { v -> onUpdateBlock(block.copy(text = v, metadataJson = sched.copy(title = v).toJson())) },
        label = { Text("Task Title") },
        singleLine = true,
        colors = canvasInputColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ScheduleBlockData.DAYS.forEach { day ->
            val selected = sched.dayOfWeek == day
            Surface(
                onClick = { onUpdateBlock(block.copy(metadataJson = sched.copy(dayOfWeek = day).toJson())) },
                shape = RoundedCornerShape(9999.dp),
                color = if (selected) palette.primaryButtonBg else palette.recessedTrayBg,
                border = BorderStroke(1.dp, palette.specularRimOutline)
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }

    OutlinedTextField(
        value = sched.timeSlot,
        onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = sched.copy(timeSlot = v).toJson())) },
        label = { Text("Time Window (e.g. 10:00 AM - 1:00 PM)") },
        singleLine = true,
        colors = canvasInputColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )

    if (teamMembers.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Assign:", style = MaterialTheme.typography.labelSmall, color = palette.secondaryInk)
            teamMembers.forEach { m ->
                val selected = sched.memberUid == m.uid
                Surface(
                    onClick = {
                        val next = sched.copy(memberUid = m.uid, memberName = m.displayName)
                        onUpdateBlock(block.copy(metadataJson = next.toJson()))
                    },
                    shape = RoundedCornerShape(9999.dp),
                    color = if (selected) CrimsonAccent else palette.recessedTrayBg
                ) {
                    Text(
                        text = "@${m.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else palette.primaryInk,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            ScheduleBlockData.STATUS_PENDING to "Pending",
            ScheduleBlockData.STATUS_WORKING to "Working",
            ScheduleBlockData.STATUS_DONE to "Done"
        ).forEach { (stKey, stLabel) ->
            val selected = sched.status == stKey
            Surface(
                onClick = {
                    val next = sched.copy(status = stKey)
                    onUpdateBlock(block.copy(metadataJson = next.toJson()))
                },
                shape = RoundedCornerShape(9999.dp),
                color = if (selected) CrimsonAccent else palette.recessedTrayBg,
                border = BorderStroke(1.dp, if (selected) CrimsonAccent else palette.specularRimOutline),
                modifier = Modifier.weight(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                    Text(
                        text = stLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else palette.primaryInk
                    )
                }
            }
        }
    }

    OutlinedButton(
        onClick = {
            val next = sched.copy(synced = true)
            onUpdateBlock(block.copy(metadataJson = next.toJson()))
            onSyncScheduleTask(next)
        },
        shape = RoundedCornerShape(9999.dp),
        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (sched.synced) "Synced to Main Schedule (Tap to Update)" else "Sync to Main Schedule",
            color = CrimsonAccent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FocusTimerBlockContent(
    block: WorkspaceBlock,
    onLogTimerSeconds: (String, Long) -> Unit,
    onUpdateBlock: (WorkspaceBlock) -> Unit
) {
    val palette = LiquidTheme.palette
    val timerData = remember(block.id, block.metadataJson) { TimerBlockData.fromJson(block.metadataJson) }
    var isRunning by remember(block.id) { mutableStateOf(false) }
    var sessionSeconds by remember(block.id) { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning, timerData.mode) {
        while (isRunning) {
            delay(1000L)
            sessionSeconds += 1L
        }
    }

    val totalDisplaySeconds = if (timerData.mode == TimerBlockData.MODE_STOPWATCH) {
        timerData.totalLoggedSeconds + sessionSeconds
    } else {
        val target = timerData.targetMinutes * 60L
        (target - sessionSeconds).coerceAtLeast(0L)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            TimerBlockData.MODE_POMODORO to "Pomodoro 25m",
            TimerBlockData.MODE_COUNTDOWN to "Countdown",
            TimerBlockData.MODE_STOPWATCH to "Stopwatch"
        ).forEach { (mKey, mLabel) ->
            val selected = timerData.mode == mKey
            Surface(
                onClick = {
                    isRunning = false
                    sessionSeconds = 0L
                    val mins = if (mKey == TimerBlockData.MODE_POMODORO) 25 else timerData.targetMinutes
                    onUpdateBlock(block.copy(metadataJson = timerData.copy(mode = mKey, targetMinutes = mins).toJson()))
                },
                shape = RoundedCornerShape(9999.dp),
                color = if (selected) palette.primaryButtonBg else palette.recessedTrayBg,
                border = BorderStroke(1.dp, palette.specularRimOutline),
                modifier = Modifier.weight(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 7.dp)) {
                    Text(
                        text = mLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) palette.primaryButtonContent else palette.primaryInk,
                        maxLines = 1
                    )
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = String.format("%02d:%02d", totalDisplaySeconds / 60L, totalDisplaySeconds % 60L),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CrimsonAccent
            )
            Text(
                text = "Logged: ${(timerData.totalLoggedSeconds + sessionSeconds) / 60L}m",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryInk
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TactilePrimaryPillButton(
                text = if (isRunning) "Pause & Save" else "Start",
                onClick = {
                    if (isRunning) {
                        isRunning = false
                        val nextTotal = timerData.totalLoggedSeconds + sessionSeconds
                        onUpdateBlock(block.copy(metadataJson = timerData.copy(totalLoggedSeconds = nextTotal).toJson()))
                        if (sessionSeconds >= 5L) onLogTimerSeconds(timerData.label, sessionSeconds)
                        sessionSeconds = 0L
                    } else {
                        isRunning = true
                    }
                }
            )
            IconButton(
                onClick = {
                    isRunning = false
                    sessionSeconds = 0L
                }
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Reset timer", tint = palette.secondaryInk)
            }
        }
    }
}

@Composable
private fun VoiceCommentBlockContent(
    block: WorkspaceBlock,
    playbackState: VoicePlaybackState,
    callbacks: SmartCanvasCallbacks,
    onUpdateBlock: (WorkspaceBlock) -> Unit
) {
    val context = LocalContext.current
    val palette = LiquidTheme.palette
    val voiceData = remember(block.id, block.metadataJson) { VoiceBlockData.fromJson(block.metadataJson) }
    var isRecordingHere by remember(block.id) { mutableStateOf(false) }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isRecordingHere = true
            callbacks.onStartVoiceRecord()
        }
    }

    OutlinedTextField(
        value = voiceData.caption,
        onValueChange = { v -> onUpdateBlock(block.copy(text = v, metadataJson = voiceData.copy(caption = v).toJson())) },
        label = { Text("Voice Note Caption") },
        singleLine = true,
        colors = canvasInputColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )

    if (voiceData.audioUri.isNotBlank()) {
        VoiceAudioPlayerCard(
            audioUri = voiceData.audioUri,
            durationMs = voiceData.durationMs,
            playbackState = playbackState,
            onTogglePlayback = callbacks.onToggleVoicePlayback,
            onSeekTo = callbacks.onSeekVoicePlayback
        )
    }

    Surface(
        onClick = {
            if (isRecordingHere) {
                isRecordingHere = false
                val res = callbacks.onStopVoiceRecord()
                if (res != null) {
                    val updated = voiceData.copy(audioUri = res.first, durationMs = res.second)
                    onUpdateBlock(block.copy(metadataJson = updated.toJson()))
                }
            } else {
                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    isRecordingHere = true
                    callbacks.onStartVoiceRecord()
                } else {
                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        },
        shape = RoundedCornerShape(9999.dp),
        color = if (isRecordingHere) CrimsonAccent else palette.accentSoftBg,
        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isRecordingHere) Icons.Outlined.Stop else Icons.Outlined.Mic,
                contentDescription = null,
                tint = if (isRecordingHere) Color.White else CrimsonAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isRecordingHere) "Tap to Stop & Attach Audio" else if (voiceData.audioUri.isBlank()) "Record Inline Voice Comment" else "Re-Record Voice Comment",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRecordingHere) Color.White else CrimsonAccent
            )
        }
    }
}

@Composable
private fun SecretVaultBlockContent(
    block: WorkspaceBlock,
    index: Int,
    context: Context,
    onUpdateBlock: (WorkspaceBlock) -> Unit
) {
    val palette = LiquidTheme.palette
    val secret = remember(block.id, block.metadataJson) { SecretBlockData.fromJson(block.metadataJson) }
    var isEditing by remember(block.id) { mutableStateOf(secret.secretValue.isBlank()) }

    OutlinedTextField(
        value = secret.label,
        onValueChange = { v -> onUpdateBlock(block.copy(text = v, metadataJson = secret.copy(label = v).toJson())) },
        label = { Text("Secret / Key Name") },
        singleLine = true,
        colors = canvasInputColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.recessedTrayBg,
        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("secret_vault_box_$index")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (secret.isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        contentDescription = null,
                        tint = CrimsonAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (secret.isLocked) "Frosted Locked Vault" else "Unlocked Secret",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallPillAction(
                        label = if (secret.isLocked) "Reveal" else "Blur Lock",
                        testTag = "secret_toggle_lock_$index"
                    ) {
                        val nextLocked = !secret.isLocked
                        onUpdateBlock(block.copy(metadataJson = secret.copy(isLocked = nextLocked).toJson()))
                    }
                    SmallPillAction(label = "Copy") {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText(secret.label, secret.secretValue))
                    }
                }
            }

            if (isEditing && !secret.isLocked) {
                OutlinedTextField(
                    value = secret.secretValue,
                    onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = secret.copy(secretValue = v).toJson())) },
                    label = { Text("API Key / Password") },
                    colors = canvasInputColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.cardSurfaceTop)
                        .clickable {
                            if (secret.isLocked) {
                                onUpdateBlock(block.copy(metadataJson = secret.copy(isLocked = false).toJson()))
                            } else {
                                isEditing = !isEditing
                            }
                        }
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (secret.isLocked) "••••••••••••-••••-••••••••" else secret.secretValue.ifBlank { "Tap to edit secret..." },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = palette.primaryInk,
                        modifier = if (secret.isLocked) Modifier.blur(8.dp) else Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientTrackerBlockContent(
    block: WorkspaceBlock,
    onLogPaymentToLedger: (String, String, Long) -> Unit,
    onUpdateBlock: (WorkspaceBlock) -> Unit
) {
    val palette = LiquidTheme.palette
    val client = remember(block.id, block.metadataJson) { ClientTrackerBlockData.fromJson(block.metadataJson) }
    var budgetText by remember(block.id) { mutableStateOf(if (client.totalBudgetCents > 0L) (client.totalBudgetCents / 100.0).toString() else "") }
    var advanceText by remember(block.id) { mutableStateOf(if (client.advancePaidCents > 0L) (client.advancePaidCents / 100.0).toString() else "") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = client.clientName,
            onValueChange = { v -> onUpdateBlock(block.copy(metadataJson = client.copy(clientName = v).toJson())) },
            label = { Text("Client Name") },
            singleLine = true,
            colors = canvasInputColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = client.projectName,
            onValueChange = { v -> onUpdateBlock(block.copy(text = v, metadataJson = client.copy(projectName = v).toJson())) },
            label = { Text("Project") },
            singleLine = true,
            colors = canvasInputColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = budgetText,
            onValueChange = { v ->
                budgetText = v
                val cents = ClientTrackerBlockData.parseMoneyToCents(v)
                onUpdateBlock(block.copy(metadataJson = client.copy(totalBudgetCents = cents).toJson()))
            },
            label = { Text("Total Budget ($)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = canvasInputColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = advanceText,
            onValueChange = { v ->
                advanceText = v
                val cents = ClientTrackerBlockData.parseMoneyToCents(v)
                onUpdateBlock(block.copy(metadataJson = client.copy(advancePaidCents = cents).toJson()))
            },
            label = { Text("Advance Paid ($)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = canvasInputColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(9999.dp),
            color = palette.accentSoftBg,
            border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.45f))
        ) {
            Text(
                text = "Remaining Due: ${client.formatDollars(client.remainingDueCents)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = CrimsonAccent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        SmallPillAction(if (client.syncedToLedger) "Logged to Ledger" else "Sync to Money Ledger") {
            val amount = if (client.advancePaidCents > 0L) client.advancePaidCents else client.totalBudgetCents
            onUpdateBlock(block.copy(metadataJson = client.copy(syncedToLedger = true).toJson()))
            onLogPaymentToLedger(client.clientName, client.projectName, amount)
        }
    }
}

@Composable
private fun SmallPillAction(
    label: String,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val palette = LiquidTheme.palette
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9999.dp),
        color = palette.frostedPillBg,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.primaryInk,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

private fun richBlockDisplayTitle(type: String): String = when (type) {
    WorkspaceBlock.TYPE_SUBPAGE -> "Nested Sub-Page"
    WorkspaceBlock.TYPE_TABLE -> "Smart Table"
    WorkspaceBlock.TYPE_INVOICE -> "Invoice / Quotation"
    WorkspaceBlock.TYPE_EMAIL -> "Direct Gmail Composer"
    WorkspaceBlock.TYPE_REMINDER -> "Page Reminder & Ping"
    WorkspaceBlock.TYPE_SCHEDULE -> "Team Task & Slot"
    WorkspaceBlock.TYPE_TIMER -> "Focus & Pomodoro Timer"
    WorkspaceBlock.TYPE_VOICE -> "Voice Audio Comment"
    WorkspaceBlock.TYPE_SECRET -> "Password & API Secret Vault"
    WorkspaceBlock.TYPE_CLIENT -> "Client Budget & Payment Tracker"
    WorkspaceBlock.TYPE_DRIVE_EMBED -> "Google Drive Resource"
    WorkspaceBlock.TYPE_EMBED -> "Website & Link Embed"
    else -> "Smart Block"
}


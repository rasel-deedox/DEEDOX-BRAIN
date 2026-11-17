package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiPagePolishMode
import com.example.data.repository.CanvasIconKeys
import com.example.data.repository.SlashBlockOption
import com.example.data.repository.SmartCanvasStarterTemplates
import com.example.data.repository.SmartCanvasTemplate
import com.example.data.repository.WorkspaceBlock
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import kotlinx.coroutines.launch

@Composable
fun canvasInputColors() = LiquidTheme.palette.let { palette ->
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.cardSurfaceTop.copy(alpha = 0.72f),
        unfocusedContainerColor = palette.cardSurfaceTop.copy(alpha = 0.56f),
        focusedBorderColor = CrimsonAccent,
        unfocusedBorderColor = palette.specularRimOutline,
        focusedTextColor = palette.primaryInk,
        unfocusedTextColor = palette.primaryInk,
        focusedLabelColor = palette.secondaryInk,
        unfocusedLabelColor = palette.secondaryInk,
        focusedPlaceholderColor = palette.mutedIcon,
        unfocusedPlaceholderColor = palette.mutedIcon
    )
}

@Composable
fun StarterTemplatesStrip(
    onApplyTemplate: (SmartCanvasTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("starter_templates_strip"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Starter Templates",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.secondaryInk
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmartCanvasStarterTemplates.TEMPLATES.forEach { tpl ->
                Surface(
                    onClick = { onApplyTemplate(tpl) },
                    shape = RoundedCornerShape(9999.dp),
                    color = palette.frostedPillBg,
                    border = BorderStroke(1.dp, palette.specularRimOutline),
                    modifier = Modifier.testTag("starter_template_${tpl.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MonochromeVectorIcon(
                            imageVector = CanvasPageIcons.forPageKey(tpl.iconKey),
                            contentDescription = null,
                            tint = CrimsonAccent,
                            size = 15.dp
                        )
                        Text(
                            text = tpl.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryInk,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmartCanvasCoverBanner(
    coverVariant: String,
    pageIconKey: String,
    onSelectVariant: (String) -> Unit,
    onSelectIconKey: (String) -> Unit,
    onHideCover: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val gradientColors = when (coverVariant) {
        "crimson_mist" -> listOf(
            CrimsonAccent.copy(alpha = if (palette.isDark) 0.30f else 0.16f),
            palette.cardSurfaceBottom
        )
        "pearl_grain" -> listOf(
            palette.frostedPillBg,
            palette.recessedTrayBg
        )
        else -> listOf(
            palette.accentSoftBg,
            palette.cardSurfaceTop
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, palette.specularRimOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CanvasPageIconBadge(iconKey = pageIconKey, size = 34.dp, iconSize = 18.dp, active = true)
                    Text(
                        text = "Page Cover & Icon",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(
                        "liquid_obsidian" to "Obsidian",
                        "pearl_grain" to "Pearl",
                        "crimson_mist" to "Crimson"
                    ).forEach { (key, label) ->
                        val active = coverVariant == key
                        Surface(
                            onClick = { onSelectVariant(key) },
                            shape = RoundedCornerShape(9999.dp),
                            color = if (active) palette.primaryButtonBg else palette.cardSurfaceTop,
                            border = BorderStroke(1.dp, palette.specularRimOutline)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) palette.primaryButtonContent else palette.primaryInk,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(onClick = onHideCover, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Hide cover", tint = palette.secondaryInk, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CanvasIconKeys.ALL_KEYS.forEach { key ->
                    val selected = CanvasIconKeys.normalize(pageIconKey) == key
                    CanvasPageIconBadge(
                        iconKey = key,
                        size = 34.dp,
                        iconSize = 17.dp,
                        active = selected,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectIconKey(key) }
                            .testTag("page_icon_key_$key")
                    )
                }
            }
        }
    }
}

@Composable
fun SmartCanvasSlashDrawer(
    onSelectCommand: (SlashBlockOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) WorkspaceBlock.SLASH_COMMANDS
        else WorkspaceBlock.SLASH_COMMANDS.filter {
            it.command.lowercase().contains(q) ||
                it.label.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.cardSurfaceTop,
        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_slash_drawer")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "/ Insert Smart Block",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonAccent
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close block menu", tint = palette.secondaryInk, modifier = Modifier.size(16.dp))
                }
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Filter blocks (/table, /invoice, /remind, /secret...)") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.secondaryInk, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                colors = canvasInputColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filtered.forEach { cmd ->
                    Surface(
                        onClick = { onSelectCommand(cmd) },
                        shape = RoundedCornerShape(14.dp),
                        color = palette.recessedTrayBg.copy(alpha = 0.65f),
                        border = BorderStroke(0.5.dp, palette.specularRimOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slash_cmd_${cmd.type.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = palette.cardSurfaceTop,
                                border = BorderStroke(1.dp, palette.specularRimOutline)
                            ) {
                                Box(
                                    modifier = Modifier.size(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MonochromeVectorIcon(
                                        imageVector = CanvasPageIcons.forBlockType(cmd.type),
                                        contentDescription = null,
                                        tint = CrimsonAccent,
                                        size = 16.dp
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cmd.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.primaryInk
                                    )
                                    Text(
                                        text = cmd.command,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = cmd.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryInk,
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
}

@Composable
fun SmartCanvasHeaderTimerPill(
    isRunning: Boolean,
    elapsedSeconds: Long,
    onToggleRunning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val mins = elapsedSeconds / 60L
    val secs = elapsedSeconds % 60L
    val timeLabel = String.format("%02d:%02d", mins, secs)

    Surface(
        onClick = onToggleRunning,
        shape = RoundedCornerShape(9999.dp),
        color = if (isRunning) CrimsonAccent else palette.frostedPillBg,
        border = BorderStroke(1.dp, if (isRunning) CrimsonAccent else palette.specularRimOutline),
        modifier = modifier.testTag("canvas_header_timer_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isRunning) "Pause page timer" else "Start page timer",
                tint = if (isRunning) Color.White else palette.primaryInk,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) Color.White else palette.primaryInk
            )
        }
    }
}

@Composable
fun AiPolishModalDialog(
    onRunPolish: suspend (AiPagePolishMode) -> Result<List<WorkspaceBlock>>,
    onInsertBlocks: (List<WorkspaceBlock>, replaceExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LiquidTheme.palette
    val scope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf(AiPagePolishMode.CHECKLIST) }
    var isBusy by remember { mutableStateOf(false) }
    var previewBlocks by remember { mutableStateOf<List<WorkspaceBlock>?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.secondaryPillBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(20.dp))
                Text("AI Smart Page Polish", color = palette.primaryInk, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Transform your current notes into structured Notion blocks cleanly styled in your workspace theme.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryInk
                )
                AiPagePolishMode.entries.forEach { mode ->
                    val selected = selectedMode == mode
                    Surface(
                        onClick = {
                            selectedMode = mode
                            previewBlocks = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) palette.accentSoftBg else palette.cardSurfaceTop,
                        border = BorderStroke(1.dp, if (selected) CrimsonAccent else palette.specularRimOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_polish_mode_${mode.name.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) CrimsonAccent else palette.primaryInk
                            )
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryInk
                            )
                        }
                    }
                }

                if (isBusy) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = CrimsonAccent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Structuring page...", style = MaterialTheme.typography.labelMedium, color = palette.primaryInk)
                    }
                }

                errorMsg?.let { err ->
                    Text(err, style = MaterialTheme.typography.labelSmall, color = CrimsonAccent)
                }

                previewBlocks?.let { generated ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Preview (${generated.size} blocks ready)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonAccent
                            )
                            Text(
                                text = WorkspaceBlock.plainTextSummary(WorkspaceBlock.encodeList(generated)).take(280),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.primaryInk
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (previewBlocks == null) {
                TactilePrimaryPillButton(
                    text = "Generate",
                    enabled = !isBusy,
                    onClick = {
                        isBusy = true
                        errorMsg = null
                        scope.launch {
                            onRunPolish(selectedMode).fold(
                                onSuccess = { generated ->
                                    previewBlocks = generated
                                    isBusy = false
                                },
                                onFailure = { err ->
                                    errorMsg = err.localizedMessage ?: "Failed to structure page"
                                    isBusy = false
                                }
                            )
                        }
                    },
                    modifier = Modifier.testTag("ai_polish_generate_button")
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onInsertBlocks(previewBlocks!!, false)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("ai_polish_append_button")
                    ) {
                        Text("Append", color = CrimsonAccent, fontWeight = FontWeight.Bold)
                    }
                    TactilePrimaryPillButton(
                        text = "Replace Page",
                        onClick = {
                            onInsertBlocks(previewBlocks!!, true)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("ai_polish_replace_button")
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.secondaryInk)
            }
        }
    )
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.local.OrganizationOperationEntity
import com.example.data.local.OrganizationPlanEntity
import com.example.ui.components.CrimsonActionPill
import com.example.ui.components.FloatingGlassIconButton
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.TactileGlassCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import com.example.ui.viewmodel.ChatMessage
import org.json.JSONArray

@Composable
fun AiAssistantScreen(
    messages: List<ChatMessage>,
    isOnline: Boolean,
    aiEnabled: Boolean,
    isAiBusy: Boolean,
    activeEngineLabel: String = "Online AI • Grounded in your saved items",
    missingKeyWarning: String? = null,
    onBack: () -> Unit,
    onSendQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val palette = LiquidTheme.palette
    var promptInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            // Header
            Row(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingGlassIconButton(
                    iconType = MinimalIconType.BACK,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.testTag("assistant_back_button")
                )
                MinimalIcon(
                    type = MinimalIconType.SPARKLE,
                    active = true,
                    tint = palette.accentSoftInk,
                    size = 24.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Library Assistant", style = MaterialTheme.typography.titleLarge, color = palette.primaryInk)
                    Text(
                        text = if (isOnline && aiEnabled) activeEngineLabel else "Unavailable offline or disabled in Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryInk
                    )
                }
            }

            if (!isOnline) {
                Box(modifier = Modifier.widthIn(max = 680.dp).padding(horizontal = 16.dp)) {
                    OfflineStatusBanner(message = "Offline: AI Assistant requires internet. Use Search tab for local search.")
                }
            } else if (!aiEnabled) {
                Box(modifier = Modifier.widthIn(max = 680.dp).padding(horizontal = 16.dp)) {
                    OfflineStatusBanner(message = "Online AI is disabled in Privacy & AI Settings.")
                }
            } else if (!missingKeyWarning.isNullOrBlank()) {
                Box(modifier = Modifier.widthIn(max = 680.dp).padding(horizontal = 16.dp)) {
                    OfflineStatusBanner(message = missingKeyWarning)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item(key = "ai_empty_starters", contentType = "starters_card") {
                        TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Ask anything about your saved knowledge", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                Text(
                                    "Try a starter prompt below:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                                val starters = remember {
                                    listOf(
                                        "What AI or GitHub links have I saved?",
                                        "Summarize my recent Inbox items",
                                        "Which local file references did I attach?"
                                    )
                                }
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    starters.forEach { q ->
                                        OutlinedButton(
                                            onClick = { onSendQuestion(q) },
                                            enabled = isOnline && aiEnabled && !isAiBusy,
                                            shape = RoundedCornerShape(9999.dp),
                                            border = BorderStroke(1.dp, palette.inputBorder)
                                        ) {
                                            Text(q, color = palette.primaryInk)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = messages,
                        key = { it.id },
                        contentType = { if (it.isUser) "chat_user" else "chat_ai" }
                    ) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = when {
                                    msg.isUser -> palette.primaryButtonBg
                                    msg.isError -> palette.accentSoftBg
                                    else -> palette.cardSurfaceTop
                                },
                                border = BorderStroke(1.dp, palette.specularRimOutline),
                                shadowElevation = 6.dp,
                                modifier = Modifier.widthIn(max = 520.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (msg.isUser) palette.primaryButtonContent else palette.primaryInk,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                if (isAiBusy) {
                    item(key = "ai_busy_indicator", contentType = "busy_indicator") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.accentSoftInk)
                            Text("Consulting your library...", style = MaterialTheme.typography.bodySmall, color = palette.secondaryInk)
                        }
                    }
                }
            }

            // Bottom Liquid Glass Prompt Bar
            RecessedFramingBezel(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                cornerRadius = 9999.dp,
                innerPadding = PaddingValues(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Ask AI about your saved items...") },
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
                            .testTag("assistant_prompt_input")
                    )
                    val canSend = promptInput.isNotBlank() && isOnline && aiEnabled && !isAiBusy
                    Surface(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                onSendQuestion(promptInput.trim())
                                promptInput = ""
                            }
                        },
                        enabled = canSend,
                        shape = CircleShape,
                        color = if (canSend) palette.primaryButtonBg else palette.recessedTrayBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("assistant_send_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            MinimalIcon(
                                type = MinimalIconType.SEND,
                                active = true,
                                tint = if (canSend) palette.primaryButtonContent else palette.secondaryInk,
                                cutoutColor = if (canSend) palette.primaryButtonBg else palette.recessedTrayBg,
                                size = 20.dp,
                                contentDescription = "Send question"
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun AiOrganizationPreviewScreen(
    plan: OrganizationPlanEntity?,
    operations: List<OrganizationOperationEntity>,
    aiError: String?,
    onBack: () -> Unit,
    onToggleOperation: (String, Boolean) -> Unit,
    onApprovePlan: () -> Unit,
    onRejectPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val palette = LiquidTheme.palette

    val warnings = remember(plan?.warningsJson) {
        try {
            val arr = JSONArray(plan?.warningsJson ?: "[]")
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
    val selectedCount = remember(operations) { operations.count { it.isSelected } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingGlassIconButton(
                        iconType = MinimalIconType.BACK,
                        contentDescription = "Back",
                        onClick = onBack,
                        modifier = Modifier.testTag("org_preview_back_button")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Organization Preview", style = MaterialTheme.typography.headlineMedium, color = palette.primaryInk)
                        Text(
                            text = "Nothing is applied until you tap Approve.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (plan == null) {
                        item(key = "org_plan_unavailable", contentType = "empty_card") {
                            TactileGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(22.dp)) {
                                    Text("Plan unavailable", style = MaterialTheme.typography.titleMedium, color = palette.primaryInk)
                                    Text(
                                        "Generate a fresh AI Organization Plan from the Home or Inbox screen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.secondaryInk
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "org_plan_summary", contentType = "plan_summary") {
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
                                        Text(plan.strategy, style = MaterialTheme.typography.titleLarge, color = palette.primaryInk)
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = palette.accentSoftBg
                                        ) {
                                            Text(
                                                text = "${plan.confidenceBand} Confidence",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.accentSoftInk,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                    if (plan.reason.isNotBlank()) {
                                        Text(
                                            text = plan.reason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.secondaryInk
                                        )
                                    }
                                }
                            }
                        }

                        if (warnings.isNotEmpty()) {
                            item(key = "org_plan_warnings", contentType = "warning_card") {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = palette.accentSoftBg,
                                    border = BorderStroke(1.dp, palette.specularRimOutline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = palette.accentSoftInk, modifier = Modifier.size(18.dp))
                                            Text("Review Warnings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.primaryInk)
                                        }
                                        warnings.forEach { w ->
                                            Text("• $w", style = MaterialTheme.typography.bodySmall, color = palette.primaryInk)
                                        }
                                    }
                                }
                            }
                        }

                        if (!aiError.isNullOrBlank()) {
                            item(key = "org_plan_error", contentType = "banner") {
                                Surface(shape = RoundedCornerShape(18.dp), color = palette.accentSoftBg) {
                                    Text(
                                        text = aiError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrimsonAccent,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        items(
                            items = operations,
                            key = { it.id },
                            contentType = { "org_operation_card" }
                        ) { op ->
                            val isLow = op.confidenceBand == "LOW"
                            TactileGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("org_operation_${op.id}"),
                                cornerRadius = 26.dp,
                                onClick = { onToggleOperation(op.id, !op.isSelected) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = op.isSelected,
                                        onCheckedChange = { checked -> onToggleOperation(op.id, checked) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = CrimsonAccent,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = op.itemTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primaryInk,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(9999.dp),
                                                color = if (isLow) CrimsonAccent else palette.accentSoftBg
                                            ) {
                                                Text(
                                                    text = if (isLow) "LOW • Review" else op.confidenceBand,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isLow) Color.White else palette.accentSoftInk,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(9999.dp),
                                                color = palette.recessedTrayBg
                                            ) {
                                                Text(
                                                    text = op.fromFolderName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.secondaryInk,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                contentDescription = "moves to",
                                                tint = palette.accentSoftInk,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(9999.dp),
                                                color = palette.accentSoftBg
                                            ) {
                                                Text(
                                                    text = op.toFolderName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.accentSoftInk,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (!op.tagName.isNullOrBlank()) {
                                            Text(
                                                text = "Suggested tags: ${op.tagName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.accentSoftInk
                                            )
                                        }
                                        if (op.reason.isNotBlank()) {
                                            Text(
                                                text = op.reason,
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

                // Bottom Liquid Glass Approval Bar
                if (plan != null) {
                    Surface(
                        color = palette.frostedPillBg,
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        shadowElevation = 14.dp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onRejectPlan,
                                shape = RoundedCornerShape(9999.dp),
                                border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("org_reject_plan_button")
                            ) {
                                Text("Reject", color = CrimsonAccent)
                            }

                            Spacer(Modifier.size(12.dp))

                            CrimsonActionPill(
                                text = "Approve ($selectedCount)",
                                enabled = selectedCount > 0,
                                onClick = onApprovePlan,
                                modifier = Modifier.testTag("org_approve_plan_button")
                            )
                        }
                }
            }
        }
    }
}
